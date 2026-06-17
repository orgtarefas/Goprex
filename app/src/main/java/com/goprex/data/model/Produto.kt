package com.goprex.data.model

data class Produto(
    val id: String = "",
    val titulo: String = "",
    val descricao: String = "",
    val preco: Double = 0.0,
    val categoria: String = "",
    val vendedorLogin: String = "",
    val cidade: String = "",
    val estado: String = "",
    val imagens: List<String> = emptyList(),
    val disponivel: Boolean = true,
    val dataCriacao: Long = System.currentTimeMillis()
)