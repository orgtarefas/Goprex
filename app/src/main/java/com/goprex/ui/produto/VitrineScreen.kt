package com.goprex.ui.produto

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.goprex.data.model.Produto
import com.goprex.ui.theme.GoPrexDark
import com.goprex.ui.theme.GoPrexOrange
import com.goprex.ui.theme.SuccessGreen
import java.text.NumberFormat
import java.util.Locale

val GoldColor = Color(0xFFFFD700)

@Composable
fun VitrineScreen(
    onProdutoClick: (Produto) -> Unit = {},
    viewModel: VitrineViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val numberFormat = remember { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }

    LaunchedEffect(Unit) { viewModel.carregarProdutos() }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Surface(modifier = Modifier.fillMaxWidth(), color = GoPrexDark, shadowElevation = 4.dp) {
            Text(
                "Vitrine de Produtos",
                modifier = Modifier.padding(16.dp),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GoPrexOrange)
                }
            }
            uiState.error != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(uiState.error!!, color = Color.Red)
                }
            }
            uiState.produtos.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhum produto disponível", color = Color.Gray, fontSize = 16.sp)
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.produtos, key = { it.id }) { produto ->
                        ProdutoVitrineCard(
                            produto = produto,
                            numberFormat = numberFormat,
                            onClick = { onProdutoClick(produto) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProdutoVitrineCard(
    produto: Produto,
    numberFormat: NumberFormat,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Imagem
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(Color.Gray.copy(alpha = 0.1f))
            ) {
                if (produto.imagens.isNotEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(produto.imagens.first()),
                        contentDescription = produto.titulo,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                // Tag de promoção
                if (produto.emPromocao) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                        color = Color.Red,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Star, null, tint = Color.White, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                "-${produto.porcentagemDesconto}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Informações
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    produto.titulo,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = GoPrexDark
                )
                Spacer(modifier = Modifier.height(4.dp))

                if (produto.emPromocao && produto.precoPromocional != null && produto.precoPromocional > 0) {
                    Text(
                        numberFormat.format(produto.preco),
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textDecoration = TextDecoration.LineThrough
                    )
                    Text(
                        numberFormat.format(produto.precoPromocional),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.Red
                    )
                } else {
                    Text(
                        numberFormat.format(produto.preco),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = SuccessGreen
                    )
                }

                // Categoria e loja
                if (produto.categoria.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        produto.categoria,
                        fontSize = 11.sp,
                        color = GoPrexOrange
                    )
                }
            }
        }
    }
}