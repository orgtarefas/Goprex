package com.goprex.ui.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goprex.data.model.MenuItem
import com.goprex.data.repository.MenuRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MenuViewModel : ViewModel() {
    private val menuRepository = MenuRepository()

    private val _itensMenu = MutableStateFlow<List<MenuItem>>(emptyList())
    val itensMenu: StateFlow<List<MenuItem>> = _itensMenu

    private val _itensTela = MutableStateFlow<List<MenuItem>>(emptyList())
    val itensTela: StateFlow<List<MenuItem>> = _itensTela

    private val _itensModal = MutableStateFlow<List<MenuItem>>(emptyList())
    val itensModal: StateFlow<List<MenuItem>> = _itensModal

    private val _carregando = MutableStateFlow(false)
    val carregando: StateFlow<Boolean> = _carregando

    private val _erro = MutableStateFlow<String?>(null)
    val erro: StateFlow<String?> = _erro

    fun carregarMenu(perfil: String) {
        viewModelScope.launch {
            _carregando.value = true
            _erro.value = null
            try {
                val todosItens = menuRepository.carregarMenuPorPerfil(perfil)
                _itensMenu.value = todosItens
                _itensTela.value = todosItens.filter { it.rotaTipo == "tela" }
                _itensModal.value = todosItens.filter { it.rotaTipo == "modal" }
            } catch (e: Exception) {
                _erro.value = e.message
                _itensMenu.value = emptyList()
                _itensTela.value = emptyList()
                _itensModal.value = emptyList()
            } finally {
                _carregando.value = false
            }
        }
    }
}