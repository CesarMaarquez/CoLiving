package net.azarquiel.coliving.viewmodel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import net.azarquiel.coliving.MainActivity

class RegisterViewModel : ViewModel() {
    private val auth = Firebase.auth
    private val db = FirebaseFirestore.getInstance()

    private var _isUserLogged = MutableLiveData(false)
    val isUserLogged: MutableLiveData<Boolean> = _isUserLogged

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        _isUserLogged.value = firebaseAuth.currentUser != null
    }

    init {
        auth.addAuthStateListener(authListener)
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authListener)
    }

    //val mainViewModel = MainViewModel(MainActivity())

    fun checkUser() {
        if (auth.currentUser != null) {
            setUserLogged(true)
        }
        else {
            setUserLogged(false)
        }
    }

    fun createUser(
        email: String,
        password: String,
        context: Context,
        onSuccess: () -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val nick = task.result.user?.email?.split("@")?.get(0)
                    val uid = task.result.user?.uid
                    val prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
                    prefs.edit().putString("userId", uid).apply()
                    updateUser(nick, uid)
                    onSuccess()
                }
            }
    }

    private fun updateUser(nick: String?, uid: String?) {
        val user = mutableMapOf(
            "nick" to (nick ?: "Anon"),
            "uid" to (uid ?: "")
        )
        db.collection("user").add(user)
    }

    fun setUserLogged(value: Boolean) {
        _isUserLogged.value = value
    }
}