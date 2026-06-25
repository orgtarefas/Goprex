package com.goprex.ui.pedido

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goprex.data.model.Login
import com.goprex.data.model.Pedido
import com.goprex.data.model.StatusPedido
import com.goprex.data.repository.PedidoRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PedidosUiState(
    val pedidos: List<Pedido> = emptyList(),
    val disponiveis: List<Pedido> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class PedidosViewModel : ViewModel() {
    private val repository = PedidoRepository()
    private var comprasJob: Job? = null
    private var entregasJob: Job? = null
    private var gestaoJob: Job? = null

    private val _uiState = MutableStateFlow(PedidosUiState())
    val uiState: StateFlow<PedidosUiState> = _uiState.asStateFlow()

    fun carregarCompras(clienteLogin: String) {
        comprasJob?.cancel()
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        comprasJob = viewModelScope.launch {
            repository.observarComprasCliente(clienteLogin)
                .catch { e -> _uiState.value = _uiState.value.copy(isLoading = false, error = e.message) }
                .collect { pedidos ->
                    _uiState.value = _uiState.value.copy(pedidos = pedidos, isLoading = false)
                }
        }
    }

    fun carregarEntregas(entregadorLogin: String) {
        entregasJob?.cancel()
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        entregasJob = viewModelScope.launch {
            combine(
                repository.observarEntregasDoEntregador(entregadorLogin),
                repository.observarEntregasDisponiveis()
            ) { minhas, disponiveis -> minhas to disponiveis }
                .catch { e -> _uiState.value = _uiState.value.copy(isLoading = false, error = e.message) }
                .collect { (minhas, disponiveis) ->
                    _uiState.value = _uiState.value.copy(
                        pedidos = minhas,
                        disponiveis = disponiveis,
                        isLoading = false
                    )
                }
        }
    }

    fun carregarGestao() {
        gestaoJob?.cancel()
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        gestaoJob = viewModelScope.launch {
            repository.observarTodosPedidos()
                .catch { e -> _uiState.value = _uiState.value.copy(isLoading = false, error = e.message) }
                .collect { pedidos ->
                    _uiState.value = _uiState.value.copy(pedidos = pedidos, isLoading = false)
                }
        }
    }

    fun aceitarPedido(pedidoId: String, entregador: Login) {
        viewModelScope.launch {
            repository.aceitarPedido(pedidoId, entregador)
            carregarEntregas(entregador.documentoId)
        }
    }

    fun atualizarStatus(pedidoId: String, status: StatusPedido, entregadorLogin: String) {
        viewModelScope.launch {
            repository.atualizarStatus(pedidoId, status)
            carregarEntregas(entregadorLogin)
        }
    }

    fun atualizarStatusGestao(pedidoId: String, status: StatusPedido) {
        viewModelScope.launch {
            repository.atualizarStatus(pedidoId, status)
            carregarGestao()
        }
    }

    fun atualizarLocalizacaoAtiva(pedidos: List<Pedido>, latitude: Double, longitude: Double) {
        val ativos = pedidos.filter { pedido ->
            pedido.status in setOf(
                StatusPedido.ACEITO.name,
                StatusPedido.COLETANDO.name,
                StatusPedido.EM_ROTA.name
            )
        }
        if (ativos.isEmpty()) return

        viewModelScope.launch {
            ativos.forEach { pedido ->
                repository.atualizarLocalizacaoEntregador(pedido.id, latitude, longitude)
            }
        }
    }
}
