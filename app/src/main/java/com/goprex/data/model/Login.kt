package com.goprex.data.model

data class Login(
    val documentoId: String = "",
    private val dados: Map<String, Any?> = emptyMap()
) {
    fun getString(campo: String): String = dados[campo]?.toString() ?: ""
    fun getLong(campo: String): Long = (dados[campo] as? Number)?.toLong() ?: 0L
    fun getBoolean(campo: String): Boolean = dados[campo] as? Boolean ?: false
    fun getMap(campo: String): Map<String, Any?>? = dados[campo] as? Map<String, Any?>
    fun getDados(): Map<String, Any?> = dados
}