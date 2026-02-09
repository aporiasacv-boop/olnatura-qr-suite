package com.olnatura.qr.ui.screen.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olnatura.qr.data.repo.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class LoginState(
    val username: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null
)

class LoginViewModel(private val authRepo: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(LoginState())
    val state = _state.asStateFlow()

    fun onUsername(v: String) = _state.update { it.copy(username = v, error = null) }
    fun onPassword(v: String) = _state.update { it.copy(password = v, error = null) }

    fun login(onSuccess: () -> Unit) = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        val s = state.value
        val res = authRepo.login(s.username.trim(), s.password)
        if (res.isSuccess) {
            _state.update { it.copy(password = "", loading = false) }
            onSuccess()
        } else {
            val e = res.exceptionOrNull()
            val msg = if (e is HttpException && e.code() == 401) "Credenciales inválidas" else "Error de conexión"
            _state.update { it.copy(loading = false, error = msg) }
        }
    }
}