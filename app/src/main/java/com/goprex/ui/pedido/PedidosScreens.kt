package com.goprex.ui.pedido

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.goprex.data.model.Login
import com.goprex.data.model.Pedido
import com.goprex.data.model.StatusPedido
import com.goprex.ui.theme.GoPrexDark
import com.goprex.ui.theme.GoPrexOrange
import com.goprex.ui.theme.SuccessGreen
import java.text.NumberFormat
import java.util.Locale

@Composable
fun MinhasComprasScreen(
    loginData: Login,
    viewModel: PedidosViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(loginData.documentoId) { viewModel.carregarCompras(loginData.documentoId) }

    PedidosList(
        titulo = "Minhas Compras",
        pedidos = uiState.pedidos,
        isLoading = uiState.isLoading,
        emptyText = "Nenhuma compra realizada"
    )
}

@Composable
fun MinhasEntregasScreen(
    loginData: Login,
    viewModel: PedidosViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(loginData.documentoId) { viewModel.carregarEntregas(loginData.documentoId) }
    val entregasAtivas = uiState.pedidos.filter {
        it.status in setOf(
            StatusPedido.ACEITO.name,
            StatusPedido.COLETANDO.name,
            StatusPedido.EM_ROTA.name
        )
    }

    EntregadorLocationTracker(enabled = entregasAtivas.isNotEmpty()) { location ->
        viewModel.atualizarLocalizacaoAtiva(
            pedidos = entregasAtivas,
            latitude = location.latitude,
            longitude = location.longitude
        )
    }

    val nf = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF6F7F9)),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { TituloSecao("Entregas disponiveis", "${uiState.disponiveis.size} aguardando aceite") }
        if (uiState.isLoading) {
            item { LoadingBox() }
        } else if (uiState.disponiveis.isEmpty()) {
            item { EmptyBox("Nenhuma entrega disponivel agora") }
        } else {
            items(uiState.disponiveis, key = { it.id }) { pedido ->
                PedidoCard(
                    pedido = pedido,
                    numberFormat = nf,
                    action = {
                        Button(
                            onClick = { viewModel.aceitarPedido(pedido.id, loginData) },
                            colors = ButtonDefaults.buttonColors(containerColor = GoPrexOrange),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Aceitar")
                        }
                    }
                )
            }
        }

        item { TituloSecao("Minhas rotas", "${uiState.pedidos.size} entrega(s)") }
        if (uiState.pedidos.isEmpty()) {
            item { EmptyBox("Voce ainda nao aceitou entregas") }
        } else {
            items(uiState.pedidos, key = { it.id }) { pedido ->
                PedidoCard(
                    pedido = pedido,
                    numberFormat = nf,
                    action = {
                        ProximaAcaoEntrega(
                            pedido = pedido,
                            onStatus = { status -> viewModel.atualizarStatus(pedido.id, status, loginData.documentoId) }
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun GestaoEntregasScreen(
    viewModel: PedidosViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.carregarGestao() }

    PedidosList(
        titulo = "Gestao de Entregas",
        pedidos = uiState.pedidos,
        isLoading = uiState.isLoading,
        emptyText = "Nenhum pedido no sistema"
    )
}

@Composable
fun GestaoComprasScreen(
    viewModel: PedidosViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.carregarGestao() }

    PedidosList(
        titulo = "Gestao de Compras",
        pedidos = uiState.pedidos,
        isLoading = uiState.isLoading,
        emptyText = "Nenhuma compra no sistema"
    )
}

@Composable
private fun PedidosList(
    titulo: String,
    pedidos: List<Pedido>,
    isLoading: Boolean,
    emptyText: String
) {
    val nf = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF6F7F9)),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { TituloSecao(titulo, "${pedidos.size} pedido(s)") }
        when {
            isLoading -> item { LoadingBox() }
            pedidos.isEmpty() -> item { EmptyBox(emptyText) }
            else -> items(pedidos, key = { it.id }) { pedido ->
                PedidoCard(pedido = pedido, numberFormat = nf)
            }
        }
    }
}

