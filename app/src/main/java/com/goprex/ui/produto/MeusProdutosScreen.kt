package com.goprex.ui.produto

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.rememberAsyncImagePainter
import com.goprex.data.model.Login
import com.goprex.data.model.Produto
import com.goprex.data.repository.ImgBBRepository
import com.goprex.ui.theme.GoPrexDark
import com.goprex.ui.theme.GoPrexOrange
import com.goprex.ui.theme.SuccessGreen
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.text.NumberFormat
import java.util.Locale

val GoldColor = Color(0xFFFFD700)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeusProdutosScreen(
    loginData: Login,
    onBack: () -> Unit,
    onEditarProduto: (String) -> Unit = {},
    viewModel: MeusProdutosViewModel = viewModel()
) {
    var produtoEditando by remember { mutableStateOf<Produto?>(null) }
    var showEdicaoModal by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()
    val nomeLoja = loginData.getString("loja")
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val imgBBRepository = remember { ImgBBRepository() }

    var logoUrl by remember { mutableStateOf(loginData.getString("logoLoja")) }
    var isUploadingLogo by remember { mutableStateOf(false) }
    var produtoDetalhe by remember { mutableStateOf<Produto?>(null) }

    DisposableEffect(lifecycleOwner, nomeLoja) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && nomeLoja.isNotBlank()) {
                viewModel.carregarProdutos(nomeLoja)
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val logoPickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { selectedUri ->
            scope.launch {
                try {
                    isUploadingLogo = true
                    val inputStream = context.contentResolver.openInputStream(selectedUri)
                    val baos = ByteArrayOutputStream()
                    inputStream?.use { it.copyTo(baos) }
                    imgBBRepository.uploadImage(baos.toByteArray(), "logo_${nomeLoja}").fold(
                        onSuccess = { url -> logoUrl = url; viewModel.atualizarLogoLoja(nomeLoja, url, loginData.documentoId) },
                        onFailure = { }
                    )
                } catch (_: Exception) { } finally { isUploadingLogo = false }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(modifier = Modifier.fillMaxWidth(), color = GoPrexDark, shadowElevation = 4.dp) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Meus Produtos", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Loja: $nomeLoja", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                }
                Box(modifier = Modifier.size(70.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.1f)).clickable(enabled = !isUploadingLogo) { logoPickerLauncher.launch("image/*") }, contentAlignment = Alignment.Center) {
                    if (isUploadingLogo) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    else if (logoUrl.isNotEmpty()) Image(painter = rememberAsyncImagePainter(logoUrl), contentDescription = "Logo", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Store, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("LOGO", fontSize = 8.sp, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                        Text("LOJA", fontSize = 8.sp, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        when {
            uiState.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = GoPrexOrange) }
            uiState.error != null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Error, null, tint = Color.Red, modifier = Modifier.size(48.dp))
                    Text(uiState.error!!, color = Color.Red)
                    Button(onClick = { viewModel.carregarProdutos(nomeLoja) }) { Text("Tentar novamente") }
                }
            }
            uiState.produtos.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Inventory2, null, tint = Color.Gray.copy(alpha = 0.5f), modifier = Modifier.size(64.dp))
                    Text("Nenhum produto cadastrado", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                }
            }
            else -> LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = GoPrexOrange.copy(alpha = 0.1f)), shape = RoundedCornerShape(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${uiState.produtos.size} produto(s)", fontWeight = FontWeight.Bold, color = GoPrexOrange)
                            Text("${uiState.produtos.count { it.disponivel }} disponível(is)", color = GoPrexDark.copy(alpha = 0.7f))
                        }
                    }
                }
                items(uiState.produtos, key = { it.id }) { produto ->
                    ProdutoCard(
                        produto = produto,
                        onToggleDisponibilidade = { viewModel.toggleDisponibilidade(nomeLoja, produto) },
                        onTogglePromocao = { viewModel.togglePromocao(nomeLoja, produto) },
                        onEditar = { produtoEditando = produto; showEdicaoModal = true },
                        onDesativar = { viewModel.selecionarProduto(produto) },
                        onVerDetalhes = { produtoDetalhe = produto },
                        onAtualizarPromocao = { d, df -> viewModel.atualizarPromocao(nomeLoja, produto.id, d, df) }
                    )
                }
            }
        }
    }

    if (produtoDetalhe != null) ProdutoDetalheDialog(produto = produtoDetalhe!!, onDismiss = { produtoDetalhe = null }, onEditar = { produtoEditando = produtoDetalhe; produtoDetalhe = null; showEdicaoModal = true })

    if (uiState.produtoSelecionado != null) AlertDialog(
        onDismissRequest = { viewModel.selecionarProduto(null) },
        icon = { Icon(Icons.Default.Warning, null, tint = Color.Red) },
        title = { Text("Desativar Produto") },
        text = { Text("Deseja desativar \"${uiState.produtoSelecionado?.titulo}\"?") },
        confirmButton = { Button(onClick = { viewModel.desativarProduto(nomeLoja, uiState.produtoSelecionado!!.id) }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Desativar") } },
        dismissButton = { TextButton(onClick = { viewModel.selecionarProduto(null) }) { Text("Cancelar") } }
    )

    if (showEdicaoModal && produtoEditando != null) EditarProdutoModal(
        produto = produtoEditando!!, loginData = loginData,
        onDismiss = { showEdicaoModal = false; produtoEditando = null },
        onProdutoAtualizado = { showEdicaoModal = false; produtoEditando = null; viewModel.carregarProdutos(nomeLoja) }
    )
}

// ============ DIALOG DETALHES ============
@Composable
fun ProdutoDetalheDialog(produto: Produto, onDismiss: () -> Unit, onEditar: () -> Unit) {
    val nf = remember { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(produto.titulo, fontWeight = FontWeight.Bold, fontSize = 20.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (produto.imagens.isNotEmpty()) Box(modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)).background(Color.Gray.copy(alpha = 0.1f))) {
                    Image(painter = rememberAsyncImagePainter(produto.imagens.first()), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (produto.emPromocao && produto.precoPromocional != null && produto.precoPromocional > 0) {
                        Text(nf.format(produto.preco), fontSize = 16.sp, color = Color.Gray, textDecoration = TextDecoration.LineThrough)
                        Text(nf.format(produto.precoPromocional), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                        Surface(color = Color.Red, shape = RoundedCornerShape(4.dp)) { Text("-${produto.porcentagemDesconto}%", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White) }
                    } else Text(nf.format(produto.preco), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = produto.disponivel, onCheckedChange = { }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SuccessGreen, uncheckedThumbColor = Color.White, uncheckedTrackColor = Color.Red.copy(alpha = 0.5f)), modifier = Modifier.height(24.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (produto.disponivel) "Disponível" else "Indisponível", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (produto.disponivel) SuccessGreen else Color.Red)
                    }
                    if (produto.emPromocao) Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Star, null, tint = GoldColor, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Em Promoção", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GoldColor)
                    }
                }
                if (produto.categoria.isNotEmpty()) Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Category, null, tint = GoPrexOrange, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("Categoria: ${produto.categoria}", fontSize = 14.sp, color = GoPrexDark) }
                Divider()
                Text("Descrição", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = GoPrexDark)
                Text(produto.descricao.ifBlank { "Sem descrição" }, fontSize = 14.sp, color = GoPrexDark.copy(alpha = 0.8f), lineHeight = 20.sp)
            }
        },
        confirmButton = { Button(onClick = onEditar, colors = ButtonDefaults.buttonColors(containerColor = GoPrexOrange)) { Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("Editar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Fechar") } }
    )
}

