package com.example.stampcollectionsapp.features.auth.presentation.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.stampcollectionsapp.features.auth.presentation.navigation.AuthNavigation
import com.example.stampcollectionsapp.features.auth.presentation.state.AuthState
import com.example.stampcollectionsapp.features.auth.presentation.viewmodel.AuthViewModel

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val authState = viewModel.authState.collectAsState().value

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onAuthSuccess()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (authState) {
            is AuthState.Initial -> {
                CircularProgressIndicator()
            }
            is AuthState.Unauthenticated -> {
                AuthNavigation(
                    onAuthSuccess = onAuthSuccess,
                    viewModel = viewModel
                )
            }
            is AuthState.Authenticated -> {
                // This will be handled by the LaunchedEffect above
            }
        }
    }
}
