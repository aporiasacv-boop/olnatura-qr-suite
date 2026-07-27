package com.olnatura.qr.ui.screen.requestaccess

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olnatura.qr.data.email.CorporateEmailSuggestions
import com.olnatura.qr.data.email.UsedEmailSuggestionsStore
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
    val success: Boolean = false,
    val emailSuggestions: List<String> = emptyList(),
    val usedEmails: Set<String> = emptySet()
)

class RequestAccessViewModel(
    private val authRepo: AuthRepository,
    private val usedEmailStore: UsedEmailSuggestionsStore
) : ViewModel() {

    private val _state = MutableStateFlow(RequestAccessState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val used = usedEmailStore.loadUsed()
            _state.update {
                it.copy(
                    usedEmails = used,
                    emailSuggestions = CorporateEmailSuggestions.available(used, it.email)
                )
            }
        }
    }

    fun setUsername(v: String) = _state.update { it.copy(username = v, error = null) }

    fun setEmail(v: String) = _state.update { s ->
        s.copy(
            email = v,
            error = null,
            emailSuggestions = CorporateEmailSuggestions.available(s.usedEmails, v)
        )
    }

    fun selectEmailSuggestion(email: String) = setEmail(email)

    fun setPassword(v: String) = _state.update { it.copy(password = v, error = null) }
    fun setRole(v: String) = _state.update { it.copy(role = v) }

    fun submit() = viewModelScope.launch {
        val s = _state.value
        if (s.username.isBlank() || s.email.isBlank() || s.password.isBlank()) {
            _state.update { it.copy(error = "Completa todos los campos") }
            return@launch
        }
        _state.update { it.copy(busy = true, error = null) }
        val emailNorm = s.email.trim()
        authRepo.requestAccess(s.username, emailNorm, s.password, s.role)
            .onSuccess {
                usedEmailStore.markUsed(emailNorm)
                val used = usedEmailStore.loadUsed()
                _state.update {
                    it.copy(
                        busy = false,
                        success = true,
                        usedEmails = used,
                        emailSuggestions = CorporateEmailSuggestions.available(used, "")
                    )
                }
            }
            .onFailure { e ->
                val msg = e.message.orEmpty()
                if (msg.contains("ya existe", ignoreCase = true)) {
                    usedEmailStore.markUsed(emailNorm)
                }
                val used = usedEmailStore.loadUsed()
                _state.update {
                    it.copy(
                        busy = false,
                        error = e.message ?: "Error al enviar solicitud",
                        usedEmails = used,
                        emailSuggestions = CorporateEmailSuggestions.available(used, it.email)
                    )
                }
            }
    }
}
