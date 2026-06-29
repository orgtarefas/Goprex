package com.goprex.ui.endereco

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goprex.data.model.LojaAtuacao
import com.goprex.data.model.LojaDisponivelEntrega
import com.goprex.data.repository.LojaAtuacaoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LojasAtuacaoUiState(
    val lojas: List<LojaDisponivelEntrega> = emptyList(),
    val atuacoes: List<LojaAtuacao> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val success: String? = null
) {
    val atuacoesPorLoja: Map<String, LojaAtuacao>
        get() = atuacoes.associateBy { it.lojaId }
}

class LojasAtuacaoViewModel : ViewModel() {
    private val repository = LojaAtuacaoRepository()

    private val _uiState = MutableStateFlow(LojasAtuacaoUiState())
    val uiState: StateFlow<LojasAtuacaoUiState> = _uiState.asStateFlow()

    fun carregar(entregadorLogin: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, success = null)
            val lojasResult = repository.listarLojasDisponiveis()
            val atuacoesResult = repository.listarAtuacoes(entregadorLogin)

            if (lojasResult.isFailure || atuacoesResult.isFailure) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = lojasResult.exceptionOrNull()?.message
                        ?: atuacoesResult.exceptionOrNull()?.message
                        ?: "Nao foi possivel carregar lojas"
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                lojas = lojasResult.getOrDefault(emptyList()),
                atuacoes = atuacoesResult.getOrDefault(emptyList()),
                isLoading = false
            )
        }
    }

    fun salvar(entregadorLogin: String, loja: LojaDisponivelEntrega, raioKm: Double, atendeTodaCidade: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null, success = null)
            val atuacao = LojaAtuacao(
                lojaId = loja.lojaId,
                nomeLoja = loja.nomeLoja,
                enderecoResumo = loja.enderecoResumo,
                cidade = loja.cidade,
                estado = loja.estado,
                latitude = loja.latitude,
                longitude = loja.longitude,
                raioKm = raioKm,
                atendeTodaCidade = atendeTodaCidade
            )
            repository.salvarAtuacao(entregadorLogin, atuacao).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isSaving = false, success = "Loja atualizada")
                    carregar(entregadorLogin)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = e.message ?: "Nao foi possivel salvar a loja"
                    )
                }
            )
        }
    }

    fun remover(entregadorLogin: String, lojaId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null, success = null)
            repository.removerAtuacao(entregadorLogin, lojaId).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isSaving = false, success = "Loja removida")
                    carregar(entregadorLogin)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = e.message ?: "Nao foi possivel remover a loja"
                    )
                }
            )
        }
    }
}
