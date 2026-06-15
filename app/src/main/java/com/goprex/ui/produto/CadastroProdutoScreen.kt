package com.goprex.ui.produto

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.goprex.data.model.Login
import com.goprex.ui.theme.GoPrexDark
import com.goprex.ui.theme.GoPrexOrange
import com.goprex.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Cadastrar Produto", color = Color.White, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Voltar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GoPrexDark)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
                    Text("Informações do Produto", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = GoPrexDark)

                    OutlinedTextField(
                        value = uiState.titulo,
                        onValueChange = { viewModel.updateTitulo(it) },
                        label = { Text("Título do Produto") },
                        placeholder = { Text("Ex: iPhone 15 Pro Max") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoPrexOrange,
                            unfocusedBorderColor = GoPrexDark.copy(alpha = 0.3f),
                            focusedLabelColor = GoPrexOrange,
                            unfocusedLabelColor = GoPrexDark.copy(alpha = 0.7f),
                            cursorColor = GoPrexOrange
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = uiState.descricao,
                        onValueChange = { viewModel.updateDescricao(it) },
                        label = { Text("Descrição") },
                        placeholder = { Text("Descreva o produto em detalhes...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        maxLines = 6,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoPrexOrange,
                            unfocusedBorderColor = GoPrexDark.copy(alpha = 0.3f),
                            focusedLabelColor = GoPrexOrange,
                            unfocusedLabelColor = GoPrexDark.copy(alpha = 0.7f),
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
                            label = { Text("Preço") },
                            placeholder = { Text("0,00") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            prefix = { Text("R$ ", color = GoPrexOrange) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoPrexOrange,
                                unfocusedBorderColor = GoPrexDark.copy(alpha = 0.3f),
                                focusedLabelColor = GoPrexOrange,
                                unfocusedLabelColor = GoPrexDark.copy(alpha = 0.7f),
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
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoPrexOrange,
                                unfocusedBorderColor = GoPrexDark.copy(alpha = 0.3f),
                                focusedLabelColor = GoPrexOrange,
                                unfocusedLabelColor = GoPrexDark.copy(alpha = 0.7f),
                                cursorColor = GoPrexOrange
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

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
                        Text("Imagens do Produto", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = GoPrexDark)
                        if (uiState.imagensUris.isNotEmpty()) {
                            Text("${uiState.imagensUris.size}/5", fontSize = 14.sp, color = GoPrexOrange, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (uiState.imagensUris.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            items(uiState.imagensUris) { uri ->
                                Box(modifier = Modifier.size(100.dp).clip(RoundedCornerShape(8.dp))) {
                                    Image(
                                        painter = rememberAsyncImagePainter(uri),
                                        contentDescription = "Imagem do produto",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    IconButton(
                                        onClick = { viewModel.removeImage(uri) },
                                        modifier = Modifier.align(Alignment.TopEnd).size(28.dp).background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                    ) {
                                        Icon(Icons.Default.Close, "Remover imagem", tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                    if (uiState.imagensUris.indexOf(uri) == 0) {
                                        Box(
                                            modifier = Modifier.align(Alignment.BottomStart)
                                                .background(GoPrexOrange.copy(alpha = 0.8f), RoundedCornerShape(topEnd = 4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("Capa", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(8.dp)).background(Color.Gray.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Add, null, tint = Color.Gray, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Nenhuma imagem adicionada", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }

                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = GoPrexOrange.copy(alpha = 0.1f), contentColor = GoPrexOrange),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Adicionar Imagens", fontWeight = FontWeight.Bold)
                    }

                    Text("Máximo 5 imagens • Formatos: JPG, PNG • A primeira imagem será a capa", fontSize = 11.sp, color = Color.Gray)
                }
            }

            if (uiState.error != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(uiState.error!!, color = MaterialTheme.colorScheme.error, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    viewModel.salvarProduto(
                        vendedorLogin = loginData.documentoId,
                        cidade = loginData.cidade,
                        estado = loginData.estado,
                        context = context
                    )
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !uiState.isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = GoPrexOrange, disabledContainerColor = GoPrexOrange.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                if (uiState.isLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Salvando...", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                } else {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Salvar Produto", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (uiState.isSuccess) {
        AlertDialog(
            onDismissRequest = { viewModel.clearSuccess(); onBack() },
            icon = { Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(48.dp)) },
            title = { Text("Produto Cadastrado!", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
            text = { Text("Seu produto foi cadastrado com sucesso e já está disponível para venda.", fontSize = 14.sp, color = Color.Gray) },
            confirmButton = {
                Button(onClick = { viewModel.clearSuccess(); onBack() }, colors = ButtonDefaults.buttonColors(containerColor = GoPrexOrange)) {
                    Text("OK")
                }
            }
        )
    }
}