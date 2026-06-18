package com.goprex.ui.produto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
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
    private val firestore = FirebaseFirestore.getInstance()

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
            val dados = if (produto.emPromocao) {
                mapOf(
                    "emPromocao" to false,
                    "porcentagemDesconto" to 0,
                    "precoPromocional" to null,
                    "dataFimPromocao" to null
                )
            } else {
                mapOf("emPromocao" to true)
            }
            produtoRepository.atualizarProduto(nomeLoja, produto.id, dados).fold(
                onSuccess = { carregarProdutos(nomeLoja) },
                onFailure = { e -> _uiState.value = _uiState.value.copy(error = "Erro: ${e.message}") }
            )
        }
    }

    fun atualizarPromocao(nomeLoja: String, produtoId: String, desconto: Int, dataFim: String) {
        viewModelScope.launch {
            val produto = _uiState.value.produtos.find { it.id == produtoId }
            val precoOriginal = produto?.preco ?: 0.0
            val precoPromocional = if (desconto > 0) precoOriginal - (precoOriginal * desconto / 100.0) else null

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

    /**
     * Atualiza a URL da logo da loja no Firestore
     * @param nomeLoja Nome da loja
     * @param url URL da logo
     * @param documentoId ID do documento do login
     */
    fun atualizarLogoLoja(nomeLoja: String, url: String, documentoId: String = "") {
        _uiState.value = _uiState.value.copy(logoUrl = url)

        if (documentoId.isNotEmpty()) {
            viewModelScope.launch {
                try {
                    firestore.collection("logins")
                        .document(documentoId)
                        .update("logoLoja", url)
                } catch (e: Exception) {
                    // Erro silencioso - a logo já está salva no estado local
                }
            }
        }
    }

    fun limparErro() { _uiState.value = _uiState.value.copy(error = null) }
    fun limparDeleteSuccess() { _uiState.value = _uiState.value.copy(deleteSuccess = false) }
}