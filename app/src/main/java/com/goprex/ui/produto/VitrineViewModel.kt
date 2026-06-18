package com.goprex.ui.produto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goprex.data.model.Produto
import com.goprex.data.repository.ProdutoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VitrineUiState(
    val produtos: List<Produto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class VitrineViewModel : ViewModel() {
    private val repository = ProdutoRepository()

    private val _uiState = MutableStateFlow(VitrineUiState())
    val uiState: StateFlow<VitrineUiState> = _uiState.asStateFlow()

    fun carregarProdutos() {
        viewModelScope.launch {
            _uiState.value = VitrineUiState(isLoading = true)
            repository.listarTodosProdutosDisponiveis().fold(
                onSuccess = { produtos ->
                    _uiState.value = VitrineUiState(produtos = produtos)
                },
                onFailure = { e ->
                    _uiState.value = VitrineUiState(error = e.message)
                }
            )
        }
    }
}