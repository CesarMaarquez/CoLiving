package net.azarquiel.coliving.navigation

import MainScreen
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import net.azarquiel.coliving.view.GastoDetailScreen
import net.azarquiel.coliving.view.LoginScreen
import net.azarquiel.coliving.view.RegisterScreen
import net.azarquiel.coliving.view.StartScreen
import net.azarquiel.coliving.view.VoteDetailScreen
import net.azarquiel.coliving.viewmodel.GastoDetailViewModel
import net.azarquiel.coliving.viewmodel.LoginViewModel
import net.azarquiel.coliving.viewmodel.MainViewModel
import net.azarquiel.coliving.viewmodel.RegisterViewModel
import net.azarquiel.coliving.viewmodel.StartViewModel
import net.azarquiel.coliving.viewmodel.VoteDetailViewModel


@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController,
        startDestination = AppScreens.StartScreen.route){
        composable(AppScreens.MainScreen.route){
            MainScreen(navController, viewModel)
        }
        composable(AppScreens.LoginScreen.route) {
            LoginScreen(navController, LoginViewModel())
        }
        composable(AppScreens.RegisterScreen.route) {
            RegisterScreen(navController, RegisterViewModel())
        }
        composable(AppScreens.StartScreen.route) {
            StartScreen(navController, StartViewModel())
        }
        //Pasamos el id de votacion en la ruta para posteriormente votar sobre ella
        composable("VoteDetailScreen/{votacionId}") { backStackEntry ->
            val votacionId = backStackEntry.arguments?.getString("votacionId")
            val votacion = viewModel.votaciones.value?.find { it.id == votacionId }

            votacion?.let {
                VoteDetailScreen(
                    navController = navController,
                    viewModel = VoteDetailViewModel(),
                    votacion = it
                )
            }
        }
        //Pasamos el id del gasto en la ruta para posteriormente realizar pagos sobre ella
        composable("GastoDetailScreen/{gastoId}") { backStackEntry ->
            val gastoId = backStackEntry.arguments?.getString("gastoId")
            val gasto = viewModel.gastosCompartidos.value?.find { it.id == gastoId }

            gasto?.let {
                GastoDetailScreen(
                    gasto = it,
                    viewModel = GastoDetailViewModel(),
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
sealed class AppScreens(val route: String) {
    object MainScreen: AppScreens(route = "MainScreen")
    object LoginScreen: AppScreens(route = "LoginScreen")
    object RegisterScreen: AppScreens(route = "RegisterScreen")
    // Pantalla inicial de la aplicación
    object StartScreen: AppScreens(route = "StartScreen")
    object VoteDetailScreen : AppScreens("VoteDetailScreen/{votacionId}") {
        fun createRoute(votacionId: String) = "VoteDetailScreen/$votacionId"
    }
    object GastoDetailScreen : AppScreens("GastoDetailScreen/{gastoId}") {
        fun createRoute(gastoId: String) = "GastoDetailScreen/$gastoId"
    }


}
