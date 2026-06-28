package com.goprex.ui.endereco

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goprex.data.model.EnderecoEntrega
import com.goprex.data.repository.EnderecoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EnderecosUiState(
    val enderecos: List<EnderecoEntrega> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isBuscandoCep: Boolean = false,
    val enderecoCep: EnderecoEntrega? = null,
    val error: String? = null,
    val success: String? = null
)

class EnderecosViewModel : ViewModel() {
    private val repository = EnderecoRepository()

    private val _uiState = MutableStateFlow(EnderecosUiState())
    val uiState: StateFlow<EnderecosUiState> = _uiState.asStateFlow()

    fun carregar(clienteLogin: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, success = null)
            repository.listarEnderecos(clienteLogin).fold(
                onSuccess = { enderecos ->
                    _uiState.value = _uiState.value.copy(enderecos = enderecos, isLoading = false)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Nao foi possivel carregar enderecos"
                    )
                }
            )
        }
    }

    fun salvar(clienteLogin: String, endereco: EnderecoEntrega) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null, success = null)
            val primeiroEndereco = _uiState.value.enderecos.isEmpty()
            val enderecoSalvar = if (primeiroEndereco) endereco.copy(principal = true) else endereco
            repository.salvarEndereco(clienteLogin, enderecoSalvar).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isSaving = false, success = "Endereco salvo")
                    carregar(clienteLogin)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = e.message ?: "Nao foi possivel salvar endereco"
                    )
                }
            )
        }
    }

    fun buscarCep(cep: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBuscandoCep = true, enderecoCep = null, error = null, success = null)
            repository.buscarEnderecoPorCep(cep).fold(
                onSuccess = { endereco ->
                    _uiState.value = _uiState.value.copy(
                        isBuscandoCep = false,
                        enderecoCep = endereco,
                        success = "Endereco preenchido pelo CEP"
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isBuscandoCep = false,
                        error = e.message ?: "Nao foi possivel buscar o CEP"
                    )
                }
            )
        }
    }

    fun definirPrincipal(clienteLogin: String, enderecoId: String) {
        viewModelScope.launch {
            repository.definirPrincipal(clienteLogin, enderecoId).fold(
                onSuccess = { carregar(clienteLogin) },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(error = e.message ?: "Nao foi possivel definir endereco principal")
                }
            )
        }
    }

    fun remover(clienteLogin: String, enderecoId: String) {
        viewModelScope.launch {
            repository.removerEndereco(clienteLogin, enderecoId).fold(
                onSuccess = { carregar(clienteLogin) },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(error = e.message ?: "Nao foi possivel remover endereco")
                }
            )
        }
    }
}
