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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import com.goprex.data.model.Login
import com.goprex.data.model.Produto
import com.goprex.data.repository.ImgBBRepository
import com.goprex.data.repository.ProdutoRepository
import com.goprex.ui.theme.GoPrexDark
import com.goprex.ui.theme.GoPrexOrange
import com.goprex.ui.theme.SuccessGreen
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarProdutoModal(
    produto: Produto,
    loginData: Login,
    onDismiss: () -> Unit,
    onProdutoAtualizado: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val produtoRepository = remember { ProdutoRepository() }
    val imgBBRepository = remember { ImgBBRepository() }
    val nomeLoja = loginData.getString("loja")

    var titulo by remember { mutableStateOf(produto.titulo) }
    var descricao by remember { mutableStateOf(produto.descricao) }
    var preco by remember { mutableStateOf(produto.preco.toString()) }
    var categoria by remember { mutableStateOf(produto.categoria) }
    var imagensUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var imagensRemovidas by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }

    val totalImagensAtual = produto.imagens.size - imagensRemovidas.size + imagensUris.size

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val remaining = 5 - totalImagensAtual
            if (remaining > 0) {
                imagensUris = imagensUris + uris.take(remaining)
            }
        }
    }

    fun salvarEdicao() {
        if (titulo.isBlank()) { error = "Título é obrigatório"; return }
        if (preco.isBlank() || preco.toDoubleOrNull() == null || preco.toDouble() <= 0) { error = "Preço inválido"; return }

        scope.launch {
            isLoading = true
            error = null
            try {
                val novasUrls = mutableListOf<String>()
                imagensUris.forEach { uri ->
                    val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                    val byteArrayOutputStream = ByteArrayOutputStream()
                    inputStream?.use { input ->
                        val buffer = ByteArray(4096)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            byteArrayOutputStream.write(buffer, 0, bytesRead)
                        }
                    }
                    val imageBytes = byteArrayOutputStream.toByteArray()
                    imgBBRepository.uploadImage(imageBytes, "edit_${System.currentTimeMillis()}").fold(
                        onSuccess = { url -> novasUrls.add(url) },
                        onFailure = { }
                    )
                }

                val imagensFinais = produto.imagens.filter { it !in imagensRemovidas } + novasUrls
                if (imagensFinais.isEmpty()) { error = "Pelo menos uma imagem"; isLoading = false; return@launch }

                produtoRepository.atualizarProduto(nomeLoja, produto.id, mapOf(
                    "titulo" to titulo.trim(),
                    "descricao" to descricao.trim(),
                    "preco" to preco.toDouble(),
                    "categoria" to categoria.trim().ifBlank { "Geral" },
                    "imagens" to imagensFinais
                )).fold(
                    onSuccess = { isLoading = false; isSuccess = true },
                    onFailure = { e -> isLoading = false; error = "Erro: ${e.message}" }
                )
            } catch (e: Exception) {
                isLoading = false; error = "Erro: ${e.message}"
            }
        }
    }

    if (isSuccess) {
        AlertDialog(
            onDismissRequest = onProdutoAtualizado,
            icon = { Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(48.dp)) },
            title = { Text("Produto Atualizado!") },
            text = { Text("Alterações salvas com sucesso.") },
            confirmButton = { Button(onClick = onProdutoAtualizado, colors = ButtonDefaults.buttonColors(containerColor = GoPrexOrange)) { Text("OK") } }
        )
        return
    }

    Dialog(onDismissRequest = { if (!isLoading) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.9f), shape = RoundedCornerShape(16.dp), color = Color.White) {
            Column(modifier = Modifier.fillMaxSize()) {
                Surface(modifier = Modifier.fillMaxWidth(), color = GoPrexDark, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Editar Produto", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        IconButton(onClick = { if (!isLoading) onDismiss() }) { Icon(Icons.Default.Close, "Fechar", tint = Color.White) }
                    }
                }

                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = titulo, onValueChange = { titulo = it }, label = { Text("Título *") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoPrexOrange, cursorColor = GoPrexOrange, focusedLabelColor = GoPrexOrange), shape = RoundedCornerShape(8.dp))
                    OutlinedTextField(value = descricao, onValueChange = { descricao = it }, label = { Text("Descrição") }, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 5, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoPrexOrange, cursorColor = GoPrexOrange, focusedLabelColor = GoPrexOrange), shape = RoundedCornerShape(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(value = preco, onValueChange = { preco = it.replace(",", ".").filter { c -> c.isDigit() || c == '.' } }, label = { Text("Preço *") }, singleLine = true, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), prefix = { Text("R$ ", color = GoPrexOrange) }, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoPrexOrange, cursorColor = GoPrexOrange, focusedLabelColor = GoPrexOrange), shape = RoundedCornerShape(8.dp))
                        OutlinedTextField(value = categoria, onValueChange = { categoria = it }, label = { Text("Categoria") }, singleLine = true, modifier = Modifier.weight(1f), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoPrexOrange, cursorColor = GoPrexOrange, focusedLabelColor = GoPrexOrange), shape = RoundedCornerShape(8.dp))
                    }

                    // Imagens existentes
                    val imagensAtivas = produto.imagens.filter { it !in imagensRemovidas }
                    if (imagensAtivas.isNotEmpty()) {
                        Text("Imagens atuais (toque no X para remover)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GoPrexDark)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(imagensAtivas) { url ->
                                Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp))) {
                                    Image(painter = rememberAsyncImagePainter(url), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    IconButton(onClick = { imagensRemovidas = imagensRemovidas + url }, modifier = Modifier.align(Alignment.TopEnd).size(24.dp).background(Color.Black.copy(alpha = 0.6f), CircleShape)) {
                                        Icon(Icons.Default.Close, "Remover", tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Novas imagens
                    if (imagensUris.isNotEmpty()) {
                        Text("Novas imagens", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SuccessGreen)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(imagensUris) { uri ->
                                Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp))) {
                                    Image(painter = rememberAsyncImagePainter(uri), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    IconButton(onClick = { imagensUris = imagensUris - uri }, modifier = Modifier.align(Alignment.TopEnd).size(24.dp).background(Color.Black.copy(alpha = 0.6f), CircleShape)) {
                                        Icon(Icons.Default.Close, "Remover", tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }

                    OutlinedButton(onClick = { imagePickerLauncher.launch("image/*") }, modifier = Modifier.fillMaxWidth(), enabled = totalImagensAtual < 5) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Adicionar imagens (${totalImagensAtual}/5)")
                    }

                    if (error != null) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), shape = RoundedCornerShape(8.dp)) {
                            Text(error!!, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                        }
                    }
                }

                Surface(modifier = Modifier.fillMaxWidth(), shadowElevation = 8.dp, color = Color.White) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { if (!isLoading) onDismiss() }, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp)) { Text("Cancelar") }
                        Button(onClick = { salvarEdicao() }, modifier = Modifier.weight(1f).height(48.dp), enabled = !isLoading, colors = ButtonDefaults.buttonColors(containerColor = GoPrexOrange), shape = RoundedCornerShape(12.dp)) {
                            if (isLoading) { CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp) }
                            else { Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("Salvar", fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }
    }
}