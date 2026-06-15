package com.goprex.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.goprex.data.model.Login
import com.goprex.data.model.Perfil
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private fun createFirebaseEmail(login: String): String {
        return "${login}@goprex.com.br"
    }

    suspend fun login(login: String, password: String): Result<Login> {
        return try {
            val email = createFirebaseEmail(login)
            Log.d("AuthRepository", "Login: $login -> $email")

            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user

            if (firebaseUser != null) {
                Log.d("AuthRepository", "Auth Firebase: SUCESSO")

                val loginData = getLoginData(login)
                    ?: run {
                        auth.signOut()
                        return Result.failure(Exception("Dados do usuário não encontrados."))
                    }

                if (!loginData.status_ativo) {
                    auth.signOut()
                    return Result.failure(Exception("Usuário desativado. Contate o administrador."))
                }

                Log.d("AuthRepository", "Login autorizado: ${loginData.nome}")
                Result.success(loginData)
            } else {
                Result.failure(Exception("Falha na autenticação."))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Erro no login: ${e.message}")
            Result.failure(e)
        }
    }

    private suspend fun getLoginData(login: String): Login? {
        return try {
            val document = firestore.collection("logins").document(login).get().await()
            if (!document.exists()) return null

            val data = document.data ?: return null

            // Todos os dados vêm do Firestore, sem defaults fixos
            val nome = data["nome"] as? String ?: ""
            val cidade = data["cidade"] as? String ?: ""
            val estado = data["estado"] as? String ?: ""
            val dataCriacao = data["data_criacao"] as? String ?: ""
            val loja = data["loja"] as? String ?: ""
            val statusAtivo = data["status_ativo"] as? Boolean ?: false
            val telefoneWhatsapp = data["telefone_whatsapp"] as? Boolean ?: false
            val fotoUrl = data["fotoUrl"] as? String ?: ""

            // Telefone pode vir em formatos diferentes
            val telefone = when (val tel = data["telefone"]) {
                is Long -> tel
                is Double -> tel.toLong()
                is String -> tel.toLongOrNull() ?: 0L
                else -> 0L
            }

            // Região de abrangência
            val regiaoAbrangencia: Map<String, Boolean> = try {
                @Suppress("UNCHECKED_CAST")
                (data["regiao_abrangencia"] as? Map<String, Boolean>) ?: emptyMap()
            } catch (e: Exception) {
                emptyMap()
            }

            // Perfil
            val perfilString = data["perfil"] as? String ?: "Cliente"
            val perfil = when (perfilString.lowercase()) {
                "admin" -> Perfil.ADMIN
                "vendedor" -> Perfil.VENDEDOR
                "entregador" -> Perfil.ENTREGADOR
                else -> Perfil.CLIENTE
            }

            Login(
                documentoId = document.id,
                nome = nome,
                cidade = cidade,
                estado = estado,
                data_criacao = dataCriacao,
                perfil = perfil,
                loja = loja,
                regiao_abrangencia = regiaoAbrangencia,
                status_ativo = statusAtivo,
                telefone = telefone,
                telefone_whatsapp = telefoneWhatsapp,
                email = createFirebaseEmail(login),
                fotoUrl = fotoUrl
            )
        } catch (e: Exception) {
            Log.e("AuthRepository", "Erro ao buscar login: ${e.message}")
            null
        }
    }

    suspend fun atualizarFotoUrl(loginId: String, fotoUrl: String): Result<Unit> {
        return try {
            firestore.collection("logins").document(loginId).update("fotoUrl", fotoUrl).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        // Força logout limpo
        auth.signOut()

        // Limpa qualquer estado persistente do Firebase
        try {
            val firebaseUser = auth.currentUser
            if (firebaseUser != null) {
                // Se ainda tiver usuário, força novamente
                auth.signOut()
            }
        } catch (e: Exception) {
            // Ignora
        }
    }
}