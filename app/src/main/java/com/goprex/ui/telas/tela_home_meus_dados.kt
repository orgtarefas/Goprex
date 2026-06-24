package com.goprex.ui.telas

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goprex.data.model.Login
import com.goprex.ui.menu.HeaderComMenu
import com.goprex.ui.pagamento.CartoesUsuarioSection
import com.goprex.ui.theme.GoPrexOrange
import com.goprex.ui.theme.GoprexTheme
import com.goprex.ui.theme.SurfaceWhite
import com.goprex.ui.theme.TextPrimary
import com.goprex.ui.theme.TextSecondary

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
                    conteudo = {
                        ConteudoMeusDados(loginData)
                    }
                )
            }
        }
    }
}

@Composable
fun ConteudoMeusDados(loginData: Login) {
    val dados = loginData.getDados()
    val nome = dados["nome"]?.toString() ?: ""
    val descricaoPerfil = dados["descricaoPerfil"]?.toString() ?: dados["perfil"]?.toString() ?: ""
    val loja = dados["loja"]?.toString() ?: ""
    val cidade = dados["cidade"]?.toString() ?: ""
    val estado = dados["estado"]?.toString() ?: ""
    val telefone = (dados["telefone"] as? Number)?.toLong() ?: 0L

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite), elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Olá, ${nome.split(" ").firstOrNull() ?: "Usuário"}!", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
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
        item {
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
        item { Spacer(modifier = Modifier.height(16.dp)) }
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
