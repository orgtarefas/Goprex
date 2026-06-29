package com.goprex.data.model

data class CreateCheckoutSessionRequest(
    val pedidoId: String,
    val clienteLogin: String,
    val clienteNome: String,
    val produtoTitulo: String,
    val loja: String,
    val valorTotalCentavos: Int,
    val tarifaTransacaoCentavos: Int,
    val prazoEntrega: String,
    val formaPagamento: String
)

data class CreateCheckoutSessionResponse(
    val checkoutSessionId: String = "",
    val checkoutUrl: String = "",
    val valorTotalCentavos: Int = 0,
    val tarifaTransacaoCentavos: Int = 0,
    val tarifaEntregaCentavos: Int = 0
)

data class CreatePixPaymentRequest(
    val pedidoId: String,
    val clienteLogin: String,
    val clienteNome: String,
    val produtoTitulo: String,
    val loja: String,
    val valorTotalCentavos: Int,
    val tarifaTransacaoCentavos: Int,
    val prazoEntrega: String
)

data class CreatePixPaymentResponse(
    val paymentIntentId: String = "",
    val status: String = "",
    val pixCopiaECola: String = "",
    val pixQrCodeUrl: String = "",
    val hostedInstructionsUrl: String = "",
    val expiresAt: Long = 0L,
    val valorTotalCentavos: Int = 0,
    val tarifaTransacaoCentavos: Int = 0,
    val tarifaEntregaCentavos: Int = 0
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
    val tarifaTransacaoCentavos: Int,
    val prazoEntrega: String,
    val paymentMethodId: String
)

data class CreateCardPaymentIntentRequest(
    val pedidoId: String,
    val clienteLogin: String,
    val clienteNome: String,
    val produtoTitulo: String,
    val loja: String,
    val valorTotalCentavos: Int,
    val tarifaTransacaoCentavos: Int,
    val prazoEntrega: String
)

data class CreateCardPaymentIntentResponse(
    val paymentIntentId: String = "",
    val paymentIntentClientSecret: String = "",
    val customerId: String = "",
    val ephemeralKeySecret: String = "",
    val publishableKey: String = "",
    val valorTotalCentavos: Int = 0,
    val tarifaTransacaoCentavos: Int = 0,
    val tarifaEntregaCentavos: Int = 0
)

data class CardPaymentResponse(
    val paymentIntentId: String = "",
    val status: String = "",
    val valorTotalCentavos: Int = 0,
    val tarifaTransacaoCentavos: Int = 0,
    val tarifaEntregaCentavos: Int = 0
)

data class OkResponse(
    val ok: Boolean = false
)

data class AdminContaDiagnosticoResponse(
    val configurada: Boolean = false,
    val comprimento: Int = 0
)

data class ContaRecebimentoPayload(
    val tipoChavePix: String = "",
    val chavePix: String = "",
    val banco: String = "",
    val agencia: String = "",
    val conta: String = "",
    val titular: String = "",
    val documentoTitular: String = ""
)

data class AdminContaRecebimentoRequest(
    val adminLogin: String,
    val senhaAlteracao: String,
    val contaRecebimento: ContaRecebimentoPayload
)

data class VerificarSenhaContaAdminRequest(
    val adminLogin: String,
    val senhaAlteracao: String
)

data class VerificarSenhaContaAdminResponse(
    val ok: Boolean = false,
    val autorizada: Boolean = false
)
