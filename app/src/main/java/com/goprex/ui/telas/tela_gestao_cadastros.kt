package com.goprex.ui.telas

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goprex.data.model.Login
import com.goprex.ui.theme.GoprexTheme
import com.goprex.ui.theme.TextSecondary

class tela_gestao_cadastros : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPrefs = getSharedPreferences("goprex_prefs", Context.MODE_PRIVATE)
        val documentoId = sharedPrefs.getString("documentoId", "") ?: ""

        val dadosMap = mutableMapOf<String, Any?>()
        sharedPrefs.all.forEach { (chave, valor) ->
            if (chave != "logado" && chave != "documentoId" &&
                chave != "saved_login" && chave != "saved_password" &&
                chave != "lembrar_dados" && chave != "acabou_de_sair") {
                dadosMap[chave] = valor
            }
        }

        val loginData = Login(documentoId = documentoId, dados = dadosMap)

        setContent {
            GoprexTheme {
                TelaHomeMeusDados(
                    loginData = loginData,
                    onLogout = {
                        sharedPrefs.edit().clear().apply()
                        val intent = Intent(this, com.goprex.MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
}