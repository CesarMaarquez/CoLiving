import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import android.provider.Settings
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import net.azarquiel.coliving.model.GastoCompartido
import net.azarquiel.coliving.model.Votacion
import net.azarquiel.coliving.navigation.AppScreens
import net.azarquiel.coliving.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID


@Composable
fun MainScreen(navController: NavHostController, viewModel: MainViewModel) {

    val usuarios by viewModel.usuarios.observeAsState(emptyList())

    Scaffold(
        topBar = { CustomTopBar(navController,viewModel) },
        content = { padding ->

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                CustomContent(padding, viewModel, navController)

                FabMenu(
                    viewModel = viewModel,
                    onCrearVotacion = {
                        viewModel.updateDialogDetailVotacion(true)
                    },
                    onCrearGasto = {
                        viewModel.updateDialogDetailGasto(true)
                    }
                )

                CustomFAB(viewModel)
                DialogFab(viewModel)

                if (viewModel.dialogDetailVotacion) {
                    Dialog(
                        onDismissRequest = { viewModel.updateDialogDetailVotacion(false) }
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            tonalElevation = 8.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            CreateVote(
                                viewModel = viewModel,
                                onClose = { viewModel.updateDialogDetailVotacion(false) }
                            )
                        }
                    }
                }

                if (viewModel.dialogDetailGasto) {
                    Dialog(
                        onDismissRequest = { viewModel.updateDialogDetailGasto(false) }
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            tonalElevation = 8.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            CreateGasto(
                                viewModel = viewModel,
                                usuarios = usuarios,
                                onClose = { viewModel.updateDialogDetailGasto(false) }
                            )
                        }
                    }
                }
            }
        }
    )
}



@Composable
fun CustomFAB(viewModel: MainViewModel) {
    val isUserLogged by viewModel.isUserLogged.observeAsState(false)

    //Valores dependiendo de si esta logueado o no
    val fabColor = if (isUserLogged) MaterialTheme.colorScheme.primary else Color.LightGray
    val opacity = if (isUserLogged) 1f else 0.8f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        FloatingActionButton(
            onClick = {
                if (isUserLogged) {
                    viewModel.setDialog(true)
                } else {
                    // Mostrar mensaje o acción alternativa
                }
            },
            containerColor = fabColor,
            contentColor = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .alpha(opacity)
                .align(Alignment.BottomEnd)
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Chat"
            )
        }
    }
}

