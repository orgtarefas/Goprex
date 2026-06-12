package com.goprex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.goprex.data.model.Login
import com.goprex.ui.home.HomeScreen
import com.goprex.ui.login.LoginScreen
import com.goprex.ui.splash.SplashScreen
import com.goprex.ui.theme.GoprexTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GoprexTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var appState by remember { mutableStateOf<AppState>(AppState.Splash) }
                    var loginData by remember { mutableStateOf<Login?>(null) }

                    when (appState) {
                        AppState.Splash -> {
                            SplashScreen(
                                onSplashFinished = {
                                    appState = AppState.Login
                                }
                            )
                        }

                        AppState.Login -> {
                            LoginScreen(
                                onLoginSuccess = { login ->
                                    loginData = login
                                    appState = AppState.Home
                                }
                            )
                        }

                        AppState.Home -> {
                            loginData?.let { data ->
                                HomeScreen(
                                    loginData = data,
                                    onLogout = {
                                        loginData = null
                                        appState = AppState.Login
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

enum class AppState {
    Splash,
    Login,
    Home
}