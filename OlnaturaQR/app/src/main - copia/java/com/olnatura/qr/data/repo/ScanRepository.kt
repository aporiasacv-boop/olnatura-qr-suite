package com.olnatura.qr.data.repo

import com.olnatura.qr.data.device.DeviceIdProvider
import com.olnatura.qr.data.model.ScanEventResponse
import com.olnatura.qr.data.network.OlnaturaApi

class ScanRepository(
    private val api: OlnaturaApi,
    private val deviceIdProvider: DeviceIdProvider
) {
    suspend fun postScan(lote: String) {
        val deviceId = deviceIdProvider.getOrCreate()
        val resp = api.postScan(lote, deviceId)
        if (!resp.isSuccessful) throw RuntimeException("POST scan failed: ${resp.code()}")
    }

    suspend fun history(lote: String): List<ScanEventResponse> =
        api.getScanHistory(lote)
}