// ============ CARD PRODUTO ============
@Composable
fun ProdutoCard(
    produto: Produto, onToggleDisponibilidade: () -> Unit, onTogglePromocao: () -> Unit,
    onEditar: () -> Unit, onDesativar: () -> Unit, onVerDetalhes: () -> Unit,
    onAtualizarPromocao: (Int, String) -> Unit = { _, _ -> }
) {
    val nf = remember { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }
    var showPromoDialog by remember { mutableStateOf(false) }
    var editandoValor by remember { mutableStateOf(false) }
    var desconto by remember { mutableStateOf(produto.porcentagemDesconto.toString()) }
    var valorFinal by remember { mutableStateOf(String.format("%.2f", produto.precoPromocional ?: produto.preco)) }
    var dataFim by remember { mutableStateOf(produto.dataFimPromocao ?: "") }

    val precoOriginal = produto.preco
    val descontoInt = desconto.toIntOrNull()?.coerceIn(0, 50) ?: 0
    val precoCalculado = if (editandoValor) valorFinal.toDoubleOrNull() ?: precoOriginal else precoOriginal - (precoOriginal * descontoInt / 100.0)
    val economia = precoOriginal - precoCalculado

    Card(modifier = Modifier.fillMaxWidth().clickable { onVerDetalhes() }, colors = CardDefaults.cardColors(containerColor = if (produto.disponivel) Color.White else Color.Gray.copy(alpha = 0.1f)), shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)).background(Color.Gray.copy(alpha = 0.2f))) {
                    if (produto.imagens.isNotEmpty()) Image(painter = rememberAsyncImagePainter(produto.imagens.first()), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else Icon(Icons.Default.Image, null, tint = Color.Gray, modifier = Modifier.fillMaxSize().padding(20.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(produto.titulo, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (produto.disponivel) GoPrexDark else Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    if (produto.emPromocao && produto.precoPromocional != null && produto.precoPromocional > 0) {
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(nf.format(produto.preco), fontSize = 12.sp, color = Color.Gray, textDecoration = TextDecoration.LineThrough)
                            Text(nf.format(produto.precoPromocional), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Red)
                        }
                    } else Text(nf.format(produto.preco), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = if (produto.disponivel) SuccessGreen else Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (produto.categoria.isNotEmpty()) Surface(color = GoPrexOrange.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) { Text(produto.categoria, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 11.sp, color = GoPrexOrange) }
                        if (produto.emPromocao) { Icon(Icons.Filled.Star, null, tint = GoldColor, modifier = Modifier.size(16.dp)); Text("-${produto.porcentagemDesconto}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Red) }
                    }
                    if (produto.descricao.isNotBlank()) { Spacer(modifier = Modifier.height(4.dp)); Text(produto.descricao, fontSize = 12.sp, color = Color.Gray, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Switch(checked = produto.disponivel, onCheckedChange = { onToggleDisponibilidade() }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SuccessGreen, uncheckedThumbColor = Color.White, uncheckedTrackColor = Color.Red.copy(alpha = 0.5f)), modifier = Modifier.height(24.dp))
                    IconButton(onClick = { showPromoDialog = true }, modifier = Modifier.size(34.dp)) { Icon(if (produto.emPromocao) Icons.Filled.Star else Icons.Outlined.StarOutline, "Promoção", tint = if (produto.emPromocao) GoldColor else Color.Gray, modifier = Modifier.size(20.dp)) }
                    IconButton(onClick = onEditar, modifier = Modifier.size(34.dp)) { Icon(Icons.Default.Edit, "Editar", tint = GoPrexOrange, modifier = Modifier.size(18.dp)) }
                    IconButton(onClick = onDesativar, modifier = Modifier.size(34.dp)) { Icon(Icons.Default.Delete, "Desativar", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(18.dp)) }
                }
            }
        }
    }

    // Dialog de Promoção com Preview
    if (showPromoDialog) AlertDialog(
        onDismissRequest = { showPromoDialog = false },
        title = { Text(if (produto.emPromocao) "Editar Promoção" else "Criar Promoção") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(colors = CardDefaults.cardColors(containerColor = GoPrexDark.copy(alpha = 0.05f)), shape = RoundedCornerShape(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Preço original:", fontSize = 14.sp, color = Color.Gray)
                        Text("R$ ${String.format("%.2f", precoOriginal)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GoPrexDark)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Editar por:", fontSize = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !editandoValor, onClick = { editandoValor = false }, label = { Text("% desconto") })
                        FilterChip(selected = editandoValor, onClick = { editandoValor = true }, label = { Text("Valor final") })
                    }
                }
                if (!editandoValor) {
                    OutlinedTextField(value = desconto, onValueChange = { val v = it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0; if (v <= 50) { desconto = v.toString(); valorFinal = String.format("%.2f", precoOriginal - (precoOriginal * v / 100.0)) } }, label = { Text("Desconto (máx 50%)") }, suffix = { Text("%") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                } else {
                    OutlinedTextField(value = valorFinal, onValueChange = { valorFinal = it.replace(",", ".").filter { c -> c.isDigit() || c == '.' }; val v = valorFinal.toDoubleOrNull(); if (v != null && v < precoOriginal) { desconto = ((precoOriginal - v) / precoOriginal * 100).toInt().coerceIn(0, 50).toString() } }, label = { Text("Valor final") }, prefix = { Text("R$ ") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                Card(colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.1f)), shape = RoundedCornerShape(8.dp)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Text("Preview:", fontSize = 12.sp, color = Color.Gray)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                            Text("R$ ${String.format("%.2f", precoOriginal)}", fontSize = 14.sp, color = Color.Gray, textDecoration = TextDecoration.LineThrough)
                            Text("R$ ${String.format("%.2f", precoCalculado)}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if (descontoInt > 0) Color.Red else SuccessGreen)
                            if (descontoInt > 0) Surface(color = Color.Red, shape = RoundedCornerShape(4.dp)) { Text("-${descontoInt}%", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White) }
                        }
                        if (economia > 0) Text("Economia: R$ ${String.format("%.2f", economia)}", fontSize = 12.sp, color = SuccessGreen)
                    }
                }
                OutlinedTextField(value = dataFim, onValueChange = { dataFim = it }, label = { Text("Data término") }, placeholder = { Text("31/12/2026") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (produto.emPromocao) TextButton(onClick = { onTogglePromocao(); showPromoDialog = false }) { Text("Remover promoção", color = Color.Red) }
            }
        },
        confirmButton = { Button(onClick = { onAtualizarPromocao(descontoInt, dataFim); showPromoDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = GoPrexOrange)) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = { showPromoDialog = false }) { Text("Cancelar") } }
    )
}
