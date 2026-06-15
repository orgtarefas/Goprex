package com.goprex

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
import com.goprex.ui.home.HomeScreen
import com.goprex.ui.login.LoginScreen
import com.goprex.ui.theme.GoprexTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GoprexTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var telaAtual by remember { mutableStateOf("LOGIN") }
                    var dadosLogin by remember { mutableStateOf<Login?>(null) }
                    var loginKey by remember { mutableStateOf(0) }

                    when (telaAtual) {
                        "LOGIN" -> {
                            // key força recriação completa
                            key(loginKey) {
                                LoginScreen(
                                    onLoginSuccess = { login ->
                                        dadosLogin = login
                                        telaAtual = "HOME"
                                    }
                                )
                            }
                        }
                        "HOME" -> {
                            if (dadosLogin != null) {
                                HomeScreen(
                                    loginData = dadosLogin!!,
                                    onLogout = {
                                        // Limpa dados locais primeiro
                                        dadosLogin = null
                                        // Depois muda a tela
                                        telaAtual = "LOGIN"
                                        // Incrementa key para forçar nova LoginScreen
                                        loginKey++
                                    }
                                )
                            } else {
                                // Segurança: se dadosLogin for null, volta para login
                                LaunchedEffect(Unit) {
                                    telaAtual = "LOGIN"
                                    loginKey++
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}