package com.goprex.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.goprex.data.model.EnderecoEntrega
import kotlinx.coroutines.tasks.await
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.UUID

data class CepResponse(
    val cep: String = "",
    val logradouro: String = "",
    val complemento: String = "",
    val bairro: String = "",
    val localidade: String = "",
    val uf: String = "",
    val erro: Boolean = false
)

private interface CepApiService {
    @GET("ws/{cep}/json/")
    suspend fun buscarCep(@Path("cep") cep: String): CepResponse
}

class EnderecoRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val cepApi = Retrofit.Builder()
        .baseUrl("https://viacep.com.br/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(CepApiService::class.java)

    private fun collection(clienteLogin: String) =
        firestore.collection("logins").document(clienteLogin).collection("enderecos")

    suspend fun buscarEnderecoPorCep(cep: String): Result<EnderecoEntrega> {
        return try {
            val cepLimpo = cep.filter { it.isDigit() }
            if (cepLimpo.length != 8) {
                return Result.failure(Exception("Informe um CEP com 8 digitos"))
            }

            val response = cepApi.buscarCep(cepLimpo)
            if (response.erro) {
                Result.failure(Exception("CEP nao encontrado"))
            } else {
                Result.success(
                    EnderecoEntrega(
                        cep = response.cep.ifBlank { cepLimpo },
                        logradouro = response.logradouro,
                        complemento = response.complemento,
                        bairro = response.bairro,
                        cidade = response.localidade,
                        estado = response.uf
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listarEnderecos(clienteLogin: String): Result<List<EnderecoEntrega>> {
        return try {
            val snapshot = collection(clienteLogin).get().await()
            val enderecos = snapshot.documents
                .mapNotNull { it.toObject(EnderecoEntrega::class.java)?.copy(id = it.id) }
                .sortedWith(compareByDescending<EnderecoEntrega> { it.principal }.thenByDescending { it.atualizadoEm })
            Result.success(enderecos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun salvarEndereco(clienteLogin: String, endereco: EnderecoEntrega): Result<EnderecoEntrega> {
        return try {
            val id = endereco.id.ifBlank { UUID.randomUUID().toString() }
            val agora = System.currentTimeMillis()
            val enderecoSalvar = endereco.copy(
                id = id,
                atualizadoEm = agora,
                criadoEm = endereco.criadoEm.takeIf { it > 0L } ?: agora
            )

            if (enderecoSalvar.principal) {
                limparPrincipal(clienteLogin)
            }

            collection(clienteLogin).document(id).set(enderecoSalvar).await()
            Result.success(enderecoSalvar)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun definirPrincipal(clienteLogin: String, enderecoId: String): Result<Unit> {
        return try {
            limparPrincipal(clienteLogin)
            collection(clienteLogin).document(enderecoId)
                .update(
                    mapOf(
                        "principal" to true,
                        "atualizadoEm" to System.currentTimeMillis()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removerEndereco(clienteLogin: String, enderecoId: String): Result<Unit> {
        return try {
            collection(clienteLogin).document(enderecoId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun limparPrincipal(clienteLogin: String) {
        val snapshot = collection(clienteLogin).get().await()
        snapshot.documents
            .filter { it.getBoolean("principal") == true }
            .forEach { doc ->
                doc.reference.update(
                    mapOf(
                        "principal" to false,
                        "atualizadoEm" to System.currentTimeMillis()
                    )
                ).await()
            }
    }
}
