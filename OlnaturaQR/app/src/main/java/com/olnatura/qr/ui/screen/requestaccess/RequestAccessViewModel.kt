package com.olnatura.qr.ui.screen.requestaccess

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olnatura.qr.data.repo.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RequestAccessState(
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val role: String = "ALMACEN",
    val busy: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

class RequestAccessViewModel(private val authRepo: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(RequestAccessState())
    val state = _state.asStateFlow()

    fun setUsername(v: String) = _state.update { it.copy(username = v, error = null) }
    fun setEmail(v: String) = _state.update { it.copy(email = v, error = null) }
    fun setPassword(v: String) = _state.update { it.copy(password = v, error = null) }
    fun setRole(v: String) = _state.update { it.copy(role = v) }

    fun submit() = viewModelScope.launch {
        val s = _state.value
        if (s.username.isBlank() || s.email.isBlank() || s.password.isBlank()) {
            _state.update { it.copy(error = "Completa todos los campos") }
            return@launch
        }
        _state.update { it.copy(busy = true, error = null) }
        authRepo.requestAccess(s.username, s.email, s.password, s.role)
            .onSuccess {
                _state.update { it.copy(busy = false, success = true) }
            }
            .onFailure { e ->
                _state.update {
                    it.copy(
                        busy = false,
                        error = e.message ?: "Error al enviar solicitud"
                    )
                }
            }
    }
}
