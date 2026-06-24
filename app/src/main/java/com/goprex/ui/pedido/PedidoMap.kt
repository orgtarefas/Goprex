package com.goprex.ui.pedido

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.goprex.data.model.Pedido
import com.goprex.ui.theme.GoPrexDark
import com.goprex.ui.theme.GoPrexOrange
import com.goprex.ui.theme.SuccessGreen

@Composable
fun PedidoMapPanel(
    pedido: Pedido,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val loja = pedido.lojaLatitude.toLatLngOrNull(pedido.lojaLongitude)
    val cliente = pedido.clienteLatitude.toLatLngOrNull(pedido.clienteLongitude)
    val entregador = pedido.entregadorLatitude.toLatLngOrNull(pedido.entregadorLongitude)
    val pontos = listOfNotNull(loja, entregador, cliente)
    val cameraTarget = entregador ?: loja ?: cliente ?: LatLng(-12.9777, -38.5016)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(cameraTarget, if (pontos.isEmpty()) 11f else 13f)
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(240.dp),
            shape = RoundedCornerShape(10.dp),
            tonalElevation = 1.dp
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxWidth(),
                cameraPositionState = cameraPositionState
            ) {
                loja?.let {
                    Marker(
                        state = MarkerState(position = it),
                        title = pedido.loja,
                        snippet = "Loja"
                    )
                }
                cliente?.let {
                    Marker(
                        state = MarkerState(position = it),
                        title = pedido.clienteNome.ifBlank { "Cliente" },
                        snippet = "Destino"
                    )
                }
                entregador?.let {
                    Marker(
                        state = MarkerState(position = it),
                        title = pedido.entregadorNome.ifBlank { "Entregador" },
                        snippet = "Localizacao atual"
                    )
                }
                if (pontos.size >= 2) {
                    Polyline(points = pontos, color = GoPrexOrange, width = 8f)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MapLegend(Icons.Filled.Store, "Loja", GoPrexOrange)
            MapLegend(Icons.Filled.LocalShipping, "Entregador", SuccessGreen)
            MapLegend(Icons.Filled.TaskAlt, "Cliente", GoPrexDark)
        }

        Button(
            onClick = {
                val destination = cliente ?: loja ?: return@Button
                val uri = Uri.parse("google.navigation:q=${destination.latitude},${destination.longitude}")
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
            },
            enabled = cliente != null || loja != null,
            colors = ButtonDefaults.buttonColors(containerColor = GoPrexDark),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Filled.Map, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Abrir rota")
        }
    }
}

@Composable
private fun MapLegend(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color
) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Icon(icon, null, tint = color, modifier = Modifier.height(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, color = color, fontWeight = FontWeight.Medium)
    }
}

private fun Double.toLatLngOrNull(longitude: Double): LatLng? {
    return if (this != 0.0 && longitude != 0.0) LatLng(this, longitude) else null
}
