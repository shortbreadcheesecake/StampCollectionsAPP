package com.example.stampcollectionsapp.features.auth.domain.repository

import com.example.stampcollectionsapp.features.auth.domain.model.User
import com.google.firebase.auth.AuthCredential
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: User?
    val authState: Flow<Boolean>
    
    suspend fun signInWithEmailAndPassword(email: String, password: String): Result<Unit>
    suspend fun signUpWithEmailAndPassword(email: String, password: String, name: String): Result<Unit>
    suspend fun signInWithCredential(credential: AuthCredential): Result<Unit>
    suspend fun signOut()
    suspend fun resetPassword(email: String): Result<Unit>
    suspend fun updateUserProfile(name: String, photoUrl: String? = null): Result<Unit>
    suspend fun deleteAccount(): Result<Unit>
}
