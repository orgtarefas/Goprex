package com.goprex.ui.produto

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.goprex.data.model.Login
import com.goprex.ui.theme.GoPrexDark
import com.goprex.ui.theme.GoPrexOrange
import com.goprex.ui.theme.SuccessGreen

@Composable
fun CadastroProdutoScreen(
    loginData: Login,
    onBack: () -> Unit,
    viewModel: ProdutoViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.addImages(uris)
        }
    }

    // Efeito para voltar após sucesso
    LaunchedEffect(uiState.estadoCadastro) {
        if (uiState.estadoCadastro is CadastroState.Sucesso) {
            // Pequeno delay para mostrar o sucesso
            kotlinx.coroutines.delay(1500)
            viewModel.limparEstado()
            onBack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Indicador de progresso quando salvando
        AnimatedVisibility(
            visible = viewModel.isLoading,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = GoPrexOrange.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (val estado = uiState.estadoCadastro) {
                        is CadastroState.EnviandoImagens -> {
                            LinearProgressIndicator(
                                progress = { estado.progresso.toFloat() / estado.total },
                                modifier = Modifier.fillMaxWidth(),
                                color = GoPrexOrange,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Enviando imagem ${estado.progresso} de ${estado.total}",
                                fontSize = 14.sp,
                                color = GoPrexOrange,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        is CadastroState.SalvandoProduto -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = GoPrexOrange,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Salvando produto no catálogo...",
                                fontSize = 14.sp,
                                color = GoPrexOrange
                            )
                        }
                        else -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = GoPrexOrange,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
            }
        }

        // Card: Informações do Produto
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Info, null, tint = GoPrexOrange, modifier = Modifier.size(24.dp))
                    Text(
                        "Informações do Produto",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = GoPrexDark
                    )
                }

                OutlinedTextField(
                    value = uiState.titulo,
                    onValueChange = { viewModel.updateTitulo(it) },
                    label = { Text("Título do Produto *") },
                    placeholder = { Text("Ex: iPhone 15 Pro Max 256GB") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = uiState.camposValidados["titulo"] == false,
                    supportingText = if (uiState.camposValidados["titulo"] == false) {
                        { Text("Título é obrigatório", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    leadingIcon = { Icon(Icons.Default.Label, null, tint = GoPrexOrange) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoPrexOrange,
                        unfocusedBorderColor = GoPrexDark.copy(alpha = 0.3f),
                        focusedLabelColor = GoPrexOrange,
                        cursorColor = GoPrexOrange
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = uiState.descricao,
                    onValueChange = { viewModel.updateDescricao(it) },
                    label = { Text("Descrição *") },
                    placeholder = { Text("Descreva características, estado de conservação, etc...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 8,
                    isError = uiState.camposValidados["descricao"] == false,
                    supportingText = if (uiState.camposValidados["descricao"] == false) {
                        { Text("Descrição é obrigatória", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    leadingIcon = { Icon(Icons.Default.Description, null, tint = GoPrexOrange) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoPrexOrange,
                        unfocusedBorderColor = GoPrexDark.copy(alpha = 0.3f),
                        focusedLabelColor = GoPrexOrange,
                        cursorColor = GoPrexOrange
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.preco,
                        onValueChange = { viewModel.updatePreco(it) },
                        label = { Text("Preço *") },
                        placeholder = { Text("0,00") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        prefix = { Text("R$ ", color = GoPrexOrange, fontWeight = FontWeight.Bold) },
                        isError = uiState.camposValidados["preco"] == false,
                        leadingIcon = { Icon(Icons.Default.AttachMoney, null, tint = GoPrexOrange) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoPrexOrange,
                            unfocusedBorderColor = GoPrexDark.copy(alpha = 0.3f),
                            focusedLabelColor = GoPrexOrange,
                            cursorColor = GoPrexOrange
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = uiState.categoria,
                        onValueChange = { viewModel.updateCategoria(it) },
                        label = { Text("Categoria") },
                        placeholder = { Text("Ex: Eletrônicos") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        leadingIcon = { Icon(Icons.Default.Category, null, tint = GoPrexOrange) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoPrexOrange,
                            unfocusedBorderColor = GoPrexDark.copy(alpha = 0.3f),
                            focusedLabelColor = GoPrexOrange,
                            cursorColor = GoPrexOrange
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }

        // Card: Imagens
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Image, null, tint = GoPrexOrange, modifier = Modifier.size(24.dp))
                        Text(
                            "Imagens do Produto *",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = GoPrexDark
                        )
                    }
                    if (uiState.imagensUris.isNotEmpty()) {
                        Surface(
                            color = GoPrexOrange.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                "${uiState.imagensUris.size}/5",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                fontSize = 14.sp,
                                color = GoPrexOrange,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Grid de imagens
                if (uiState.imagensUris.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(uiState.imagensUris) { uri ->
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(2.dp, GoPrexOrange.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(uri),
                                    contentDescription = "Imagem do produto",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )

                                // Botão remover
                                IconButton(
                                    onClick = { viewModel.removeImage(uri) },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(28.dp)
                                        .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        "Remover imagem",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Indicador de capa
                                if (uiState.imagensUris.indexOf(uri) == 0) {
                                    Surface(
                                        modifier = Modifier.align(Alignment.BottomStart),
                                        color = GoPrexOrange,
                                        shape = RoundedCornerShape(topEnd = 8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Star,
                                                null,
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                "Capa",
                                                fontSize = 11.sp,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Placeholder vazio
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Gray.copy(alpha = 0.05f))
                            .border(2.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.AddPhotoAlternate,
                                null,
                                tint = Color.Gray.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Nenhuma imagem adicionada",
                                fontSize = 14.sp,
                                color = Color.Gray.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Toque no botão abaixo para adicionar",
                                fontSize = 12.sp,
                                color = Color.Gray.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                Button(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoPrexOrange.copy(alpha = 0.1f),
                        contentColor = GoPrexOrange
                    ),
                    shape = RoundedCornerShape(8.dp),
                    enabled = uiState.imagensUris.size < 5
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Adicionar Imagens", fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Máximo 5 imagens",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Text(
                        "Formatos: JPG, PNG",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        // Card: Erro
        AnimatedVisibility(
            visible = uiState.estadoCadastro is CadastroState.Erro,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            val mensagemErro = (uiState.estadoCadastro as? CadastroState.Erro)?.mensagem ?: ""

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Error,
                        null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Erro ao cadastrar",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp
                        )
                        Text(
                            mensagemErro,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Botão Salvar
        Button(
            onClick = {
                viewModel.salvarProduto(
                    vendedorLogin = loginData.documentoId,
                    cidade = loginData.getString("cidade"),
                    estado = loginData.getString("estado"),
                    nomeLoja = loginData.getString("loja"),
                    context = context
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !viewModel.isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = GoPrexOrange,
                disabledContainerColor = GoPrexOrange.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            if (viewModel.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Processando...", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            } else {
                Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (uiState.produtoEditando != null) "Atualizar Produto" else "Cadastrar Produto",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        // Botão Cancelar (secundário)
        if (!viewModel.isLoading) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GoPrexDark),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.ArrowBack, null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cancelar", fontWeight = FontWeight.Medium)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}