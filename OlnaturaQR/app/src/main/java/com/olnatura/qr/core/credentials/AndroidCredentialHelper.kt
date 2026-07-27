package com.olnatura.qr.core.credentials

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPasswordOption
import androidx.credentials.PasswordCredential
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException

/**
 * Integración con Credential Manager de Android.
 * La app no persiste usuario/contraseña; solo delega en el sistema.
 */
object AndroidCredentialHelper {

    private const val TAG = "AndroidCredentials"

    data class SavedPassword(val id: String, val password: String)

    /**
     * Ofrece guardar usuario/contraseña tras un login exitoso.
     * Si el usuario cancela o el proveedor no está disponible, no falla el flujo.
     */
    suspend fun offerSavePassword(context: Context, username: String, password: String) {
        val id = username.trim()
        if (id.isEmpty() || password.isEmpty()) return
        val activity = context.findActivity() ?: return
        val cm = CredentialManager.create(context)
        try {
            cm.createCredential(
                context = activity,
                request = CreatePasswordRequest(id = id, password = password)
            )
        } catch (_: CreateCredentialCancellationException) {
            // Usuario rechazó guardar.
        } catch (e: CreateCredentialException) {
            Log.d(TAG, "No se pudo ofrecer guardar credencial: ${e.message}")
        }
    }

    /**
     * Solicita credenciales guardadas por Android (Password Manager / Credential Manager).
     * @return credencial seleccionada o null si no hay / usuario cancela.
     */
    suspend fun requestSavedPassword(context: Context): SavedPassword? {
        val activity = context.findActivity() ?: return null
        val cm = CredentialManager.create(context)
        return try {
            val result = cm.getCredential(
                context = activity,
                request = GetCredentialRequest(listOf(GetPasswordOption()))
            )
            val cred = result.credential
            if (cred is PasswordCredential) {
                SavedPassword(id = cred.id, password = cred.password)
            } else {
                null
            }
        } catch (_: NoCredentialException) {
            null
        } catch (_: GetCredentialCancellationException) {
            null
        } catch (e: GetCredentialException) {
            Log.d(TAG, "No se pudo obtener credencial: ${e.message}")
            null
        }
    }

    private fun Context.findActivity(): Activity? {
        var ctx: Context? = this
        while (ctx is android.content.ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }
}
