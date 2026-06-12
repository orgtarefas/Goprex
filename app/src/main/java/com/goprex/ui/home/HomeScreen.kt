package com.goprex.ui.home

import android.net.Uri
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
import com.goprex.ui.theme.NavyBlue
import com.goprex.ui.theme.Orange
import kotlinx.coroutines.launch

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

    // Estado para controlar a atualização da foto
    var fotoUrl by remember { mutableStateOf(loginData.fotoUrl) }
    var showAvatarDialog by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // Define os itens do menu baseado no perfil
    val menuItems = remember(loginData.perfil) {
        getMenuItems(loginData.perfil)
    }

    // Lançador para selecionar imagem do avatar
    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            showAvatarDialog = true
        }
    }

    // Diálogo de confirmação para trocar avatar
    if (showAvatarDialog && selectedImageUri != null) {
        AlertDialog(
            onDismissRequest = {
                showAvatarDialog = false
                selectedImageUri = null
            },
            title = { Text("Atualizar Foto") },
            text = { Text("Deseja usar esta imagem como foto de perfil?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                // Aqui você precisará converter a URI para bytes
                                // Por enquanto, vamos fechar o diálogo
                                showAvatarDialog = false
                                selectedImageUri = null
                            } catch (e: Exception) {
                                showAvatarDialog = false
                                selectedImageUri = null
                            }
                        }
                    }
                ) {
                    Text("Sim")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAvatarDialog = false
                        selectedImageUri = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                // Cabeçalho do Drawer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NavyBlue)
                        .padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Avatar clicável
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Orange)
                                .clickable {
                                    avatarPickerLauncher.launch("image/*")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (fotoUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = fotoUrl,
                                    contentDescription = "Avatar",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = getInitials(loginData.nome),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Texto indicando que pode trocar a foto
                        Text(
                            text = "Toque para alterar",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 10.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = loginData.nome,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )

                        Text(
                            text = when (loginData.perfil) {
                                Perfil.ADMIN -> "Administrador"
                                Perfil.VENDEDOR -> "Vendedor"
                                Perfil.ENTREGADOR -> "Entregador"
                                Perfil.CLIENTE -> "Cliente"
                            },
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )

                        Text(
                            text = "${loginData.cidade} - ${loginData.estado}",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Itens do Menu
                menuItems.forEach { item ->
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = item.icone,
                                contentDescription = item.titulo
                            )
                        },
                        label = { Text(item.titulo) },
                        selected = false,
                        onClick = {
                            scope.launch {
                                drawerState.close()
                                // Navegar para a tela correspondente
                                when (item.titulo) {
                                    "Minhas Vendas" -> onNavigateToCadastroProduto()
                                    "Meus Dados" -> onNavigateToMeusDados()
                                    "Compra Rápida" -> onNavigateToCompras()
                                    "Entregas" -> onNavigateToEntregas()
                                    "Gestão de Vendas" -> onNavigateToGestaoVendas()
                                    "Gestão de Compras" -> onNavigateToGestaoCompras()
                                    "Gestão de Entregas" -> onNavigateToGestaoEntregas()
                                    "Gestão de Cadastros e Parceiros" -> onNavigateToGestaoCadastros()
                                }
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Botão Sair
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Sair"
                        )
                    },
                    label = { Text("Sair", color = Color.Red) },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            onLogout()
                        }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.logo),
                                contentDescription = "Logo",
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "GoPex",
                                color = Orange,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                drawerState.open()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu"
                            )
                        }
                    },
                    actions = {
                        // Botão de notificações
                        IconButton(onClick = { /* Notificações */ }) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notificações",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = NavyBlue,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            },
            containerColor = Color(0xFFF5F5F5)
        ) { paddingValues ->
            // Conteúdo principal
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card de boas-vindas
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = NavyBlue),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Olá, ${loginData.nome.split(" ").firstOrNull() ?: ""}!",
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Bem-vindo ao GoPex",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 14.sp
                                    )
                                }

                                // Ícone do perfil
                                Icon(
                                    imageVector = when (loginData.perfil) {
                                        Perfil.ADMIN -> Icons.Default.Star
                                        Perfil.VENDEDOR -> Icons.Default.ShoppingCart
                                        Perfil.ENTREGADOR -> Icons.Default.Email
                                        Perfil.CLIENTE -> Icons.Default.ShoppingCart
                                    },
                                    contentDescription = null,
                                    tint = Orange,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                    }
                }

                // Card de informações rápidas
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Status
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = if (loginData.status_ativo)
                                        Icons.Default.CheckCircle
                                    else
                                        Icons.Default.Close,
                                    contentDescription = null,
                                    tint = if (loginData.status_ativo) Color.Green else Color.Red,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (loginData.status_ativo) "Ativo" else "Inativo",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }

                            // Cidade
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Orange,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = loginData.cidade,
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }

                            // Perfil
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = NavyBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = when (loginData.perfil) {
                                        Perfil.ADMIN -> "Admin"
                                        Perfil.VENDEDOR -> "Vendedor"
                                        Perfil.ENTREGADOR -> "Entregador"
                                        Perfil.CLIENTE -> "Cliente"
                                    },
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }

                // Título da seção
                item {
                    Text(
                        text = "Menu Principal",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyBlue,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                // Menu em cards
                items(menuItems.size) { index ->
                    val item = menuItems[index]
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                when (item.titulo) {
                                    "Minhas Vendas" -> onNavigateToCadastroProduto()
                                    "Meus Dados" -> onNavigateToMeusDados()
                                    "Compra Rápida" -> onNavigateToCompras()
                                    "Entregas" -> onNavigateToEntregas()
                                    "Gestão de Vendas" -> onNavigateToGestaoVendas()
                                    "Gestão de Compras" -> onNavigateToGestaoCompras()
                                    "Gestão de Entregas" -> onNavigateToGestaoEntregas()
                                    "Gestão de Cadastros e Parceiros" -> onNavigateToGestaoCadastros()
                                }
                            },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Orange.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.icone,
                                    contentDescription = item.titulo,
                                    tint = Orange,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.titulo,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = NavyBlue
                                )
                                if (item.descricao.isNotEmpty()) {
                                    Text(
                                        text = item.descricao,
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = "Abrir",
                                tint = Color.Gray
                            )
                        }
                    }
                }

                // Espaço extra no final
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

