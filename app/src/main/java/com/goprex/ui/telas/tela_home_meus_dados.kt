package com.goprex.ui.telas

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.goprex.data.model.EnderecoEntrega
import com.goprex.data.model.Login
import com.goprex.data.model.Pedido
import com.goprex.data.model.StatusPedido
import com.goprex.ui.endereco.EnderecosViewModel
import com.goprex.ui.menu.HeaderComMenu
import com.goprex.ui.pagamento.CartoesUsuarioSection
import com.goprex.ui.pedido.PedidosViewModel
import com.goprex.ui.theme.GoPrexOrange
import com.goprex.ui.theme.GoprexTheme
import com.goprex.ui.theme.SurfaceWhite
import com.goprex.ui.theme.TextPrimary
import com.goprex.ui.theme.TextSecondary
import java.text.NumberFormat
import java.util.Locale

class tela_home_meus_dados : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPrefs = getSharedPreferences("goprex_prefs", Context.MODE_PRIVATE)
        val documentoId = sharedPrefs.getString("documentoId", "") ?: ""
        val dadosMap = mutableMapOf<String, Any?>()
        sharedPrefs.all.forEach { (chave, valor) ->
            if (chave != "logado" && chave != "documentoId" && chave != "saved_login" && chave != "saved_password" && chave != "lembrar_dados" && chave != "acabou_de_sair") {
                dadosMap[chave] = valor
            }
        }
        val loginData = Login(documentoId = documentoId, dados = dadosMap)

        setContent {
            GoprexTheme {
                HeaderComMenu(
                    loginData = loginData,
                    titulo = "Meus Dados",
                    onLogout = {
                        sharedPrefs.edit().clear().apply()
                        startActivity(Intent(this, com.goprex.MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK })
                        finish()
                    },
                    conteudo = { ConteudoMeusDados(loginData, abaInicial = intent.getIntExtra("abaInicial", 0)) }
                )
            }
        }
    }
}

@Composable
fun ConteudoMeusDados(loginData: Login, abaInicial: Int = 0) {
    val dados = loginData.getDados()
    val nome = dados["nome"]?.toString() ?: ""
    var abaSelecionada by remember { mutableStateOf(abaInicial.coerceIn(0, 2)) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { BoasVindasCard(nome) }
        item {
            TabRow(selectedTabIndex = abaSelecionada) {
                Tab(selected = abaSelecionada == 0, onClick = { abaSelecionada = 0 }, text = { Text("Cadastro") })
                Tab(selected = abaSelecionada == 1, onClick = { abaSelecionada = 1 }, text = { Text("Pagamentos") })
                Tab(selected = abaSelecionada == 2, onClick = { abaSelecionada = 2 }, text = { Text("Enderecos") })
            }
        }

        when (abaSelecionada) {
            0 -> item { DadosCadastroCard(loginData) }
            1 -> {
                item { CartoesCard(loginData) }
                item { TransacoesUsuarioSection(loginData = loginData) }
            }
            2 -> item { EnderecosUsuarioCard(loginData) }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun BoasVindasCard(nome: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Ola, ${nome.split(" ").firstOrNull() ?: "Usuario"}!", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Bem-vindo a GoPrex", color = TextSecondary, fontSize = 14.sp)
            }
            Icon(Icons.Default.Star, null, tint = GoPrexOrange, modifier = Modifier.size(36.dp))
        }
    }
}

@Composable
private fun DadosCadastroCard(loginData: Login) {
    val dados = loginData.getDados()
    val nome = dados["nome"]?.toString() ?: ""
    val descricaoPerfil = dados["descricaoPerfil"]?.toString() ?: dados["perfil"]?.toString() ?: ""
    val loja = dados["loja"]?.toString() ?: ""
    val cidade = dados["cidade"]?.toString() ?: ""
    val estado = dados["estado"]?.toString() ?: ""
    val telefone = (dados["telefone"] as? Number)?.toLong() ?: 0L

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Meus Dados", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
            if (nome.isNotEmpty()) DadoLinha(Icons.Default.Person, "Nome", nome)
            if (descricaoPerfil.isNotEmpty()) DadoLinha(Icons.Default.Star, "Perfil", descricaoPerfil)
            if (loja.isNotEmpty()) DadoLinha(Icons.Default.ShoppingCart, "Loja", loja)
            if (cidade.isNotEmpty() || estado.isNotEmpty()) {
                DadoLinha(Icons.Default.LocationOn, "Localizacao", listOfNotNull(cidade.takeIf { it.isNotEmpty() }, estado.takeIf { it.isNotEmpty() }).joinToString(" / "))
            }
            if (telefone > 0) DadoLinha(Icons.Default.Phone, "Telefone", formatPhone(telefone))
        }
    }
}

