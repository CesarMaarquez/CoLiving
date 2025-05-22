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
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import net.azarquiel.coliving.MainActivity
import net.azarquiel.coliving.model.Post
import net.azarquiel.coliving.model.Votacion
import net.azarquiel.coliving.model.Voto
import java.util.UUID

class MainViewModel(mainActivity: MainActivity): ViewModel() {

    val mainActivity by lazy { mainActivity }

    private val auth: FirebaseAuth = Firebase.auth

    private val _loading = MutableLiveData(false)

    private var _mensajes = MutableLiveData<List<Post>>()
    val mensajes: MutableLiveData<List<Post>> = _mensajes

    private var _openDialog = MutableLiveData(false)
    val openDialog: MutableLiveData<Boolean> = _openDialog

    private var _isUserLogged = MutableLiveData(false)
    val isUserLogged: MutableLiveData<Boolean> = _isUserLogged

    //recogerlas a traves de un metodo de la bdd firestore
    private val _votaciones: MutableLiveData<List<Votacion>> = MutableLiveData()
    val votaciones: LiveData<List<Votacion>> = _votaciones

    // utilizo esta forma ya que el livedata da problemas
    var dialogDetailVotacion by mutableStateOf(false)
        private set

    companion object {
        const val TAG = "ChatDAM"
    }

    val db = Firebase.firestore

    init {
        setListener()
        cargarVotaciones()
        // chequea en el arranque de la app si estamos logueados o no
        checkUser()
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

    private fun checkUser() {
        if (auth.currentUser != null) {
            setUserLogged(true)
        }
        else {
            setUserLogged(false)
        }
    }

    fun logUser(email: String, password:String, home: () -> Unit) =
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("LogUser", "signInWithEmail:success")
                        home()
                    }
                    else {
                        Log.w("LogUser", "signInWithEmail:failure", task.exception)
                    }

                }
            } catch (e: Exception) {
                Log.w("LogUser", "signInWithEmail:failure", e)
            }
    }

    fun createUser(email: String, password: String, home: () -> Unit) {
        if(_loading.value == false) {
            _loading.value = true
            auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("CreateUser", "createUserWithEmail:success")
                    val nick= task.result.user?.email?.split("@")?.get(0)
                    updateUser(nick)
                    home()
                } else {
                    Log.w("CreateUser", "createUserWithEmail:failure", task.exception)
                }
                _loading.value = false
            }
        }
    }

    // mete el nick automaticamente (antes del @) usado en el chat en la bdd
    private fun updateUser(nick: String?) {
        val userId= auth.currentUser?.uid
        // para introducir duplas de datos en las colecciones (clave, valor)
        val user = mutableMapOf<String, Any>()

        user["nick"] = nick.toString()
        user["uid"] = userId.toString()

        FirebaseFirestore.getInstance().collection("user")
            .add(user)
            .addOnSuccessListener {
                Log.d("updateUser", "DocumentSnapshot added with ID: ${it.id}")
            }
            .addOnFailureListener { e ->
                Log.w("updateUser", "Error adding document", e)
            }
    }

    fun enviarVotoAFirebase(
        votacionId: String,
        opcionSeleccionada: String,
        userId: String?,
        context: Context,
        onSuccess: () -> Unit
    ) {
        val voto = Voto(
            userId = userId,
            opcion = opcionSeleccionada,
            timestamp = System.currentTimeMillis()
        )

        val db = FirebaseFirestore.getInstance()
        val votoId = userId ?: UUID.randomUUID().toString()

        db.collection("votaciones")
            .document(votacionId)
            .collection("votos")
            .document(votoId)
            .set(voto)
            .addOnSuccessListener {
                Toast.makeText(context, "Voto registrado", Toast.LENGTH_SHORT).show()
                onSuccess()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error al votar", Toast.LENGTH_LONG).show()
            }
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

    //para acceder a la variable auth desde las views
    fun getCurrentUserNick(): String {
        return auth.currentUser?.email?.split("@")?.get(0) ?: "Anon"
    }

}
