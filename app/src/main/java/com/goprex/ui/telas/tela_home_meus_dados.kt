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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.viewmodel.compose.viewModel
import com.goprex.data.model.EnderecoEntrega
import com.goprex.data.model.Login
import com.goprex.data.model.Loja
import com.goprex.data.model.LojaAtuacao
import com.goprex.data.model.LojaDisponivelEntrega
import com.goprex.data.model.Pedido
import com.goprex.data.model.StatusPedido
import com.goprex.ui.endereco.EnderecosViewModel
import com.goprex.ui.endereco.LojasAdminViewModel
import com.goprex.ui.endereco.LojasAtuacaoViewModel
import com.goprex.ui.menu.HeaderComMenu
import com.goprex.ui.pagamento.CartoesUsuarioSection
import com.goprex.ui.pagamento.RecebimentoScreen
import com.goprex.ui.pedido.PedidosViewModel
import com.goprex.ui.theme.GoPrexOrange
import com.goprex.ui.theme.GoprexTheme
import com.goprex.ui.theme.SurfaceWhite
import com.goprex.ui.theme.TextPrimary
import com.goprex.ui.theme.TextSecondary
import com.goprex.data.repository.EnderecoRepository
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.launch

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
    val isAdmin = isAdmin(loginData)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { BoasVindasCard(nome) }
        item {
            TabRow(selectedTabIndex = abaSelecionada) {
                Tab(selected = abaSelecionada == 0, onClick = { abaSelecionada = 0 }, text = { Text("Cadastro") })
                Tab(selected = abaSelecionada == 1, onClick = { abaSelecionada = 1 }, text = { Text("Pagamentos") })
                Tab(selected = abaSelecionada == 2, onClick = { abaSelecionada = 2 }, text = { Text(if (isAdmin) "Lojas" else "Enderecos") })
            }
        }

        when (abaSelecionada) {
            0 -> item { DadosCadastroCard(loginData) }
            1 -> {
                item { CartoesCard(loginData) }
                item { TransacoesUsuarioSection(loginData = loginData) }
            }
            2 -> item {
                if (isAdmin) {
                    LojasAdminCard()
                } else if (isEntregador(loginData)) {
                    LojasAtuacaoEntregadorCard(loginData)
                } else {
                    EnderecosUsuarioCard(loginData)
                }
            }
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
    val usaContaRecebimento = usaContaRecebimento(loginData)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            if (usaContaRecebimento) {
                RecebimentoScreen(loginData = loginData, embedded = true)
            } else {
                CartoesUsuarioSection(loginData = loginData)
            }
        }
    }
}

private fun usaContaRecebimento(loginData: Login): Boolean {
    val perfil = loginData.getString("perfil").lowercase()
    val descricaoPerfil = loginData.getString("descricaoPerfil").lowercase()
    val tipo = "$perfil $descricaoPerfil"
    return tipo.contains("vendedor") || tipo.contains("entregador") || tipo.contains("admin")
}

private fun isEntregador(loginData: Login): Boolean {
    val perfil = loginData.getString("perfil").lowercase()
    val descricaoPerfil = loginData.getString("descricaoPerfil").lowercase()
    return "$perfil $descricaoPerfil".contains("entregador")
}

private fun isAdmin(loginData: Login): Boolean {
    val perfil = loginData.getString("perfil").lowercase()
    val descricaoPerfil = loginData.getString("descricaoPerfil").lowercase()
    return "$perfil $descricaoPerfil".contains("admin")
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
private fun LojasAdminCard(
    viewModel: LojasAdminViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var lojaEditando by remember { mutableStateOf<Loja?>(null) }

    LaunchedEffect(Unit) {
        viewModel.carregar()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Lojas", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                    Text("Controle endereco e status das lojas.", color = TextSecondary, fontSize = 13.sp)
                }
                IconButton(onClick = { lojaEditando = Loja() }) {
                    Icon(Icons.Default.Add, contentDescription = "Cadastrar loja", tint = GoPrexOrange)
                }
            }

            when {
                uiState.isLoading -> CircularProgressIndicator(color = GoPrexOrange)
                uiState.lojas.isEmpty() -> Text("Nenhuma loja cadastrada", color = TextSecondary, fontSize = 13.sp)
                else -> uiState.lojas.forEach { loja ->
                    LojaAdminResumoCard(
                        loja = loja,
                        onEditar = { lojaEditando = loja }
                    )
                }
            }

            uiState.error?.let { Text(it, color = Color.Red, fontSize = 12.sp) }
            uiState.success?.let { Text(it, color = GoPrexOrange, fontSize = 12.sp) }
        }
    }

    lojaEditando?.let { loja ->
        LojaAdminDialog(
            loja = loja,
            isSaving = uiState.isSaving,
            onDismiss = {
                if (!uiState.isSaving) lojaEditando = null
            },
            onSalvar = { lojaSalvar ->
                viewModel.salvar(lojaSalvar)
                lojaEditando = null
            }
        )
    }
}

