package com.example.stampcollectionsapp.features.auth.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.stampcollectionsapp.features.auth.presentation.screens.LoginScreen
import com.example.stampcollectionsapp.features.auth.presentation.screens.RegisterScreen
import com.example.stampcollectionsapp.features.auth.presentation.viewmodel.AuthViewModel

sealed class AuthScreen(val route: String) {
    object Login : AuthScreen("login")
    object Register : AuthScreen("register")
}

@Composable
fun AuthNavigation(
    onAuthSuccess: () -> Unit,
    navController: NavHostController = rememberNavController(),
    viewModel: AuthViewModel
) {
    NavHost(
        navController = navController,
        startDestination = AuthScreen.Login.route
    ) {
        composable(AuthScreen.Login.route) {
            LoginScreen(
                onNavigateToRegister = {
                    viewModel.resetLoginState()
                    navController.navigate(AuthScreen.Register.route) {
                        popUpTo(AuthScreen.Login.route) {
                            inclusive = false
                        }
                    }
                },
                onLoginSuccess = onAuthSuccess
            )
        }

        composable(AuthScreen.Register.route) {
            RegisterScreen(
                onBackClick = { 
                    viewModel.resetRegisterState()
                    navController.popBackStack() 
                },
                onRegisterSuccess = onAuthSuccess
            )
        }
    }
}
