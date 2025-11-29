package com.example.stampcollectionsapp.features.auth.data.repository

import com.example.stampcollectionsapp.features.auth.domain.model.User
import com.example.stampcollectionsapp.features.auth.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth
) : AuthRepository {

    private val _currentUser = MutableStateFlow(auth.currentUser?.toUser())
    private val _authState = MutableStateFlow(auth.currentUser != null)

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser?.toUser()
            _authState.value = firebaseAuth.currentUser != null
        }
    }

    override val currentUser: User?
        get() = _currentUser.value

    override val authState: StateFlow<Boolean> = _authState

    override suspend fun signInWithEmailAndPassword(email: String, password: String): Result<Unit> {
        return try {
            println("Attempting to sign in with email: $email")
            val result = auth.signInWithEmailAndPassword(email, password).await()
            println("Sign in successful: ${result.user?.uid}")
            Result.success(Unit)
        } catch (e: Exception) {
            println("Sign in failed: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun signUpWithEmailAndPassword(email: String, password: String, name: String): Result<Unit> {
        return try {
            println("Attempting to create user with email: $email")
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            println("User created: ${authResult.user?.uid}")
            
            authResult.user?.let { user ->
                println("Updating user profile with name: $name")
                user.updateProfile(
                    com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()
                )?.await()
                println("User profile updated successfully")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            println("Sign up failed: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun signInWithCredential(credential: AuthCredential): Result<Unit> {
        return try {
            auth.signInWithCredential(credential).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut() {
        auth.signOut()
    }

    override suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateUserProfile(name: String, photoUrl: String?): Result<Unit> {
        return try {
            val updates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .apply {
                    if (photoUrl != null) {
                        setPhotoUri(android.net.Uri.parse(photoUrl))
                    }
                }
                .build()
            
            auth.currentUser?.updateProfile(updates)?.await()
            
            // Принудительно обновляем текущего пользователя после изменения профиля
            auth.currentUser?.reload()?.await()
            _currentUser.value = auth.currentUser?.toUser()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteAccount(): Result<Unit> {
        return try {
            auth.currentUser?.delete()?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun FirebaseUser.toUser(): User {
        return User(
            id = uid,
            email = email ?: "",
            displayName = displayName ?: "",
            photoUrl = photoUrl?.toString()
        )
    }
}
