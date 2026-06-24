package com.goprex.ui.pagamento

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goprex.data.model.DeleteCardRequest
import com.goprex.data.model.Login
import com.goprex.data.model.StripeCard
import com.goprex.data.model.StripeClienteRequest
import com.goprex.data.model.UpdateCardAliasRequest
import com.goprex.data.repository.StripeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CartoesUiState(
    val isLoading: Boolean = false,
    val cards: List<StripeCard> = emptyList(),
    val setupUrl: String? = null,
    val error: String? = null,
    val success: String? = null
)

class CartoesViewModel : ViewModel() {
    private val stripeRepository = StripeRepository()

    private val _uiState = MutableStateFlow(CartoesUiState())
    val uiState: StateFlow<CartoesUiState> = _uiState.asStateFlow()

    fun carregarCartoes(cliente: Login) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, success = null)
            stripeRepository.listarCartoes(cliente.toStripeRequest()).fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        cards = response.cards,
                        error = null
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Erro ao carregar cartoes"
                    )
                }
            )
        }
    }

    fun iniciarCadastroCartao(cliente: Login) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, success = null)
            stripeRepository.criarSessaoCadastroCartao(cliente.toStripeRequest()).fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        setupUrl = response.setupUrl,
                        error = null
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Erro ao iniciar cadastro do cartao"
                    )
                }
            )
        }
    }

    fun atualizarApelido(cliente: Login, paymentMethodId: String, apelido: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, success = null)
            stripeRepository.atualizarApelidoCartao(
                UpdateCardAliasRequest(
                    clienteLogin = cliente.documentoId,
                    clienteNome = cliente.getString("nome"),
                    paymentMethodId = paymentMethodId,
                    apelido = apelido.trim()
                )
            ).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false, success = "Apelido atualizado")
                    carregarCartoes(cliente)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Erro ao salvar apelido"
                    )
                }
            )
        }
    }

    fun removerCartao(cliente: Login, paymentMethodId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, success = null)
            stripeRepository.removerCartao(
                DeleteCardRequest(
                    clienteLogin = cliente.documentoId,
                    clienteNome = cliente.getString("nome"),
                    paymentMethodId = paymentMethodId
                )
            ).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false, success = "Cartao removido")
                    carregarCartoes(cliente)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Erro ao remover cartao"
                    )
                }
            )
        }
    }

    fun limparSetupUrl() {
        _uiState.value = _uiState.value.copy(setupUrl = null)
    }

    private fun Login.toStripeRequest(): StripeClienteRequest {
        return StripeClienteRequest(
            clienteLogin = documentoId,
            clienteNome = getString("nome")
        )
    }
}
