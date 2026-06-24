package com.goprex.ui.telas

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.goprex.data.model.Login
import com.goprex.ui.menu.HeaderComMenu
import com.goprex.ui.theme.GoPrexDark
import com.goprex.ui.theme.GoPrexOrange
import com.goprex.ui.theme.GoprexTheme

class tela_teste_mapa : ComponentActivity() {

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
                    titulo = "Teste Mapa",
                    onLogout = { logout(prefs) },
                    conteudo = {
                        TesteMapaScreen()
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

@Composable
private fun TesteMapaScreen() {
    val salvador = LatLng(-12.9777, -38.5016)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(salvador, 13f)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(10.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Map, null, tint = GoPrexOrange)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mapa de teste", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = GoPrexDark)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Se o mapa abaixo renderizar Salvador com um marcador, a chave do Maps SDK for Android esta funcionando neste app.",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(10.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                Marker(
                    state = MarkerState(position = salvador),
                    title = "Salvador",
                    snippet = "Centro de teste GoPrex"
                )
            }
        }

        Button(
            onClick = { },
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(disabledContainerColor = GoPrexOrange.copy(alpha = 0.55f)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Filled.LocationOn, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Centro de Salvador: -12.9777, -38.5016")
        }
    }
}
