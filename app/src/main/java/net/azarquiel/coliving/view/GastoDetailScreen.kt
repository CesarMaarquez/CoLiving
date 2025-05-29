package net.azarquiel.coliving.view

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.azarquiel.coliving.model.GastoCompartido
import net.azarquiel.coliving.viewmodel.GastoDetailViewModel
import net.azarquiel.coliving.viewmodel.MainViewModel
@Composable
fun GastoDetailScreen(
    gasto: GastoCompartido,
    viewModel: GastoDetailViewModel,
    onBack: () -> Unit
) {
    val currentUserId = viewModel.getCurrentUserNick()
    val pagos = remember { mutableStateOf(gasto.pagos) }
    val context = LocalContext.current

    // Calculamos cuánto toca pagar cada participante
    val totalPorParticipante = if (gasto.participantes.isNotEmpty()) {
        gasto.total / gasto.participantes.size
    } else 0.0

    Column(
        modifier = Modifier
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Text("Detalle del gasto", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        Text("Descripción: ${gasto.descripcion}")
        Text("Total: €${"%.2f".format(gasto.total)}")
        Spacer(Modifier.height(16.dp))

        Text("Participantes", fontWeight = FontWeight.SemiBold)
        gasto.participantes.forEach { userId ->
            val pagado = pagos.value[userId] ?: false
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = userId,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "€${"%.2f".format(totalPorParticipante)}",
                    modifier = Modifier.width(80.dp),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (pagado) "Pagado" else "Pendiente",
                    color = if (pagado) Color.Green else Color.Red,
                    modifier = Modifier.width(80.dp)
                )
                if (!pagado && userId == currentUserId) {
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        viewModel.marcarPagado(
                            gastoId = gasto.id,
                            userId = currentUserId,
                            onSuccess = {
                                Toast.makeText(context, "Pago marcado", Toast.LENGTH_SHORT).show()
                                pagos.value = pagos.value.toMutableMap().apply {
                                    put(currentUserId, true)
                                }
                            },
                            onFailure = {
                                Toast.makeText(context, "Error al marcar pago", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }) {
                        Text("Marcar pagado")
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Volver")
        }
    }
}
