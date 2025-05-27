package net.azarquiel.coliving.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

class UserViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val _usuarios = MutableLiveData<List<String>>()
    val usuarios: LiveData<List<String>> = _usuarios

    fun cargarUsuarios() {
        db.collection("user").get().addOnSuccessListener { snapshot ->
            val listaUsuarios = snapshot.documents.mapNotNull { it.getString("nick") ?: it.getString("email") }
            _usuarios.postValue(listaUsuarios)
        }
    }
}