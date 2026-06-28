package com.goprex.data.model

data class EnderecoEntrega(
    val id: String = "",
    val cep: String = "",
    val apelido: String = "",
    val logradouro: String = "",
    val numero: String = "",
    val complemento: String = "",
    val bairro: String = "",
    val cidade: String = "Salvador",
    val estado: String = "BA",
    val principal: Boolean = false,
    val criadoEm: Long = System.currentTimeMillis(),
    val atualizadoEm: Long = System.currentTimeMillis()
) {
    fun resumo(): String {
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
