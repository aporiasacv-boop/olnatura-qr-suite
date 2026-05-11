package com.olnatura.qr.data.model

data class LoginRequest(val username: String, val password: String)

data class RequestAccessRequest(
    val username: String,
    val email: String,
    val password: String,
    val roleRequested: String
)

data class RequestAccessResponse(val requestId: String, val status: String)

data class MeResponse(
    val id: String,
    val username: String,
    val roles: List<String>
)

data class QrResponse(
    val label: LabelDto,
    val dynamic: DynamicDto?
)

data class LabelDto(
    val tipoMaterial: String?,
    val nombre: String?,
    val codigo: String?,
    val lote: String?,
    val fechaEntrada: String?,
    val caducidad: String?,
    val reanalisis: String?,
    val envaseNum: Int?,
    val envaseTotal: Int?
)

data class DynamicDto(
    val status: String?,
    val cantidad: Double?,
    val uom: String?,
    val ubicacion: String?,
    val fuente: String?
)

data class ScanEventResponse(
    val id: String?,
    val lote: String?,
    val createdAt: String?,
    val deviceId: String?
)
data class LoginResponse(
    val user: UserResponse
)

data class UserResponse(
    val id: String,
    val username: String,
    val email: String?,
    val roles: List<String>
)