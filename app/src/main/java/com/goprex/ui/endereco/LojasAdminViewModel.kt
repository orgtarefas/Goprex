package com.goprex.ui.endereco

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goprex.data.model.Loja
import com.goprex.data.repository.LojaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LojasAdminUiState(
    val lojas: List<Loja> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val success: String? = null
)

class LojasAdminViewModel : ViewModel() {
    private val repository = LojaRepository()

    private val _uiState = MutableStateFlow(LojasAdminUiState())
    val uiState: StateFlow<LojasAdminUiState> = _uiState.asStateFlow()

    fun carregar() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, success = null)
            repository.listarLojas(incluirInativas = true).fold(
                onSuccess = { lojas ->
                    _uiState.value = _uiState.value.copy(lojas = lojas, isLoading = false)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Nao foi possivel carregar lojas"
                    )
                }
            )
        }
    }

    fun salvar(loja: Loja) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null, success = null)
            repository.salvarLoja(loja).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isSaving = false, success = "Loja salva")
                    carregar()
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = e.message ?: "Nao foi possivel salvar loja"
                    )
                }
            )
        }
    }
}
