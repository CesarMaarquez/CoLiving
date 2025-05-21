package net.azarquiel.coliving

import MainScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import net.azarquiel.coliving.navigation.AppNavigation
import net.azarquiel.coliving.ui.theme.CoLivingTheme
import net.azarquiel.coliving.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val viewModel = MainViewModel(this)
        setContent {
            CoLivingTheme {
                AppNavigation(viewModel)
            }
        }
    }
}