// compose de creación de una votación, que va dentro de un dialog
@Composable
fun CreateVote(
    viewModel: MainViewModel,
    onClose: () -> Unit = {} // callback para cerrar el diálogo desde el contenedor
) {
    var question by remember { mutableStateOf("") }
    var options by remember { mutableStateOf(listOf("", "")) }
    var showDatePicker by remember { mutableStateOf(false) }
    var deadline by remember { mutableStateOf<Long?>(null) }
    var anonymous by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Crear nueva votación", fontSize = 20.sp, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = question,
            onValueChange = { question = it },
            label = { Text("Pregunta de la votación") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        Text("Opciones", fontWeight = FontWeight.SemiBold)
        options.forEachIndexed { index, option ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = option,
                    onValueChange = {
                        options = options.mapIndexed { i, old ->
                            if (i == index) it else old
                        }
                    },
                    label = { Text("Opción ${index + 1}") },
                    modifier = Modifier.weight(1f)
                )
                if (options.size > 2) {
                    IconButton(onClick = {
                        options = options.filterIndexed { i, _ -> i != index }
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar opción")
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        Button(onClick = {
            options = options + ""
        }) {
            Text("Añadir opción")
        }

        Spacer(Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Fecha límite: ", fontWeight = FontWeight.SemiBold)
            Text(
                text = deadline?.let { dateFormatter.format(Date(it)) } ?: "--",
                modifier = Modifier.weight(1f)
            )
            Button(onClick = { showDatePicker = true }) {
                Text("Seleccionar")
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("¿Votación anónima?", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Switch(checked = anonymous, onCheckedChange = { anonymous = it })
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                if (question.isNotBlank() && options.all { it.isNotBlank() } && deadline != null) {
                    val votacion = Votacion(
                        id = UUID.randomUUID().toString(),
                        pregunta = question,
                        opciones = options,
                        fechaLimite = deadline!!,
                        anonima = anonymous
                    )

                    viewModel.guardarVotacion(
                        votacion = votacion,
                        onSuccess = {
                            Toast.makeText(context, "Votación creada", Toast.LENGTH_SHORT).show()
                            onClose()
                        },
                        onFailure = {
                            Toast.makeText(context, "Error al guardar", Toast.LENGTH_LONG).show()
                        }
                    )
                } else {
                    Toast.makeText(context, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Crear votación")
        }
    }

    LaunchedEffect(showDatePicker) {
        if (showDatePicker) {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                context,
                { _, year, month, day ->
                    calendar.set(year, month, day, 23, 59, 59)
                    deadline = calendar.timeInMillis
                    showDatePicker = false
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }
}


@Composable
fun ActiveVotes(
    votaciones: List<Votacion>,
    onVotacionClick: (Votacion) -> Unit,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val firebaseUser = FirebaseAuth.getInstance().currentUser
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Votaciones activas", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        val actuales = votaciones.filter { it.fechaLimite > System.currentTimeMillis() }

        if (actuales.isEmpty()) {
            Text("No hay votaciones activas en este momento.")
        } else {
            actuales.forEach { votacion ->
                val yaVoto = remember(votacion.id) { mutableStateOf(false) }
                val recuento = remember(votacion.id) { mutableStateOf<Map<String, Int>>(emptyMap()) }

                LaunchedEffect(votacion.id) {
                    val docId = if (votacion.anonima) {
                        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                    } else {
                        firebaseUser?.uid
                    }

                    docId?.let {
                        Firebase.firestore.collection("votaciones")
                            .document(votacion.id)
                            .collection("votos")
                            .document(it)
                            .get()
                            .addOnSuccessListener { doc ->
                                yaVoto.value = doc.exists()
                            }
                    }

                    viewModel.contarVotos(votacion.id) {
                        recuento.value = it
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { onVotacionClick(votacion) },
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = votacion.pregunta,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Fecha límite: ${dateFormatter.format(Date(votacion.fechaLimite))}",
                            style = MaterialTheme.typography.bodySmall
                        )

                        Text(
                            text = if (votacion.anonima) "Votación anónima" else "Votación pública",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Mostrar recuento
                        if (recuento.value.isNotEmpty()) {
                            recuento.value.forEach { (opcion, cantidad) ->
                                Text(text = "$opcion: $cantidad votos", fontSize = 14.sp)
                            }
                        } else {
                            Text("Contando votos...", fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Mostrar si ya votó
                        Text(
                            text = if (yaVoto.value) "Ya has votado" else "Aún no has votado",
                            fontSize = 14.sp,
                            color = if (yaVoto.value) Color.Gray else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CreateGasto(
    viewModel: MainViewModel,
    usuarios: List<String>, // lista de userIds (puedes pasar nombres y mapear a ids)
    onClose: () -> Unit = {}
) {
    var descripcion by remember { mutableStateOf("") }
    var total by remember { mutableStateOf("") }
    var participantes by remember { mutableStateOf(setOf<String>()) }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Crear Gasto Compartido", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = descripcion,
            onValueChange = { descripcion = it },
            label = { Text("Descripción") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = total,
            onValueChange = { total = it },
            label = { Text("Total (€)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Spacer(Modifier.height(16.dp))

        Text("Participantes", fontWeight = FontWeight.SemiBold)

        usuarios.forEach { userId ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = participantes.contains(userId),
                    onCheckedChange = {
                        participantes = if (it) participantes + userId else participantes - userId
                    }
                )
                Text(text = userId) // Aquí mejor mostrar el nombre real si tienes esa info
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(onClick = {
            val totalDouble = total.toDoubleOrNull()
            if (descripcion.isBlank() || totalDouble == null || totalDouble <= 0 || participantes.isEmpty()) {
                Toast.makeText(context, "Completa todos los campos correctamente", Toast.LENGTH_SHORT).show()
                return@Button
            }

            val nuevoGasto = GastoCompartido(
                id = UUID.randomUUID().toString(),
                descripcion = descripcion,
                total = totalDouble,
                participantes = participantes.toList(),
                pagos = participantes.associateWith { false },
                creadorId = viewModel.getCurrentUserNick(), // o uid si prefieres
                timestamp = System.currentTimeMillis()
            )

            viewModel.guardarGastoCompartido(
                gasto = nuevoGasto,
                onSuccess = {
                    Toast.makeText(context, "Gasto creado", Toast.LENGTH_SHORT).show()
                    onClose()
                },
                onFailure = {
                    Toast.makeText(context, "Error al crear gasto", Toast.LENGTH_SHORT).show()
                }
            )
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Crear Gasto")
        }
    }
}

@Composable
fun GastosCompartidos(
    gastos: List<GastoCompartido>,
    viewModel: MainViewModel,
    onGastoClick: (GastoCompartido) -> Unit

) {
    val formatter = remember { java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (gastos.isEmpty()) {
            item {
                Text("No hay gastos compartidos aún.")
            }
        } else {
            items(gastos.sortedByDescending { it.timestamp }) { gasto ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { onGastoClick(gasto) },
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = gasto.descripcion,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Total: €${gasto.total}")
                        Text("Participantes: ${gasto.participantes.size}")
                        Text("Fecha: ${formatter.format(Date(gasto.timestamp))}")
                    }
                }
            }
        }
    }
}



@Composable
fun FabMenu(
    viewModel: MainViewModel,
    onCrearVotacion: () -> Unit,
    onCrearGasto: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Menú desplegable
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 72.dp, start = 16.dp)
        ) {
            DropdownMenuItem(
                text = { Text("Crear votación") },
                onClick = {
                    expanded = false
                    onCrearVotacion()
                }
            )
            DropdownMenuItem(
                text = { Text("Añadir gasto compartido") },
                onClick = {
                    expanded = false
                    onCrearGasto()
                }
            )
        }

        // FAB principal
        FloatingActionButton(
            onClick = { expanded = !expanded },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Crear",
                tint = Color.White
            )
        }
    }
}


//CHAT
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogFab(viewModel: MainViewModel) {
    val context = LocalContext.current
    val openDialog by viewModel.openDialog.observeAsState(false)
    val mensajes by viewModel.mensajes.observeAsState(emptyList())
    var msg by remember { mutableStateOf("") }

    if (openDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setDialog(false) },
            confirmButton = {},
            dismissButton = {},
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(500.dp)
                        .background(Color(0xFFF3F5F7))
                        .padding(8.dp)
                ) {
                    // Lista de mensajes
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color(0xFFE0F7FA)) // fondo más alegre
                            .padding(8.dp),
                        reverseLayout = true
                    ) {
                        items(mensajes) { mensaje ->
                            val isCurrentUser = mensaje.user == viewModel.getCurrentUserNick()
                            val bgColor = if (isCurrentUser) Color(0xFFDCF8C6) else Color.White
                            val alignment = if (isCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
                            val bubbleShape = RoundedCornerShape(12.dp)

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(bgColor, bubbleShape)
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = "${mensaje.user}: ${mensaje.msg}",
                                            color = Color.Black
                                        )
                                        Text(
                                            text = viewModel.formatTimestamp(mensaje.timestamp),
                                            fontSize = 10.sp,
                                            color = Color.Gray,
                                            modifier = Modifier.align(Alignment.End)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Entrada de texto + botón enviar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(8.dp)
                    ) {
                        TextField(
                            value = msg,
                            onValueChange = { msg = it },
                            placeholder = { Text("Escribe tu mensaje") },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(24.dp),
                            singleLine = true,
                            colors = TextFieldDefaults.textFieldColors(
                                containerColor = Color(0xFFF0F0F0),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )

                        )
                        IconButton(
                            onClick = {
                                if (msg.isNotBlank()) {
                                    viewModel.enviarMensaje(msg)
                                    msg = ""
                                } else {
                                    Toast.makeText(context, "Escribe un mensaje", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Enviar",
                                tint = Color(0xFF128C7E)
                            )
                        }
                    }
                }
            }
        )
    }
}







@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTopBar(navController: NavHostController, viewModel: MainViewModel) {
    TopAppBar(
        title = { Text(text = "CoLiving") },
        colors = topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.background
        ),
        actions = {
        IconButton(onClick = {
        FirebaseAuth.getInstance().signOut()
        viewModel.setUserLogged(false)
        navController.navigate(AppScreens.LoginScreen.route) {
            popUpTo(AppScreens.MainScreen.route) { inclusive = true }
        }
            }) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Cerrar sesión"
            )
            }
        }
    )
}

@Composable
fun CustomContent(padding: PaddingValues, viewModel: MainViewModel, navController: NavHostController) {
    val votaciones by viewModel.votaciones.observeAsState(emptyList())
    val gastos by viewModel.gastosCompartidos.observeAsState(emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        ActiveVotes(
            votaciones = votaciones,
            onVotacionClick = { votacion ->
                navController.navigate(AppScreens.VoteDetailScreen.createRoute(votacion.id))
            },
            viewModel = viewModel
        )

        Spacer(modifier = Modifier.height(16.dp))

        GastosCompartidos(
            gastos = gastos,
            viewModel = viewModel,
            onGastoClick = { gasto ->
                navController.navigate(AppScreens.GastoDetailScreen.createRoute(gasto.id))
            }
        )
    }
}




