package com.goprex.data.model

data class LojaDisponivelEntrega(
    val lojaId: String = "",
    val nomeLoja: String = "",
    val enderecoResumo: String = "",
    val cidade: String = "",
    val estado: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val ativa: Boolean = true
)

data class LojaAtuacao(
    val lojaId: String = "",
    val nomeLoja: String = "",
    val enderecoResumo: String = "",
    val cidade: String = "",
    val estado: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val raioKm: Double = 1.0,
    val atendeTodaCidade: Boolean = false,
    val atualizadoEm: Long = System.currentTimeMillis()
)

data class Loja(
    val id: String = "",
    val nome: String = "",
    val cep: String = "",
    val logradouro: String = "",
    val numero: String = "",
    val complemento: String = "",
    val bairro: String = "",
    val cidade: String = "Salvador",
    val estado: String = "BA",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val logoLoja: String = "",
    val ativa: Boolean = true,
    val criadoEm: Long = System.currentTimeMillis(),
    val atualizadoEm: Long = System.currentTimeMillis()
) {
    fun resumoEndereco(): String {
        return listOf(
            logradouro,
            numero.takeIf { it.isNotBlank() }?.let { "n. $it" },
            bairro,
            cidade.takeIf { it.isNotBlank() }?.let { cidadeValor ->
                estado.takeIf { it.isNotBlank() }?.let { "$cidadeValor/$it" } ?: cidadeValor
            },
            cep.takeIf { it.isNotBlank() }?.let { "CEP $it" }
        ).filterNotNull().filter { it.isNotBlank() }.joinToString(", ")
    }
}
