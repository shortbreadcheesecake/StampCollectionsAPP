package com.example.stampcollectionsapp.features.auth.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stampcollectionsapp.features.auth.domain.model.User
import com.example.stampcollectionsapp.features.auth.domain.repository.AuthRepository
import com.example.stampcollectionsapp.features.auth.presentation.state.AuthState
import com.example.stampcollectionsapp.features.auth.presentation.state.LoginState
import com.example.stampcollectionsapp.features.auth.presentation.state.RegisterState
import com.google.firebase.auth.FirebaseAuthException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState = _authState.asStateFlow()

    private val _loginState = MutableStateFlow(LoginState())
    val loginState = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow(RegisterState())
    val registerState = _registerState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.authState.collectLatest { isAuthenticated ->
                _authState.value = if (isAuthenticated) {
                    AuthState.Authenticated(authRepository.currentUser)
                } else {
                    AuthState.Unauthenticated
                }
            }
        }
    }

    fun onLoginEmailChange(email: String) {
        _loginState.value = _loginState.value.copy(email = email)
    }

    fun onLoginPasswordChange(password: String) {
        _loginState.value = _loginState.value.copy(password = password)
    }

    fun onRegisterEmailChange(email: String) {
        _registerState.value = _registerState.value.copy(email = email)
    }

    fun onRegisterPasswordChange(password: String) {
        _registerState.value = _registerState.value.copy(password = password)
    }

    fun onRegisterNameChange(name: String) {
        _registerState.value = _registerState.value.copy(name = name)
    }

    fun onConfirmPasswordChange(confirmPassword: String) {
        _registerState.value = _registerState.value.copy(confirmPassword = confirmPassword)
    }

    fun clearLoginError() {
        val currentState = _loginState.value
        _loginState.value = currentState.copy(error = null, isLoading = false)
    }

    fun clearRegisterError() {
        val currentState = _registerState.value
        _registerState.value = currentState.copy(error = null, isLoading = false)
    }
    
    fun resetLoginState() {
        _loginState.value = LoginState()
    }
    
    fun resetRegisterState() {
        _registerState.value = RegisterState()
    }

    fun login() {
        viewModelScope.launch {
            try {
                println("=== LOGIN STARTED ===")
                
                // Получаем текущие значения ПЕРЕД установкой состояния
                val currentState = _loginState.value
                val email = currentState.email.trim()
                val password = currentState.password
                
                println("Current state - email: '$email', password length: ${password.length}")
                
                // Валидация ДО установки isLoading
                when {
                    email.isBlank() -> {
                        println("Validation failed: email is blank")
                        _loginState.value = currentState.copy(
                            isLoading = false,
                            error = "Введите электронную почту"
                        )
                        return@launch
                    }
                    password.isBlank() -> {
                        println("Validation failed: password is blank")
                        _loginState.value = currentState.copy(
                            isLoading = false,
                            error = "Введите пароль"
                        )
                        return@launch
                    }
                }
                
                // Только после успешной валидации устанавливаем загрузку
                _loginState.value = currentState.copy(isLoading = true, error = null)
                println("Validation passed, calling repository...")
                
                val result = authRepository.signInWithEmailAndPassword(
                    email = email,
                    password = password
                )
                
                println("Repository call completed")
                
                result.fold(
                    onSuccess = {
                        println("Login SUCCESS")
                        _loginState.value = _loginState.value.copy(
                            isLoading = false,
                            error = null
                        )
                    },
                    onFailure = { exception ->
                        println("Login FAILED: ${exception.javaClass.simpleName} - ${exception.message}")
                        println("Exception errorCode: ${(exception as? FirebaseAuthException)?.errorCode}")
                        val errorMessage = when (exception) {
                            is FirebaseAuthException -> {
                                when (exception.errorCode) {
                                    "auth/user-not-found", "ERROR_USER_NOT_FOUND" -> "Аккаунт с такой почтой не найден"
                                    "auth/wrong-password", "ERROR_WRONG_PASSWORD" -> "Неверный пароль"
                                    "auth/invalid-email", "ERROR_INVALID_EMAIL" -> "Введите корректный email"
                                    "auth/network-request-failed", "ERROR_NETWORK_REQUEST_FAILED" -> "Ошибка сети. Проверьте подключение к интернету"
                                    "auth/user-disabled", "ERROR_USER_DISABLED" -> "Аккаунт заблокирован"
                                    "auth/too-many-requests", "ERROR_TOO_MANY_REQUESTS" -> "Слишком много попыток. Попробуйте позже"
                                    "auth/email-already-in-use", "ERROR_EMAIL_ALREADY_IN_USE" -> "Аккаунт с такой почтой уже зарегистрирован"
                                    "auth/weak-password", "ERROR_WEAK_PASSWORD" -> "Слишком слабый пароль"
                                    "auth/operation-not-allowed", "ERROR_OPERATION_NOT_ALLOWED" -> "Операция не разрешена"
                                    "auth/invalid-credential", "ERROR_INVALID_CREDENTIAL" -> "Неверные учетные данные"
                                    else -> {
                                        // Если errorCode неизвестен, проверяем сообщение об ошибке
                                        val message = exception.message ?: ""
                                        when {
                                            message.contains("badly formatted", ignoreCase = true) -> "Введите корректный email"
                                            message.contains("invalid email", ignoreCase = true) -> "Введите корректный email"
                                            message.contains("user not found", ignoreCase = true) -> "Аккаунт с такой почтой не найден"
                                            message.contains("wrong password", ignoreCase = true) -> "Неверный пароль"
                                            message.contains("network", ignoreCase = true) -> "Ошибка сети. Проверьте подключение к интернету"
                                            message.contains("email already in use", ignoreCase = true) -> "Аккаунт с такой почтой уже зарегистрирован"
                                            message.contains("weak password", ignoreCase = true) -> "Слишком слабый пароль"
                                            else -> "Ошибка входа. Попробуйте снова"
                                        }
                                    }
                                }
                            }
                            else -> {
                                val message = exception.message ?: ""
                                when {
                                    message.contains("badly formatted", ignoreCase = true) -> "Введите корректный email"
                                    message.contains("invalid email", ignoreCase = true) -> "Введите корректный email"
                                    message.contains("user not found", ignoreCase = true) -> "Аккаунт с такой почтой не найден"
                                    message.contains("wrong password", ignoreCase = true) -> "Неверный пароль"
                                    message.contains("network", ignoreCase = true) -> "Ошибка сети. Проверьте подключение к интернету"
                                    else -> "Ошибка входа. Попробуйте снова"
                                }
                            }
                        }
                        _loginState.value = _loginState.value.copy(
                            isLoading = false,
                            error = errorMessage
                        )
                    }
                )
            } catch (e: Exception) {
                println("Login EXCEPTION: ${e.javaClass.simpleName} - ${e.message}")
                e.printStackTrace()
                val errorMessage = when (e) {
                    is FirebaseAuthException -> {
                        when (e.errorCode) {
                            "auth/user-not-found", "ERROR_USER_NOT_FOUND" -> "Аккаунт с такой почтой не найден"
                            "auth/wrong-password", "ERROR_WRONG_PASSWORD" -> "Неверный пароль"
                            "auth/invalid-email", "ERROR_INVALID_EMAIL" -> "Введите корректный email"
                            "auth/network-request-failed", "ERROR_NETWORK_REQUEST_FAILED" -> "Ошибка сети. Проверьте подключение к интернету"
                            else -> {
                                val message = e.message ?: ""
                                when {
                                    message.contains("badly formatted", ignoreCase = true) -> "Введите корректный email"
                                    message.contains("invalid email", ignoreCase = true) -> "Введите корректный email"
                                    else -> "Ошибка входа. Попробуйте снова"
                                }
                            }
                        }
                    }
                    else -> {
                        val message = e.message ?: ""
                        when {
                            message.contains("badly formatted", ignoreCase = true) -> "Введите корректный email"
                            message.contains("invalid email", ignoreCase = true) -> "Введите корректный email"
                            else -> "Ошибка входа. Попробуйте снова"
                        }
                    }
                }
                _loginState.value = _loginState.value.copy(
                    isLoading = false,
                    error = errorMessage
                )
            }
        }
    }

    fun register() {
        viewModelScope.launch {
            try {
                println("=== REGISTRATION STARTED ===")
                
                // Получаем текущие значения ПЕРЕД установкой состояния
                val currentState = _registerState.value
                val email = currentState.email.trim()
                val password = currentState.password
                val confirmPassword = currentState.confirmPassword
                val name = currentState.name.trim()
                
                println("Current state - email: '$email', name: '$name', password length: ${password.length}")
                
                // Валидация ДО установки isLoading
                when {
                    name.isBlank() -> {
                        println("Validation failed: name is blank")
                        _registerState.value = currentState.copy(
                            isLoading = false,
                            error = "Введите ФИО"
                        )
                        return@launch
                    }
                    email.isBlank() -> {
                        println("Validation failed: email is blank")
                        _registerState.value = currentState.copy(
                            isLoading = false,
                            error = "Введите электронную почту"
                        )
                        return@launch
                    }
                    password.isBlank() -> {
                        println("Validation failed: password is blank")
                        _registerState.value = currentState.copy(
                            isLoading = false,
                            error = "Введите пароль"
                        )
                        return@launch
                    }
                    confirmPassword.isBlank() -> {
                        println("Validation failed: confirmPassword is blank")
                        _registerState.value = currentState.copy(
                            isLoading = false,
                            error = "Подтвердите пароль"
                        )
                        return@launch
                    }
                    password.length < 6 -> {
                        println("Validation failed: password too short")
                        _registerState.value = currentState.copy(
                            isLoading = false,
                            error = "Пароль должен содержать не менее 6 символов"
                        )
                        return@launch
                    }
                    password != confirmPassword -> {
                        println("Validation failed: passwords don't match")
                        _registerState.value = currentState.copy(
                            isLoading = false,
                            error = "Пароли не совпадают"
                        )
                        return@launch
                    }
                }
                
                // Только после успешной валидации устанавливаем загрузку
                _registerState.value = currentState.copy(isLoading = true, error = null)
                println("Validation passed, calling repository...")
                
                val result = authRepository.signUpWithEmailAndPassword(
                    email = email,
                    password = password,
                    name = name
                )
                
                println("Repository call completed")
                
                result.fold(
                    onSuccess = {
                        println("Registration SUCCESS")
                        _registerState.value = _registerState.value.copy(
                            isLoading = false,
                            error = null
                        )
                    },
                    onFailure = { exception ->
                        println("Registration FAILED: ${exception.javaClass.simpleName} - ${exception.message}")
                        println("Exception errorCode: ${(exception as? FirebaseAuthException)?.errorCode}")
                        val errorMessage = when (exception) {
                            is FirebaseAuthException -> {
                                when (exception.errorCode) {
                                    "auth/email-already-in-use", "ERROR_EMAIL_ALREADY_IN_USE" -> "Аккаунт с такой почтой уже зарегистрирован"
                                    "auth/invalid-email", "ERROR_INVALID_EMAIL" -> "Введите корректный email"
                                    "auth/weak-password", "ERROR_WEAK_PASSWORD" -> "Слишком слабый пароль. Используйте более сложный пароль"
                                    "auth/network-request-failed", "ERROR_NETWORK_REQUEST_FAILED" -> "Ошибка сети. Проверьте подключение к интернету"
                                    "auth/too-many-requests", "ERROR_TOO_MANY_REQUESTS" -> "Слишком много попыток. Попробуйте позже"
                                    "auth/operation-not-allowed", "ERROR_OPERATION_NOT_ALLOWED" -> "Операция не разрешена"
                                    else -> {
                                        // Если errorCode неизвестен, проверяем сообщение об ошибке
                                        val message = exception.message ?: ""
                                        when {
                                            message.contains("badly formatted", ignoreCase = true) -> "Введите корректный email"
                                            message.contains("invalid email", ignoreCase = true) -> "Введите корректный email"
                                            message.contains("email already in use", ignoreCase = true) -> "Аккаунт с такой почтой уже зарегистрирован"
                                            message.contains("weak password", ignoreCase = true) -> "Слишком слабый пароль. Используйте более сложный пароль"
                                            message.contains("network", ignoreCase = true) -> "Ошибка сети. Проверьте подключение к интернету"
                                            else -> "Ошибка регистрации. Попробуйте снова"
                                        }
                                    }
                                }
                            }
                            else -> {
                                val message = exception.message ?: ""
                                when {
                                    message.contains("badly formatted", ignoreCase = true) -> "Введите корректный email"
                                    message.contains("invalid email", ignoreCase = true) -> "Введите корректный email"
                                    message.contains("email already in use", ignoreCase = true) -> "Аккаунт с такой почтой уже зарегистрирован"
                                    message.contains("weak password", ignoreCase = true) -> "Слишком слабый пароль. Используйте более сложный пароль"
                                    message.contains("network", ignoreCase = true) -> "Ошибка сети. Проверьте подключение к интернету"
                                    else -> "Ошибка регистрации. Попробуйте снова"
                                }
                            }
                        }
                        _registerState.value = _registerState.value.copy(
                            isLoading = false,
                            error = errorMessage
                        )
                    }
                )
            } catch (e: Exception) {
                println("Registration EXCEPTION: ${e.javaClass.simpleName} - ${e.message}")
                e.printStackTrace()
                val errorMessage = when (e) {
                    is FirebaseAuthException -> {
                        when (e.errorCode) {
                            "auth/email-already-in-use", "ERROR_EMAIL_ALREADY_IN_USE" -> "Аккаунт с такой почтой уже зарегистрирован"
                            "auth/invalid-email", "ERROR_INVALID_EMAIL" -> "Введите корректный email"
                            "auth/weak-password", "ERROR_WEAK_PASSWORD" -> "Слишком слабый пароль. Используйте более сложный пароль"
                            "auth/network-request-failed", "ERROR_NETWORK_REQUEST_FAILED" -> "Ошибка сети. Проверьте подключение к интернету"
                            else -> {
                                val message = e.message ?: ""
                                when {
                                    message.contains("badly formatted", ignoreCase = true) -> "Введите корректный email"
                                    message.contains("invalid email", ignoreCase = true) -> "Введите корректный email"
                                    message.contains("email already in use", ignoreCase = true) -> "Аккаунт с такой почтой уже зарегистрирован"
                                    else -> "Ошибка регистрации. Попробуйте снова"
                                }
                            }
                        }
                    }
                    else -> {
                        val message = e.message ?: ""
                        when {
                            message.contains("badly formatted", ignoreCase = true) -> "Введите корректный email"
                            message.contains("invalid email", ignoreCase = true) -> "Введите корректный email"
                            message.contains("email already in use", ignoreCase = true) -> "Аккаунт с такой почтой уже зарегистрирован"
                            else -> "Ошибка регистрации. Попробуйте снова"
                        }
                    }
                }
                _registerState.value = _registerState.value.copy(
                    isLoading = false,
                    error = errorMessage
                )
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }

    fun resetPassword(email: String, onComplete: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = authRepository.resetPassword(email)
            onComplete(result)
        }
    }
    
    suspend fun updateProfile(name: String, photoUrl: String?): Result<Unit> {
        val result = authRepository.updateUserProfile(name, photoUrl)
        // Принудительно обновляем authState после изменения профиля
        if (result.isSuccess) {
            // Обновляем authState с новыми данными пользователя
            _authState.value = AuthState.Authenticated(authRepository.currentUser)
        }
        return result
    }
    
    suspend fun deleteAccount(): Result<Unit> {
        val result = authRepository.deleteAccount()
        // После удаления аккаунта пользователь автоматически разлогинится
        // authState обновится через AuthStateListener в AuthRepositoryImpl
        return result
    }
}
