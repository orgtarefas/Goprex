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

// Cor dourada para estrela de promoção
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
    val scope = rememberCoroutineScope()
    val imgBBRepository = remember { ImgBBRepository() }

    var logoUrl by remember { mutableStateOf(loginData.getString("logoLoja")) }
    var isUploadingLogo by remember { mutableStateOf(false) }
    var produtoDetalhe by remember { mutableStateOf<Produto?>(null) }

    LaunchedEffect(nomeLoja) {
        viewModel.carregarProdutos(nomeLoja)
    }

    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            scope.launch {
                try {
                    isUploadingLogo = true
                    val inputStream: InputStream? = context.contentResolver.openInputStream(selectedUri)
                    val byteArrayOutputStream = ByteArrayOutputStream()
                    inputStream?.use { input ->
                        val buffer = ByteArray(4096)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            byteArrayOutputStream.write(buffer, 0, bytesRead)
                        }
                    }
                    val imageBytes = byteArrayOutputStream.toByteArray()
                    val result = imgBBRepository.uploadImage(imageBytes, "logo_${nomeLoja}")

                    result.fold(
                        onSuccess = { url ->
                            logoUrl = url
                            viewModel.atualizarLogoLoja(
                                nomeLoja = nomeLoja,
                                url = url,
                                documentoId = loginData.documentoId
                            )
                        },
                        onFailure = { }
                    )
                } catch (e: Exception) { } finally {
                    isUploadingLogo = false
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Cabeçalho com logo
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = GoPrexDark,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Meus Produtos", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Loja: $nomeLoja", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                }

                Box(
                    modifier = Modifier.size(70.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.1f))
                        .clickable(enabled = !isUploadingLogo) { logoPickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (isUploadingLogo) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    } else if (logoUrl.isNotEmpty()) {
                        Image(painter = rememberAsyncImagePainter(logoUrl), contentDescription = "Logo", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Store, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("LOGO", fontSize = 8.sp, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                            Text("LOJA", fontSize = 8.sp, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = GoPrexOrange)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Carregando produtos...", color = GoPrexDark)
                    }
                }
            }
            uiState.error != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Error, null, tint = Color.Red, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(uiState.error!!, color = Color.Red, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.carregarProdutos(nomeLoja) }) { Text("Tentar novamente") }
                    }
                }
            }
            uiState.produtos.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Inventory2, null, tint = Color.Gray.copy(alpha = 0.5f), modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Nenhum produto cadastrado", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Cadastre seu primeiro produto!", fontSize = 14.sp, color = Color.Gray.copy(alpha = 0.7f))
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = GoPrexOrange.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
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
                            onEditar = {
                                produtoEditando = produto
                                showEdicaoModal = true
                            },
                            onDesativar = { viewModel.selecionarProduto(produto) },
                            onVerDetalhes = { produtoDetalhe = produto },
                            onAtualizarPromocao = { desconto, dataFim ->
                                viewModel.atualizarPromocao(nomeLoja, produto.id, desconto, dataFim)
                            }
                        )
                    }
                }
            }
        }
    }

    if (produtoDetalhe != null) {
        ProdutoDetalheDialog(
            produto = produtoDetalhe!!,
            onDismiss = { produtoDetalhe = null },
            onEditar = { onEditarProduto(produtoDetalhe!!.id); produtoDetalhe = null }
        )
    }

    if (uiState.produtoSelecionado != null) {
        AlertDialog(
            onDismissRequest = { viewModel.selecionarProduto(null) },
            icon = { Icon(Icons.Default.Warning, null, tint = Color.Red) },
            title = { Text("Desativar Produto") },
            text = { Text("Deseja realmente desativar \"${uiState.produtoSelecionado?.titulo}\"?") },
            confirmButton = {
                Button(onClick = { viewModel.desativarProduto(nomeLoja, uiState.produtoSelecionado!!.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                    Text(if (uiState.isDeleting) "..." else "Desativar")
                }
            },
            dismissButton = { TextButton(onClick = { viewModel.selecionarProduto(null) }) { Text("Cancelar") } }
        )
    }
    // Modal de Edição do Produto
    if (showEdicaoModal && produtoEditando != null) {
        EditarProdutoModal(
            produto = produtoEditando!!,
            loginData = loginData,
            onDismiss = {
                showEdicaoModal = false
                produtoEditando = null
            },
            onProdutoAtualizado = {
                showEdicaoModal = false
                produtoEditando = null
                viewModel.carregarProdutos(nomeLoja) // Recarregar lista
            }
        )
    }

}

// ============ DIALOG DETALHES ============
@Composable
fun ProdutoDetalheDialog(produto: Produto, onDismiss: () -> Unit, onEditar: () -> Unit) {
    val numberFormat = remember { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(produto.titulo, fontWeight = FontWeight.Bold, fontSize = 20.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (produto.imagens.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)).background(Color.Gray.copy(alpha = 0.1f))) {
                        Image(painter = rememberAsyncImagePainter(produto.imagens.first()), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    }
                }

                // Preço
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (produto.emPromocao && produto.precoPromocional != null && produto.precoPromocional > 0) {
                        Text(numberFormat.format(produto.preco), fontSize = 16.sp, color = Color.Gray, textDecoration = TextDecoration.LineThrough)
                        Text(numberFormat.format(produto.precoPromocional), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                        Surface(color = Color.Red, shape = RoundedCornerShape(4.dp)) {
                            Text("-${produto.porcentagemDesconto}%", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    } else {
                        Text(numberFormat.format(produto.preco), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                    }
                }

                // Status com Switch e Estrela
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Switch de disponibilidade
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = produto.disponivel,
                            onCheckedChange = { },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = SuccessGreen,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color.Red.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.height(24.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (produto.disponivel) "Disponível" else "Indisponível",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (produto.disponivel) SuccessGreen else Color.Red
                        )
                    }

                    // Estrela de promoção
                    if (produto.emPromocao) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Star, null, tint = GoldColor, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Em Promoção", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GoldColor)
                        }
                    }
                }

                if (produto.categoria.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Category, null, tint = GoPrexOrange, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Categoria: ${produto.categoria}", fontSize = 14.sp, color = GoPrexDark)
                    }
                }

                Divider()
                Text("Descrição", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = GoPrexDark)
                Text(produto.descricao.ifBlank { "Sem descrição" }, fontSize = 14.sp, color = GoPrexDark.copy(alpha = 0.8f), lineHeight = 20.sp)
            }
        },
        confirmButton = {
            Button(onClick = onEditar, colors = ButtonDefaults.buttonColors(containerColor = GoPrexOrange)) {
                Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Editar Produto")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Fechar") } }
    )
}

