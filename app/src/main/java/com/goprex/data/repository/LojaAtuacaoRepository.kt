package com.goprex.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.goprex.data.model.LojaAtuacao
import com.goprex.data.model.LojaDisponivelEntrega
import kotlinx.coroutines.tasks.await

class LojaAtuacaoRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val lojaRepository = LojaRepository()

    private fun collection(entregadorLogin: String) =
        firestore.collection("logins").document(entregadorLogin).collection("lojasAtuacao")

    suspend fun listarLojasDisponiveis(): Result<List<LojaDisponivelEntrega>> {
        return lojaRepository.listarLojas(incluirInativas = false).map { lojas ->
            lojas.map { loja ->
                LojaDisponivelEntrega(
                    lojaId = loja.id,
                    nomeLoja = loja.nome,
                    enderecoResumo = loja.resumoEndereco().ifBlank { "Endereco nao informado" },
                    cidade = loja.cidade,
                    estado = loja.estado,
                    latitude = loja.latitude,
                    longitude = loja.longitude,
                    ativa = loja.ativa
                )
            }
        }
    }

    suspend fun listarAtuacoes(entregadorLogin: String): Result<List<LojaAtuacao>> {
        return try {
            val snapshot = collection(entregadorLogin).get().await()
            val atuacoes = snapshot.documents
                .mapNotNull { doc -> doc.toObject(LojaAtuacao::class.java)?.copy(lojaId = doc.id) }
                .sortedBy { it.nomeLoja.lowercase() }
            Result.success(atuacoes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun salvarAtuacao(entregadorLogin: String, atuacao: LojaAtuacao): Result<Unit> {
        return try {
            val id = atuacao.lojaId.ifBlank { atuacao.nomeLoja }
            collection(entregadorLogin)
                .document(id)
                .set(atuacao.copy(lojaId = id, atualizadoEm = System.currentTimeMillis()))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removerAtuacao(entregadorLogin: String, lojaId: String): Result<Unit> {
        return try {
            collection(entregadorLogin).document(lojaId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}
