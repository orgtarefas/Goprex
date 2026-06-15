package com.goprex.data.model

data class Login(
    val documentoId: String = "",
    val nome: String = "",
    val cidade: String = "Salvador",
    val estado: String = "Bahia",
    val data_criacao: String = "",
    val perfil: Perfil = Perfil.CLIENTE,
    val loja: String = "",
    val regiao_abrangencia: Map<String, Boolean> = mapOf("Salvador" to true),
    val status_ativo: Boolean = true,
    val telefone: Long = 0,
    val telefone_whatsapp: Boolean = false,
    val email: String = "",
    val fotoUrl: String = ""
)

enum class Perfil {
    CLIENTE,
    VENDEDOR,
    ENTREGADOR,
    ADMIN
}