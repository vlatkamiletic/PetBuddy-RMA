package com.example.petbuddy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.petbuddy.ui.theme.PetBuddyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PetBuddyTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {

        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate("register")
                }
            )
        }

        composable("register") {
            RegisterScreen(
                onRegistrationSuccess = {
                    navController.navigate("home") {
                        popUpTo("register") { inclusive = true }
                    }
                },
                onBackToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable("home") {
            HomeScreen(
                navController = navController,
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }

        composable("add_pet") {
            AddPetScreen(
                onPetAdded = {
                    navController.navigate("home") {
                        popUpTo("add_pet") { inclusive = true }
                    }
                },
                onCancel = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "pet_details/{petId}/{petName}",
            arguments = listOf(
                navArgument("petId") { type = NavType.StringType },
                navArgument("petName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val petId = backStackEntry.arguments?.getString("petId") ?: ""
            val petName = backStackEntry.arguments?.getString("petName") ?: ""
            PetDetailsScreen(navController, petId, petName)
        }

    }
}
