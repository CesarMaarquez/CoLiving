package net.azarquiel.coliving.model

data class Votacion(
    val id: String = "",
    val pregunta: String = "",
    val opciones: List<String> = emptyList(),
    val fechaLimite: Long = 0L,
    val anonima: Boolean = false
)

data class Voto(
    val userId: String? = null,                 // null si es anónima
    val opcion: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val deviceId: String? = null                // null si es pública
)


data class Post(
    var user: String = "",
    var msg: String = "",
    var timestamp: Long = System.currentTimeMillis()
)

data class GastoCompartido(
    val id: String = "",
    val descripcion: String = "",
    val total: Double = 0.0,
    val participantes: List<String> = emptyList(),
    val pagos: Map<String, Boolean> = emptyMap(),
    val creadorId: String = "",
    val timestamp: Long = 0L,
    val finalizado: Boolean = false
)



