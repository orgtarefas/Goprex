package com.goprex.ui.menu

import android.content.Intent
import android.net.Uri
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import java.net.URLEncoder

@Composable
fun ModalSuporteDialog(
    onDismiss: () -> Unit,
    telefoneUsuario: Long = 0,
    nomeUsuario: String = ""
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Suporte Goprex",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Text(
                text = "Como podemos ajudar?\n\n" +
                        "📞 Telefone: (11) 99999-9999\n" +
                        "📧 Email: suporte@goprex.com\n" +
                        "💬 WhatsApp: (11) 98888-8888\n\n" +
                        "Horário de atendimento:\n" +
                        "Segunda a Sexta: 08h às 18h\n" +
                        "Sábado: 08h às 12h"
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:11999999999")
                    }
                    context.startActivity(intent)
                    onDismiss()
                }
            ) {
                Text("Ligar")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    try {
                        val numero = "5511999999999"
                        val mensagem = if (nomeUsuario.isNotEmpty()) {
                            "Olá, sou $nomeUsuario, preciso de ajuda com o Goprex"
                        } else {
                            "Olá! Preciso de ajuda com o Goprex"
                        }
                        val url = "https://wa.me/$numero?text=${URLEncoder.encode(mensagem, "UTF-8")}"
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse(url)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    onDismiss()
                }
            ) {
                Text("WhatsApp")
            }
        },
        neutralButton = {
            TextButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:")
                        putExtra(Intent.EXTRA_EMAIL, arrayOf("suporte@goprex.com"))
                        putExtra(Intent.EXTRA_SUBJECT, "Suporte Goprex")
                    }
                    context.startActivity(intent)
                    onDismiss()
                }
            ) {
                Text("Email")
            }
        }
    )
}