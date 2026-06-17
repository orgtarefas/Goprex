package com.goprex.ui.menu

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun ModalGenericoDialog(
    titulo: String,
    mensagem: String = "Funcionalidade em desenvolvimento",
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(titulo, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        },
        text = {
            Text(mensagem)
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}