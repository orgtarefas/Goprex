package com.goprex.ui.telas

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.goprex.data.model.Login
import com.goprex.ui.menu.HeaderComMenu
import com.goprex.ui.produto.VitrineScreen
import com.goprex.ui.theme.GoprexTheme

class tela_comprar : ComponentActivity() {

    private val ignoredKeys = setOf(
        "logado", "documentoId", "saved_login",
        "saved_password", "lembrar_dados", "acabou_de_sair"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("goprex_prefs", Context.MODE_PRIVATE)
        val loginData = buildLoginData(prefs)

        setContent {
            GoprexTheme {
                HeaderComMenu(
                    loginData = loginData,
                    titulo = "Comprar",
                    onLogout = { logout(prefs) },
                    conteudo = {
                        VitrineScreen()
                    }
                )
            }
        }
    }

    private fun buildLoginData(prefs: android.content.SharedPreferences): Login {
        val dados = prefs.all
            .filterKeys { it !in ignoredKeys }
            .mapValues { it.value }

        return Login(
            documentoId = prefs.getString("documentoId", "") ?: "",
            dados = dados
        )
    }

    private fun logout(prefs: android.content.SharedPreferences) {
        prefs.edit().putBoolean("logado", false).apply()

        val intent = Intent(this, com.goprex.MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}