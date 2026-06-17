package com.goprex.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.goprex.data.model.Login
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
                if (loginData == null) {
                    auth.signOut()
                    return Result.failure(Exception("Dados do usuário não encontrados."))
                }

                // Verifica status_ativo de forma genérica
                val statusAtivo = loginData.getBoolean("status_ativo")
                if (!statusAtivo) {
                    auth.signOut()
                    return Result.failure(Exception("Usuário desativado. Contate o administrador."))
                }

                Log.d("AuthRepository", "Login autorizado: ${loginData.getString("nome")}")
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

            Login(
                documentoId = document.id,
                dados = data
            )
        } catch (e: Exception) {
            Log.e("AuthRepository", "Erro ao buscar login: ${e.message}")
            null
        }
    }

    suspend fun getLoginDataByUid(uid: String): Result<Login> {
        return try {
            val document = firestore.collection("logins")
                .document(uid)
                .get()
                .await()

            if (document.exists()) {
                val dados = document.data ?: emptyMap()
                Result.success(Login(documentoId = uid, dados = dados))
            } else {
                Result.failure(Exception("Documento não encontrado"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun atualizarCampo(uid: String, campo: String, valor: Any): Result<Unit> {
        return try {
            firestore.collection("logins").document(uid).update(campo, valor).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        auth.signOut()
        try {
            val firebaseUser = auth.currentUser
            if (firebaseUser != null) {
                auth.signOut()
            }
        } catch (e: Exception) {
            // Ignora
        }
    }
}