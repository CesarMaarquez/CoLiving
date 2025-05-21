package net.azarquiel.coliving.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import net.azarquiel.coliving.navigation.AppScreens
import net.azarquiel.coliving.viewmodel.MainViewModel

@Composable
fun StartScreen(navController: NavHostController, viewModel: MainViewModel) {
    val user = FirebaseAuth.getInstance().currentUser

    LaunchedEffect(Unit) {
        delay(1000) // Pequeña pausa para simular una carga inicial
        if (user != null) {
            navController.navigate(AppScreens.MainScreen.route) {
                popUpTo(AppScreens.StartScreen.route) { inclusive = true }
            }
        } else {
            navController.navigate(AppScreens.LoginScreen.route) {
                popUpTo(AppScreens.StartScreen.route) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
