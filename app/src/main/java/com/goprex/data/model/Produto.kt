package com.goprex.data.model

data class Produto(
    val id: String = "",
    val vendedorLogin: String = "", // Login do vendedor
    val titulo: String = "",
    val descricao: String = "",
    val preco: Double = 0.0,
    val categoria: String = "",
    val imagens: List<String> = emptyList(), // URLs das imagens no Storage
    val cidade: String = "Salvador",
    val estado: String = "Bahia",
    val disponivel: Boolean = true,
    val dataCriacao: Long = System.currentTimeMillis(),
    val quantidadeVendida: Int = 0
)