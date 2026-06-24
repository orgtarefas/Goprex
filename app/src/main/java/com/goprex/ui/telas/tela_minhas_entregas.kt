package com.goprex.ui.telas

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.goprex.data.model.Login
import com.goprex.ui.menu.HeaderComMenu
import com.goprex.ui.pedido.MinhasEntregasScreen
import com.goprex.ui.theme.GoprexTheme

class tela_minhas_entregas : ComponentActivity() {
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
                HeaderComMenu(loginData = loginData, titulo = "Minhas Entregas", onLogout = {
                    sharedPrefs.edit().clear().apply()
                    startActivity(Intent(this, com.goprex.MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK })
                    finish()
                }, conteudo = {
                    MinhasEntregasScreen(loginData = loginData)
                })
            }
        }
    }
}
