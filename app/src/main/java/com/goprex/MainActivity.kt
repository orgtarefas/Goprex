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

        val sharedPrefs = getSharedPreferences("goprex_prefs", Context.MODE_PRIVATE)
        val logado = sharedPrefs.getBoolean("logado", false)

        if (logado) {
            startActivity(Intent(this, tela_home_meus_dados::class.java))
            finish()
            return
        }

        setContent {
            GoprexTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var navegou by remember { mutableStateOf(false) }

                    LoginScreen(
                        onLoginSuccess = { login ->
                            if (!navegou) {
                                navegou = true
                                salvarDadosLogin(login)
                                val intent = Intent(this@MainActivity, tela_home_meus_dados::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            }
                        }
                    )
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