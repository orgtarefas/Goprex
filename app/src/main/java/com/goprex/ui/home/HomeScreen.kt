package com.goprex.ui.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.goprex.R
import com.goprex.data.model.Login
import com.goprex.data.model.Perfil
import com.goprex.data.repository.AuthRepository
import com.goprex.data.repository.ImgBBRepository
import com.goprex.ui.theme.BackgroundLight
import com.goprex.ui.theme.GoPrexDark
import com.goprex.ui.theme.GoPrexOrange
import com.goprex.ui.theme.SuccessGreen
import com.goprex.ui.theme.SurfaceWhite
import com.goprex.ui.theme.TextPrimary
import com.goprex.ui.theme.TextSecondary
import com.goprex.ui.theme.WhatsAppGreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.URLEncoder

data class MenuItem(
    val titulo: String,
    val icone: ImageVector,
    val descricao: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    loginData: Login,
    onLogout: () -> Unit,
    onNavigateToCadastroProduto: () -> Unit = {},
    onNavigateToMeusDados: () -> Unit = {},
    onNavigateToCompras: () -> Unit = {},
    onNavigateToEntregas: () -> Unit = {},
    onNavigateToGestaoVendas: () -> Unit = {},
    onNavigateToGestaoCompras: () -> Unit = {},
    onNavigateToGestaoEntregas: () -> Unit = {},
    onNavigateToGestaoCadastros: () -> Unit = {}
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val authRepository = remember { AuthRepository() }
    val imgBBRepository = remember { ImgBBRepository() }

    var fotoUrl by remember { mutableStateOf(loginData.fotoUrl) }
    var isUploadingAvatar by remember { mutableStateOf(false) }
    var avatarError by remember { mutableStateOf<String?>(null) }
    var isLoggingOut by remember { mutableStateOf(false) }
    var showSuporteDialog by remember { mutableStateOf(false) }

    val menuItems = remember(loginData.perfil) { getMenuItems(loginData.perfil) }

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            scope.launch {
                try {
                    isUploadingAvatar = true
                    avatarError = null
                    val inputStream: InputStream? = context.contentResolver.openInputStream(selectedUri)
                    val byteArrayOutputStream = ByteArrayOutputStream()
                    inputStream?.use { input ->
                        val buffer = ByteArray(1024)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            byteArrayOutputStream.write(buffer, 0, bytesRead)
                        }
                    }
                    val imageBytes = byteArrayOutputStream.toByteArray()
                    val result = imgBBRepository.uploadImage(imageBytes, "avatar_${loginData.documentoId}")
                    result.fold(
                        onSuccess = { url ->
                            authRepository.atualizarFotoUrl(loginData.documentoId, url)
                                .fold(
                                    onSuccess = { fotoUrl = url },
                                    onFailure = { e -> avatarError = "Erro ao salvar: ${e.message}" }
                                )
                        },
                        onFailure = { e -> avatarError = "Erro ao enviar: ${e.message}" }
                    )
                } catch (e: Exception) {
                    avatarError = "Erro: ${e.message}"
                } finally {
                    isUploadingAvatar = false
                }
            }
        }
    }

    fun abrirWhatsApp(telefone: Long, mensagem: String) {
        try {
            val url = "https://api.whatsapp.com/send?phone=55${telefone}&text=${URLEncoder.encode(mensagem, "UTF-8")}"
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            Log.e("HomeScreen", "Erro ao abrir WhatsApp: ${e.message}")
        }
    }

    fun fazerLogout() {
        scope.launch { drawerState.close() }
        scope.launch {
            delay(200)
            isLoggingOut = true

            val sharedPrefs = context.getSharedPreferences("goprex_prefs", Context.MODE_PRIVATE)
            sharedPrefs.edit().putBoolean("acabou_de_sair", true).apply()

            try { context.cacheDir.deleteRecursively() } catch (_: Exception) {}
            authRepository.logout()

            delay(500)
            isLoggingOut = false
            onLogout()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                Box(modifier = Modifier.fillMaxWidth().background(BackgroundLight).padding(24.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            IconButton(onClick = { scope.launch { drawerState.close() } }) {
                                Icon(Icons.Default.Close, "Fechar menu", tint = GoPrexDark)
                            }
                        }
                        Box(
                            modifier = Modifier.size(80.dp).clip(CircleShape).background(GoPrexOrange)
                                .clickable(enabled = !isUploadingAvatar) { avatarPickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isUploadingAvatar) {
                                CircularProgressIndicator(modifier = Modifier.size(40.dp), color = Color.White, strokeWidth = 3.dp)
                            } else if (fotoUrl.isNotEmpty()) {
                                AsyncImage(model = fotoUrl, contentDescription = "Avatar", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            } else {
                                Text(getInitials(loginData.nome), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(if (isUploadingAvatar) "Enviando..." else "Toque para alterar", color = TextSecondary, fontSize = 10.sp)
                        if (avatarError != null) Text(avatarError!!, color = Color.Red, fontSize = 10.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(loginData.nome, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(when (loginData.perfil) { Perfil.ADMIN -> "Administrador"; Perfil.VENDEDOR -> "Vendedor"; Perfil.ENTREGADOR -> "Entregador"; Perfil.CLIENTE -> "Cliente" }, color = TextSecondary, fontSize = 14.sp)
                        Text("${loginData.cidade} - ${loginData.estado}", color = TextSecondary, fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                menuItems.forEach { item ->
                    NavigationDrawerItem(
                        icon = { Icon(item.icone, item.titulo) },
                        label = { Text(item.titulo) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            when (item.titulo) {
                                "Minhas Vendas" -> onNavigateToCadastroProduto()
                                "Meus Dados" -> onNavigateToMeusDados()
                                "Compra Rápida" -> onNavigateToCompras()
                                "Entregas" -> onNavigateToEntregas()
                                "Gestão de Vendas" -> onNavigateToGestaoVendas()
                                "Gestão de Compras" -> onNavigateToGestaoCompras()
                                "Gestão de Entregas" -> onNavigateToGestaoEntregas()
                                "Gestão de Cadastros e Parceiros" -> onNavigateToGestaoCadastros()
                                "Suporte" -> showSuporteDialog = true
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.ExitToApp, "Sair") },
                    label = { Text("Sair", color = Color.Red) },
                    selected = false,
                    onClick = { fazerLogout() }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Image(painterResource(R.drawable.logo), "GoPrex", modifier = Modifier.height(40.dp).fillMaxWidth(0.4f))
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = { }) { Icon(Icons.Default.Notifications, "Notificações", tint = GoPrexDark, modifier = Modifier.size(22.dp)) }
                            IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Default.Menu, "Menu", tint = GoPrexDark, modifier = Modifier.size(24.dp)) }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundLight)
                )
            },
            containerColor = BackgroundLight
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = SurfaceWhite), elevation = CardDefaults.cardElevation(2.dp)) {
                        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Olá, ${loginData.nome.split(" ").firstOrNull() ?: ""}!", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Bem-vindo à GoPrex", color = TextSecondary, fontSize = 14.sp)
                            }
                            Icon(when (loginData.perfil) { Perfil.ADMIN -> Icons.Default.Star; Perfil.VENDEDOR -> Icons.Default.ShoppingCart; Perfil.ENTREGADOR -> Icons.Default.Email; Perfil.CLIENTE -> Icons.Default.ShoppingCart }, null, tint = GoPrexOrange, modifier = Modifier.size(36.dp))
                        }
                    }
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SurfaceWhite), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("Meus Dados", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, null, tint = GoPrexOrange, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column { Text("Nome", fontSize = 11.sp, color = TextSecondary); Text(loginData.nome, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary) }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, null, tint = GoPrexOrange, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column { Text("Perfil", fontSize = 11.sp, color = TextSecondary); Text(when (loginData.perfil) { Perfil.ADMIN -> "Administrador"; Perfil.VENDEDOR -> "Vendedor"; Perfil.ENTREGADOR -> "Entregador"; Perfil.CLIENTE -> "Cliente" }, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary) }
                            }
                            if (loginData.perfil == Perfil.VENDEDOR && loginData.loja.isNotEmpty()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ShoppingCart, null, tint = GoPrexOrange, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column { Text("Loja", fontSize = 11.sp, color = TextSecondary); Text(loginData.loja, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary) }
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, null, tint = GoPrexOrange, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column { Text("Àrea de Atuação", fontSize = 11.sp, color = TextSecondary); Text("${loginData.cidade} / ${loginData.estado}", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary) }
                            }
                            if (loginData.telefone > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Phone, null, tint = GoPrexOrange, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column { Text("Telefone", fontSize = 11.sp, color = TextSecondary); Text(formatPhone(loginData.telefone), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary) }
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    // Diálogo de Suporte
    if (showSuporteDialog) {
        AlertDialog(
            onDismissRequest = { showSuporteDialog = false },
            icon = { Icon(Icons.Default.Phone, null, tint = GoPrexOrange, modifier = Modifier.size(40.dp)) },
            title = { Text("Suporte GoPrex", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // WhatsApp
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(WhatsAppGreen.copy(alpha = 0.1f))
                            .clickable(enabled = loginData.telefone_whatsapp && loginData.telefone > 0) {
                                showSuporteDialog = false
                                abrirWhatsApp(loginData.telefone, "Olá, sou ${loginData.nome}, gostaria de ter suporte com a GoPrex.")
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Phone, null, tint = if (loginData.telefone_whatsapp) WhatsAppGreen else Color.Gray, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("WhatsApp", fontWeight = FontWeight.Bold, color = if (loginData.telefone_whatsapp) TextPrimary else Color.Gray)
                            Text(if (loginData.telefone_whatsapp) "Falar com suporte agora" else "Indisponível no momento", fontSize = 12.sp, color = TextSecondary)
                        }
                    }

                    // E-mail (desabilitado)
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color.Gray.copy(alpha = 0.05f)).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Email, null, tint = Color.Gray, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("E-mail", fontWeight = FontWeight.Bold, color = Color.Gray)
                            Text("Em breve", fontSize = 12.sp, color = TextSecondary)
                        }
                    }

                    // Chat (desabilitado)
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color.Gray.copy(alpha = 0.05f)).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Person, null, tint = Color.Gray, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Chat", fontWeight = FontWeight.Bold, color = Color.Gray)
                            Text("Em breve", fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSuporteDialog = false }) {
                    Text("Fechar", color = GoPrexOrange)
                }
            }
        )
    }

    // Diálogo de logout
    if (isLoggingOut) {
        AlertDialog(
            onDismissRequest = { },
            icon = { CircularProgressIndicator(modifier = Modifier.size(32.dp), color = GoPrexOrange, strokeWidth = 3.dp) },
            title = { Text("Saindo...", fontWeight = FontWeight.Bold) },
            text = { Text("Você está sendo desconectado da GoPrex.") },
            confirmButton = { },
            dismissButton = { }
        )
    }
}

