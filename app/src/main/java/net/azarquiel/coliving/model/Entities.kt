package net.azarquiel.coliving.model

data class Votacion(
    val id: String,
    val pregunta: String,
    val opciones: List<String>,
    val fechaLimite: Long,
    val anonima: Boolean
)
