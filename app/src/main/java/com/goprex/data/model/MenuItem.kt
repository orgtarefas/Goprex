package com.goprex.data.model

data class MenuItem(
    val id: String = "",
    val titulo: String = "",
    val menuAtivo: Boolean = false,
    val rota: String = "",
    val rotaTipo: String = "",
    val menuPosicao: Int = 999
)