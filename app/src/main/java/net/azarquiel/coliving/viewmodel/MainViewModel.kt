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

    private val _gastosCompartidos = MutableLiveData<List<GastoCompartido>>()
    val gastosCompartidos: LiveData<List<GastoCompartido>> = _gastosCompartidos

    private val _usuarios = MutableLiveData<List<String>>() // lista de nicks o emails
    val usuarios: LiveData<List<String>> = _usuarios

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
        setListener()
        cargarGastosCompartidos()
        cargarVotaciones()
        cargarUsuarios()
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

    // Marca un usuario como pagado para un gasto compartido
    fun marcarPagado(
        gastoId: String,
        userId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val docRef = db.collection("gastosCompartidos").document(gastoId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val gasto = snapshot.toObject(GastoCompartido::class.java)
            if (gasto != null) {
                val nuevosPagos = gasto.pagos.toMutableMap()
                nuevosPagos[userId] = true
                transaction.update(docRef, "pagos", nuevosPagos)
            } else {
                throw Exception("Gasto no encontrado")
            }
        }.addOnSuccessListener { onSuccess() }
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
    fun logUser(
        email: String,
        password: String,
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d("LogUser", "signInWithEmail:success")
                            val uid = task.result.user?.uid

                            // Guarda el userId en SharePreferences
                            val prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
                            prefs.edit().putString("userId", uid).apply()

                            onSuccess()
                        } else {
                            val exception = task.exception
                            val errorMsg = when {
                                exception?.message?.contains("There is no user record") == true -> {
                                    "El usuario no está registrado"
                                }
                                exception?.message?.contains("The password is invalid") == true -> {
                                    "La contraseña es incorrecta"
                                }
                                exception?.message?.contains("The email address is badly formatted") == true -> {
                                    "El formato del correo es inválido"
                                }
                                else -> {
                                    "Correo o contraseña incorrectos o el usuario no está registrado"
                                }
                            }
                            onError(errorMsg)
                        }

                    }
            } catch (e: Exception) {
                Log.w("LogUser", "signInWithEmail:failure", e)
                onError("Excepción al iniciar sesión: ${e.localizedMessage}")
            }
        }
    }


    fun createUser(
        email: String,
        password: String,
        context: Context,
        home: () -> Unit
    ) {
        if (_loading.value == false) {
            _loading.value = true
            auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("CreateUser", "createUserWithEmail:success")

                    val nick = task.result.user?.email?.split("@")?.get(0)
                    updateUser(nick)

                    val uid = task.result.user?.uid
                    val prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
                    prefs.edit().putString("userId", uid).apply()

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




    fun votar(
        context: Context,
        votacionId: String,
        opcion: String,
        anonima: Boolean,
        userId: String?,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val votosRef = db.collection("votaciones").document(votacionId).collection("votos")

        val voto = if (anonima) {
            val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            Voto(userId = null, opcion = opcion, deviceId = deviceId)
        } else {
            Voto(userId = userId, opcion = opcion, deviceId = null)
        }

        val docId = if (anonima) {
            // usar el deviceId como ID del documento evita votos repetidos desde el mismo dispositivo
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        } else {
            userId!!
        }

        votosRef.document(docId)
            .set(voto)
            .addOnSuccessListener { onSuccess() }
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
