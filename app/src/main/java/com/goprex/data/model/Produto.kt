package com.goprex.data.model

data class Produto(
    val id: String = "",
    val nome: String = "",
    val descricao: String = "",
    val preco: Double = 0.0,
    val imagemUrl: String = "",
    val vendedorId: String = "",
    val categoria: String = ""
)