@Composable
private fun LojaAdminResumoCard(
    loja: Loja,
    onEditar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F7F9)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (loja.ativa) Icons.Default.CheckCircle else Icons.Default.Delete,
                    null,
                    tint = if (loja.ativa) GoPrexOrange else Color.Red,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(loja.nome, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(if (loja.ativa) "Ativa" else "Inativa", color = if (loja.ativa) GoPrexOrange else Color.Red, fontSize = 12.sp)
                    Text(loja.resumoEndereco().ifBlank { "Endereco nao informado" }, color = TextSecondary, fontSize = 12.sp)
                }
            }
            Button(
                onClick = onEditar,
                colors = ButtonDefaults.buttonColors(containerColor = GoPrexOrange),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Editar loja")
            }
        }
    }
}

@Composable
private fun LojaAdminDialog(
    loja: Loja,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSalvar: (Loja) -> Unit
) {
    val scope = rememberCoroutineScope()
    val enderecoRepository = remember { EnderecoRepository() }
    var nome by remember(loja.id) { mutableStateOf(loja.nome) }
    var cep by remember(loja.id) { mutableStateOf(loja.cep) }
    var logradouro by remember(loja.id) { mutableStateOf(loja.logradouro) }
    var numero by remember(loja.id) { mutableStateOf(loja.numero) }
    var complemento by remember(loja.id) { mutableStateOf(loja.complemento) }
    var bairro by remember(loja.id) { mutableStateOf(loja.bairro) }
    var cidade by remember(loja.id) { mutableStateOf(loja.cidade.ifBlank { "Salvador" }) }
    var estado by remember(loja.id) { mutableStateOf(loja.estado.ifBlank { "BA" }) }
    var ativa by remember(loja.id) { mutableStateOf(loja.ativa) }
    var buscandoCep by remember(loja.id) { mutableStateOf(false) }
    var erro by remember(loja.id) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (loja.id.isBlank()) "Cadastrar loja" else "Editar loja", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome da loja") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = loja.id.isBlank()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = ativa, onCheckedChange = { ativa = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (ativa) "Loja ativa" else "Loja inativa", color = TextPrimary)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = cep,
                        onValueChange = { cep = it.filter { c -> c.isDigit() }.take(8) },
                        label = { Text("CEP") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            if (cep.length != 8) {
                                erro = "Informe um CEP com 8 digitos"
                                return@Button
                            }
                            buscandoCep = true
                            erro = null
                            scope.launch {
                                enderecoRepository.buscarEnderecoPorCep(cep).fold(
                                    onSuccess = { endereco ->
                                        cep = endereco.cep
                                        logradouro = endereco.logradouro
                                        bairro = endereco.bairro
                                        cidade = endereco.cidade.ifBlank { cidade }
                                        estado = endereco.estado.ifBlank { estado }
                                        complemento = endereco.complemento.ifBlank { complemento }
                                        buscandoCep = false
                                    },
                                    onFailure = { e ->
                                        buscandoCep = false
                                        erro = e.message ?: "Nao foi possivel buscar o CEP"
                                    }
                                )
                            }
                        },
                        enabled = !buscandoCep,
                        colors = ButtonDefaults.buttonColors(containerColor = GoPrexOrange),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (buscandoCep) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Buscar")
                        }
                    }
                }
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
                erro?.let { Text(it, color = Color.Red, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nome.isBlank()) {
                        erro = "Informe o nome da loja"
                        return@Button
                    }
                    if (logradouro.isBlank() || numero.isBlank() || bairro.isBlank() || cidade.isBlank() || estado.isBlank()) {
                        erro = "Preencha rua, numero, bairro, cidade e UF"
                        return@Button
                    }
                    onSalvar(
                        loja.copy(
                            id = loja.id.ifBlank { nome.trim() },
                            nome = nome.trim(),
                            cep = cep.trim(),
                            logradouro = logradouro.trim(),
                            numero = numero.trim(),
                            complemento = complemento.trim(),
                            bairro = bairro.trim(),
                            cidade = cidade.trim(),
                            estado = estado.trim().uppercase(),
                            ativa = ativa
                        )
                    )
                },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = GoPrexOrange),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Salvar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun LojasAtuacaoEntregadorCard(
    loginData: Login,
    viewModel: LojasAtuacaoViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var lojaEditando by remember { mutableStateOf<LojaDisponivelEntrega?>(null) }

    LaunchedEffect(loginData.documentoId) {
        viewModel.carregar(loginData.documentoId)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Lojas de atuacao", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
            Text(
                "Selecione em quais lojas voce entrega e defina o limite de atendimento de cada endereco.",
                color = TextSecondary,
                fontSize = 13.sp
            )

            when {
                uiState.isLoading -> CircularProgressIndicator(color = GoPrexOrange)
                uiState.lojas.isEmpty() -> Text("Nenhuma loja disponivel para entrega", color = TextSecondary, fontSize = 13.sp)
                else -> uiState.lojas.forEach { loja ->
                    LojaAtuacaoResumoCard(
                        loja = loja,
                        atuacao = uiState.atuacoesPorLoja[loja.lojaId],
                        onConfigurar = { lojaEditando = loja }
                    )
                }
            }

            uiState.error?.let { Text(it, color = Color.Red, fontSize = 12.sp) }
            uiState.success?.let { Text(it, color = GoPrexOrange, fontSize = 12.sp) }
        }
    }

    lojaEditando?.let { loja ->
        val atuacaoAtual = uiState.atuacoesPorLoja[loja.lojaId]
        LojaAtuacaoDialog(
            loja = loja,
            atuacaoAtual = atuacaoAtual,
            isSaving = uiState.isSaving,
            onDismiss = {
                if (!uiState.isSaving) lojaEditando = null
            },
            onSalvar = { raioKm, atendeTodaCidade ->
                viewModel.salvar(loginData.documentoId, loja, raioKm, atendeTodaCidade)
                lojaEditando = null
            },
            onRemover = {
                viewModel.remover(loginData.documentoId, loja.lojaId)
                lojaEditando = null
            }
        )
    }
}

