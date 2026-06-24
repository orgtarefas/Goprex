package com.goprex.data.model

data class CreateCheckoutSessionRequest(
    val pedidoId: String,
    val clienteLogin: String,
    val clienteNome: String,
    val produtoTitulo: String,
    val loja: String,
    val valorTotalCentavos: Int,
    val prazoEntrega: String
)

data class CreateCheckoutSessionResponse(
    val checkoutSessionId: String = "",
    val checkoutUrl: String = ""
)
