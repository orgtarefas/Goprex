package com.goprex.ui.produto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goprex.data.model.Login
import com.goprex.data.model.Pedido
import com.goprex.data.model.CreateCardPaymentRequest
import com.goprex.data.model.CreateCheckoutSessionRequest
import com.goprex.data.model.CreatePixPaymentRequest
import com.goprex.data.model.CreatePixPaymentResponse
import com.goprex.data.model.StripeCard
import com.goprex.data.model.StripeClienteRequest
import com.goprex.data.repository.PedidoRepository
import com.goprex.data.repository.ProdutoRepository
import com.goprex.data.repository.ProdutoVitrine
import com.goprex.data.repository.StripeRepository
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
    val cartoes: List<StripeCard> = emptyList(),
    val cartaoSelecionadoId: String? = null,
    val checkoutUrl: String? = null,
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

    private val _uiState = MutableStateFlow(VitrineUiState())
    val uiState: StateFlow<VitrineUiState> = _uiState.asStateFlow()

    fun carregarProdutos() {
        viewModelScope.launch {
            _uiState.value = VitrineUiState(isLoading = true)
            repository.listarProdutosVitrineDisponiveis().fold(
                onSuccess = { produtos ->
                    _uiState.value = VitrineUiState(
                        produtos = produtos,
                        lojas = produtos.map { it.nomeLoja }
                            .filter { it.isNotBlank() }
                            .distinct()
                            .sorted(),
                        categorias = produtos.map { it.produto.categoria }
                            .filter { it.isNotBlank() }
                            .distinct()
                            .sorted()
                    )
                },
                onFailure = { e ->
                    _uiState.value = VitrineUiState(error = e.message)
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
                        }
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(error = e.message ?: "Erro ao carregar cartoes")
                }
            )
        }
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
            _uiState.value = _uiState.value.copy(comprando = true, error = null, compraCriada = null)

            pedidoRepository.criarPedido(
                item = item,
                cliente = cliente,
                entrega = entrega,
                clienteLatitudeAtual = clienteLatitude,
                clienteLongitudeAtual = clienteLongitude
            ).fold(
                onSuccess = { pedido ->
                    if (formaPagamento == FormaPagamento.PIX) {
                        criarPix(pedido, cliente)
                    } else if (formaPagamento == FormaPagamento.CARTAO && _uiState.value.cartaoSelecionadoId != null) {
                        pagarComCartaoSalvo(pedido, cliente, _uiState.value.cartaoSelecionadoId!!)
                    } else {
                        criarCheckout(pedido, cliente, formaPagamento)
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
        val request = CreateCheckoutSessionRequest(
            pedidoId = pedido.id,
            clienteLogin = cliente.documentoId,
            clienteNome = cliente.getString("nome"),
            produtoTitulo = pedido.produtoTitulo,
            loja = pedido.loja,
            valorTotalCentavos = (pedido.valorTotal * 100).toInt(),
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
                    _uiState.value = _uiState.value.copy(
                        comprando = false,
                        compraCriada = pedido,
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
                    _uiState.value = _uiState.value.copy(
                        comprando = false,
                        compraCriada = pedido,
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
                    _uiState.value = _uiState.value.copy(
                        comprando = false,
                        compraCriada = pedido,
                        error = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        comprando = false,
                        compraCriada = pedido,
                        error = "Pagamento com cartao em processamento: ${response.status}"
                    )
                }
            },
            onFailure = { e ->
                _uiState.value = _uiState.value.copy(
                    comprando = false,
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