@Composable
private fun DadoLinha(icon: androidx.compose.ui.graphics.vector.ImageVector, titulo: String, valor: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = GoPrexOrange, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(titulo, fontSize = 11.sp, color = TextSecondary)
            Text(valor, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
        }
    }
}

@Composable
private fun CartoesCard(loginData: Login) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            CartoesUsuarioSection(loginData = loginData)
        }
    }
}

@Composable
private fun EnderecosUsuarioCard(
    loginData: Login,
    viewModel: EnderecosViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var cep by remember { mutableStateOf("") }
    var apelido by remember { mutableStateOf("") }
    var logradouro by remember { mutableStateOf("") }
    var numero by remember { mutableStateOf("") }
    var complemento by remember { mutableStateOf("") }
    var bairro by remember { mutableStateOf("") }
    var cidade by remember { mutableStateOf(loginData.getString("cidade").ifBlank { "Salvador" }) }
    var estado by remember { mutableStateOf(loginData.getString("estado").ifBlank { "BA" }) }
    var principal by remember { mutableStateOf(false) }
    var erroFormulario by remember { mutableStateOf<String?>(null) }
    var mostrarFormulario by remember { mutableStateOf(false) }

    LaunchedEffect(loginData.documentoId) {
        viewModel.carregar(loginData.documentoId)
    }

    LaunchedEffect(uiState.enderecoCep) {
        val endereco = uiState.enderecoCep ?: return@LaunchedEffect
        cep = endereco.cep
        logradouro = endereco.logradouro
        if (complemento.isBlank()) complemento = endereco.complemento
        bairro = endereco.bairro
        cidade = endereco.cidade.ifBlank { cidade }
        estado = endereco.estado.ifBlank { estado }
    }

    fun limparFormulario() {
        cep = ""
        apelido = ""
        logradouro = ""
        numero = ""
        complemento = ""
        bairro = ""
        cidade = loginData.getString("cidade").ifBlank { "Salvador" }
        estado = loginData.getString("estado").ifBlank { "BA" }
        principal = false
        erroFormulario = null
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Enderecos de entrega", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                IconButton(
                    onClick = {
                        limparFormulario()
                        mostrarFormulario = true
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Cadastrar novo endereco", tint = GoPrexOrange)
                }
            }

            if (mostrarFormulario) {
                AlertDialog(
                    onDismissRequest = {
                        if (!uiState.isSaving) {
                            limparFormulario()
                            mostrarFormulario = false
                        }
                    },
                    title = { Text("Cadastrar endereco", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = cep,
                        onValueChange = { cep = it.filter { char -> char.isDigit() }.take(8) },
                        label = { Text("CEP") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(
                        onClick = { viewModel.buscarCep(cep) },
                        enabled = !uiState.isBuscandoCep && cep.length == 8,
                        colors = ButtonDefaults.buttonColors(containerColor = GoPrexOrange),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (uiState.isBuscandoCep) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Buscar")
                        }
                    }
                }
                OutlinedTextField(value = apelido, onValueChange = { apelido = it }, label = { Text("Apelido") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = logradouro, onValueChange = { logradouro = it }, label = { Text("Rua / Avenida") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = numero, onValueChange = { numero = it }, label = { Text("Numero") }, modifier = Modifier.weight(0.7f), singleLine = true)
                    OutlinedTextField(value = complemento, onValueChange = { complemento = it }, label = { Text("Complemento") }, modifier = Modifier.weight(1.3f), singleLine = true)
                }
                OutlinedTextField(value = bairro, onValueChange = { bairro = it }, label = { Text("Bairro") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = cidade, onValueChange = { cidade = it }, label = { Text("Cidade") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = estado, onValueChange = { estado = it.uppercase().take(2) }, label = { Text("UF") }, modifier = Modifier.weight(0.45f), singleLine = true)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = principal, onCheckedChange = { principal = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Definir como endereco principal", color = TextPrimary)
                }

                erroFormulario?.let { Text(it, color = Color.Red, fontSize = 12.sp) }
                uiState.error?.let { Text(it, color = Color.Red, fontSize = 12.sp) }
                uiState.success?.let { Text(it, color = GoPrexOrange, fontSize = 12.sp) }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val camposObrigatorios = listOf(cep, logradouro, numero, bairro, cidade, estado)
                            if (camposObrigatorios.any { it.isBlank() }) {
                                erroFormulario = "Preencha CEP, rua, numero, bairro, cidade e UF"
                                return@Button
                            }

                            viewModel.salvar(
                                clienteLogin = loginData.documentoId,
                                endereco = EnderecoEntrega(
                                    cep = cep.trim(),
                                    apelido = apelido.ifBlank { "Entrega" },
                                    logradouro = logradouro.trim(),
                                    numero = numero.trim(),
                                    complemento = complemento.trim(),
                                    bairro = bairro.trim(),
                                    cidade = cidade.trim(),
                                    estado = estado.trim().uppercase(),
                                    principal = principal
                                )
                            )
                            limparFormulario()
                            mostrarFormulario = false
                        },
                        enabled = !uiState.isSaving,
                        colors = ButtonDefaults.buttonColors(containerColor = GoPrexOrange),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Cadastrar Endereco")
                        }
                    }
                    TextButton(
                        onClick = {
                            limparFormulario()
                            mostrarFormulario = false
                        },
                        enabled = !uiState.isSaving
                    ) {
                        Text("Cancelar")
                    }
                }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {}
                )
            }

            if (uiState.isLoading) {
                CircularProgressIndicator(color = GoPrexOrange)
            } else if (uiState.enderecos.isEmpty()) {
                Text("Nenhum endereco cadastrado", color = TextSecondary, fontSize = 13.sp)
            } else {
                uiState.enderecos.forEach { endereco ->
                    EnderecoResumoCard(
                        endereco = endereco,
                        onPrincipal = { viewModel.definirPrincipal(loginData.documentoId, endereco.id) },
                        onRemover = { viewModel.remover(loginData.documentoId, endereco.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EnderecoResumoCard(
    endereco: EnderecoEntrega,
    onPrincipal: () -> Unit,
    onRemover: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F7F9)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (endereco.principal) Icons.Default.CheckCircle else Icons.Default.Home, null, tint = GoPrexOrange, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(endereco.apelido.ifBlank { "Endereco" }, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(endereco.resumo(), color = TextSecondary, fontSize = 12.sp)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onPrincipal, enabled = !endereco.principal) {
                    Text(if (endereco.principal) "Principal" else "Tornar principal")
                }
                TextButton(onClick = onRemover) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Remover")
                }
            }
        }
    }
}

@Composable
private fun TransacoesUsuarioSection(
    loginData: Login,
    viewModel: PedidosViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val nf = remember { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }

    LaunchedEffect(loginData.documentoId) {
        viewModel.carregarCompras(loginData.documentoId)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Historico de transacoes", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
            when {
                uiState.isLoading -> CircularProgressIndicator(color = GoPrexOrange)
                uiState.pedidos.isEmpty() -> Text("Nenhuma transacao encontrada", color = TextSecondary, fontSize = 13.sp)
                else -> uiState.pedidos.take(10).forEach { pedido ->
                    TransacaoResumo(pedido = pedido, total = nf.format(pedido.valorTotal))
                }
            }
        }
    }
}