// ============ CARD PRODUTO ============
@Composable
fun ProdutoCard(
    produto: Produto,
    onToggleDisponibilidade: () -> Unit,
    onTogglePromocao: () -> Unit,
    onEditar: () -> Unit,
    onDesativar: () -> Unit,
    onVerDetalhes: () -> Unit,
    onAtualizarPromocao: (Int, String) -> Unit = { _, _ -> }
) {
    val numberFormat = remember { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }
    var showPromoDialog by remember { mutableStateOf(false) }
    var desconto by remember { mutableStateOf(produto.porcentagemDesconto.toString()) }
    var dataFim by remember { mutableStateOf(produto.dataFimPromocao ?: "") }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onVerDetalhes() },
        colors = CardDefaults.cardColors(containerColor = if (produto.disponivel) Color.White else Color.Gray.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Imagem
                Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)).background(Color.Gray.copy(alpha = 0.2f))) {
                    if (produto.imagens.isNotEmpty()) {
                        Image(painter = rememberAsyncImagePainter(produto.imagens.first()), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Default.Image, null, tint = Color.Gray, modifier = Modifier.fillMaxSize().padding(20.dp))
                    }
                }

                // Informações
                Column(modifier = Modifier.weight(1f)) {
                    Text(produto.titulo, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (produto.disponivel) GoPrexDark else Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))

                    // Preço
                    if (produto.emPromocao && produto.precoPromocional != null && produto.precoPromocional > 0) {
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(numberFormat.format(produto.preco), fontSize = 12.sp, color = Color.Gray, textDecoration = TextDecoration.LineThrough)
                            Text(numberFormat.format(produto.precoPromocional), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Red)
                        }
                    } else {
                        Text(numberFormat.format(produto.preco), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = if (produto.disponivel) SuccessGreen else Color.Gray)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Tags
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (produto.categoria.isNotEmpty()) {
                            Surface(color = GoPrexOrange.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                                Text(produto.categoria, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 11.sp, color = GoPrexOrange)
                            }
                        }
                        if (produto.emPromocao) {
                            Icon(Icons.Filled.Star, null, tint = GoldColor, modifier = Modifier.size(16.dp))
                            Text("-${produto.porcentagemDesconto}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                        }
                    }

                    // Descrição
                    if (produto.descricao.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(produto.descricao, fontSize = 12.sp, color = Color.Gray, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }

                // Ações
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    // Switch Disponibilidade
                    Switch(
                        checked = produto.disponivel,
                        onCheckedChange = { onToggleDisponibilidade() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SuccessGreen,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color.Red.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.height(24.dp)
                    )

                    // Estrela Promoção
                    IconButton(onClick = { showPromoDialog = true }, modifier = Modifier.size(34.dp)) {
                        Icon(
                            if (produto.emPromocao) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = "Promoção",
                            tint = if (produto.emPromocao) GoldColor else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Editar
                    IconButton(onClick = onEditar, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = GoPrexOrange, modifier = Modifier.size(18.dp))
                    }

                    // Desativar
                    IconButton(onClick = onDesativar, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Desativar", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }

    // Dialog de Promoção
    if (showPromoDialog) {
        AlertDialog(
            onDismissRequest = { showPromoDialog = false },
            title = { Text(if (produto.emPromocao) "Editar Promoção" else "Criar Promoção") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = desconto,
                        onValueChange = { desconto = it.filter { c -> c.isDigit() } },
                        label = { Text("Porcentagem de desconto") },
                        suffix = { Text("%") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = dataFim,
                        onValueChange = { dataFim = it },
                        label = { Text("Data de término (DD/MM/AAAA)") },
                        placeholder = { Text("31/12/2026") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (produto.emPromocao) {
                        TextButton(onClick = {
                            onTogglePromocao()
                            showPromoDialog = false
                        }) {
                            Text("Remover promoção", color = Color.Red)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onAtualizarPromocao(desconto.toIntOrNull() ?: 0, dataFim)
                    showPromoDialog = false
                }, colors = ButtonDefaults.buttonColors(containerColor = GoPrexOrange)) {
                    Text("Salvar")
                }
            },
            dismissButton = { TextButton(onClick = { showPromoDialog = false }) { Text("Cancelar") } }
        )
    }
}