package com.olnatura.qr.data.repo

import com.olnatura.qr.data.model.CreateLoteCommentRequest
import com.olnatura.qr.data.model.LoteCommentResponse
import com.olnatura.qr.data.network.OlnaturaApi

class CommentRepository(
    private val api: OlnaturaApi
) {
    suspend fun list(lote: String): List<LoteCommentResponse> =
        api.getComments(lote)

    suspend fun add(lote: String, comment: String): LoteCommentResponse =
        api.postComment(lote, CreateLoteCommentRequest(comment = comment.trim()))
}
