package com.goprex.ui.produto

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Pix
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import com.goprex.data.model.CreatePixPaymentResponse
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.goprex.data.model.EnderecoEntrega
import com.goprex.data.model.Login
import com.goprex.data.model.Pedido
import com.goprex.data.model.Produto
import com.goprex.data.model.StripeCard
import com.goprex.data.repository.ProdutoVitrine
import com.goprex.ui.telas.tela_home_meus_dados
import com.goprex.ui.theme.GoPrexDark
import com.goprex.ui.theme.GoPrexOrange
import com.goprex.ui.theme.SuccessGreen
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.rememberPaymentSheet
import java.text.NumberFormat
import java.util.Locale

private data class LojaVitrine(
    val nome: String,
    val produtos: List<ProdutoVitrine>,
    val categorias: List<String>,
    val cidade: String,
    val estado: String
) {
    val totalProdutos: Int get() = produtos.size
    val totalPromocoes: Int get() = produtos.count { it.produto.emPromocao }
    val imagemCapa: String? get() = produtos.firstOrNull { it.produto.imagens.isNotEmpty() }?.produto?.imagens?.first()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VitrineScreen(
    loginData: Login? = null,
    onProdutoClick: (Produto) -> Unit = {},
    viewModel: VitrineViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val paymentSheet = rememberPaymentSheet { result ->
        viewModel.processarResultadoPagamentoCartao(result)
    }
    val numberFormat = remember { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }
    var modo by remember { mutableStateOf(ModoVitrine.LOJAS) }
    var itemSelecionado by remember { mutableStateOf<ProdutoVitrine?>(null) }
    var formaPagamentoSelecionada by remember { mutableStateOf(FormaPagamento.CARTAO) }

    LaunchedEffect(Unit) { viewModel.carregarProdutos() }
    LaunchedEffect(loginData?.documentoId) {
        if (loginData != null) {
            viewModel.carregarEnderecos(loginData)
            viewModel.carregarCartoes(loginData)
        }
    }

    LaunchedEffect(uiState.checkoutUrl) {
        val url = uiState.checkoutUrl
        if (!url.isNullOrBlank()) {
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
            viewModel.limparCheckoutUrl()
        }
    }

    LaunchedEffect(uiState.cardPaymentClientSecret, uiState.cardPaymentPublishableKey) {
        val clientSecret = uiState.cardPaymentClientSecret
        val publishableKey = uiState.cardPaymentPublishableKey
        if (!clientSecret.isNullOrBlank() && !publishableKey.isNullOrBlank()) {
            PaymentConfiguration.init(context.applicationContext, publishableKey)
            paymentSheet.presentWithPaymentIntent(
                paymentIntentClientSecret = clientSecret,
                configuration = PaymentSheet.Configuration(
                    merchantDisplayName = "GoPrex"
                )
            )
        }
    }

    val produtosSalvador = remember(uiState.produtos) {
        uiState.produtos.filter { item ->
            item.produto.cidade.isBlank() || item.produto.cidade.contains("salvador", ignoreCase = true)
        }
    }
    val produtosFiltrados = remember(
        produtosSalvador,
        uiState.busca,
        uiState.lojaSelecionada,
        uiState.categoriaSelecionada,
        uiState.apenasPromocoes
    ) {
        produtosSalvador.filter { item ->
            val produto = item.produto
            val termo = uiState.busca.trim()
            val combinaBusca = termo.isBlank() ||
                    produto.titulo.contains(termo, ignoreCase = true) ||
                    produto.descricao.contains(termo, ignoreCase = true) ||
                    produto.categoria.contains(termo, ignoreCase = true) ||
                    item.nomeLoja.contains(termo, ignoreCase = true)
            val combinaLoja = uiState.lojaSelecionada == null || item.nomeLoja == uiState.lojaSelecionada
            val combinaCategoria = uiState.categoriaSelecionada == null || produto.categoria == uiState.categoriaSelecionada
            val combinaPromocao = !uiState.apenasPromocoes || produto.emPromocao

            combinaBusca && combinaLoja && combinaCategoria && combinaPromocao
        }
    }
    val lojas = remember(produtosSalvador, uiState.busca, uiState.categoriaSelecionada, uiState.apenasPromocoes) {
        produtosSalvador
            .groupBy { it.nomeLoja.ifBlank { "Loja sem nome" } }
            .map { (nome, produtos) ->
                val primeiroProduto = produtos.firstOrNull()?.produto
                LojaVitrine(
                    nome = nome,
                    produtos = produtos,
                    categorias = produtos.map { it.produto.categoria }.filter { it.isNotBlank() }.distinct().sorted(),
                    cidade = primeiroProduto?.cidade.orEmpty(),
                    estado = primeiroProduto?.estado.orEmpty()
                )
            }
            .filter { loja ->
                val termo = uiState.busca.trim()
                val combinaBusca = termo.isBlank() ||
                        loja.nome.contains(termo, ignoreCase = true) ||
                        loja.categorias.any { it.contains(termo, ignoreCase = true) } ||
                        loja.produtos.any { it.produto.titulo.contains(termo, ignoreCase = true) }
                val combinaCategoria = uiState.categoriaSelecionada == null || uiState.categoriaSelecionada in loja.categorias
                val combinaPromocao = !uiState.apenasPromocoes || loja.totalPromocoes > 0

                combinaBusca && combinaCategoria && combinaPromocao
            }
            .sortedBy { it.nome }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF6F7F9))) {
        VitrineHeader(
            busca = uiState.busca,
            onBuscaChange = viewModel::atualizarBusca,
            totalProdutos = produtosSalvador.size,
            totalLojas = lojas.size,
            entrega = uiState.entregaSelecionada,
            modo = modo,
            onModoChange = { novoModo ->
                modo = novoModo
                if (novoModo == ModoVitrine.LOJAS) viewModel.selecionarLoja(null)
            }
        )

        when {
            uiState.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GoPrexOrange)
            }

            uiState.error != null && uiState.produtos.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(uiState.error ?: "Erro ao carregar vitrine", color = Color.Red)
            }

            produtosSalvador.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nenhuma loja disponivel em Salvador", color = Color.Gray, fontSize = 16.sp)
            }

            else -> Column(modifier = Modifier.fillMaxSize()) {
                FiltrosVitrine(
                    categorias = uiState.categorias,
                    categoriaSelecionada = uiState.categoriaSelecionada,
                    apenasPromocoes = uiState.apenasPromocoes,
                    entregaSelecionada = uiState.entregaSelecionada,
                    onCategoriaClick = viewModel::selecionarCategoria,
                    onPromocoesClick = viewModel::alternarPromocoes,
                    onEntregaClick = viewModel::selecionarEntrega
                )

                if (uiState.lojaSelecionada != null) {
                    LojaAbertaHeader(
                        nomeLoja = uiState.lojaSelecionada.orEmpty(),
                        totalProdutos = produtosFiltrados.size,
                        onVoltar = {
                            viewModel.selecionarLoja(null)
                            modo = ModoVitrine.LOJAS
                        }
                    )
                }

                when (modo) {
                    ModoVitrine.LOJAS -> ListaLojas(
                        lojas = lojas,
                        entrega = uiState.entregaSelecionada,
                        onEntrarLoja = { loja ->
                            viewModel.selecionarLoja(loja.nome)
                            modo = ModoVitrine.PRODUTOS
                        }
                    )

                    ModoVitrine.PRODUTOS -> ListaProdutos(
                        produtos = produtosFiltrados,
                        numberFormat = numberFormat,
                        entrega = uiState.entregaSelecionada,
                        onProdutoClick = { item ->
                            itemSelecionado = item
                            onProdutoClick(item.produto)
                        }
                    )
                }
            }
        }
    }

    if (uiState.pixPayment == null && uiState.compraCriada == null) itemSelecionado?.let { item ->
        ConfirmarCompraDialog(
            item = item,
            entrega = uiState.entregaSelecionada,
            numberFormat = numberFormat,
            isLoading = uiState.comprando,
            formaPagamento = formaPagamentoSelecionada,
            errorMessage = uiState.error,
            enderecos = uiState.enderecos,
            enderecoSelecionadoId = uiState.enderecoSelecionadoId,
            enderecosLoading = uiState.enderecosLoading,
            enderecosError = uiState.enderecosError,
            cartoes = uiState.cartoes,
            cartaoSelecionadoId = uiState.cartaoSelecionadoId,
            onEnderecoSelecionadoChange = viewModel::selecionarEndereco,
            onCadastrarEndereco = {
                context.startActivity(
                    Intent(context, tela_home_meus_dados::class.java)
                        .putExtra("abaInicial", 2)
                )
            },
            onFormaPagamentoChange = { formaPagamentoSelecionada = it },
            onCartaoSelecionadoChange = viewModel::selecionarCartao,
            onAtualizarCartoes = {
                if (loginData != null) viewModel.carregarCartoes(loginData)
            },
            onDismiss = { if (!uiState.comprando) itemSelecionado = null },
            onConfirmar = {
                if (loginData != null) {
                    viewModel.comprar(
                        item = item,
                        cliente = loginData,
                        formaPagamento = formaPagamentoSelecionada
                    )
                }
            }
        )
    }

    uiState.compraCriada?.let { pedido ->
        if (uiState.pixPayment != null) {
            PixPagamentoDialog(
                pedido = pedido,
                pixPayment = uiState.pixPayment!!,
                numberFormat = numberFormat,
                onDismiss = {
                    itemSelecionado = null
                    viewModel.limparPixPayment()
                }
            )
        } else {
            CompraCriadaDialog(
                pedido = pedido,
                numberFormat = numberFormat,
                errorMessage = uiState.error,
                onDismiss = {
                    itemSelecionado = null
                    viewModel.limparCompraCriada()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VitrineHeader(
    busca: String,
    onBuscaChange: (String) -> Unit,
    totalProdutos: Int,
    totalLojas: Int,
    entrega: EntregaRapida,
    modo: ModoVitrine,
    onModoChange: (ModoVitrine) -> Unit
) {
    Surface(modifier = Modifier.fillMaxWidth(), color = GoPrexDark, shadowElevation = 4.dp) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Shopping GoPrex", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(
                        "$totalLojas loja(s) e $totalProdutos produto(s) em Salvador",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                Surface(color = GoPrexOrange, shape = RoundedCornerShape(8.dp)) {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AccessTime, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(entrega.titulo, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            OutlinedTextField(
                value = busca,
                onValueChange = onBuscaChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Pesquisar lojas ou produtos") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = GoPrexOrange,
                    unfocusedBorderColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = modo == ModoVitrine.LOJAS,
                    onClick = { onModoChange(ModoVitrine.LOJAS) },
                    label = { Text("Ver lojas") },
                    leadingIcon = { Icon(Icons.Filled.Store, null, modifier = Modifier.size(16.dp)) }
                )
                FilterChip(
                    selected = modo == ModoVitrine.PRODUTOS,
                    onClick = { onModoChange(ModoVitrine.PRODUTOS) },
                    label = { Text("Ver produtos") },
                    leadingIcon = { Icon(Icons.Filled.ShoppingBag, null, modifier = Modifier.size(16.dp)) }
                )
            }
        }
    }
}

@Composable
private fun FiltrosVitrine(
    categorias: List<String>,
    categoriaSelecionada: String?,
    apenasPromocoes: Boolean,
    entregaSelecionada: EntregaRapida,
    onCategoriaClick: (String?) -> Unit,
    onPromocoesClick: () -> Unit,
    onEntregaClick: (EntregaRapida) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = apenasPromocoes,
                onClick = onPromocoesClick,
                label = { Text("Promocoes") },
                leadingIcon = { Icon(Icons.Filled.Star, null, modifier = Modifier.size(16.dp)) }
            )
        }
        item {
            FilterChip(
                selected = categoriaSelecionada == null,
                onClick = { onCategoriaClick(null) },
                label = { Text("Todas categorias") }
            )
        }
        lazyItems(categorias) { categoria ->
            FilterChip(
                selected = categoriaSelecionada == categoria,
                onClick = { onCategoriaClick(categoria) },
                label = { Text(categoria, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            )
        }
        lazyItems(EntregaRapida.values().toList()) { entrega ->
            FilterChip(
                selected = entregaSelecionada == entrega,
                onClick = { onEntregaClick(entrega) },
                label = { Text("${entrega.titulo} ${formatTaxa(entrega.taxa)}") },
                leadingIcon = { Icon(Icons.Filled.LocalShipping, null, modifier = Modifier.size(16.dp)) }
            )
        }
    }
}

@Composable
private fun LojaAbertaHeader(
    nomeLoja: String,
    totalProdutos: Int,
    onVoltar: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color.White, shadowElevation = 1.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onVoltar) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar")
            }
            Column {
                Text(nomeLoja, fontWeight = FontWeight.Bold, color = GoPrexDark, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$totalProdutos produto(s) disponiveis", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
private fun ListaLojas(
    lojas: List<LojaVitrine>,
    entrega: EntregaRapida,
    onEntrarLoja: (LojaVitrine) -> Unit
) {
    if (lojas.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nenhuma loja encontrada", color = Color.Gray)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        lazyItems(lojas) { loja ->
            LojaCard(loja = loja, entrega = entrega, onEntrar = { onEntrarLoja(loja) })
        }
    }
}

@Composable
private fun LojaCard(
    loja: LojaVitrine,
    entrega: EntregaRapida,
    onEntrar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEntrar() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.size(86.dp).clip(RoundedCornerShape(10.dp)).background(GoPrexOrange.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                if (loja.imagemCapa != null) {
                    Image(
                        painter = rememberAsyncImagePainter(loja.imagemCapa),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Filled.Store, null, tint = GoPrexOrange, modifier = Modifier.size(36.dp))
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(loja.nome, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = GoPrexDark, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    loja.categorias.take(3).joinToString(" • ").ifBlank { "Produtos variados" },
                    fontSize = 12.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${loja.totalProdutos} produtos", fontSize = 12.sp, color = GoPrexDark.copy(alpha = 0.75f))
                    if (loja.totalPromocoes > 0) Text("${loja.totalPromocoes} ofertas", fontSize = 12.sp, color = Color.Red)
                }
                EntregaResumo(entrega)
            }

            Button(onClick = onEntrar, colors = ButtonDefaults.buttonColors(containerColor = GoPrexOrange), shape = RoundedCornerShape(8.dp)) {
                Text("Entrar")
            }
        }
    }
}

@Composable
private fun ListaProdutos(
    produtos: List<ProdutoVitrine>,
    numberFormat: NumberFormat,
    entrega: EntregaRapida,
    onProdutoClick: (ProdutoVitrine) -> Unit
) {
    if (produtos.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nenhum produto encontrado", color = Color.Gray)
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(produtos, key = { "${it.nomeLoja}_${it.produto.id}" }) { item ->
            ProdutoVitrineCard(
                item = item,
                numberFormat = numberFormat,
                entrega = entrega,
                onClick = { onProdutoClick(item) }
            )
        }
    }
}

@Composable
private fun ProdutoVitrineCard(
    item: ProdutoVitrine,
    numberFormat: NumberFormat,
    entrega: EntregaRapida,
    onClick: () -> Unit
) {
    val produto = item.produto

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(135.dp)
                    .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                    .background(Color.Gray.copy(alpha = 0.1f))
            ) {
                if (produto.imagens.isNotEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(produto.imagens.first()),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                if (produto.emPromocao) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                        color = Color.Red,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Star, null, tint = Color.White, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("-${produto.porcentagemDesconto}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(produto.titulo, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, color = GoPrexDark)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Store, null, tint = GoPrexOrange, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(item.nomeLoja, fontSize = 12.sp, color = GoPrexDark.copy(alpha = 0.75f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                if (produto.emPromocao && produto.precoPromocional != null && produto.precoPromocional > 0) {
                    Text(numberFormat.format(produto.preco), fontSize = 12.sp, color = Color.Gray, textDecoration = TextDecoration.LineThrough)
                    Text(numberFormat.format(produto.precoPromocional), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Red)
                } else {
                    Text(numberFormat.format(produto.preco), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SuccessGreen)
                }

                EntregaResumo(entrega)
            }
        }
    }
}

