package com.example.stampcollectionsapp.features.auth.presentation.state

import com.example.stampcollectionsapp.features.auth.domain.model.User

sealed class AuthState {
    object Initial : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val user: User?) : AuthState()
}
