package com.goprex.ui.telas

import android.os.Bundle
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.goprex.R
import com.goprex.data.model.Login
import com.goprex.data.model.MenuItem
import com.goprex.data.repository.AuthRepository
import com.goprex.data.repository.ImgBBRepository
import com.goprex.ui.theme.BackgroundLight
import com.goprex.ui.theme.GoPrexDark
import com.goprex.ui.theme.GoPrexOrange
import com.goprex.ui.theme.GoprexTheme
import com.goprex.ui.theme.SurfaceWhite
import com.goprex.ui.theme.TextPrimary
import com.goprex.ui.theme.TextSecondary
import com.goprex.ui.menu.MenuViewModel
import com.goprex.ui.menu.ModalSuporteDialog
import com.goprex.ui.menu.ModalGenericoDialog
import com.goprex.ui.menu.getIconForMenuItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.InputStream

class tela_home_meus_dados : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPrefs = getSharedPreferences("goprex_prefs", Context.MODE_PRIVATE)

        // Verifica se está logado
        if (!sharedPrefs.getBoolean("logado", false)) {
            startActivity(Intent(this, com.goprex.MainActivity::class.java))
            finish()
            return
        }

        val documentoId = sharedPrefs.getString("documentoId", "") ?: ""

        // Cria mapa com TODOS os dados salvos
        val dadosMap = mutableMapOf<String, Any?>()
        sharedPrefs.all.forEach { (chave, valor) ->
            if (chave != "logado" && chave != "documentoId") {
                dadosMap[chave] = valor
            }
        }

        val loginData = Login(
            documentoId = documentoId,
            dados = dadosMap
        )

        setContent {
            GoprexTheme {
                TelaHomeMeusDados(
                    loginData = loginData,
                    onLogout = {
                        sharedPrefs.edit().clear().apply()
                        val intent = Intent(this, com.goprex.MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaHomeMeusDados(
    loginData: Login,
    onLogout: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val authRepository = remember { AuthRepository() }
    val imgBBRepository = remember { ImgBBRepository() }
    val menuViewModel: MenuViewModel = viewModel()

    val dados = loginData.getDados()
    val nome = remember { dados["nome"]?.toString() ?: "" }
    val perfil = remember { dados["perfil"]?.toString() ?: "" }
    val descricaoPerfil = remember { dados["descricaoPerfil"]?.toString() ?: perfil }
    val cidade = remember { dados["cidade"]?.toString() ?: "" }
    val estado = remember { dados["estado"]?.toString() ?: "" }
    val loja = remember { dados["loja"]?.toString() ?: "" }
    val telefone = remember { (dados["telefone"] as? Number)?.toLong() ?: 0L }
    val fotoPerfil = remember { dados["fotoUrl"]?.toString() ?: "" }

    var fotoUrl by remember { mutableStateOf(fotoPerfil) }
    var isUploadingAvatar by remember { mutableStateOf(false) }
    var avatarError by remember { mutableStateOf<String?>(null) }
    var isLoggingOut by remember { mutableStateOf(false) }
    var modalAtivo by remember { mutableStateOf<String?>(null) }
    var modalTitulo by remember { mutableStateOf("") }

    val menuItemsFirebase by menuViewModel.itensMenu.collectAsState()
    val isLoadingMenu by menuViewModel.carregando.collectAsState()
    val menuError by menuViewModel.erro.collectAsState()

    LaunchedEffect(perfil) {
        if (perfil.isNotEmpty()) {
            menuViewModel.carregarMenu(perfil)
        }
    }

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
                            authRepository.atualizarCampo(loginData.documentoId, "fotoUrl", url)
                                .fold(
                                    onSuccess = { fotoUrl = url },
                                    onFailure = { avatarError = "Erro ao salvar" }
                                )
                        },
                        onFailure = { avatarError = "Erro ao enviar" }
                    )
                } catch (e: Exception) {
                    avatarError = "Erro"
                } finally {
                    isUploadingAvatar = false
                }
            }
        }
    }

    fun fazerLogout() {
        scope.launch { drawerState.close() }
        scope.launch {
            delay(200)
            isLoggingOut = true
            val sharedPrefs = context.getSharedPreferences("goprex_prefs", Context.MODE_PRIVATE)
            sharedPrefs.edit().clear().apply()
            try { context.cacheDir.deleteRecursively() } catch (_: Exception) {}
            authRepository.logout()
            delay(500)
            isLoggingOut = false
            onLogout()
        }
    }

    fun executarAcaoMenu(item: MenuItem) {
        scope.launch { drawerState.close() }

        when (item.rotaTipo) {
            "tela" -> {
                try {
                    val nomeClasse = item.rota
                    if (nomeClasse.contains("tela_home_meus_dados")) return
                    val intent = Intent()
                    intent.setClassName(context, "${context.packageName}.ui.telas.$nomeClasse")
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Log.e("TelaHome", "Erro ao abrir tela: ${item.rota}", e)
                }
            }
            "modal" -> {
                modalAtivo = item.rota
                modalTitulo = item.titulo
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                Box(
                    modifier = Modifier.fillMaxWidth().background(BackgroundLight).padding(24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            IconButton(onClick = { scope.launch { drawerState.close() } }) {
                                Icon(Icons.Default.Close, "Fechar", tint = GoPrexDark)
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
                                Text(
                                    nome.split(" ").take(2).map { it.firstOrNull()?.toString() ?: "" }.joinToString("").uppercase(),
                                    fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(if (isUploadingAvatar) "Enviando..." else "Toque para alterar", color = TextSecondary, fontSize = 10.sp)
                        if (avatarError != null) Text(avatarError!!, color = Color.Red, fontSize = 10.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(nome, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(descricaoPerfil, color = TextSecondary, fontSize = 14.sp)
                        if (cidade.isNotEmpty() || estado.isNotEmpty()) {
                            Text(
                                listOfNotNull(cidade.takeIf { it.isNotEmpty() }, estado.takeIf { it.isNotEmpty() }).joinToString(" - "),
                                color = TextSecondary, fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                when {
                    isLoadingMenu -> {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(modifier = Modifier.size(32.dp), color = GoPrexOrange, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Carregando menu...", fontSize = 12.sp, color = TextSecondary)
                            }
                        }
                    }
                    menuError != null -> {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            Text("Erro ao carregar menu", color = Color.Red, fontSize = 14.sp)
                        }
                    }
                    menuItemsFirebase.isEmpty() && !isLoadingMenu -> {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            Text("Nenhum menu disponível", color = TextSecondary, fontSize = 14.sp)
                        }
                    }
                    else -> {
                        menuItemsFirebase.forEach { itemMenu ->
                            NavigationDrawerItem(
                                icon = { Icon(getIconForMenuItem(itemMenu.rota), itemMenu.titulo) },
                                label = { Text(itemMenu.titulo) },
                                selected = false,
                                onClick = { executarAcaoMenu(itemMenu) }
                            )
                        }
                    }
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
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, "Menu", tint = GoPrexDark, modifier = Modifier.size(24.dp))
                            }
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
                    Card(
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite), elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Olá, ${nome.split(" ").firstOrNull() ?: ""}!", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Bem-vindo à GoPrex", color = TextSecondary, fontSize = 14.sp)
                            }
                            Icon(Icons.Default.Star, null, tint = GoPrexOrange, modifier = Modifier.size(36.dp))
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("Meus Dados", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)

                            if (nome.isNotEmpty()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Person, null, tint = GoPrexOrange, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Nome", fontSize = 11.sp, color = TextSecondary)
                                        Text(nome, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                                    }
                                }
                            }

                            if (descricaoPerfil.isNotEmpty()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, null, tint = GoPrexOrange, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Perfil", fontSize = 11.sp, color = TextSecondary)
                                        Text(descricaoPerfil, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                                    }
                                }
                            }

                            if (loja.isNotEmpty()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ShoppingCart, null, tint = GoPrexOrange, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Loja", fontSize = 11.sp, color = TextSecondary)
                                        Text(loja, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                                    }
                                }
                            }

                            if (cidade.isNotEmpty() || estado.isNotEmpty()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocationOn, null, tint = GoPrexOrange, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Localização", fontSize = 11.sp, color = TextSecondary)
                                        Text(
                                            listOfNotNull(cidade.takeIf { it.isNotEmpty() }, estado.takeIf { it.isNotEmpty() }).joinToString(" / "),
                                            fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary
                                        )
                                    }
                                }
                            }

                            if (telefone > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Phone, null, tint = GoPrexOrange, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Telefone", fontSize = 11.sp, color = TextSecondary)
                                        Text(formatPhone(telefone), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    if (modalAtivo != null) {
        when (modalAtivo) {
            "modal_suporte" -> {
                ModalSuporteDialog(
                    onDismiss = { modalAtivo = null },
                    telefoneUsuario = telefone,
                    nomeUsuario = nome
                )
            }
            else -> {
                ModalGenericoDialog(
                    titulo = modalTitulo,
                    onDismiss = { modalAtivo = null }
                )
            }
        }
    }

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

fun formatPhone(phone: Long): String {
    val s = phone.toString()
    return when {
        s.length == 11 -> "(${s.substring(0, 2)}) ${s.substring(2, 7)}-${s.substring(7)}"
        s.length == 10 -> "(${s.substring(0, 2)}) ${s.substring(2, 6)}-${s.substring(6)}"
        else -> s
    }
}