@Composable
private fun LojaAtuacaoResumoCard(
    loja: LojaDisponivelEntrega,
    atuacao: LojaAtuacao?,
    onConfigurar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F7F9)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, null, tint = GoPrexOrange, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(loja.nomeLoja, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(loja.enderecoResumo, color = TextSecondary, fontSize = 12.sp)
                    Text(
                        atuacao?.let { resumoAtuacao(it) } ?: "Voce ainda nao atua nesta loja",
                        color = if (atuacao == null) TextSecondary else GoPrexOrange,
                        fontSize = 12.sp,
                        fontWeight = if (atuacao == null) FontWeight.Normal else FontWeight.Medium
                    )
                }
            }
            Button(
                onClick = onConfigurar,
                colors = ButtonDefaults.buttonColors(containerColor = GoPrexOrange),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(if (atuacao == null) Icons.Default.Add else Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (atuacao == null) "Selecionar loja" else "Editar atuacao")
            }
        }
    }
}

@Composable
private fun LojaAtuacaoDialog(
    loja: LojaDisponivelEntrega,
    atuacaoAtual: LojaAtuacao?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSalvar: (Double, Boolean) -> Unit,
    onRemover: () -> Unit
) {
    var atendeTodaCidade by remember(loja.lojaId) { mutableStateOf(atuacaoAtual?.atendeTodaCidade ?: false) }
    var raioTexto by remember(loja.lojaId) {
        mutableStateOf((atuacaoAtual?.raioKm ?: 1.0).let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() })
    }
    var erro by remember(loja.lojaId) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configurar entrega", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(loja.nomeLoja, color = TextPrimary, fontWeight = FontWeight.Medium)
                Text(loja.enderecoResumo, color = TextSecondary, fontSize = 12.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = atendeTodaCidade, onCheckedChange = { atendeTodaCidade = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Atuar em toda a cidade/regiao da loja", color = TextPrimary, fontSize = 13.sp)
                }
                if (!atendeTodaCidade) {
                    OutlinedTextField(
                        value = raioTexto,
                        onValueChange = { valor ->
                            raioTexto = valor.filter { it.isDigit() || it == ',' || it == '.' }.take(6)
                            erro = null
                        },
                        label = { Text("Raio maximo (km)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
                erro?.let { Text(it, color = Color.Red, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val raio = if (atendeTodaCidade) {
                        0.0
                    } else {
                        raioTexto.replace(",", ".").toDoubleOrNull() ?: 0.0
                    }
                    if (!atendeTodaCidade && raio <= 0.0) {
                        erro = "Informe um raio maior que zero"
                        return@Button
                    }
                    onSalvar(raio, atendeTodaCidade)
                },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = GoPrexOrange),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Salvar")
                }
            }
        },
        dismissButton = {
            Row {
                if (atuacaoAtual != null) {
                    TextButton(onClick = onRemover, enabled = !isSaving) {
                        Text("Remover")
                    }
                }
                TextButton(onClick = onDismiss, enabled = !isSaving) {
                    Text("Cancelar")
                }
            }
        }
    )
}

private fun resumoAtuacao(atuacao: LojaAtuacao): String {
    return if (atuacao.atendeTodaCidade) {
        "Atua em toda a cidade/regiao da loja"
    } else {
        val raio = if (atuacao.raioKm % 1.0 == 0.0) atuacao.raioKm.toInt().toString() else atuacao.raioKm.toString()
        "Raio de atuacao: $raio km"
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
