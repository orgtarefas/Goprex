package com.goprex.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.goprex.data.model.Produto
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ProdutoRepository {
    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Salva um produto no Firestore
     * As imagens já devem ter sido enviadas para o ImgBB antes
     */
    suspend fun salvarProduto(produto: Produto): Result<Produto> {
        return try {
            val produtoId = if (produto.id.isEmpty()) UUID.randomUUID().toString() else produto.id
            val produtoComId = produto.copy(id = produtoId)

            firestore.collection("produtos")
                .document(produtoId)
                .set(produtoComId)
                .await()

            Result.success(produtoComId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Lista produtos de um vendedor específico
     */
    suspend fun listarProdutosVendedor(vendedorLogin: String): Result<List<Produto>> {
        return try {
            val snapshot = firestore.collection("produtos")
                .whereEqualTo("vendedorLogin", vendedorLogin)
                .orderBy("dataCriacao")
                .get()
                .await()

            val produtos = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Produto::class.java)
            }

            Result.success(produtos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Lista todos os produtos disponíveis
     */
    suspend fun listarTodosProdutos(): Result<List<Produto>> {
        return try {
            val snapshot = firestore.collection("produtos")
                .whereEqualTo("disponivel", true)
                .orderBy("dataCriacao")
                .get()
                .await()

            val produtos = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Produto::class.java)
            }

            Result.success(produtos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Busca um produto por ID
     */
    suspend fun buscarProdutoPorId(produtoId: String): Result<Produto?> {
        return try {
            val document = firestore.collection("produtos")
                .document(produtoId)
                .get()
                .await()

            Result.success(document.toObject(Produto::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Atualiza um produto
     */
    suspend fun atualizarProduto(produtoId: String, dados: Map<String, Any>): Result<Unit> {
        return try {
            firestore.collection("produtos")
                .document(produtoId)
                .update(dados)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deleta um produto
     */
    suspend fun deletarProduto(produtoId: String): Result<Unit> {
        return try {
            firestore.collection("produtos")
                .document(produtoId)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}