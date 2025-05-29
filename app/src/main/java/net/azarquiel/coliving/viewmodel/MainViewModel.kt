package net.azarquiel.coliving.viewmodel

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import android.provider.Settings
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import net.azarquiel.coliving.MainActivity
import net.azarquiel.coliving.model.GastoCompartido
import net.azarquiel.coliving.model.Post
import net.azarquiel.coliving.model.Votacion
import net.azarquiel.coliving.model.Voto
import java.util.UUID

class MainViewModel(mainActivity: MainActivity): ViewModel() {

    val mainActivity by lazy { mainActivity }

    private val auth: FirebaseAuth = Firebase.auth

    private var _mensajes = MutableLiveData<List<Post>>()
    val mensajes: MutableLiveData<List<Post>> = _mensajes

    private var _openDialog = MutableLiveData(false)
    val openDialog: MutableLiveData<Boolean> = _openDialog

    private var _isUserLogged = MutableLiveData(false)
    val isUserLogged: MutableLiveData<Boolean> = _isUserLogged

    //recogerlas a traves de un metodo de la bdd firestore
    private val _votaciones: MutableLiveData<List<Votacion>> = MutableLiveData()
    val votaciones: LiveData<List<Votacion>> = _votaciones

    private val _gastosCompartidos = MutableLiveData<List<GastoCompartido>>()
    val gastosCompartidos: LiveData<List<GastoCompartido>> = _gastosCompartidos

    private val _usuarios = MutableLiveData<List<String>>() // lista de nicks o emails
    val usuarios: LiveData<List<String>> = _usuarios

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        _isUserLogged.value = firebaseAuth.currentUser != null
    }

    // utilizo esta forma ya que el livedata da problemas
    var dialogDetailVotacion by mutableStateOf(false)
        private set

    var dialogDetailGasto by mutableStateOf(false)
        private set

    companion object {
        const val TAG = "ChatDAM"
    }

    val db = Firebase.firestore

    init {
        auth.addAuthStateListener(authListener)
        setListener()
        cargarGastosCompartidos()
        cargarVotaciones()
        cargarUsuarios()
        // chequea en el arranque de la app si estamos logueados o no
        checkUser()
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authListener)
    }

    private fun setListener() {
        db.collection("posts")
            .orderBy(
                "timestamp",
                Query.Direction.DESCENDING
            ) // ordena por fecha (mas recientes abajo)
            .addSnapshotListener { snapshot, e ->
                e?.let {
                    Log.d(TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }
                snapshot?.let {
                    documentToList(snapshot.documents)
                }
            }
    }

    fun formatTimestamp(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }


    private fun documentToList(documents: List<DocumentSnapshot>) {
        val mensajes = ArrayList<Post>()
        documents.forEach { d ->
            val user = d["usuario"] as? String ?: "Anon"
            val msg = d["post"] as? String ?: ""
            val timestamp = d["timestamp"] as? Long ?: System.currentTimeMillis()
            mensajes.add(Post(user, msg, timestamp))
        }
        _mensajes.value = mensajes
    }


    fun cargarUsuarios() {
        Log.d(TAG, "Iniciando carga de usuarios")
        db.collection("user")
            .get()
            .addOnSuccessListener { snapshot ->
                val listaUsuarios = snapshot.documents.mapNotNull { doc ->
                    doc.getString("nick") ?: doc.getString("email") // dependiendo qué guardes
                }
                _usuarios.postValue(listaUsuarios)
            }
            .addOnFailureListener {
                Log.e(TAG, "Error al cargar usuarios", it)
            }
    }

    fun enviarMensaje(texto: String) {
        val usuario = auth.currentUser?.email?.split("@")?.get(0) ?: "Anon"
        val post = hashMapOf(
            "usuario" to usuario,
            "post" to texto,
            "timestamp" to System.currentTimeMillis()
        )
        db.collection("posts")
            .add(post)
            .addOnSuccessListener {
                Log.d(TAG, "Mensaje enviado")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error al enviar mensaje", e)
            }
    }

    private fun cargarVotaciones() {
        Log.d("TAG", "Iniciando carga de votaciones")
        db.collection("votaciones")
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    Log.e("MainViewModel", "Error al cargar votaciones", exception)
                    return@addSnapshotListener
                }

                snapshot?.let {
                    val lista = it.documents.mapNotNull { doc ->
                        doc.toObject(Votacion::class.java)
                    }
                    _votaciones.postValue(lista)
                }
            }
    }


    fun guardarVotacion(
        votacion: Votacion,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("votaciones").document(votacion.id)
            .set(votacion)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    private fun cargarGastosCompartidos() {
        Log.d(TAG, "Iniciando carga de gastos compartidos")
        db.collection("gastosCompartidos")
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    Log.e("MainViewModel", "Error al cargar gastos", exception)
                    return@addSnapshotListener
                }

                snapshot?.let {
                    val lista = it.documents.mapNotNull { doc ->
                        doc.toObject(GastoCompartido::class.java)
                    }
                    _gastosCompartidos.postValue(lista)
                }
            }
    }


    fun guardarGastoCompartido(
        gasto: GastoCompartido,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("gastosCompartidos")
            .document(gasto.id)
            .set(gasto)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }


    fun checkUser() {
        if (auth.currentUser != null) {
            setUserLogged(true)
        }
        else {
            setUserLogged(false)
        }
    }

    fun marcarGastoComoFinalizado(
        gastoId: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        val docRef = db.collection("gastosCompartidos").document(gastoId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val gasto = snapshot.toObject(GastoCompartido::class.java)
                ?: throw Exception("Gasto no encontrado")

            val pagos = gasto.pagos
            val participantes = gasto.participantes

            val todosPagaron = participantes.all { pagos[it] == true }

            if (todosPagaron && !gasto.finalizado) {
                transaction.update(docRef, "finalizado", true)
            }
        }.addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }




    fun contarVotos(
        votacionId: String,
        onResult: (Map<String, Int>) -> Unit
    ) {
        db.collection("votaciones")
            .document(votacionId)
            .collection("votos")
            .get()
            .addOnSuccessListener { snapshot ->
                val votos = snapshot.documents.mapNotNull { it.toObject(Voto::class.java) }
                val recuento = votos.groupingBy { it.opcion }.eachCount()
                onResult(recuento)
            }
            .addOnFailureListener {
                Log.e("MainViewModel", "Error al contar votos", it)
            }
    }




    fun setDialog(value: Boolean) {
        _openDialog.value = value
    }

    //mas adelante la uso
    fun setUserLogged(value: Boolean) {
        _isUserLogged.value = value
    }

    fun updateDialogDetailVotacion(value: Boolean) {
        dialogDetailVotacion = value
    }

    fun updateDialogDetailGasto(value: Boolean) {
        dialogDetailGasto = value
    }


    //para acceder a la variable auth desde las views
    fun getCurrentUserNick(): String {
        return auth.currentUser?.email?.split("@")?.get(0) ?: "Anon"
    }



}
