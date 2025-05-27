package net.azarquiel.coliving.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import net.azarquiel.coliving.model.GastoCompartido

class GastoDetailViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val auth: FirebaseAuth = Firebase.auth

    private val _gastosCompartidos = MutableLiveData<List<GastoCompartido>>()
    val gastosCompartidos: LiveData<List<GastoCompartido>> = _gastosCompartidos

    var dialogDetailGasto by mutableStateOf(false)
        private set

    init {
        cargarGastosCompartidos()
    }

    private fun cargarGastosCompartidos() {
        db.collection("gastosCompartidos").addSnapshotListener { snapshot, _ ->
            snapshot?.let {
                val lista = it.documents.mapNotNull { doc -> doc.toObject(GastoCompartido::class.java) }
                _gastosCompartidos.postValue(lista)
            }
        }
    }

    fun guardarGastoCompartido(gasto: GastoCompartido, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("gastosCompartidos").document(gasto.id).set(gasto)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun marcarPagado(gastoId: String, userId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val docRef = db.collection("gastosCompartidos").document(gastoId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val gasto = snapshot.toObject(GastoCompartido::class.java)
            val nuevosPagos = gasto?.pagos?.toMutableMap() ?: throw Exception("Gasto no encontrado")
            nuevosPagos[userId] = true
            transaction.update(docRef, "pagos", nuevosPagos)
        }.addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun updateDialogDetailGasto(value: Boolean) {
        dialogDetailGasto = value
    }

    //para acceder a la variable auth desde las views
    fun getCurrentUserNick(): String {
        return auth.currentUser?.email?.split("@")?.get(0) ?: "Anon"
    }
}