@Composable
private fun EntregaResumo(entrega: EntregaRapida) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.LocalShipping, null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("${entrega.titulo} • ${formatTaxa(entrega.taxa)}", fontSize = 11.sp, color = SuccessGreen, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun formatTaxa(taxa: Double): String {
    return if (taxa <= 0.0) "gratis" else "R$ ${String.format(Locale("pt", "BR"), "%.2f", taxa)}"
}

@Composable
private fun ConfirmarCompraDialog(
    item: ProdutoVitrine,
    entrega: EntregaRapida,
    numberFormat: NumberFormat,
    isLoading: Boolean,
    formaPagamento: FormaPagamento,
    errorMessage: String?,
    enderecos: List<EnderecoEntrega>,
    enderecoSelecionadoId: String?,
    enderecosLoading: Boolean,
    enderecosError: String?,
    cartoes: List<StripeCard>,
    cartaoSelecionadoId: String?,
    onEnderecoSelecionadoChange: (String) -> Unit,
    onCadastrarEndereco: () -> Unit,
    onFormaPagamentoChange: (FormaPagamento) -> Unit,
    onCartaoSelecionadoChange: (String) -> Unit,
    onAtualizarCartoes: () -> Unit,
    onDismiss: () -> Unit,
    onConfirmar: () -> Unit
) {
    val produto = item.produto
    val valorProduto = produto.precoPromocional
        ?.takeIf { produto.emPromocao && it > 0.0 }
        ?: produto.preco
    val total = valorProduto + entrega.taxa

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.LocalShipping, null, tint = GoPrexOrange) },
        title = { Text("Confirmar compra", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(produto.titulo, fontWeight = FontWeight.Bold, color = GoPrexDark)
                Text("Loja: ${item.nomeLoja}", color = Color.Gray, fontSize = 13.sp)
                Text("Entrega: ${entrega.titulo} (${entrega.descricao})", color = SuccessGreen, fontWeight = FontWeight.Bold)
                Text("Produto: ${numberFormat.format(valorProduto)}")
                Text("Taxa: ${numberFormat.format(entrega.taxa)}")
                Text("Total: ${numberFormat.format(total)}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GoPrexOrange)
                EnderecosEntregaSection(
                    enderecos = enderecos,
                    enderecoSelecionadoId = enderecoSelecionadoId,
                    isLoading = enderecosLoading,
                    errorMessage = enderecosError,
                    enabled = !isLoading,
                    onEnderecoSelecionadoChange = onEnderecoSelecionadoChange,
                    onCadastrarEndereco = onCadastrarEndereco
                )
                Text("Forma de pagamento", fontWeight = FontWeight.Bold, color = GoPrexDark)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormaPagamentoChip(
                        forma = FormaPagamento.CARTAO,
                        selected = formaPagamento == FormaPagamento.CARTAO,
                        enabled = !isLoading,
                        onClick = { onFormaPagamentoChange(FormaPagamento.CARTAO) }
                    )
                    FormaPagamentoChip(
                        forma = FormaPagamento.PIX,
                        selected = formaPagamento == FormaPagamento.PIX,
                        enabled = false,
                        onClick = {}
                    )
                }
                Text(
                    if (formaPagamento == FormaPagamento.PIX) "Pix indisponivel no momento" else formaPagamento.descricao,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                if (formaPagamento == FormaPagamento.CARTAO) {
                    Text(
                        "Voce sera direcionado para o checkout seguro para informar ou confirmar os dados do cartao.",
                        fontSize = 12.sp,
                        color = GoPrexDark.copy(alpha = 0.75f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GoPrexOrange.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    )
                }
                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Red.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    )
                }
                Text("Disponivel apenas para Salvador. A entrega sera liberada apos o pagamento aprovado.", fontSize = 12.sp, color = Color.Gray)
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmar,
                enabled = !isLoading && enderecoSelecionadoId != null,
                colors = ButtonDefaults.buttonColors(containerColor = GoPrexOrange)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Ir para pagamento")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun EnderecosEntregaSection(
    enderecos: List<EnderecoEntrega>,
    enderecoSelecionadoId: String?,
    isLoading: Boolean,
    errorMessage: String?,
    enabled: Boolean,
    onEnderecoSelecionadoChange: (String) -> Unit,
    onCadastrarEndereco: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Endereco de entrega", fontWeight = FontWeight.Bold, color = GoPrexDark)
            if (enderecos.isEmpty() && !isLoading) {
                TextButton(onClick = onCadastrarEndereco, enabled = enabled) {
                    Text("Cadastrar Endereco")
                }
            }
        }

        when {
            isLoading -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = GoPrexOrange, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Carregando enderecos...", fontSize = 12.sp, color = Color.Gray)
            }

            !errorMessage.isNullOrBlank() -> Text(
                errorMessage,
                fontSize = 12.sp,
                color = Color.Red,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Red.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            )

            enderecos.isEmpty() -> Text(
                "Cadastre um endereco para continuar a compra.",
                fontSize = 12.sp,
                color = Color.Red,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Red.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            )

            else -> enderecos.forEach { endereco ->
                FilterChip(
                    selected = endereco.id == enderecoSelecionadoId,
                    onClick = { onEnderecoSelecionadoChange(endereco.id) },
                    enabled = enabled,
                    label = {
                        Column {
                            Text(
                                endereco.apelido.ifBlank { "Endereco" } + if (endereco.principal) " - Principal" else "",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                endereco.resumo(),
                                fontSize = 11.sp,
                                color = Color.Gray,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    leadingIcon = {
                        Icon(Icons.Filled.LocationOn, null, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun FormaPagamentoChip(
    forma: FormaPagamento,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        label = { Text(forma.titulo) },
        leadingIcon = {
            Icon(
                imageVector = if (forma == FormaPagamento.PIX) Icons.Filled.Pix else Icons.Filled.CreditCard,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }
    )
}

@Composable
private fun CartoesPagamentoSection(
    cartoes: List<StripeCard>,
    cartaoSelecionadoId: String?,
    enabled: Boolean,
    onCartaoSelecionadoChange: (String) -> Unit,
    onAtualizarCartoes: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Cartao para pagamento", fontWeight = FontWeight.Bold, color = GoPrexDark)
            TextButton(onClick = onAtualizarCartoes, enabled = enabled) {
                Text("Atualizar")
            }
        }

        if (cartoes.isEmpty()) {
            Text(
                "Nenhum cartao salvo. Voce pode continuar para preencher os dados no checkout seguro.",
                fontSize = 12.sp,
                color = GoPrexDark.copy(alpha = 0.75f),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GoPrexOrange.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            )
        } else {
            cartoes.forEach { card ->
                FilterChip(
                    selected = card.id == cartaoSelecionadoId,
                    onClick = { onCartaoSelecionadoChange(card.id) },
                    enabled = enabled,
                    label = {
                        Text(
                            cardResumo(card),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Filled.CreditCard, null, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun cardResumo(card: StripeCard): String {
    val brand = card.brand.ifBlank { "Cartao" }.replaceFirstChar { it.uppercase() }
    val apelido = card.apelido.takeIf { it.isNotBlank() }?.let { "$it - " }.orEmpty()
    return "$apelido$brand final ${card.last4}"
}

@Composable
private fun PixPagamentoDialog(
    pedido: Pedido,
    pixPayment: CreatePixPaymentResponse,
    numberFormat: NumberFormat,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val minutos = 5

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Pix, null, tint = SuccessGreen) },
        title = { Text("Pagar com Pix", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(pedido.produtoTitulo, fontWeight = FontWeight.Bold, color = GoPrexDark)
                Text("Total: ${numberFormat.format(pedido.valorTotal)}", color = GoPrexOrange, fontWeight = FontWeight.Bold)
                Text("Este Pix expira em $minutos minutos. Pague no app do seu banco usando o QR Code ou copia e cola.", fontSize = 12.sp, color = Color.Gray)

                if (pixPayment.pixQrCodeUrl.isNotBlank()) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(pixPayment.pixQrCodeUrl),
                            contentDescription = "QR Code Pix",
                            modifier = Modifier.size(220.dp)
                        )
                    }
                }

                if (pixPayment.pixCopiaECola.isNotBlank()) {
                    SelectionContainer {
                        Text(
                            text = pixPayment.pixCopiaECola,
                            fontSize = 12.sp,
                            color = GoPrexDark,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF1F3F5), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    clipboardManager.setText(AnnotatedString(pixPayment.pixCopiaECola))
                },
                enabled = pixPayment.pixCopiaECola.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = GoPrexOrange)
            ) {
                Icon(Icons.Filled.ContentCopy, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Copiar Pix")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    )
}

@Composable
private fun CompraCriadaDialog(
    pedido: Pedido,
    numberFormat: NumberFormat,
    errorMessage: String?,
    onDismiss: () -> Unit
) {
    val pagamentoAprovado = pedido.pagamentoStatus == "PAGO"
    val pagamentoRecusado = pedido.pagamentoStatus == "RECUSADO"
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.ShoppingBag, null, tint = if (pagamentoRecusado) Color.Red else SuccessGreen) },
        title = {
            Text(
                when {
                    pagamentoAprovado -> "Pagamento aprovado"
                    pagamentoRecusado -> "Erro no pagamento"
                    else -> "Pedido criado"
                },
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(pedido.produtoTitulo, fontWeight = FontWeight.Bold)
                Text("Prazo: ${pedido.prazoEntrega}")
                Text("Total: ${numberFormat.format(pedido.valorTotal)}", color = GoPrexOrange, fontWeight = FontWeight.Bold)
                if (pagamentoRecusado) {
                    Text(errorMessage ?: "O pagamento foi recusado. Confira o cartao ou tente outra forma de pagamento.", color = Color.Red, fontSize = 12.sp)
                } else {
                    Text(if (pagamentoAprovado) "Status: Pagamento Aprovado" else "Status: Aguardando Pagamento", color = if (pagamentoAprovado) SuccessGreen else GoPrexOrange, fontWeight = FontWeight.Bold)
                    Text("Proximos passos:", fontWeight = FontWeight.Bold, color = GoPrexDark)
                    Text("1. Produto em Preparacao", fontSize = 12.sp, color = Color.Gray)
                    Text("2. Produto liberado para Entrega", fontSize = 12.sp, color = Color.Gray)
                    Text("3. Produto em rota de Entrega", fontSize = 12.sp, color = Color.Gray)
                    Text("4. Produto Entregue", fontSize = 12.sp, color = Color.Gray)
                    Text("Acompanhe a evolucao em Minhas Compras.", fontSize = 12.sp, color = Color.Gray)
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = GoPrexOrange)) {
                Text("Ok")
            }
        }
    )
}
