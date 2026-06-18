package com.goprex.ui.produto

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.goprex.data.model.Login
import com.goprex.data.model.Produto
import com.goprex.ui.theme.GoPrexDark
import com.goprex.ui.theme.GoPrexOrange
import com.goprex.ui.theme.SuccessGreen
import java.text.NumberFormat
import java.util.Locale

@Composable
fun MeusProdutosScreen(
    loginData: Login,
    onBack: () -> Unit,
    onEditarProduto: (String) -> Unit = {},
    viewModel: MeusProdutosViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val nomeLoja = loginData.getString("loja")

    // Carregar produtos ao iniciar
    LaunchedEffect(nomeLoja) {
        viewModel.carregarProdutos(nomeLoja)
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Cabeçalho
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = GoPrexDark,
            shadowElevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Meus Produtos",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Loja: $nomeLoja",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        // Indicador de loading
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = GoPrexOrange)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Carregando produtos...", color = GoPrexDark)
                }
            }
        }
        // Mensagem de erro
        else if (uiState.error != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Error, null, tint = Color.Red, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(uiState.error!!, color = Color.Red, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.carregarProdutos(nomeLoja) }) {
                        Text("Tentar novamente")
                    }
                }
            }
        }
        // Lista vazia
        else if (uiState.produtos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Inventory2,
                        null,
                        tint = Color.Gray.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Nenhum produto cadastrado",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Cadastre seu primeiro produto!",
                        fontSize = 14.sp,
                        color = Color.Gray.copy(alpha = 0.7f)
                    )
                }
            }
        }
        // Lista de produtos
        else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Resumo
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = GoPrexOrange.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "${uiState.produtos.size} produto(s)",
                                fontWeight = FontWeight.Bold,
                                color = GoPrexOrange
                            )
                            Text(
                                "${uiState.produtos.count { it.disponivel }} disponível(is)",
                                color = GoPrexDark.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Lista de produtos
                items(uiState.produtos, key = { it.id }) { produto ->
                    ProdutoCard(
                        produto = produto,
                        onToggleDisponibilidade = {
                            viewModel.toggleDisponibilidade(nomeLoja, produto)
                        },
                        onEditar = {
                            onEditarProduto(produto.id)
                        },
                        onDesativar = {
                            viewModel.selecionarProduto(produto)
                        }
                    )
                }
            }
        }
    }

    // Dialog de confirmação para desativar
    if (uiState.produtoSelecionado != null) {
        AlertDialog(
            onDismissRequest = { viewModel.selecionarProduto(null) },
            icon = { Icon(Icons.Default.Warning, null, tint = Color.Red) },
            title = { Text("Desativar Produto") },
            text = {
                Text("Deseja realmente desativar \"${uiState.produtoSelecionado?.titulo}\"?\n\nO produto ficará indisponível para venda.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.desativarProduto(nomeLoja, uiState.produtoSelecionado!!.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    if (uiState.isDeleting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                    } else {
                        Text("Desativar")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.selecionarProduto(null) }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Snackbar de sucesso
    if (uiState.deleteSuccess) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(2000)
            viewModel.limparDeleteSuccess()
        }
    }
}

@Composable
fun ProdutoCard(
    produto: Produto,
    onToggleDisponibilidade: () -> Unit,
    onEditar: () -> Unit,
    onDesativar: () -> Unit
) {
    val numberFormat = remember { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (produto.disponivel) Color.White else Color.Gray.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Imagem do produto
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Gray.copy(alpha = 0.2f))
            ) {
                if (produto.imagens.isNotEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(produto.imagens.first()),
                        contentDescription = produto.titulo,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Image,
                        null,
                        tint = Color.Gray,
                        modifier = Modifier.fillMaxSize().padding(20.dp)
                    )
                }
            }

            // Informações
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    produto.titulo,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (produto.disponivel) GoPrexDark else Color.Gray
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    numberFormat.format(produto.preco),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (produto.disponivel) SuccessGreen else Color.Gray
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (produto.categoria.isNotEmpty()) {
                        Surface(
                            color = GoPrexOrange.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                produto.categoria,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 11.sp,
                                color = GoPrexOrange
                            )
                        }
                    }

                    Surface(
                        color = if (produto.disponivel) SuccessGreen.copy(alpha = 0.1f) else Color.Red.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            if (produto.disponivel) "Disponível" else "Indisponível",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 11.sp,
                            color = if (produto.disponivel) SuccessGreen else Color.Red
                        )
                    }
                }
            }

            // Ações
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onToggleDisponibilidade,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        if (produto.disponivel) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Disponibilidade",
                        tint = if (produto.disponivel) Color.Gray else SuccessGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onEditar,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = GoPrexOrange,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onDesativar,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Desativar",
                        tint = Color.Red.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}