package com.goprex.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.goprex.data.model.MenuItem
import kotlinx.coroutines.tasks.await

class MenuRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun carregarMenuPorPerfil(perfil: String): List<MenuItem> {
        return try {
            val documento = db.collection("menus")
                .document(perfil)
                .get()
                .await()

            val itensMenu = mutableListOf<MenuItem>()

            if (documento.exists()) {
                val dados = documento.data ?: emptyMap()

                dados.forEach { (chave, valor) ->
                    if (valor is Map<*, *>) {
                        val menuAtivo = valor["menu_ativo"] as? Boolean ?: false
                        val rota = valor["rota"] as? String ?: ""
                        val rotaTipo = valor["rota_tipo"] as? String ?: "tela"
                        val menuPosicao = (valor["menu_posição"] as? Number)?.toInt()
                            ?: (valor["menu_posicao"] as? Number)?.toInt()
                            ?: 999

                        if (menuAtivo) {
                            itensMenu.add(
                                MenuItem(
                                    id = chave,
                                    titulo = chave,
                                    menuAtivo = menuAtivo,
                                    rota = rota,
                                    rotaTipo = rotaTipo,
                                    menuPosicao = menuPosicao
                                )
                            )
                        }
                    }
                }
            }

            itensMenu.sortBy { it.menuPosicao }
            itensMenu

        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}