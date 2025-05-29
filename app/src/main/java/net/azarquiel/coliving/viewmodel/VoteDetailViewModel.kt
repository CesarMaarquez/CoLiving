package net.azarquiel.coliving.viewmodel

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.font.FontVariation
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import net.azarquiel.coliving.model.Votacion
import net.azarquiel.coliving.model.Voto

class VoteDetailViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val _votaciones = MutableLiveData<List<Votacion>>()
    val votaciones: LiveData<List<Votacion>> = _votaciones
    var dialogDetailVotacion by mutableStateOf(false)
        private set

    init {
        cargarVotaciones()
    }

    private fun cargarVotaciones() {
        db.collection("votaciones").addSnapshotListener { snapshot, _ ->
            snapshot?.let {
                val lista = it.documents.mapNotNull { doc -> doc.toObject(Votacion::class.java) }
                _votaciones.postValue(lista)
            }
        }
    }


    fun votar(context: Context, votacionId: String, opcion: String, anonima: Boolean, userId: String?, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val voto = if (anonima) {
            val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            Voto(null, opcion, deviceId.toLong())
        } else {
            Voto(userId, opcion)
        }
        val docId = voto.deviceId ?: userId!!
        db.collection("votaciones").document(votacionId).collection("votos").document(docId).set(voto)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

}
