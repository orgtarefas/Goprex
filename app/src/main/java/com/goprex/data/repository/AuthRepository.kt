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

    /**
     * Cria o email fake para autenticação no Firebase Auth
     * Exemplo: login "dev.admin" -> "dev.admin@goprex.com.br"
     */
    private fun createFirebaseEmail(login: String): String {
        return "${login}@goprex.com.br"
    }

    /**
     * Realiza o login do usuário
     * 1. Autentica no Firebase Auth com email fake
     * 2. Busca dados na coleção "logins" no Firestore
     * 3. Verifica se o usuário está ativo (status_ativo = true)
     */
    suspend fun login(login: String, password: String): Result<Login> {
        return try {
            val email = createFirebaseEmail(login)
            Log.d("AuthRepository", "=========================================")
            Log.d("AuthRepository", "Iniciando processo de login")
            Log.d("AuthRepository", "Login digitado: $login")
            Log.d("AuthRepository", "Email gerado: $email")
            Log.d("AuthRepository", "=========================================")

            // 1. Autenticar no Firebase Auth
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user

            if (firebaseUser != null) {
                Log.d("AuthRepository", "✅ Autenticação Firebase: SUCESSO")
                Log.d("AuthRepository", "UID: ${firebaseUser.uid}")

                // 2. Buscar dados na coleção "logins" usando o login como ID do documento
                val loginData = getLoginData(login)

                if (loginData != null) {
                    Log.d("AuthRepository", "✅ Dados encontrados na coleção 'logins'")
                    Log.d("AuthRepository", "Nome: ${loginData.nome}")
                    Log.d("AuthRepository", "Perfil: ${loginData.perfil}")
                    Log.d("AuthRepository", "Status: ${if (loginData.status_ativo) "ATIVO" else "INATIVO"}")

                    // 3. VERIFICAR SE O USUÁRIO ESTÁ ATIVO
                    if (!loginData.status_ativo) {
                        Log.w("AuthRepository", "❌ Usuário INATIVO: ${loginData.nome}")
                        Log.w("AuthRepository", "Fazendo logout do Firebase Auth...")
                        auth.signOut()
                        Log.d("AuthRepository", "Logout realizado com sucesso")
                        return Result.failure(
                            Exception("Usuário desativado. Contate o administrador para reativar seu acesso.")
                        )
                    }

                    Log.d("AuthRepository", "✅ Login autorizado com sucesso!")
                    Log.d("AuthRepository", "=========================================")
                    Result.success(loginData)

                } else {
                    Log.e("AuthRepository", "❌ Documento não encontrado na coleção 'logins'")
                    Log.e("AuthRepository", "Login buscado: $login")
                    Log.e("AuthRepository", "Fazendo logout do Firebase Auth...")
                    auth.signOut()
                    Log.d("AuthRepository", "Logout realizado com sucesso")
                    Result.failure(
                        Exception("Dados do usuário não encontrados. Contate o administrador.")
                    )
                }
            } else {
                Log.e("AuthRepository", "❌ Falha na autenticação: usuário null")
                Result.failure(Exception("Falha na autenticação. Tente novamente."))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "❌ Erro durante o login: ${e.message}")
            Log.e("AuthRepository", "Stack trace: ", e)
            Log.d("AuthRepository", "=========================================")
            Result.failure(e)
        }
    }

    /**
     * Busca os dados do usuário na coleção "logins" do Firestore
     * O ID do documento é o próprio login do usuário
     */
    private suspend fun getLoginData(login: String): Login? {
        return try {
            Log.d("AuthRepository", "Buscando documento na coleção 'logins': $login")

            val document = firestore.collection("logins")
                .document(login)
                .get()
                .await()

            if (document.exists()) {
                Log.d("AuthRepository", "Documento encontrado!")
                val data = document.data

                if (data != null) {
                    Log.d("AuthRepository", "Dados brutos: $data")

                    // Converter o mapa de região de abrangência
                    val regiaoAbrangencia = try {
                        @Suppress("UNCHECKED_CAST")
                        (data["regiao_abrangencia"] as? Map<String, Boolean>) ?: mapOf("Salvador" to true)
                    } catch (e: Exception) {
                        Log.w("AuthRepository", "Erro ao converter regiao_abrangencia: ${e.message}")
                        mapOf("Salvador" to true)
                    }

                    // Determinar o perfil
                    val perfilString = data["perfil"] as? String ?: "Cliente"
                    val perfil = when (perfilString.lowercase()) {
                        "admin" -> Perfil.ADMIN
                        "vendedor" -> Perfil.VENDEDOR
                        "entregador" -> Perfil.ENTREGADOR
                        "cliente" -> Perfil.CLIENTE
                        else -> {
                            Log.w("AuthRepository", "Perfil desconhecido: $perfilString. Usando CLIENTE como padrão")
                            Perfil.CLIENTE
                        }
                    }

                    val loginData = Login(
                        documentoId = document.id,
                        nome = data["nome"] as? String ?: "",
                        cidade = data["cidade"] as? String ?: "Salvador",
                        estado = data["estado"] as? String ?: "Bahia",
                        data_criacao = data["data_criacao"] as? String ?: "",
                        perfil = perfil,
                        regiao_abrangencia = regiaoAbrangencia,
                        status_ativo = data["status_ativo"] as? Boolean ?: false,
                        telefone = when (val tel = data["telefone"]) {
                            is Long -> tel
                            is Double -> tel.toLong()
                            is String -> tel.toLongOrNull() ?: 0L
                            else -> 0L
                        },
                        telefone_whatsapp = data["telefone_whatsapp"] as? Boolean ?: false,
                        email = createFirebaseEmail(login),
                        fotoUrl = data["fotoUrl"] as? String ?: ""
                    )

                    Log.d("AuthRepository", "Objeto Login criado com sucesso")
                    return loginData
                } else {
                    Log.w("AuthRepository", "Documento existe mas data é null")
                    return null
                }
            } else {
                Log.w("AuthRepository", "Documento NÃO encontrado: $login")
                return null
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Erro ao buscar dados do login: ${e.message}")
            Log.e("AuthRepository", "Stack trace: ", e)
            return null
        }
    }

    /**
     * Atualiza a URL da foto de perfil no Firestore
     * @param loginId ID do login (nome do documento no Firestore)
     * @param fotoUrl Nova URL da foto (do ImgBB)
     */
    suspend fun atualizarFotoUrl(loginId: String, fotoUrl: String): Result<Unit> {
        return try {
            Log.d("AuthRepository", "Atualizando fotoUrl para: $loginId")
            Log.d("AuthRepository", "Nova URL: $fotoUrl")

            firestore.collection("logins")
                .document(loginId)
                .update("fotoUrl", fotoUrl)
                .await()

            Log.d("AuthRepository", "✅ FotoUrl atualizada com sucesso!")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "❌ Erro ao atualizar fotoUrl: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Atualiza dados do usuário no Firestore
     * @param loginId ID do login
     * @param dados Mapa com os campos a serem atualizados
     */
    suspend fun atualizarDadosUsuario(loginId: String, dados: Map<String, Any>): Result<Unit> {
        return try {
            Log.d("AuthRepository", "Atualizando dados do usuário: $loginId")
            Log.d("AuthRepository", "Dados: $dados")

            firestore.collection("logins")
                .document(loginId)
                .update(dados)
                .await()

            Log.d("AuthRepository", "✅ Dados atualizados com sucesso!")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "❌ Erro ao atualizar dados: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Busca dados atualizados do usuário
     * @param loginId ID do login
     */
    suspend fun buscarDadosAtualizados(loginId: String): Result<Login> {
        return try {
            val loginData = getLoginData(loginId)
            if (loginData != null) {
                Result.success(loginData)
            } else {
                Result.failure(Exception("Dados não encontrados"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retorna o usuário atualmente logado no Firebase Auth
     */
    fun getCurrentUser() = auth.currentUser

    /**
     * Verifica se existe um usuário logado
     */
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    /**
     * Faz logout do usuário
     */
    fun logout() {
        Log.d("AuthRepository", "Realizando logout...")
        auth.signOut()
        Log.d("AuthRepository", "✅ Logout realizado com sucesso!")
    }
}