package com.goprex.ui.produto

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goprex.data.model.Produto
import com.goprex.data.repository.ImgBBRepository
import com.goprex.data.repository.ProdutoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.InputStream

// Estados específicos para cada etapa do processo
sealed class CadastroState {
    object Idle : CadastroState()
    object PreparandoImagens : CadastroState()
    data class EnviandoImagens(val progresso: Int, val total: Int) : CadastroState()
    object SalvandoProduto : CadastroState()
    object Sucesso : CadastroState()
    data class Erro(val mensagem: String, val etapa: String? = null) : CadastroState()
}

data class ProdutoUiState(
    val titulo: String = "",
    val descricao: String = "",
    val preco: String = "",
    val categoria: String = "",
    val quantidade: String = "1",
    val imagensUris: List<Uri> = emptyList(),
    val imagensRemovidas: List<String> = emptyList(), // Para edição
    val estadoCadastro: CadastroState = CadastroState.Idle,
    val camposValidados: Map<String, Boolean> = emptyMap(),
    val produtoEditando: String? = null // ID do produto se for edição
)

class ProdutoViewModel : ViewModel() {
    private val produtoRepository = ProdutoRepository()
    private val imgBBRepository = ImgBBRepository()

    private val _uiState = MutableStateFlow(ProdutoUiState())
    val uiState: StateFlow<ProdutoUiState> = _uiState.asStateFlow()

    // Getters computados para facilitar acesso
    val isLoading: Boolean get() = _uiState.value.estadoCadastro !is CadastroState.Idle &&
            _uiState.value.estadoCadastro !is CadastroState.Erro &&
            _uiState.value.estadoCadastro !is CadastroState.Sucesso
    val isSuccess: Boolean get() = _uiState.value.estadoCadastro is CadastroState.Sucesso
    val error: String? get() = (_uiState.value.estadoCadastro as? CadastroState.Erro)?.mensagem

    // Estados individuais para UI
    val progressoUpload: Pair<Int, Int>? get() {
        val state = _uiState.value.estadoCadastro
        return if (state is CadastroState.EnviandoImagens) {
            Pair(state.progresso, state.total)
        } else null
    }

    val etapaAtual: String get() = when (_uiState.value.estadoCadastro) {
        is CadastroState.Idle -> ""
        is CadastroState.PreparandoImagens -> "Preparando imagens..."
        is CadastroState.EnviandoImagens -> "Enviando imagens..."
        is CadastroState.SalvandoProduto -> "Salvando produto..."
        is CadastroState.Sucesso -> "Produto cadastrado!"
        is CadastroState.Erro -> "Erro"
    }

    fun updateTitulo(titulo: String) {
        _uiState.value = _uiState.value.copy(
            titulo = titulo,
            camposValidados = _uiState.value.camposValidados + ("titulo" to titulo.isNotBlank())
        )
    }

    fun updateDescricao(descricao: String) {
        _uiState.value = _uiState.value.copy(
            descricao = descricao,
            camposValidados = _uiState.value.camposValidados + ("descricao" to descricao.isNotBlank())
        )
    }

    fun updatePreco(preco: String) {
        val filteredPreco = preco.replace(",", ".")
            .filter { it.isDigit() || it == '.' }
        val parts = filteredPreco.split(".")
        val finalPreco = if (parts.size > 2) {
            parts[0] + "." + parts.drop(1).joinToString("")
        } else {
            filteredPreco
        }
        val precoValido = finalPreco.toDoubleOrNull()?.let { it > 0 } ?: false

        _uiState.value = _uiState.value.copy(
            preco = finalPreco,
            camposValidados = _uiState.value.camposValidados + ("preco" to precoValido)
        )
    }

    fun updateCategoria(categoria: String) {
        _uiState.value = _uiState.value.copy(categoria = categoria)
    }

    fun updateQuantidade(quantidade: String) {
        val filtered = quantidade.filter { it.isDigit() }
        _uiState.value = _uiState.value.copy(quantidade = filtered.ifEmpty { "1" })
    }

    fun addImages(uris: List<Uri>) {
        val currentImages = _uiState.value.imagensUris.toMutableList()
        val remaining = 5 - currentImages.size

        if (remaining <= 0) {
            _uiState.value = _uiState.value.copy(
                estadoCadastro = CadastroState.Erro("Máximo de 5 imagens permitido")
            )
            return
        }

        currentImages.addAll(uris.take(remaining))
        _uiState.value = _uiState.value.copy(
            imagensUris = currentImages,
            camposValidados = _uiState.value.camposValidados + ("imagens" to currentImages.isNotEmpty()),
            estadoCadastro = CadastroState.Idle
        )
    }

    fun removeImage(uri: Uri) {
        val currentImages = _uiState.value.imagensUris.toMutableList()
        currentImages.remove(uri)
        _uiState.value = _uiState.value.copy(
            imagensUris = currentImages,
            camposValidados = _uiState.value.camposValidados + ("imagens" to currentImages.isNotEmpty())
        )
    }