fun getInitials(name: String): String {
    return name.split(" ")
        .take(2)
        .map { it.firstOrNull()?.toString() ?: "" }
        .joinToString("")
        .uppercase()
}

fun getMenuItems(perfil: Perfil): List<MenuItem> {
    return when (perfil) {
        Perfil.CLIENTE -> listOf(
            MenuItem("Compra Rápida", Icons.Default.ShoppingCart, "Produtos disponíveis e histórico"),
            MenuItem("Meus Dados", Icons.Default.Person, "Dados cadastrais, endereços, telefones")
        )

        Perfil.VENDEDOR -> listOf(
            MenuItem("Minhas Vendas", Icons.Default.ShoppingCart, "Cadastrar produtos e ver histórico"),
            MenuItem("Relatórios", Icons.Default.Star, "Em construção"),
            MenuItem("Meus Dados", Icons.Default.Person, "Dados cadastrais, endereços, telefones")
        )

        Perfil.ENTREGADOR -> listOf(
            MenuItem("Entregas", Icons.Default.Email, "Solicitações e histórico"),
            MenuItem("Relatórios", Icons.Default.Star, "Em construção"),
            MenuItem("Meus Dados", Icons.Default.Person, "Rotas de atuação, endereço, telefones")
        )

        Perfil.ADMIN -> listOf(
            MenuItem("Gestão de Vendas", Icons.Default.ShoppingCart, "Gerenciar vendas"),
            MenuItem("Gestão de Compras", Icons.Default.ShoppingCart, "Gerenciar compras"),
            MenuItem("Gestão de Entregas", Icons.Default.Email, "Gerenciar entregas"),
            MenuItem("Gestão de Cadastros e Parceiros", Icons.Default.Person, "Gerenciar usuários"),
            MenuItem("Relatórios", Icons.Default.Star, "Em construção"),
            MenuItem("Meus Dados", Icons.Default.Person, "Dados cadastrais, endereços, telefones")
        )
    }
}