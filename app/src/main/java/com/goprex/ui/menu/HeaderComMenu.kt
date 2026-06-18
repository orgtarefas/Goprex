package com.goprex.ui.menu

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
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
import com.goprex.ui.theme.TextPrimary
import com.goprex.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeaderComMenu(
    loginData: Login,
    titulo: String,
    onLogout: () -> Unit,
    conteudo: @Composable (() -> Unit)? = null
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
        if (perfil.isNotEmpty()) menuViewModel.carregarMenu(perfil)
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
            val sp = context.getSharedPreferences("goprex_prefs", Context.MODE_PRIVATE)
            sp.edit().clear().apply()
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
                    val intent = Intent()
                    intent.setClassName(context, "${context.packageName}.ui.telas.${item.rota}")
                    context.startActivity(intent)
                } catch (_: Exception) { }
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
                // Header do menu
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BackgroundLight)
                        .padding(24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(onClick = { scope.launch { drawerState.close() } }) {
                                Icon(Icons.Default.Close, "Fechar", tint = GoPrexDark)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(GoPrexOrange)
                                .clickable(enabled = !isUploadingAvatar) {
                                    avatarPickerLauncher.launch("image/*")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isUploadingAvatar) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(40.dp),
                                    color = Color.White,
                                    strokeWidth = 3.dp
                                )
                            } else if (fotoUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = fotoUrl,
                                    contentDescription = "Avatar",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    nome.split(" ")
                                        .take(2)
                                        .map { it.firstOrNull()?.toString() ?: "" }
                                        .joinToString("")
                                        .uppercase()
                                        .ifEmpty { "?" },
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            if (isUploadingAvatar) "Enviando..." else "Toque para alterar",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                        if (avatarError != null) {
                            Text(avatarError!!, color = Color.Red, fontSize = 10.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            nome.ifEmpty { "Usuário" },
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(descricaoPerfil, color = TextSecondary, fontSize = 14.sp)
                        if (cidade.isNotEmpty() || estado.isNotEmpty()) {
                            Text(
                                listOfNotNull(
                                    cidade.takeIf { it.isNotEmpty() },
                                    estado.takeIf { it.isNotEmpty() }
                                ).joinToString(" - "),
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Menu
                when {
                    isLoadingMenu -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    color = GoPrexOrange,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Carregando menu...", fontSize = 12.sp, color = TextSecondary)
                            }
                        }
                    }
                    menuError != null -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Erro ao carregar menu", color = Color.Red, fontSize = 14.sp)
                        }
                    }
                    menuItemsFirebase.isEmpty() && !isLoadingMenu -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Nenhum menu disponível", color = TextSecondary, fontSize = 14.sp)
                        }
                    }
                    else -> {
                        menuItemsFirebase.forEach { itemMenu ->
                            NavigationDrawerItem(
                                icon = {
                                    Icon(
                                        getIconForMenuItem(itemMenu.rota),
                                        itemMenu.titulo
                                    )
                                },
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Image(
                                painterResource(R.drawable.logo),
                                "GoPrex",
                                modifier = Modifier
                                    .height(40.dp)
                                    .fillMaxWidth(0.4f)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(
                                    Icons.Default.Menu,
                                    "Menu",
                                    tint = GoPrexDark,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundLight)
                )
            },
            containerColor = BackgroundLight
        ) { paddingValues ->
            if (conteudo != null) {
                Box(modifier = Modifier.padding(paddingValues)) {
                    conteudo()
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Em Desenvolvimento\n$titulo",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // Modais
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
            icon = {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = GoPrexOrange,
                    strokeWidth = 3.dp
                )
            },
            title = { Text("Saindo...", fontWeight = FontWeight.Bold) },
            text = { Text("Você está sendo desconectado da GoPrex.") },
            confirmButton = { },
            dismissButton = { }
        )
    }
}