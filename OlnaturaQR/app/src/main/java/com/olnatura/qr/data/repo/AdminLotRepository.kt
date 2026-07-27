package com.olnatura.qr.data.repo

import com.olnatura.qr.data.model.AdminCorrectLabelRequest
import com.olnatura.qr.data.model.AdminCorrectLabelResponse
import com.olnatura.qr.data.model.AdminCorrectStatusRequest
import com.olnatura.qr.data.model.AdminCorrectStatusResponse
import com.olnatura.qr.data.network.OlnaturaApi

class AdminLotRepository(
    private val api: OlnaturaApi
) {
    suspend fun correct(lote: String, body: AdminCorrectLabelRequest): AdminCorrectLabelResponse =
        api.correctLabel(lote, body)

    suspend fun correctStatus(lote: String, status: String, motivo: String): AdminCorrectStatusResponse =
        api.correctStatus(lote, AdminCorrectStatusRequest(status = status, motivo = motivo))
}
