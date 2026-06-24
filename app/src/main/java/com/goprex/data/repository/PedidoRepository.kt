package com.goprex.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.goprex.data.model.Login
import com.goprex.data.model.Pedido
import com.goprex.data.model.StatusPedido
import com.goprex.ui.produto.EntregaRapida
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.roundToInt

class PedidoRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val pedidos = firestore.collection("pedidos")

    suspend fun criarPedido(
        item: ProdutoVitrine,
        cliente: Login,
        entrega: EntregaRapida,
        clienteLatitudeAtual: Double = 0.0,
        clienteLongitudeAtual: Double = 0.0
    ): Result<Pedido> {
        return try {
            val produto = item.produto
            val pedidoId = UUID.randomUUID().toString()
            val valorProduto = produto.precoPromocional
                ?.takeIf { produto.emPromocao && it > 0.0 }
                ?: produto.preco
            val clienteLat = clienteLatitudeAtual.takeIf { it != 0.0 } ?: cliente.getDouble("latitude")
            val clienteLng = clienteLongitudeAtual.takeIf { it != 0.0 } ?: cliente.getDouble("longitude")
            val estimativa = estimarEntrega(
                entrega = entrega,
                lojaLat = item.lojaLatitude,
                lojaLng = item.lojaLongitude,
                clienteLat = clienteLat,
                clienteLng = clienteLng
            )
            val pedido = Pedido(
                id = pedidoId,
                clienteLogin = cliente.documentoId,
                clienteNome = cliente.getString("nome"),
                clienteCidade = cliente.getString("cidade").ifBlank { "Salvador" },
                clienteEstado = cliente.getString("estado").ifBlank { "BA" },
                clienteLatitude = clienteLat,
                clienteLongitude = clienteLng,
                loja = item.nomeLoja,
                lojaLatitude = item.lojaLatitude,
                lojaLongitude = item.lojaLongitude,
                vendedorLogin = produto.vendedorLogin,
                produtoId = produto.id,
                produtoTitulo = produto.titulo,
                produtoImagem = produto.imagens.firstOrNull().orEmpty(),
                categoria = produto.categoria,
                valorProduto = valorProduto,
                prazoEntrega = entrega.titulo,
                minutosPrometidos = entrega.minutos,
                taxaEntrega = entrega.taxa,
                valorTotal = valorProduto + entrega.taxa,
                cidadeBase = "Salvador",
                estimativaMinutos = estimativa.first,
                distanciaEstimadaKm = estimativa.second
            )

            pedidos.document(pedidoId).set(pedido).await()
            Result.success(pedido)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listarComprasCliente(clienteLogin: String): Result<List<Pedido>> {
        return listarPorCampo("clienteLogin", clienteLogin)
    }

    fun observarComprasCliente(clienteLogin: String): Flow<List<Pedido>> {
        return observarPorCampo("clienteLogin", clienteLogin)
    }

    suspend fun listarEntregasDoEntregador(entregadorLogin: String): Result<List<Pedido>> {
        return listarPorCampo("entregadorLogin", entregadorLogin)
    }

    fun observarEntregasDoEntregador(entregadorLogin: String): Flow<List<Pedido>> {
        return observarPorCampo("entregadorLogin", entregadorLogin)
    }

    suspend fun listarEntregasDisponiveis(): Result<List<Pedido>> {
        return try {
            val snapshot = pedidos
                .whereEqualTo("status", StatusPedido.AGUARDANDO_ENTREGADOR.name)
                .get()
                .await()

            Result.success(snapshot.documents.mapNotNull { it.toObject(Pedido::class.java) }.sortedByDescending { it.criadoEm })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observarEntregasDisponiveis(): Flow<List<Pedido>> = callbackFlow {
        val registration = pedidos
            .whereEqualTo("status", StatusPedido.AGUARDANDO_ENTREGADOR.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val lista = snapshot?.documents
                    ?.mapNotNull { it.toObject(Pedido::class.java) }
                    ?.sortedByDescending { it.criadoEm }
                    .orEmpty()
                trySend(lista)
            }

        awaitClose { registration.remove() }
    }

    suspend fun listarTodosPedidos(): Result<List<Pedido>> {
        return try {
            val snapshot = pedidos.orderBy("criadoEm", Query.Direction.DESCENDING).get().await()
            Result.success(snapshot.documents.mapNotNull { it.toObject(Pedido::class.java) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observarTodosPedidos(): Flow<List<Pedido>> = callbackFlow {
        val registration = pedidos
            .orderBy("criadoEm", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                trySend(snapshot?.documents?.mapNotNull { it.toObject(Pedido::class.java) }.orEmpty())
            }

        awaitClose { registration.remove() }
    }

    suspend fun aceitarPedido(pedidoId: String, entregador: Login): Result<Unit> {
        return atualizarPedido(
            pedidoId,
            mapOf(
                "status" to StatusPedido.ACEITO.name,
                "entregadorLogin" to entregador.documentoId,
                "entregadorNome" to entregador.getString("nome"),
                "entregadorLatitude" to entregador.getDouble("latitude"),
                "entregadorLongitude" to entregador.getDouble("longitude"),
                "aceitoEm" to System.currentTimeMillis()
            )
        )
    }

    suspend fun atualizarStatus(pedidoId: String, status: StatusPedido): Result<Unit> {
        return atualizarPedido(pedidoId, mapOf("status" to status.name))
    }

    suspend fun atualizarLocalizacaoEntregador(
        pedidoId: String,
        latitude: Double,
        longitude: Double
    ): Result<Unit> {
        return atualizarPedido(
            pedidoId,
            mapOf(
                "entregadorLatitude" to latitude,
                "entregadorLongitude" to longitude,
                "ultimaLocalizacaoEm" to System.currentTimeMillis()
            )
        )
    }

    private suspend fun listarPorCampo(campo: String, valor: String): Result<List<Pedido>> {
        return try {
            val snapshot = pedidos.whereEqualTo(campo, valor).get().await()
            Result.success(snapshot.documents.mapNotNull { it.toObject(Pedido::class.java) }.sortedByDescending { it.criadoEm })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun observarPorCampo(campo: String, valor: String): Flow<List<Pedido>> = callbackFlow {
        val registration = pedidos
            .whereEqualTo(campo, valor)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val lista = snapshot?.documents
                    ?.mapNotNull { it.toObject(Pedido::class.java) }
                    ?.sortedByDescending { it.criadoEm }
                    .orEmpty()
                trySend(lista)
            }

        awaitClose { registration.remove() }
    }

    private suspend fun atualizarPedido(pedidoId: String, dados: Map<String, Any>): Result<Unit> {
        return try {
            pedidos.document(pedidoId)
                .update(dados + ("atualizadoEm" to System.currentTimeMillis()))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun estimarEntrega(
        entrega: EntregaRapida,
        lojaLat: Double,
        lojaLng: Double,
        clienteLat: Double,
        clienteLng: Double
    ): Pair<Int, Double> {
        val distanciaKm = if (temCoordenadas(lojaLat, lojaLng) && temCoordenadas(clienteLat, clienteLng)) {
            haversineKm(lojaLat, lojaLng, clienteLat, clienteLng)
        } else when (entrega) {
            EntregaRapida.UMA_HORA -> 4.0
            EntregaRapida.DUAS_HORAS -> 7.0
            EntregaRapida.TRES_HORAS -> 10.0
            EntregaRapida.QUATRO_HORAS -> 13.0
            EntregaRapida.HOJE -> 16.0
        }
        val minutosBase = (distanciaKm / 22.0 * 60.0).roundToInt() + 18
        return minOf(minutosBase, entrega.minutos) to distanciaKm
    }

    private fun temCoordenadas(latitude: Double, longitude: Double): Boolean {
        return latitude != 0.0 && longitude != 0.0
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }
}
