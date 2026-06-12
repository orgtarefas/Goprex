package com.goprex.ui.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goprex.data.model.Login
import com.goprex.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val login: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val loginData: Login? = null
)

class LoginViewModel : ViewModel() {
    private val authRepository = AuthRepository()

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun updateLogin(login: String) {
        _uiState.value = _uiState.value.copy(login = login, error = null)
    }

    fun updatePassword(password: String) {
        _uiState.value = _uiState.value.copy(password = password, error = null)
    }

    fun login() {
        val currentState = _uiState.value

        // Validações de campo vazio
        if (currentState.login.isBlank()) {
            _uiState.value = currentState.copy(error = "Por favor, digite seu login")
            return
        }

        if (currentState.password.isBlank()) {
            _uiState.value = currentState.copy(error = "Por favor, digite sua senha")
            return
        }

        viewModelScope.launch {
            _uiState.value = currentState.copy(isLoading = true, error = null)

            try {
                val result = authRepository.login(currentState.login, currentState.password)

                result.fold(
                    onSuccess = { loginData ->
                        Log.d("LoginViewModel", "Login bem-sucedido: ${loginData.nome}")
                        Log.d("LoginViewModel", "Perfil: ${loginData.perfil}")
                        Log.d("LoginViewModel", "Status: ${loginData.status_ativo}")

                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isSuccess = true,
                            loginData = loginData,
                            error = null
                        )
                    },
                    onFailure = { exception ->
                        Log.e("LoginViewModel", "Erro no login", exception)

                        val errorMessage = when {
                            // Usuário desativado
                            exception.message?.contains("desativado", ignoreCase = true) == true ||
                                    exception.message?.contains("inativo", ignoreCase = true) == true ->
                                "Usuário desativado. Contate o administrador."

                            // Dados não encontrados na coleção logins
                            exception.message?.contains("não encontrados", ignoreCase = true) == true ->
                                exception.message!!

                            // Firebase Auth - Usuário não existe
                            exception.message?.contains("no user record", ignoreCase = true) == true ||
                                    exception.message?.contains("INVALID_LOGIN", ignoreCase = true) == true ||
                                    exception.message?.contains("There is no user record", ignoreCase = true) == true ->
                                "Login não encontrado. Verifique seu usuário."

                            // Firebase Auth - Senha incorreta
                            exception.message?.contains("password is invalid", ignoreCase = true) == true ||
                                    exception.message?.contains("INVALID_PASSWORD", ignoreCase = true) == true ||
                                    exception.message?.contains("The password is invalid", ignoreCase = true) == true ->
                                "Senha incorreta. Tente novamente."

                            // Firebase Auth - Email inválido
                            exception.message?.contains("email address is badly formatted", ignoreCase = true) == true ||
                                    exception.message?.contains("INVALID_EMAIL", ignoreCase = true) == true ->
                                "Formato de login inválido."

                            // Erro de rede/conexão
                            exception.message?.contains("network", ignoreCase = true) == true ||
                                    exception.message?.contains("NETWORK_ERROR", ignoreCase = true) == true ||
                                    exception.message?.contains("Unable to resolve host", ignoreCase = true) == true ->
                                "Erro de conexão. Verifique sua internet."

                            // Muitas tentativas
                            exception.message?.contains("too many", ignoreCase = true) == true ||
                                    exception.message?.contains("TOO_MANY_ATTEMPTS", ignoreCase = true) == true ->
                                "Muitas tentativas. Aguarde um momento e tente novamente."

                            // Usuário bloqueado
                            exception.message?.contains("user disabled", ignoreCase = true) == true ||
                                    exception.message?.contains("USER_DISABLED", ignoreCase = true) == true ->
                                "Usuário bloqueado. Contate o administrador."

                            // Erro genérico do Firebase
                            exception.message?.contains("firebase", ignoreCase = true) == true ->
                                "Erro no serviço de autenticação. Tente novamente."

                            // Outros erros
                            else -> exception.message ?: "Erro ao realizar login. Tente novamente."
                        }

                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = errorMessage,
                            isSuccess = false
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Erro inesperado no login", e)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erro inesperado: ${e.message}",
                    isSuccess = false
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun resetState() {
        _uiState.value = LoginUiState()
    }
}