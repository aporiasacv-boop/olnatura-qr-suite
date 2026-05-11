package com.olnatura.qr.core.session

import android.content.Context
import java.util.UUID
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class SessionManager(context: Context) {

    val deviceId: String = UUID.randomUUID().toString()

    private val _unauthorized = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val unauthorized = _unauthorized.asSharedFlow()

    fun onUnauthorized() {
        _unauthorized.tryEmit(Unit)
    }
}