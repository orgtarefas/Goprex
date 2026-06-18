package com.goprex.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.goprex.data.model.Produto
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ProdutoRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val collection = firestore.collection("produtos")

    companion object {
        private const val TAG = "ProdutoRepository"
    }

    /**
     * Salva um produto no Firestore
     * O documento será o nome da loja
     */
    suspend fun salvarProduto(produto: Produto, nomeLoja: String): Result<Produto> {
        return try {
            val produtoId = if (produto.id.isEmpty()) UUID.randomUUID().toString() else produto.id
            val produtoComId = produto.copy(id = produtoId)

            // Salvar na subcoleção da loja
            firestore.collection("produtos")
                .document(nomeLoja)
                .collection("itens")
                .document(produtoId)
                .set(produtoComId)
                .await()

            Log.d(TAG, "✅ Produto salvo: $produtoId na loja $nomeLoja")
            Result.success(produtoComId)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao salvar produto: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Lista produtos de uma loja específica
     */
    suspend fun listarProdutosPorLoja(nomeLoja: String): Result<List<Produto>> {
        return try {
            val snapshot = firestore.collection("produtos")
                .document(nomeLoja)
                .collection("itens")
                .orderBy("dataCriacao", Query.Direction.DESCENDING)
                .get()
                .await()

            val produtos = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Produto::class.java)
            }

            Log.d(TAG, "📦 ${produtos.size} produtos encontrados na loja $nomeLoja")
            Result.success(produtos)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao listar produtos: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Lista apenas produtos disponíveis de uma loja
     */
    suspend fun listarProdutosDisponiveisPorLoja(nomeLoja: String): Result<List<Produto>> {
        return try {
            val snapshot = firestore.collection("produtos")
                .document(nomeLoja)
                .collection("itens")
                .whereEqualTo("disponivel", true)
                .orderBy("dataCriacao", Query.Direction.DESCENDING)
                .get()
                .await()

            val produtos = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Produto::class.java)
            }

            Log.d(TAG, "📦 ${produtos.size} produtos disponíveis na loja $nomeLoja")
            Result.success(produtos)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao listar produtos: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Lista todos os produtos de todas as lojas (para vitrine geral)
     */
    suspend fun listarTodosProdutosDisponiveis(): Result<List<Produto>> {
        return try {
            val lojasSnapshot = firestore.collection("produtos").get().await()
            val todosProdutos = mutableListOf<Produto>()

            for (lojaDoc in lojasSnapshot.documents) {
                val itensSnapshot = lojaDoc.reference
                    .collection("itens")
                    .whereEqualTo("disponivel", true)
                    .orderBy("dataCriacao", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .await()

                itensSnapshot.documents.mapNotNull { doc ->
                    doc.toObject(Produto::class.java)
                }.also { todosProdutos.addAll(it) }
            }

            Result.success(todosProdutos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Busca um produto por ID em uma loja específica
     */
    suspend fun buscarProdutoPorId(nomeLoja: String, produtoId: String): Result<Produto?> {
        return try {
            val document = firestore.collection("produtos")
                .document(nomeLoja)
                .collection("itens")
                .document(produtoId)
                .get()
                .await()

            Result.success(document.toObject(Produto::class.java))
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao buscar produto: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Atualiza um produto
     */
    suspend fun atualizarProduto(nomeLoja: String, produtoId: String, dados: Map<String, Any>): Result<Unit> {
        return try {
            firestore.collection("produtos")
                .document(nomeLoja)
                .collection("itens")
                .document(produtoId)
                .update(dados)
                .await()

            Log.d(TAG, "✅ Produto atualizado: $produtoId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao atualizar produto: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Marca produto como indisponível (exclusão lógica)
     */
    suspend fun desativarProduto(nomeLoja: String, produtoId: String): Result<Unit> {
        return try {
            firestore.collection("produtos")
                .document(nomeLoja)
                .collection("itens")
                .document(produtoId)
                .update("disponivel", false)
                .await()

            Log.d(TAG, "✅ Produto desativado: $produtoId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao desativar produto: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Deleta um produto permanentemente
     */
    suspend fun deletarProduto(nomeLoja: String, produtoId: String): Result<Unit> {
        return try {
            firestore.collection("produtos")
                .document(nomeLoja)
                .collection("itens")
                .document(produtoId)
                .delete()
                .await()

            Log.d(TAG, "🗑️ Produto deletado: $produtoId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao deletar produto: ${e.message}")
            Result.failure(e)
        }
    }
}