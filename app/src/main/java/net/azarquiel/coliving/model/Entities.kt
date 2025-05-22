package net.azarquiel.coliving.model

data class Votacion(
    val id: String = "",
    val pregunta: String = "",
    val opciones: List<String> = emptyList(),
    val fechaLimite: Long = 0L,
    val anonima: Boolean = false
)

data class Voto(
    // si es anonima, este campo sería null
    val userId: String? = null,
    val opcion: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class Post(
    var user: String = "",
    var msg: String = "",
    var timestamp: Long = System.currentTimeMillis()
)



