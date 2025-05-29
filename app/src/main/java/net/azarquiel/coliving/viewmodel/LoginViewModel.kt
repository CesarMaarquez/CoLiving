package net.azarquiel.coliving.viewmodel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import net.azarquiel.coliving.MainActivity

class LoginViewModel : ViewModel() {
    private val auth = Firebase.auth

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        _isUserLogged.value = firebaseAuth.currentUser != null
    }

    private var _isUserLogged = MutableLiveData(false)
    val isUserLogged: MutableLiveData<Boolean> = _isUserLogged

    //val mainViewModel = MainViewModel(MainActivity())

    init {
        auth.addAuthStateListener(authListener)
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authListener)
    }

//    fun checkUser() {
//        if (auth.currentUser != null) {
//            setUserLogged(true)
//        }
//        else {
//            setUserLogged(false)
//        }
//    }

    fun setUserLogged(value: Boolean) {
        _isUserLogged.value = value
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
                            val uid = task.result.user?.uid
                            val prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
                            prefs.edit().putString("userId", uid).apply()
                            //isUserLogged.postValue(true)
                            onSuccess()
                        } else {
                            val exception = task.exception
                            val errorMsg = when {
                                exception?.message?.contains("There is no user record") == true -> "El usuario no está registrado"
                                exception?.message?.contains("The password is invalid") == true -> "La contraseña es incorrecta"
                                exception?.message?.contains("The email address is badly formatted") == true -> "El formato del correo es inválido"
                                else -> "Correo o contraseña incorrectos"
                            }
                            onError(errorMsg)
                        }
                    }
            } catch (e: Exception) {
                onError("Error: \${e.localizedMessage}")
            }
        }
    }
}