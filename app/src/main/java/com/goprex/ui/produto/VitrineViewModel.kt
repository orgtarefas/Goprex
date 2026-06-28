package com.goprex.ui.produto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goprex.data.model.EnderecoEntrega
import com.goprex.data.model.Login
import com.goprex.data.model.Pedido
import com.goprex.data.model.StatusPedido
import com.goprex.data.model.CreateCardPaymentRequest
import com.goprex.data.model.CreateCardPaymentIntentRequest
import com.goprex.data.model.CreateCheckoutSessionRequest
import com.goprex.data.model.CreatePixPaymentRequest
import com.goprex.data.model.CreatePixPaymentResponse
import com.goprex.data.model.StripeCard
import com.goprex.data.model.StripeClienteRequest
import com.goprex.data.repository.EnderecoRepository
import com.goprex.data.repository.PedidoRepository
import com.goprex.data.repository.ProdutoRepository
import com.goprex.data.repository.ProdutoVitrine
import com.goprex.data.repository.StripeRepository
import com.stripe.android.paymentsheet.PaymentSheetResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VitrineUiState(
    val produtos: List<ProdutoVitrine> = emptyList(),
    val lojas: List<String> = emptyList(),
    val categorias: List<String> = emptyList(),
    val lojaSelecionada: String? = null,
    val busca: String = "",
    val categoriaSelecionada: String? = null,
    val apenasPromocoes: Boolean = false,
    val entregaSelecionada: EntregaRapida = EntregaRapida.HOJE,
    val comprando: Boolean = false,
    val compraCriada: Pedido? = null,
    val pixPayment: CreatePixPaymentResponse? = null,
    val enderecos: List<EnderecoEntrega> = emptyList(),
    val enderecoSelecionadoId: String? = null,
    val enderecosLoading: Boolean = false,
    val enderecosError: String? = null,
    val cartoes: List<StripeCard> = emptyList(),
    val cartaoSelecionadoId: String? = null,
    val cartoesError: String? = null,
    val checkoutUrl: String? = null,
    val cardPaymentClientSecret: String? = null,
    val cardPaymentPublishableKey: String? = null,
    val cardPaymentCustomerId: String? = null,
    val cardPaymentEphemeralKeySecret: String? = null,
    val cardPaymentPedido: Pedido? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

enum class ModoVitrine {
    LOJAS,
    PRODUTOS
}

enum class EntregaRapida(
    val titulo: String,
    val descricao: String,
    val taxa: Double,
    val minutos: Int
) {
    UMA_HORA("1h", "Entrega expressa", 5.0, 60),
    DUAS_HORAS("2h", "Entrega rapida", 4.0, 120),
    TRES_HORAS("3h", "Entrega programada", 3.0, 180),
    QUATRO_HORAS("4h", "Entrega economica", 2.0, 240),
    HOJE("Hoje", "Frete gratis", 0.0, 480)
}

enum class FormaPagamento(
    val codigo: String,
    val titulo: String,
    val descricao: String
) {
    CARTAO("card", "Cartao de credito", "Preencha os dados do cartao no checkout seguro"),
    PIX("pix", "Pix", "Pague com QR Code ou copia e cola no checkout")
}

class VitrineViewModel : ViewModel() {
    private val repository = ProdutoRepository()
    private val pedidoRepository = PedidoRepository()
    private val stripeRepository = StripeRepository()
    private val enderecoRepository = EnderecoRepository()

    private val _uiState = MutableStateFlow(VitrineUiState())
    val uiState: StateFlow<VitrineUiState> = _uiState.asStateFlow()

    fun carregarProdutos() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.listarProdutosVitrineDisponiveis().fold(
                onSuccess = { produtos ->
                    _uiState.value = _uiState.value.copy(
                        produtos = produtos,
                        lojas = produtos.map { it.nomeLoja }
                            .filter { it.isNotBlank() }
                            .distinct()
                            .sorted(),
                        categorias = produtos.map { it.produto.categoria }
                            .filter { it.isNotBlank() }
                            .distinct()
                            .sorted(),
                        isLoading = false,
                        error = null
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Erro ao carregar produtos"
                    )
                }
            )
        }
    }

    fun selecionarLoja(nomeLoja: String?) {
        _uiState.value = _uiState.value.copy(lojaSelecionada = nomeLoja)
    }

    fun atualizarBusca(valor: String) {
        _uiState.value = _uiState.value.copy(busca = valor)
    }

    fun selecionarCategoria(categoria: String?) {
        _uiState.value = _uiState.value.copy(categoriaSelecionada = categoria)
    }

    fun alternarPromocoes() {
        _uiState.value = _uiState.value.copy(apenasPromocoes = !_uiState.value.apenasPromocoes)
    }

    fun selecionarEntrega(entrega: EntregaRapida) {
        _uiState.value = _uiState.value.copy(entregaSelecionada = entrega)
    }

    fun carregarCartoes(cliente: Login) {
        viewModelScope.launch {
            stripeRepository.listarCartoes(
                StripeClienteRequest(
                    clienteLogin = cliente.documentoId,
                    clienteNome = cliente.getString("nome")
                )
            ).fold(
                onSuccess = { response ->
                    val selecionadoAtual = _uiState.value.cartaoSelecionadoId
                    val selecionadoExiste = response.cards.any { it.id == selecionadoAtual }
                    _uiState.value = _uiState.value.copy(
                        cartoes = response.cards,
                        cartaoSelecionadoId = when {
                            selecionadoExiste -> selecionadoAtual
                            response.cards.isNotEmpty() -> response.cards.first().id
                            else -> null
                        },
                        cartoesError = null
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        cartoes = emptyList(),
                        cartaoSelecionadoId = null,
                        cartoesError = null
                    )
                }
            )
        }
    }

    fun carregarEnderecos(cliente: Login) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(enderecosLoading = true, enderecosError = null)
            enderecoRepository.listarEnderecos(cliente.documentoId).fold(
                onSuccess = { enderecos ->
                    val selecionadoAtual = _uiState.value.enderecoSelecionadoId
                    val selecionadoExiste = enderecos.any { it.id == selecionadoAtual }
                    val principal = enderecos.firstOrNull { it.principal } ?: enderecos.firstOrNull()
                    _uiState.value = _uiState.value.copy(
                        enderecos = enderecos,
                        enderecoSelecionadoId = when {
                            selecionadoExiste -> selecionadoAtual
                            principal != null -> principal.id
                            else -> null
                        },
                        enderecosLoading = false,
                        enderecosError = null
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        enderecosLoading = false,
                        enderecosError = e.message ?: "Nao foi possivel carregar enderecos"
                    )
                }
            )
        }
    }

    fun selecionarEndereco(enderecoId: String) {
        _uiState.value = _uiState.value.copy(enderecoSelecionadoId = enderecoId, error = null)
    }

    fun selecionarCartao(paymentMethodId: String) {
        _uiState.value = _uiState.value.copy(cartaoSelecionadoId = paymentMethodId)
    }

    fun comprar(
        item: ProdutoVitrine,
        cliente: Login,
        formaPagamento: FormaPagamento,
        clienteLatitude: Double = 0.0,
        clienteLongitude: Double = 0.0
    ) {
        viewModelScope.launch {
            val entrega = _uiState.value.entregaSelecionada
            val enderecoEntrega = _uiState.value.enderecos.firstOrNull { it.id == _uiState.value.enderecoSelecionadoId }
            if (enderecoEntrega == null) {
                _uiState.value = _uiState.value.copy(error = "Selecione um endereco de entrega para continuar")
                return@launch
            }
            _uiState.value = _uiState.value.copy(comprando = true, error = null, compraCriada = null)

            pedidoRepository.criarPedido(
                item = item,
                cliente = cliente,
                entrega = entrega,
                enderecoEntrega = enderecoEntrega,
                clienteLatitudeAtual = clienteLatitude,
                clienteLongitudeAtual = clienteLongitude
            ).fold(
                onSuccess = { pedido ->
                    if (formaPagamento == FormaPagamento.PIX) {
                        criarPix(pedido, cliente)
                    } else {
                        criarPagamentoCartaoNoApp(pedido, cliente)
                    }
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        comprando = false,
                        error = e.message ?: "Erro ao criar pedido"
                    )
                }
            )
        }
    }

    private suspend fun criarCheckout(
        pedido: Pedido,
        cliente: Login,
        formaPagamento: FormaPagamento
    ) {
        val valorTotalCentavos = (pedido.valorTotal * 100).toInt()
        if (valorTotalCentavos < 100) {
            _uiState.value = _uiState.value.copy(
                comprando = false,
                error = "O valor minimo para pagamento com cartao e R$ 1,00"
            )
            return
        }

        val request = CreateCheckoutSessionRequest(
            pedidoId = pedido.id,
            clienteLogin = cliente.documentoId,
            clienteNome = cliente.getString("nome"),
            produtoTitulo = pedido.produtoTitulo,
            loja = pedido.loja,
            valorTotalCentavos = valorTotalCentavos,
            prazoEntrega = pedido.prazoEntrega,
            formaPagamento = formaPagamento.codigo
        )

        stripeRepository.criarCheckout(request).fold(
            onSuccess = { response ->
                if (response.checkoutUrl.isBlank()) {
                    _uiState.value = _uiState.value.copy(
                        comprando = false,
                        error = "Checkout Stripe sem URL de pagamento"
                    )
                } else {
                    pedidoRepository.atualizarPagamento(
                        pedidoId = pedido.id,
                        status = StatusPedido.AGUARDANDO_PAGAMENTO,
                        pagamentoStatus = "PENDENTE",
                        stripeCheckoutSessionId = response.checkoutSessionId,
                        checkoutUrl = response.checkoutUrl
                    )
                    _uiState.value = _uiState.value.copy(
                        comprando = false,
                        compraCriada = pedido.copy(
                            stripeCheckoutSessionId = response.checkoutSessionId,
                            checkoutUrl = response.checkoutUrl
                        ),
                        checkoutUrl = response.checkoutUrl
                    )
                }
            },
            onFailure = { e ->
                _uiState.value = _uiState.value.copy(
                    comprando = false,
                    error = e.message ?: "Erro ao iniciar pagamento"
                )
            }
        )
    }

    private suspend fun criarPagamentoCartaoNoApp(
        pedido: Pedido,
        cliente: Login
    ) {
        val valorTotalCentavos = (pedido.valorTotal * 100).toInt()
        if (valorTotalCentavos < 100) {
            _uiState.value = _uiState.value.copy(
                comprando = false,
                error = "O valor minimo para pagamento com cartao e R$ 1,00"
            )
            return
        }

        val request = CreateCardPaymentIntentRequest(
            pedidoId = pedido.id,
            clienteLogin = cliente.documentoId,
            clienteNome = cliente.getString("nome"),
            produtoTitulo = pedido.produtoTitulo,
            loja = pedido.loja,
            valorTotalCentavos = valorTotalCentavos,
            prazoEntrega = pedido.prazoEntrega
        )

        stripeRepository.criarPaymentIntentCartao(request).fold(
            onSuccess = { response ->
                if (response.paymentIntentClientSecret.isBlank() || response.publishableKey.isBlank()) {
                    _uiState.value = _uiState.value.copy(
                        comprando = false,
                        error = "Pagamento iniciado sem dados para abrir o formulario no app"
                    )
                } else {
                    pedidoRepository.atualizarPagamento(
                        pedidoId = pedido.id,
                        status = StatusPedido.AGUARDANDO_PAGAMENTO,
                        pagamentoStatus = "PENDENTE",
                        stripePaymentIntentId = response.paymentIntentId
                    )
                    _uiState.value = _uiState.value.copy(
                        comprando = false,
                        compraCriada = null,
                        cardPaymentClientSecret = response.paymentIntentClientSecret,
                        cardPaymentPublishableKey = response.publishableKey,
                        cardPaymentCustomerId = response.customerId,
                        cardPaymentEphemeralKeySecret = response.ephemeralKeySecret,
                        cardPaymentPedido = pedido.copy(stripePaymentIntentId = response.paymentIntentId)
                    )
                }
            },
            onFailure = { e ->
                _uiState.value = _uiState.value.copy(
                    comprando = false,
                    error = e.message ?: "Erro ao iniciar pagamento no app"
                )
            }
        )
    }

    fun processarResultadoPagamentoCartao(result: PaymentSheetResult) {
        val pedido = _uiState.value.cardPaymentPedido
        when (result) {
            is PaymentSheetResult.Completed -> {
                if (pedido != null) {
                    val pedidoPago = pedido.copy(
                        status = StatusPedido.PRODUTO_EM_PREPARACAO.name,
                        pagamentoStatus = "PAGO",
                        pagoEm = System.currentTimeMillis()
                    )
                    viewModelScope.launch {
                        pedidoRepository.atualizarPagamento(
                            pedidoId = pedido.id,
                            status = StatusPedido.PRODUTO_EM_PREPARACAO,
                            pagamentoStatus = "PAGO",
                            stripePaymentIntentId = pedido.stripePaymentIntentId,
                            pagoEm = pedidoPago.pagoEm
                        )
                    }
                    _uiState.value = _uiState.value.copy(
                        compraCriada = pedidoPago,
                        cardPaymentClientSecret = null,
                        cardPaymentPublishableKey = null,
                        cardPaymentCustomerId = null,
                        cardPaymentEphemeralKeySecret = null,
                        cardPaymentPedido = null,
                        error = null
                    )
                }
            }
            is PaymentSheetResult.Canceled -> {
                _uiState.value = _uiState.value.copy(
                    cardPaymentClientSecret = null,
                    cardPaymentPublishableKey = null,
                    cardPaymentCustomerId = null,
                    cardPaymentEphemeralKeySecret = null,
                    cardPaymentPedido = null
                )
            }
            is PaymentSheetResult.Failed -> {
                _uiState.value = _uiState.value.copy(
                    cardPaymentClientSecret = null,
                    cardPaymentPublishableKey = null,
                    cardPaymentCustomerId = null,
                    cardPaymentEphemeralKeySecret = null,
                    cardPaymentPedido = null,
                    error = result.error.localizedMessage ?: "Erro no pagamento"
                )
            }
        }
    }

    private suspend fun criarPix(pedido: Pedido, cliente: Login) {
        val request = CreatePixPaymentRequest(
            pedidoId = pedido.id,
            clienteLogin = cliente.documentoId,
            clienteNome = cliente.getString("nome"),
            produtoTitulo = pedido.produtoTitulo,
            loja = pedido.loja,
            valorTotalCentavos = (pedido.valorTotal * 100).toInt(),
            prazoEntrega = pedido.prazoEntrega
        )

        stripeRepository.criarPix(request).fold(
            onSuccess = { response ->
                if (response.pixCopiaECola.isBlank() && response.pixQrCodeUrl.isBlank()) {
                    _uiState.value = _uiState.value.copy(
                        comprando = false,
                        error = "Pix gerado sem QR Code ou copia e cola"
                    )
                } else {
                    pedidoRepository.atualizarPagamento(
                        pedidoId = pedido.id,
                        status = StatusPedido.AGUARDANDO_PAGAMENTO,
                        pagamentoStatus = "PENDENTE",
                        stripePaymentIntentId = response.paymentIntentId
                    )
                    _uiState.value = _uiState.value.copy(
                        comprando = false,
                        compraCriada = pedido.copy(
                            stripePaymentIntentId = response.paymentIntentId
                        ),
                        pixPayment = response
                    )
                }
            },
            onFailure = { e ->
                _uiState.value = _uiState.value.copy(
                    comprando = false,
                    error = e.message ?: "Erro ao gerar Pix"
                )
            }
        )
    }

    private suspend fun pagarComCartaoSalvo(
        pedido: Pedido,
        cliente: Login,
        paymentMethodId: String
    ) {
        val request = CreateCardPaymentRequest(
            pedidoId = pedido.id,
            clienteLogin = cliente.documentoId,
            clienteNome = cliente.getString("nome"),
            produtoTitulo = pedido.produtoTitulo,
            loja = pedido.loja,
            valorTotalCentavos = (pedido.valorTotal * 100).toInt(),
            prazoEntrega = pedido.prazoEntrega,
            paymentMethodId = paymentMethodId
        )

        stripeRepository.pagarComCartao(request).fold(
            onSuccess = { response ->
                if (response.status == "succeeded") {
                    val pedidoPago = pedido.copy(
                        status = StatusPedido.PRODUTO_EM_PREPARACAO.name,
                        pagamentoStatus = "PAGO",
                        stripePaymentIntentId = response.paymentIntentId,
                        pagoEm = System.currentTimeMillis()
                    )
                    pedidoRepository.atualizarPagamento(
                        pedidoId = pedido.id,
                        status = StatusPedido.PRODUTO_EM_PREPARACAO,
                        pagamentoStatus = "PAGO",
                        stripePaymentIntentId = response.paymentIntentId,
                        pagoEm = pedidoPago.pagoEm
                    )
                    _uiState.value = _uiState.value.copy(
                        comprando = false,
                        compraCriada = pedidoPago,
                        error = null
                    )
                } else {
                    val pedidoProcessando = pedido.copy(
                        status = StatusPedido.AGUARDANDO_PAGAMENTO.name,
                        pagamentoStatus = "PROCESSANDO",
                        stripePaymentIntentId = response.paymentIntentId
                    )
                    pedidoRepository.atualizarPagamento(
                        pedidoId = pedido.id,
                        status = StatusPedido.AGUARDANDO_PAGAMENTO,
                        pagamentoStatus = "PROCESSANDO",
                        stripePaymentIntentId = response.paymentIntentId
                    )
                    _uiState.value = _uiState.value.copy(
                        comprando = false,
                        compraCriada = pedidoProcessando,
                        error = "Pagamento com cartao em processamento: ${response.status}"
                    )
                }
            },
            onFailure = { e ->
                pedidoRepository.atualizarPagamento(
                    pedidoId = pedido.id,
                    status = StatusPedido.PAGAMENTO_RECUSADO,
                    pagamentoStatus = "RECUSADO"
                )
                _uiState.value = _uiState.value.copy(
                    comprando = false,
                    compraCriada = pedido.copy(
                        status = StatusPedido.PAGAMENTO_RECUSADO.name,
                        pagamentoStatus = "RECUSADO"
                    ),
                    error = e.message ?: "Erro ao cobrar cartao"
                )
            }
        )
    }

    fun limparCompraCriada() {
        _uiState.value = _uiState.value.copy(compraCriada = null)
    }

    fun limparPixPayment() {
        _uiState.value = _uiState.value.copy(pixPayment = null, compraCriada = null)
    }

    fun limparCheckoutUrl() {
        _uiState.value = _uiState.value.copy(checkoutUrl = null)
    }
}
