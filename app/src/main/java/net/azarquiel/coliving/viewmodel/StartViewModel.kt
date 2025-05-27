package net.azarquiel.coliving.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class StartViewModel : ViewModel() {
    private val auth = Firebase.auth
    val isUserLogged = MutableLiveData<Boolean>()

    fun checkUser() {
        isUserLogged.value = auth.currentUser != null
    }
}