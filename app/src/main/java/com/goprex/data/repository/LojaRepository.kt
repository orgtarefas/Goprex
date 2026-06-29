package com.goprex.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.goprex.data.model.Loja
import kotlinx.coroutines.tasks.await

class LojaRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val collection = firestore.collection("lojas")

    suspend fun listarLojas(incluirInativas: Boolean = true): Result<List<Loja>> {
        return try {
            val snapshot = collection.get().await()
            val lojas = snapshot.documents
                .map { doc -> docToLoja(doc.id, doc.data.orEmpty()) }
                .filter { incluirInativas || it.ativa }
                .sortedBy { it.nome.lowercase() }
            Result.success(lojas)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun buscarLoja(lojaIdOuNome: String): Result<Loja?> {
        return try {
            val chave = lojaIdOuNome.trim()
            if (chave.isBlank()) return Result.success(null)

            val porId = collection.document(chave).get().await()
            if (porId.exists()) {
                return Result.success(docToLoja(porId.id, porId.data.orEmpty()))
            }

            val porNome = collection.whereEqualTo("nome", chave).limit(1).get().await()
            val doc = porNome.documents.firstOrNull()
            Result.success(doc?.let { docToLoja(it.id, it.data.orEmpty()) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun salvarLoja(loja: Loja): Result<Unit> {
        return try {
            val id = loja.id.ifBlank { loja.nome.trim() }
            require(id.isNotBlank()) { "Informe o nome da loja" }
            val agora = System.currentTimeMillis()
            val lojaSalvar = loja.copy(
                id = id,
                nome = loja.nome.ifBlank { id },
                estado = loja.estado.uppercase().take(2),
                criadoEm = loja.criadoEm.takeIf { it > 0L } ?: agora,
                atualizadoEm = agora
            )

            collection.document(id).set(lojaSalvar).await()

            if (!lojaSalvar.ativa) {
                desativarLoginsVinculados(lojaSalvar)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun desativarLoginsVinculados(loja: Loja) {
        val candidatos = linkedSetOf<String>()

        val porLojaId = firestore.collection("logins")
            .whereEqualTo("lojaId", loja.id)
            .get()
            .await()
        porLojaId.documents.forEach { candidatos.add(it.id) }

        val porNome = firestore.collection("logins")
            .whereEqualTo("loja", loja.nome)
            .get()
            .await()
        porNome.documents.forEach { candidatos.add(it.id) }

        if (candidatos.isEmpty()) return

        val batch = firestore.batch()
        candidatos.forEach { loginId ->
            val ref = firestore.collection("logins").document(loginId)
            batch.set(ref, mapOf("status_ativo" to false, "ativa" to false), SetOptions.merge())
        }
        batch.commit().await()
    }

    private fun docToLoja(id: String, dados: Map<String, Any>): Loja {
        val ativa = (dados["ativa"] as? Boolean)
            ?: (dados["status_ativo"] as? Boolean)
            ?: (dados["ativo"] as? Boolean)
            ?: true

        return Loja(
            id = id,
            nome = dados["nome"]?.toString().orEmpty().ifBlank { id },
            cep = dados["cep"]?.toString().orEmpty(),
            logradouro = dados["logradouro"]?.toString().orEmpty(),
            numero = dados["numero"]?.toString().orEmpty(),
            complemento = dados["complemento"]?.toString().orEmpty(),
            bairro = dados["bairro"]?.toString().orEmpty(),
            cidade = dados["cidade"]?.toString().orEmpty().ifBlank { "Salvador" },
            estado = dados["estado"]?.toString().orEmpty().ifBlank { "BA" },
            latitude = (dados["latitude"] as? Number)?.toDouble() ?: 0.0,
            longitude = (dados["longitude"] as? Number)?.toDouble() ?: 0.0,
            logoLoja = dados["logoLoja"]?.toString().orEmpty(),
            ativa = ativa,
            criadoEm = (dados["criadoEm"] as? Number)?.toLong() ?: 0L,
            atualizadoEm = (dados["atualizadoEm"] as? Number)?.toLong() ?: 0L
        )
    }
}
