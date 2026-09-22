package com.olnatura.qr.ui.screen.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olnatura.qr.core.credentials.SavedAccountStore
import com.olnatura.qr.data.repo.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class LoginState(
    val username: String = "",
    val password: String = "",
    val savedUsername: String? = null,
    val showSavedAccount: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null
)

class LoginViewModel(
    private val authRepo: AuthRepository,
    private val savedAccountStore: SavedAccountStore
) : ViewModel() {
    private val _state = MutableStateFlow(LoginState())
    val state = _state.asStateFlow()

    init {
        refreshSavedAccount()
    }

    fun onUsername(v: String) = _state.update { it.copy(username = v, error = null) }
    fun onPassword(v: String) = _state.update { it.copy(password = v, error = null) }

    fun refreshSavedAccount() {
        val saved = savedAccountStore.load()
        _state.update {
            it.copy(
                savedUsername = saved?.username,
                showSavedAccount = saved != null
            )
        }
    }

    fun forgetSavedAccount() {
        savedAccountStore.clear()
        _state.update {
            it.copy(
                savedUsername = null,
                showSavedAccount = false,
                username = "",
                password = "",
                error = null
            )
        }
    }

    fun loginWithSaved(onSuccess: () -> Unit) = viewModelScope.launch {
        val saved = savedAccountStore.load() ?: run {
            _state.update { it.copy(showSavedAccount = false, savedUsername = null) }
            return@launch
        }
        _state.update {
            it.copy(
                username = saved.username,
                password = saved.password,
                loading = true,
                error = null
            )
        }
        performLogin(saved.username, saved.password, onSuccess)
    }

    fun login(onSuccess: () -> Unit) = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        val s = state.value
        performLogin(s.username.trim(), s.password, onSuccess)
    }

    private suspend fun performLogin(username: String, password: String, onSuccess: () -> Unit) {
        if (username.isEmpty() || password.isEmpty()) {
            _state.update { it.copy(loading = false, error = "Usuario y contraseña requeridos") }
            return
        }
        val res = authRepo.login(username, password)
        if (res.isSuccess) {
            savedAccountStore.save(username, password)
            _state.update {
                it.copy(
                    password = "",
                    loading = false,
                    savedUsername = username,
                    showSavedAccount = true,
                    error = null
                )
            }
            onSuccess()
        } else {
            val e = res.exceptionOrNull()
            val msg = if (e is HttpException && e.code() == 401) "Credenciales inválidas" else "Error de conexión"
            if (e is HttpException && e.code() == 401 && state.value.showSavedAccount) {
                savedAccountStore.clear()
                _state.update {
                    it.copy(
                        loading = false,
                        error = msg,
                        savedUsername = null,
                        showSavedAccount = false,
                        password = ""
                    )
                }
            } else {
                _state.update { it.copy(loading = false, error = msg) }
            }
        }
    }
}