@Composable
private fun TransacaoResumo(pedido: Pedido, total: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Receipt, null, tint = GoPrexOrange, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(pedido.produtoTitulo, color = TextPrimary, fontWeight = FontWeight.Medium, maxLines = 1)
            Text("${pedido.loja} - ${statusPagamentoResumo(pedido)}", color = TextSecondary, fontSize = 12.sp)
        }
        Text(total, color = GoPrexOrange, fontWeight = FontWeight.Bold)
    }
}

private fun statusPagamentoResumo(pedido: Pedido): String {
    return when {
        pedido.pagamentoStatus == "PAGO" -> "Pagamento aprovado"
        pedido.pagamentoStatus == "RECUSADO" -> "Pagamento recusado"
        pedido.status == StatusPedido.AGUARDANDO_PAGAMENTO.name -> "Aguardando pagamento"
        else -> pedido.pagamentoStatus.ifBlank { "Pendente" }
    }
}

fun formatPhone(phone: Long): String {
    val s = phone.toString()
    return when {
        s.length == 11 -> "(${s.substring(0, 2)}) ${s.substring(2, 7)}-${s.substring(7)}"
        s.length == 10 -> "(${s.substring(0, 2)}) ${s.substring(2, 6)}-${s.substring(6)}"
        else -> s
    }
}