@Composable
private fun PedidoCard(
    pedido: Pedido,
    numberFormat: NumberFormat,
    action: @Composable (() -> Unit)? = null
) {
    var showMap by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.size(74.dp).clip(RoundedCornerShape(8.dp)).background(GoPrexOrange.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                if (pedido.produtoImagem.isNotBlank()) {
                    Image(
                        painter = rememberAsyncImagePainter(pedido.produtoImagem),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Filled.LocalShipping, null, tint = GoPrexOrange, modifier = Modifier.size(32.dp))
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(pedido.produtoTitulo, fontWeight = FontWeight.Bold, color = GoPrexDark, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Store, null, tint = GoPrexOrange, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(pedido.loja, fontSize = 12.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                StatusChip(pedido.status)
                Text("Entrega ${pedido.prazoEntrega} • taxa ${numberFormat.format(pedido.taxaEntrega)}", fontSize = 12.sp, color = SuccessGreen)
                Text("Total ${numberFormat.format(pedido.valorTotal)}", fontWeight = FontWeight.Bold, color = GoPrexOrange)
                Text("Estimativa: ${pedido.estimativaMinutos} min • ${String.format(Locale("pt", "BR"), "%.1f", pedido.distanciaEstimadaKm)} km em Salvador", fontSize = 11.sp, color = Color.Gray)
                if (pedido.entregadorNome.isNotBlank()) {
                    Text("Entregador: ${pedido.entregadorNome}", fontSize = 12.sp, color = GoPrexDark.copy(alpha = 0.75f))
                }
                if (pedido.temAlgumaCoordenada()) {
                    Button(
                        onClick = { showMap = true },
                        colors = ButtonDefaults.buttonColors(containerColor = GoPrexDark),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Acompanhar no mapa")
                    }
                }
                action?.invoke()
            }
        }
    }

    if (showMap) {
        AlertDialog(
            onDismissRequest = { showMap = false },
            title = { Text("Acompanhamento", fontWeight = FontWeight.Bold) },
            text = {
                PedidoMapPanel(pedido = pedido)
            },
            confirmButton = {
                TextButton(onClick = { showMap = false }) {
                    Text("Fechar")
                }
            }
        )
    }
}

@Composable
private fun ProximaAcaoEntrega(
    pedido: Pedido,
    onStatus: (StatusPedido) -> Unit
) {
    val status = runCatching { StatusPedido.valueOf(pedido.status) }.getOrDefault(StatusPedido.AGUARDANDO_ENTREGADOR)
    val proximo = when (status) {
        StatusPedido.ACEITO -> StatusPedido.COLETANDO
        StatusPedido.COLETANDO -> StatusPedido.EM_ROTA
        StatusPedido.EM_ROTA -> StatusPedido.ENTREGUE
        else -> null
    }

    if (proximo != null) {
        Button(
            onClick = { onStatus(proximo) },
            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(proximo.titulo)
        }
    }
}

@Composable
private fun StatusChip(statusName: String) {
    val status = runCatching { StatusPedido.valueOf(statusName) }.getOrDefault(StatusPedido.AGUARDANDO_ENTREGADOR)
    val color = when (status) {
        StatusPedido.ENTREGUE -> SuccessGreen
        StatusPedido.CANCELADO -> Color.Red
        StatusPedido.AGUARDANDO_ENTREGADOR -> GoPrexOrange
        else -> GoPrexDark
    }

    Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(6.dp)) {
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (status == StatusPedido.ENTREGUE) Icons.Filled.TaskAlt else Icons.Filled.AccessTime, null, tint = color, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(status.titulo, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TituloSecao(titulo: String, subtitulo: String) {
    Column {
        Text(titulo, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = GoPrexDark)
        Text(subtitulo, color = Color.Gray, fontSize = 13.sp)
    }
}

@Composable
private fun LoadingBox() {
    Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = GoPrexOrange)
    }
}

@Composable
private fun EmptyBox(text: String) {
    Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
        Text(text, color = Color.Gray)
    }
}

private fun Pedido.temAlgumaCoordenada(): Boolean {
    return (lojaLatitude != 0.0 && lojaLongitude != 0.0) ||
            (clienteLatitude != 0.0 && clienteLongitude != 0.0) ||
            (entregadorLatitude != 0.0 && entregadorLongitude != 0.0)
}
