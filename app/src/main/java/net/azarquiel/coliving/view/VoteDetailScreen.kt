package net.azarquiel.coliving.view

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import net.azarquiel.coliving.model.Votacion
import net.azarquiel.coliving.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VoteDetailScreen(
    navController: NavController,
    viewModel: MainViewModel,
    votacion: Votacion
) {
    var selectedOption by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    // Simulamos que tienes un userId, si no es anónima
    val userId = "user123"  // <-- cámbialo por el valor real desde ViewModel o Auth

    val onVotoRealizado: () -> Unit = {
        Toast.makeText(context, "Voto registrado correctamente", Toast.LENGTH_SHORT).show()
        navController.popBackStack()
    }


    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Votación", fontSize = 20.sp, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(12.dp))

        Text(text = votacion.pregunta, style = MaterialTheme.typography.titleLarge)

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Fecha límite: ${dateFormatter.format(Date(votacion.fechaLimite))}",
            style = MaterialTheme.typography.bodySmall
        )

        Text(
            text = if (votacion.anonima) "Votación anónima" else "Votación pública",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(16.dp))

        votacion.opciones.forEach { opcion ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedOption = opcion }
                    .padding(vertical = 8.dp)
            ) {
                RadioButton(
                    selected = selectedOption == opcion,
                    onClick = { selectedOption = opcion }
                )
                Text(text = opcion)
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                if (selectedOption != null) {
                    viewModel.enviarVotoAFirebase(
                        votacionId = votacion.id,
                        opcionSeleccionada = selectedOption!!,
                        userId = if (votacion.anonima) null else userId,
                        context = context,
                        onSuccess = onVotoRealizado
                    )
                } else {
                    Toast.makeText(context, "Selecciona una opción", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Votar")
        }
    }
}
