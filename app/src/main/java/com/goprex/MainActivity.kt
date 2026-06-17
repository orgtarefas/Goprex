package com.goprex

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.goprex.data.model.Login
import com.goprex.ui.login.LoginScreen
import com.goprex.ui.telas.tela_home_meus_dados
import com.goprex.ui.theme.GoprexTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Verifica se já está logado
        val sharedPrefs = getSharedPreferences("goprex_prefs", Context.MODE_PRIVATE)
        val logado = sharedPrefs.getBoolean("logado", false)

        if (logado) {
            // Já logado - vai direto para home
            startActivity(Intent(this, tela_home_meus_dados::class.java))
            finish()
            return
        }

        setContent {
            GoprexTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var telaAtual by remember { mutableStateOf("LOGIN") }
                    var loginKey by remember { mutableStateOf(0) }

                    when (telaAtual) {
                        "LOGIN" -> {
                            key(loginKey) {
                                LoginScreen(
                                    onLoginSuccess = { login ->
                                        // Salva dados no SharedPreferences
                                        salvarDadosLogin(login)
                                        // Vai para a Activity home
                                        startActivity(Intent(this@MainActivity, tela_home_meus_dados::class.java))
                                        finish()
                                    }
                                )
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

            // Salva campos comuns de forma genérica
            login.getDados().forEach { (chave, valor) ->
                when (valor) {
                    is String -> putString(chave, valor)
                    is Long -> putLong(chave, valor)
                    is Int -> putInt(chave, valor)
                    is Boolean -> putBoolean(chave, valor)
                    is Float -> putFloat(chave, valor)
                    // Mapas e listas não salvamos no SharedPreferences
                }
            }
            apply()
        }
    }
}