package com.olnatura.qr.data.repo

import com.olnatura.qr.data.model.QrResponse
import com.olnatura.qr.data.network.OlnaturaApi

class QrRepository(private val api: OlnaturaApi) {
    suspend fun getQr(lote: String): QrResponse = api.getQr(lote)
}