package com.olnatura.qr.data.repo

import com.olnatura.qr.data.model.CreateProblemReportRequest
import com.olnatura.qr.data.network.OlnaturaApi
import retrofit2.HttpException

class ProblemReportRepository(
    private val api: OlnaturaApi
) {
    suspend fun submit(
        kind: String,
        lote: String?,
        reason: String,
        comment: String
    ): Result<Unit> {
        val resp = api.createProblemReport(
            CreateProblemReportRequest(
                kind = kind,
                lote = lote?.takeIf { it.isNotBlank() },
                reason = reason.trim(),
                comment = comment.trim().ifBlank { null }
            )
        )
        return when {
            resp.isSuccessful -> Result.success(Unit)
            else -> Result.failure(HttpException(resp))
        }
    }
}
