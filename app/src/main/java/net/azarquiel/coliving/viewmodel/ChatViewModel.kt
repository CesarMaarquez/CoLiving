package net.azarquiel.coliving.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import net.azarquiel.coliving.model.Post

class ChatViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val _mensajes = MutableLiveData<List<Post>>()
    val mensajes: LiveData<List<Post>> = _mensajes

    init {
        setListener()
    }

    private fun setListener() {
        db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let {
                    val posts = it.documents.mapNotNull { d ->
                        val user = d.getString("usuario") ?: "Anon"
                        val msg = d.getString("post") ?: ""
                        val timestamp = d.getLong("timestamp") ?: System.currentTimeMillis()
                        Post(user, msg, timestamp)
                    }
                    _mensajes.postValue(posts)
                }
            }
    }

    fun enviarMensaje(texto: String) {
        val usuario = Firebase.auth.currentUser?.email?.split("@")?.get(0) ?: "Anon"
        val post = mapOf(
            "usuario" to usuario,
            "post" to texto,
            "timestamp" to System.currentTimeMillis()
        )
        db.collection("posts").add(post)
    }
}
