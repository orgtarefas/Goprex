package com.goprex.data.model

data class CreateCheckoutSessionRequest(
    val pedidoId: String,
    val clienteLogin: String,
    val clienteNome: String,
    val produtoTitulo: String,
    val loja: String,
    val valorTotalCentavos: Int,
    val prazoEntrega: String,
    val formaPagamento: String
)

data class CreateCheckoutSessionResponse(
    val checkoutSessionId: String = "",
    val checkoutUrl: String = ""
)

data class CreatePixPaymentRequest(
    val pedidoId: String,
    val clienteLogin: String,
    val clienteNome: String,
    val produtoTitulo: String,
    val loja: String,
    val valorTotalCentavos: Int,
    val prazoEntrega: String
)

data class CreatePixPaymentResponse(
    val paymentIntentId: String = "",
    val status: String = "",
    val pixCopiaECola: String = "",
    val pixQrCodeUrl: String = "",
    val hostedInstructionsUrl: String = "",
    val expiresAt: Long = 0L
)

data class StripeClienteRequest(
    val clienteLogin: String,
    val clienteNome: String
)

data class CreateCardSetupSessionResponse(
    val setupSessionId: String = "",
    val setupUrl: String = ""
)

data class CreateCardSetupIntentResponse(
    val customerId: String = "",
    val setupIntentClientSecret: String = "",
    val publishableKey: String = ""
)

data class StripeCard(
    val id: String = "",
    val brand: String = "",
    val last4: String = "",
    val expMonth: Int = 0,
    val expYear: Int = 0,
    val apelido: String = ""
)

data class ListCardsResponse(
    val cards: List<StripeCard> = emptyList()
)

data class UpdateCardAliasRequest(
    val clienteLogin: String,
    val clienteNome: String,
    val paymentMethodId: String,
    val apelido: String
)

data class DeleteCardRequest(
    val clienteLogin: String,
    val clienteNome: String,
    val paymentMethodId: String
)

data class CreateCardPaymentRequest(
    val pedidoId: String,
    val clienteLogin: String,
    val clienteNome: String,
    val produtoTitulo: String,
    val loja: String,
    val valorTotalCentavos: Int,
    val prazoEntrega: String,
    val paymentMethodId: String
)

data class CardPaymentResponse(
    val paymentIntentId: String = "",
    val status: String = ""
)

data class OkResponse(
    val ok: Boolean = false
)
