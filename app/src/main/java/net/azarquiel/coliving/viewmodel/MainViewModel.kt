package net.azarquiel.coliving.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import net.azarquiel.coliving.MainActivity
import net.azarquiel.coliving.model.Votacion
import java.util.UUID

class MainViewModel(mainActivity: MainActivity): ViewModel()  {

    val mainActivity by lazy { mainActivity }
    private val auth: FirebaseAuth = Firebase.auth
    private val _loading = MutableLiveData(false)

    private  var _openDialog = MutableLiveData(false)
    val openDialog: MutableLiveData<Boolean> = _openDialog

    private  var _isUserLogged = MutableLiveData(false)
    val isUserLogged: MutableLiveData<Boolean> = _isUserLogged

    //recogerlas a traves de un metodo de la bdd firestore
    private val _votaciones: MutableLiveData<List<Votacion>> = MutableLiveData()
    val votaciones: LiveData<List<Votacion>> = _votaciones

    private val _dialogDetailVotacion = MutableLiveData(false)
    val dialogDetailVotacion: MutableLiveData<Boolean> = _dialogDetailVotacion


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

    fun setDialog(value: Boolean) {
        _openDialog.value = value
    }

    //mas adelante la uso
    fun setUserLogged(value: Boolean) {
        _isUserLogged.value = value
    }

    fun setDialogDetailVotacion(value: Boolean) {
        _dialogDetailVotacion.value = value
    }

    //private val db = Firebase.firestore

    fun guardarVotacion(
        pregunta: String,
        opciones: List<String>,
        fechaLimite: Long,
        anonima: Boolean,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        viewModelScope.launch {
            val nuevaVotacion = Votacion(
                id = UUID.randomUUID().toString(),
                pregunta = pregunta,
                opciones = opciones,
                fechaLimite = fechaLimite,
                anonima = anonima
            )

//            db.collection("votaciones")
//                .document(nuevaVotacion.id)
//                .set(nuevaVotacion)
//                .addOnSuccessListener { onSuccess() }
//                .addOnFailureListener { exception -> onFailure(exception) }
        }
    }

}