    // Carregar produto para edição
    fun carregarProdutoParaEdicao(produtoId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(estadoCadastro = CadastroState.PreparandoImagens)

            val result = produtoRepository.buscarProdutoPorId(produtoId)
            result.fold(
                onSuccess = { produto ->
                    produto?.let {
                        _uiState.value = _uiState.value.copy(
                            titulo = it.titulo,
                            descricao = it.descricao,
                            preco = it.preco.toString(),
                            categoria = it.categoria,
                            quantidade = "1", // Ou buscar do estoque
                            produtoEditando = produtoId,
                            estadoCadastro = CadastroState.Idle
                        )
                    }
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(
                        estadoCadastro = CadastroState.Erro("Erro ao carregar produto")
                    )
                }
            )
        }
    }

    private fun readBytesFromUri(context: Context, uri: Uri): ByteArray? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val byteArrayOutputStream = ByteArrayOutputStream()
            inputStream?.use { input ->
                val buffer = ByteArray(8192) // Buffer maior para performance
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    byteArrayOutputStream.write(buffer, 0, bytesRead)
                }
            }
            byteArrayOutputStream.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun validarFormulario(): Map<String, String> {
        val erros = mutableMapOf<String, String>()
        val state = _uiState.value

        if (state.titulo.isBlank()) erros["titulo"] = "Título é obrigatório"
        if (state.descricao.isBlank()) erros["descricao"] = "Descrição é obrigatória"
        if (state.preco.isBlank() || state.preco.toDoubleOrNull() == null || state.preco.toDouble() <= 0) {
            erros["preco"] = "Preço deve ser maior que zero"
        }
        if (state.imagensUris.isEmpty()) erros["imagens"] = "Adicione pelo menos uma imagem"

        return erros
    }

    fun salvarProduto(
        vendedorLogin: String,
        cidade: String,
        estado: String,
        context: Context
    ) {
        // Validação
        val erros = validarFormulario()
        if (erros.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(
                estadoCadastro = CadastroState.Erro(erros.values.first())
            )
            return
        }

        viewModelScope.launch {
            try {
                val state = _uiState.value

                // Etapa 1: Preparar imagens
                _uiState.value = _uiState.value.copy(
                    estadoCadastro = CadastroState.PreparandoImagens
                )

                val imagensBytes = mutableListOf<ByteArray>()
                state.imagensUris.forEach { uri ->
                    readBytesFromUri(context, uri)?.let { imagensBytes.add(it) }
                }

                if (imagensBytes.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        estadoCadastro = CadastroState.Erro("Não foi possível ler as imagens")
                    )
                    return@launch
                }

                // Etapa 2: Upload das imagens com progresso
                val nomeBase = buildString {
                    append("produto_")
                    append(state.titulo.take(30)
                        .replace(" ", "_")
                        .replace("[^a-zA-Z0-9_]".toRegex(), ""))
                    append("_${System.currentTimeMillis()}")
                }

                val urlsEnviadas = mutableListOf<String>()

                imagensBytes.forEachIndexed { index, bytes ->
                    _uiState.value = _uiState.value.copy(
                        estadoCadastro = CadastroState.EnviandoImagens(index + 1, imagensBytes.size)
                    )

                    val imageName = "${nomeBase}_${index + 1}"
                    val result = imgBBRepository.uploadImage(bytes, imageName)

                    result.fold(
                        onSuccess = { url -> urlsEnviadas.add(url) },
                        onFailure = { /* Continua mesmo com falha */ }
                    )
                }

                if (urlsEnviadas.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        estadoCadastro = CadastroState.Erro("Falha ao enviar imagens")
                    )
                    return@launch
                }

                // Etapa 3: Salvar produto
                _uiState.value = _uiState.value.copy(
                    estadoCadastro = CadastroState.SalvandoProduto
                )

                val produto = Produto(
                    id = state.produtoEditando ?: "",
                    vendedorLogin = vendedorLogin,
                    titulo = state.titulo.trim(),
                    descricao = state.descricao.trim(),
                    preco = state.preco.toDouble(),
                    categoria = state.categoria.trim().ifBlank { "Geral" },
                    cidade = cidade,
                    estado = estado,
                    imagens = urlsEnviadas,
                    disponivel = true,
                    dataCriacao = System.currentTimeMillis()
                )

                val resultSalvar = produtoRepository.salvarProduto(produto)

                resultSalvar.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            estadoCadastro = CadastroState.Sucesso
                        )
                    },
                    onFailure = { e ->
                        _uiState.value = _uiState.value.copy(
                            estadoCadastro = CadastroState.Erro(
                                "Erro ao salvar: ${e.message}",
                                "salvamento"
                            )
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    estadoCadastro = CadastroState.Erro(
                        "Erro inesperado: ${e.message}",
                        "geral"
                    )
                )
            }
        }
    }

    fun limparEstado() {
        _uiState.value = ProdutoUiState()
    }

    fun limparErro() {
        _uiState.value = _uiState.value.copy(estadoCadastro = CadastroState.Idle)
    }
}