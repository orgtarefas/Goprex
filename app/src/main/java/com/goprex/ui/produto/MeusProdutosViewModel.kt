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
    val deleteSuccess: Boolean = false,
    val logoUrl: String = ""
)

class MeusProdutosViewModel : ViewModel() {
    private val produtoRepository = ProdutoRepository()

    private val _uiState = MutableStateFlow(MeusProdutosUiState())
    val uiState: StateFlow<MeusProdutosUiState> = _uiState.asStateFlow()

    fun carregarProdutos(nomeLoja: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            produtoRepository.listarProdutosPorLoja(nomeLoja).fold(
                onSuccess = { produtos -> _uiState.value = _uiState.value.copy(produtos = produtos, isLoading = false) },
                onFailure = { e -> _uiState.value = _uiState.value.copy(isLoading = false, error = "Erro: ${e.message}") }
            )
        }
    }

    fun selecionarProduto(produto: Produto?) {
        _uiState.value = _uiState.value.copy(produtoSelecionado = produto)
    }

    fun desativarProduto(nomeLoja: String, produtoId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true)
            produtoRepository.desativarProduto(nomeLoja, produtoId).fold(
                onSuccess = {
                    carregarProdutos(nomeLoja)
                    _uiState.value = _uiState.value.copy(isDeleting = false, deleteSuccess = true, produtoSelecionado = null)
                },
                onFailure = { e -> _uiState.value = _uiState.value.copy(isDeleting = false, error = "Erro: ${e.message}") }
            )
        }
    }

    fun toggleDisponibilidade(nomeLoja: String, produto: Produto) {
        viewModelScope.launch {
            produtoRepository.atualizarProduto(nomeLoja, produto.id, mapOf("disponivel" to !produto.disponivel)).fold(
                onSuccess = { carregarProdutos(nomeLoja) },
                onFailure = { e -> _uiState.value = _uiState.value.copy(error = "Erro: ${e.message}") }
            )
        }
    }

    fun togglePromocao(nomeLoja: String, produto: Produto) {
        viewModelScope.launch {
            produtoRepository.atualizarProduto(nomeLoja, produto.id, mapOf("emPromocao" to !produto.emPromocao)).fold(
                onSuccess = { carregarProdutos(nomeLoja) },
                onFailure = { e -> _uiState.value = _uiState.value.copy(error = "Erro: ${e.message}") }
            )
        }
    }

    fun atualizarPromocao(nomeLoja: String, produtoId: String, desconto: Int, dataFim: String) {
        viewModelScope.launch {
            val precoPromocional = if (desconto > 0) {
                val produto = _uiState.value.produtos.find { it.id == produtoId }
                produto?.preco?.let { it - (it * desconto / 100.0) } ?: 0.0
            } else null

            val dados = mapOf(
                "emPromocao" to (desconto > 0),
                "porcentagemDesconto" to desconto,
                "precoPromocional" to (precoPromocional ?: 0.0),
                "dataFimPromocao" to dataFim
            )

            produtoRepository.atualizarProduto(nomeLoja, produtoId, dados).fold(
                onSuccess = { carregarProdutos(nomeLoja) },
                onFailure = { e -> _uiState.value = _uiState.value.copy(error = "Erro: ${e.message}") }
            )
        }
    }

    fun atualizarLogoLoja(nomeLoja: String, url: String) {
        _uiState.value = _uiState.value.copy(logoUrl = url)
    }

    fun limparErro() { _uiState.value = _uiState.value.copy(error = null) }
    fun limparDeleteSuccess() { _uiState.value = _uiState.value.copy(deleteSuccess = false) }
}