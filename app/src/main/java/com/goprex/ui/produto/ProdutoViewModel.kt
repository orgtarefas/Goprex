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

data class ProdutoUiState(
    val titulo: String = "",
    val descricao: String = "",
    val preco: String = "",
    val categoria: String = "",
    val imagensUris: List<Uri> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

class ProdutoViewModel : ViewModel() {
    private val produtoRepository = ProdutoRepository()
    private val imgBBRepository = ImgBBRepository()

    private val _uiState = MutableStateFlow(ProdutoUiState())
    val uiState: StateFlow<ProdutoUiState> = _uiState.asStateFlow()

    fun updateTitulo(titulo: String) {
        _uiState.value = _uiState.value.copy(titulo = titulo)
    }

    fun updateDescricao(descricao: String) {
        _uiState.value = _uiState.value.copy(descricao = descricao)
    }

    fun updatePreco(preco: String) {
        _uiState.value = _uiState.value.copy(preco = preco)
    }

    fun updateCategoria(categoria: String) {
        _uiState.value = _uiState.value.copy(categoria = categoria)
    }

    fun addImages(uris: List<Uri>) {
        val currentImages = _uiState.value.imagensUris.toMutableList()
        currentImages.addAll(uris)
        _uiState.value = _uiState.value.copy(imagensUris = currentImages.take(5))
    }

    fun removeImage(uri: Uri) {
        val currentImages = _uiState.value.imagensUris.toMutableList()
        currentImages.remove(uri)
        _uiState.value = _uiState.value.copy(imagensUris = currentImages)
    }

    /**
     * Lê os bytes de uma URI usando ContentResolver
     */
    private fun readBytesFromUri(context: Context, uri: Uri): ByteArray? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val byteArrayOutputStream = ByteArrayOutputStream()

            inputStream?.use { input ->
                val buffer = ByteArray(1024)
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

    fun salvarProduto(
        vendedorLogin: String,
        cidade: String,
        estado: String,
        context: Context
    ) {
        val state = _uiState.value

        // Validações
        if (state.titulo.isBlank()) {
            _uiState.value = state.copy(error = "Digite o título do produto")
            return
        }
        if (state.preco.isBlank() || state.preco.toDoubleOrNull() == null) {
            _uiState.value = state.copy(error = "Digite um preço válido")
            return
        }
        if (state.imagensUris.isEmpty()) {
            _uiState.value = state.copy(error = "Adicione pelo menos uma imagem")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, error = null)

            try {
                // 1. Converter URIs para bytes e fazer upload para ImgBB
                val imagensUrls = mutableListOf<String>()

                state.imagensUris.forEachIndexed { index, uri ->
                    val imageBytes = readBytesFromUri(context, uri)

                    if (imageBytes != null) {
                        val imageName = "produto_${state.titulo.take(20).replace(" ", "_")}_${index + 1}"
                        val result = imgBBRepository.uploadImage(imageBytes, imageName)

                        result.fold(
                            onSuccess = { url ->
                                imagensUrls.add(url)
                            },
                            onFailure = { e ->
                                // Continua mesmo se uma imagem falhar
                                e.printStackTrace()
                            }
                        )
                    }
                }

                if (imagensUrls.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Falha ao enviar imagens. Tente novamente."
                    )
                    return@launch
                }

                // 2. Criar produto com as URLs das imagens
                val produto = Produto(
                    vendedorLogin = vendedorLogin,
                    titulo = state.titulo.trim(),
                    descricao = state.descricao.trim(),
                    preco = state.preco.toDouble(),
                    categoria = state.categoria.trim(),
                    cidade = cidade,
                    estado = estado,
                    imagens = imagensUrls
                )

                // 3. Salvar no Firestore
                val result = produtoRepository.salvarProduto(produto)

                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isSuccess = true
                        )
                    },
                    onFailure = { e ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = e.message ?: "Erro ao salvar produto"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Erro inesperado"
                )
            }
        }
    }

    fun clearSuccess() {
        _uiState.value = ProdutoUiState()
    }
}