import android.app.DatePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
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
    Scaffold(
        topBar = { CustomTopBar(navController,viewModel) },
        content = { padding ->

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {

                // Contenido principal
                CustomContent(padding, viewModel)

                // FAB izquierdo con menú
                FabMenu(
                    viewModel = viewModel,
                    onCrearVotacion = {
                        viewModel.dialogDetailVotacion.value = true
                    },
                    onCrearGasto = {
                        // Navegación o lógica
                    }
                )

                // FAB derecho personalizado
                CustomFAB(viewModel)

                // Diálogo flotante adicional si lo tienes
                DialogFab(viewModel)

                if (viewModel.dialogDetailVotacion.value == true) {
                    Dialog(
                        onDismissRequest = { viewModel.dialogDetailVotacion.value = false }
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            tonalElevation = 8.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            CreateVoteScreen(
                                viewModel = viewModel,
                                onClose = { viewModel.dialogDetailVotacion.value = false }
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


//ESTA FUNCION IRIA DENTRO DE UNA OPCION DE CREAR VOTACION QUE CXREAREMOS DESPUES (MENU CON DISTINTAS OPCIONES)
@Composable
fun CreateVoteScreen(
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
                text = deadline?.let { dateFormatter.format(Date(it)) } ?: "No seleccionada",
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
                        pregunta = votacion.pregunta,
                        opciones = votacion.opciones,
                        fechaLimite = votacion.fechaLimite,
                        anonima = votacion.anonima,
                        onSuccess = {
                            Toast.makeText(context, "Votación creada", Toast.LENGTH_SHORT).show()
                            onClose()
                        },
                        onFailure = {
                            //Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_LONG).show()
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
fun ActiveVotesScreen(
    votaciones: List<Votacion>,
    onVotacionClick: (Votacion) -> Unit
) {
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
                            text = "Fecha límite: ${dateFormatter.format(java.util.Date(votacion.fechaLimite))}",
                            style = MaterialTheme.typography.bodySmall
                        )

                        Text(
                            text = if (votacion.anonima) "Votación anónima" else "Votación pública",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VoteDetailScreen(
    votacion: Votacion,
    userId: String, // desde FirebaseAuth.getInstance().currentUser?.uid
    onVotoRealizado: () -> Unit // para navegar atrás o mostrar mensaje
) {
    var selectedOption by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }



    Column(modifier = Modifier
        .padding(16.dp)
        .verticalScroll(rememberScrollState())
    ) {
        Text("Votación", fontSize = 20.sp, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(12.dp))

        Text(text = votacion.pregunta, style = MaterialTheme.typography.titleLarge)

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Fecha límite: ${dateFormatter.format(java.util.Date(votacion.fechaLimite))}",
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
                    enviarVotoAFirebase(
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


fun enviarVotoAFirebase(
    votacionId: String,
    opcionSeleccionada: String,
    userId: String?, // si es null, se guarda como anónimo
    context: Context,
    onSuccess: () -> Unit
) {
//    val db = Firebase.firestore
//    val votosRef = db.collection("votaciones").document(votacionId).collection("votos")
//
//    val voto = hashMapOf(
//        "opcion" to opcionSeleccionada,
//        "fecha" to FieldValue.serverTimestamp()
//    )
//    if (userId != null) {
//        voto["usuario"] = userId
//    }
//
//    votosRef.add(voto)
//        .addOnSuccessListener {
//            Toast.makeText(context, "Voto registrado", Toast.LENGTH_SHORT).show()
//            onSuccess()
//        }
//        .addOnFailureListener {
//            Toast.makeText(context, "Error al votar", Toast.LENGTH_SHORT).show()
//        }
}


//@Composable
//fun DialogFab(viewModel: MainViewModel) {
//    val context = LocalContext.current
//    val openDialog = viewModel.openDialog.observeAsState(false)
//    var msg by remember { mutableStateOf("") }
//    var isErrorEmptyNombre by remember { mutableStateOf(true) }
//    if (openDialog.value) {
//        AlertDialog(
//            title = { Text(text = "Add Post") },
//            text = {
//                Column{
//                    TextField(
//                        modifier = Modifier.padding(bottom = 30.dp),
//                        value = msg,
//                        onValueChange = {
//                            msg = it
//                            isErrorEmptyNombre = msg.isEmpty() },
//                        label = { Text("Message") },
//                        placeholder = { Text("Message") },
//                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = "Edit") },
//                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
//                        singleLine = true
//                    )
//                }},
//            onDismissRequest = {  // Si pulsamos fuera cierra
//                viewModel.setDialog(false)
//            },
//            confirmButton = {
//                Button(
//                    onClick = {
//                        if (msg.isEmpty()) {
//                            Toast.makeText( context, "required fields", Toast.LENGTH_LONG).show()
//                        }
//                        else {
//                            //viewModel.addPost(Post("César", msg))
//                            viewModel.setDialog(false)
//                            msg = ""
//                        }})
//                { Text("Ok") }
//            },
//            dismissButton = {
//                Button(
//                    onClick = { viewModel.setDialog(false) })
//                { Text("Cancel") }
//            }
//        )
//    }
//}

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




@Composable
fun DialogFab(viewModel: MainViewModel) {
    val context = LocalContext.current
    val openDialog = viewModel.openDialog.observeAsState(false)
    var msg by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<String>() }

    if (openDialog.value) {
        AlertDialog(
            onDismissRequest = { viewModel.setDialog(false) },
            confirmButton = {},
            dismissButton = {},
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(500.dp) // Altura tipo chat
                        .padding(8.dp)
                ) {
                    // Área de mensajes
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color(0xFFEFEFEF))
                            .padding(8.dp),
                        reverseLayout = true // Nuevos mensajes al final pero visualmente arriba
                    ) {
                        items(messages.reversed()) { message ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = message,
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .background(Color.White, shape = RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    color = Color.Black
                                )
                            }
                        }
                    }

                    // Barra de entrada de texto
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
//                            colors = TextFieldDefaults.textFieldColors(
//                                backgroundColor = Color.White
//                            ),
                            shape = RoundedCornerShape(24.dp),
                            singleLine = true
                        )
                        IconButton(
                            onClick = {
                                if (msg.isNotBlank()) {
                                    messages.add("Tú: $msg")
                                    msg = ""
                                } else {
                                    Toast.makeText(context, "Escribe un mensaje", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Enviar",
                                tint = Color(0xFF075E54) // Verde estilo WhatsApp
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
fun CustomContent(padding: PaddingValues, viewModel: MainViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
    )
    {
        // a pintar
        val votaciones = viewModel.votaciones.observeAsState(emptyList())

//        ActiveVotesScreen(
//            votaciones = votaciones.value,
//            onVotacionClick = { votacion ->
//                //navController.navigate("votacionDetalle/${votacion.id}")
//            }
//        )

        CreateVoteScreen(
            viewModel = viewModel,
            onClose = {}
        )
    }
}



