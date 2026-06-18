package com.goprex.ui.produto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goprex.data.model.Produto
import com.goprex.data.repository.ProdutoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MeusProdutosUiState(
    val produtos: List<Produto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val produtoSelecionado: Produto? = null,
    val isDeleting: Boolean = false,
    val deleteSuccess: Boolean = false
)

class MeusProdutosViewModel : ViewModel() {
    private val produtoRepository = ProdutoRepository()

    private val _uiState = MutableStateFlow(MeusProdutosUiState())
    val uiState: StateFlow<MeusProdutosUiState> = _uiState.asStateFlow()

    fun carregarProdutos(nomeLoja: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val result = produtoRepository.listarProdutosPorLoja(nomeLoja)

            result.fold(
                onSuccess = { produtos ->
                    _uiState.value = _uiState.value.copy(
                        produtos = produtos,
                        isLoading = false
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Erro ao carregar produtos: ${e.message}"
                    )
                }
            )
        }
    }

    fun selecionarProduto(produto: Produto?) {
        _uiState.value = _uiState.value.copy(produtoSelecionado = produto)
    }

    fun desativarProduto(nomeLoja: String, produtoId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true)

            val result = produtoRepository.desativarProduto(nomeLoja, produtoId)

            result.fold(
                onSuccess = {
                    // Recarregar lista
                    carregarProdutos(nomeLoja)
                    _uiState.value = _uiState.value.copy(
                        isDeleting = false,
                        deleteSuccess = true,
                        produtoSelecionado = null
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isDeleting = false,
                        error = "Erro ao desativar: ${e.message}"
                    )
                }
            )
        }
    }

    fun toggleDisponibilidade(nomeLoja: String, produto: Produto) {
        viewModelScope.launch {
            val novoEstado = mapOf("disponivel" to !produto.disponivel)

            val result = produtoRepository.atualizarProduto(nomeLoja, produto.id, novoEstado)

            result.fold(
                onSuccess = {
                    carregarProdutos(nomeLoja)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        error = "Erro ao atualizar: ${e.message}"
                    )
                }
            )
        }
    }

    fun limparErro() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun limparDeleteSuccess() {
        _uiState.value = _uiState.value.copy(deleteSuccess = false)
    }
}