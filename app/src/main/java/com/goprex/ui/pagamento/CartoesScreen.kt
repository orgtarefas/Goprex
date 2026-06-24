package com.goprex.ui.pagamento

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.goprex.data.model.Login
import com.goprex.data.model.StripeCard
import com.goprex.ui.theme.GoPrexDark
import com.goprex.ui.theme.GoPrexOrange
import com.goprex.ui.theme.SuccessGreen

@Composable
fun CartoesScreen(
    loginData: Login,
    viewModel: CartoesViewModel = viewModel()
) {
    CartoesContent(
        loginData = loginData,
        viewModel = viewModel,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F7F9))
            .padding(16.dp),
        showHeader = true,
        useLazyList = true
    )
}

@Composable
fun CartoesUsuarioSection(
    loginData: Login,
    viewModel: CartoesViewModel = viewModel()
) {
    CartoesContent(
        loginData = loginData,
        viewModel = viewModel,
        modifier = Modifier.fillMaxWidth(),
        showHeader = false,
        useLazyList = false
    )
}

@Composable
private fun CartoesContent(
    loginData: Login,
    viewModel: CartoesViewModel,
    modifier: Modifier,
    showHeader: Boolean,
    useLazyList: Boolean
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(loginData.documentoId) {
        viewModel.carregarCartoes(loginData)
    }

    DisposableEffect(lifecycleOwner, loginData.documentoId) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.carregarCartoes(loginData)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(uiState.setupUrl) {
        val url = uiState.setupUrl
        if (!url.isNullOrBlank()) {
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
            viewModel.limparSetupUrl()
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (showHeader) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Cartoes", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = GoPrexDark)
                    Text("Cadastre ate 3 cartoes para pagar compras com mais rapidez.", fontSize = 13.sp, color = Color.Gray)
                }
                IconButton(onClick = { viewModel.carregarCartoes(loginData) }, enabled = !uiState.isLoading) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Atualizar", tint = GoPrexDark)
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Formas de pagamento", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = GoPrexDark)
                    Text("Cartoes salvos para compras na GoPrex.", fontSize = 12.sp, color = Color.Gray)
                }
                IconButton(onClick = { viewModel.carregarCartoes(loginData) }, enabled = !uiState.isLoading) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Atualizar", tint = GoPrexDark)
                }
            }
        }

        Button(
            onClick = { viewModel.iniciarCadastroCartao(loginData) },
            enabled = !uiState.isLoading && uiState.cards.size < 3,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = GoPrexOrange),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Filled.AddCard, null)
            Spacer(modifier = Modifier.size(8.dp))
            Text(if (uiState.cards.size >= 3) "Limite de 3 cartoes atingido" else "Cadastrar cartao")
        }

        if (uiState.error != null) {
            Text(
                uiState.error.orEmpty(),
                color = Color.Red,
                fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth().background(Color.Red.copy(alpha = 0.08f), RoundedCornerShape(8.dp)).padding(10.dp)
            )
        }

        if (uiState.success != null) {
            Text(
                uiState.success.orEmpty(),
                color = SuccessGreen,
                fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth().background(SuccessGreen.copy(alpha = 0.08f), RoundedCornerShape(8.dp)).padding(10.dp)
            )
        }

        when {
            uiState.isLoading && uiState.cards.isEmpty() -> Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GoPrexOrange)
            }

            uiState.cards.isEmpty() -> Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.CreditCard, null, tint = GoPrexOrange, modifier = Modifier.size(36.dp))
                    Text("Nenhum cartao cadastrado", fontWeight = FontWeight.Bold, color = GoPrexDark)
                    Text("O cadastro e validado pela Stripe sem cobranca. Alguns bancos podem pedir confirmacao no app.", fontSize = 13.sp, color = Color.Gray)
                }
            }

            useLazyList -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(uiState.cards, key = { it.id }) { card ->
                    CartaoCard(
                        card = card,
                        enabled = !uiState.isLoading,
                        onSalvarApelido = { apelido -> viewModel.atualizarApelido(loginData, card.id, apelido) },
                        onRemover = { viewModel.removerCartao(loginData, card.id) }
                    )
                }
            }

            else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                uiState.cards.forEach { card ->
                    CartaoCard(
                        card = card,
                        enabled = !uiState.isLoading,
                        onSalvarApelido = { apelido -> viewModel.atualizarApelido(loginData, card.id, apelido) },
                        onRemover = { viewModel.removerCartao(loginData, card.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CartaoCard(
    card: StripeCard,
    enabled: Boolean,
    onSalvarApelido: (String) -> Unit,
    onRemover: () -> Unit
) {
    val apelidos = remember { mutableStateMapOf<String, String>() }
    val apelidoAtual = apelidos.getOrPut(card.id) { card.apelido }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Filled.CreditCard, null, tint = GoPrexOrange, modifier = Modifier.size(32.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(cardLabel(card), fontWeight = FontWeight.Bold, color = GoPrexDark, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Validade ${card.expMonth.toString().padStart(2, '0')}/${card.expYear}", fontSize = 12.sp, color = Color.Gray)
                }
                IconButton(onClick = onRemover, enabled = enabled) {
                    Icon(Icons.Filled.Delete, contentDescription = "Remover", tint = Color.Red)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = apelidoAtual,
                    onValueChange = { apelidos[card.id] = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    enabled = enabled,
                    label = { Text("Apelido opcional") }
                )
                Button(
                    onClick = { onSalvarApelido(apelidos[card.id].orEmpty()) },
                    enabled = enabled,
                    colors = ButtonDefaults.buttonColors(containerColor = GoPrexDark),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Filled.Save, null, modifier = Modifier.size(18.dp))
                }
            }

            TextButton(onClick = onRemover, enabled = enabled) {
                Text("Remover cartao", color = Color.Red)
            }
        }
    }
}

private fun cardLabel(card: StripeCard): String {
    val brand = card.brand.ifBlank { "Cartao" }.replaceFirstChar { it.uppercase() }
    val apelido = card.apelido.takeIf { it.isNotBlank() }?.let { "$it - " }.orEmpty()
    return "$apelido$brand final ${card.last4}"
}
