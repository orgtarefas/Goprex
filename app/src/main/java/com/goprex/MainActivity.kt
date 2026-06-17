package com.goprex

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.goprex.data.model.Login
import com.goprex.ui.login.LoginScreen
import com.goprex.ui.telas.tela_home_meus_dados
import com.goprex.ui.theme.GoprexTheme
import com.goprex.ui.theme.GoPrexOrange
import com.goprex.ui.theme.SuccessGreen
import com.goprex.ui.theme.ErrorRed
import com.goprex.ui.theme.TextPrimary
import com.goprex.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sharedPrefs = getSharedPreferences("goprex_prefs", Context.MODE_PRIVATE)
        val logado = sharedPrefs.getBoolean("logado", false)

        if (logado) {
            startActivity(Intent(this, tela_home_meus_dados::class.java))
            finish()
            return
        }

        setContent {
            GoprexTheme {
                var tela by remember { mutableStateOf("LOGIN") }
                var loginData by remember { mutableStateOf<Login?>(null) }
                var statusLogin by remember { mutableStateOf("") }
                var erroLogin by remember { mutableStateOf("") }

                when (tela) {
                    "LOGIN" -> {
                        LoginScreen(
                            onLoginSuccess = { login ->
                                loginData = login

                                // Verifica o que foi recebido
                                val nome = login.getString("nome")
                                val perfil = login.getString("perfil")
                                val statusAtivo = login.getBoolean("status_ativo")

                                statusLogin = """
                                    ✅ Autenticação: OK
                                    ✅ Documento: ${login.documentoId}
                                    ✅ Nome: ${nome.ifEmpty { "NÃO ENCONTRADO" }}
                                    ✅ Perfil: ${perfil.ifEmpty { "NÃO ENCONTRADO" }}
                                    ✅ Status Ativo: $statusAtivo
                                    ✅ Total de campos: ${login.getDados().size}
                                """.trimIndent()

                                tela = "CONFIRMACAO"
                            }
                        )
                    }

                    "CONFIRMACAO" -> {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    "Login Realizado!",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SuccessGreen
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(4.dp)
                                ) {
                                    Text(
                                        text = statusLogin,
                                        modifier = Modifier.padding(16.dp),
                                        fontSize = 14.sp,
                                        color = TextPrimary,
                                        lineHeight = 22.sp
                                    )
                                }

                                if (erroLogin.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.1f))
                                    ) {
                                        Text(
                                            text = erroLogin,
                                            modifier = Modifier.padding(16.dp),
                                            fontSize = 14.sp,
                                            color = ErrorRed
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(32.dp))

                                Button(
                                    onClick = {
                                        try {
                                            // Salva dados
                                            salvarDadosLogin(loginData!!)

                                            // Tenta abrir home
                                            val intent = Intent(this@MainActivity, tela_home_meus_dados::class.java)
                                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                            startActivity(intent)
                                            finish()
                                        } catch (e: Exception) {
                                            erroLogin = "❌ ERRO ao abrir home:\n${e.message}\n\nTipo: ${e.javaClass.simpleName}"
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = GoPrexOrange),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Text("SIM - Ir para Home", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                OutlinedButton(
                                    onClick = {
                                        tela = "LOGIN"
                                        loginData = null
                                        statusLogin = ""
                                        erroLogin = ""
                                    },
                                    modifier = Modifier.fillMaxWidth().height(56.dp)
                                ) {
                                    Text("NÃO - Voltar ao Login", fontSize = 16.sp, color = TextSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun salvarDadosLogin(login: Login) {
        val sharedPrefs = getSharedPreferences("goprex_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().apply {
            putBoolean("logado", true)
            putString("documentoId", login.documentoId)
            login.getDados().forEach { (chave, valor) ->
                when (valor) {
                    is String -> putString(chave, valor)
                    is Long -> putLong(chave, valor)
                    is Int -> putInt(chave, valor)
                    is Boolean -> putBoolean(chave, valor)
                    is Float -> putFloat(chave, valor)
                    is Double -> putFloat(chave, valor.toFloat())
                }
            }
            apply()
        }
    }
}