fun getInitials(name: String): String {
    return name.split(" ").take(2).map { it.firstOrNull()?.toString() ?: "" }.joinToString("").uppercase()
}

fun formatPhone(phone: Long): String {
    val s = phone.toString()
    return when { s.length == 11 -> "(${s.substring(0,2)}) ${s.substring(2,7)}-${s.substring(7)}"; s.length == 10 -> "(${s.substring(0,2)}) ${s.substring(2,6)}-${s.substring(6)}"; else -> s }
}

fun getMenuItems(perfil: Perfil): List<MenuItem> {
    val suporte = MenuItem("Suporte", Icons.Default.Phone, "E-mail, Chat e WhatsApp")
    return when (perfil) {
        Perfil.CLIENTE -> listOf(MenuItem("Compra Rápida", Icons.Default.ShoppingCart, "Produtos disponíveis e histórico"), MenuItem("Meus Dados", Icons.Default.Person, "Dados cadastrais, endereços, telefones"), suporte)
        Perfil.VENDEDOR -> listOf(MenuItem("Minhas Vendas", Icons.Default.ShoppingCart, "Cadastrar produtos e ver histórico"), MenuItem("Relatórios", Icons.Default.Star, "Em construção"), MenuItem("Meus Dados", Icons.Default.Person, "Dados cadastrais, endereços, telefones"), suporte)
        Perfil.ENTREGADOR -> listOf(MenuItem("Entregas", Icons.Default.Email, "Solicitações e histórico"), MenuItem("Relatórios", Icons.Default.Star, "Em construção"), MenuItem("Meus Dados", Icons.Default.Person, "Rotas de atuação, endereço, telefones"), suporte)
        Perfil.ADMIN -> listOf(MenuItem("Gestão de Vendas", Icons.Default.ShoppingCart, "Gerenciar vendas"), MenuItem("Gestão de Compras", Icons.Default.ShoppingCart, "Gerenciar compras"), MenuItem("Gestão de Entregas", Icons.Default.Email, "Gerenciar entregas"), MenuItem("Gestão de Cadastros e Parceiros", Icons.Default.Person, "Gerenciar usuários"), MenuItem("Relatórios", Icons.Default.Star, "Em construção"), MenuItem("Meus Dados", Icons.Default.Person, "Dados cadastrais, endereços, telefones"), suporte)
    }
}