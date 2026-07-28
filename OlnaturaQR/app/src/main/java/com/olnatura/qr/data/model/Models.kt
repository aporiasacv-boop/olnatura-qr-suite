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
    val envaseTotal: Int?,
    val cantidadPorEnvase: String? = null
)

data class DynamicDto(
    val codigo: String? = null,
    val nombre: String? = null,
    val lote: String? = null,
    val caducidad: String? = null,
    val cantidadAlmacen: Double? = null,
    /** InventoryUnitSymbol desde ReleasedProductsV2. */
    val unidadInventario: String? = null,
    /** MIN(DatePhysical) Received desde InventTrans. */
    val fechaEntrada: String? = null,
    /**
     * Estado Operativo (banner). Solo lectura: proviene de Dynamics vía OperationalStatusResolver.
     * La app nunca lo escribe. No confundir con [platformStatus].
     */
    val status: String? = null,
    val operationalStatusRule: String? = null,
    val statusSource: String? = null,
    /**
     * Estado de plataforma (`qr_labels.status`): workflow interno / corrección admin.
     * No controla el banner de Estado Operativo.
     */
    val platformStatus: String? = null,
    val statusDynamics: String? = null,
    val qualityOrderStatus: String? = null,
    val passedBatchDispositionCode: String? = null,
    val batchDispositionCode: String? = null,
    val almacen: String? = null,
    val ubicacion: String? = null,
    val fuente: String? = null,
    /**
     * Momento de la última lectura OData (ISO-8601). Solo lectura; no se persiste en la app.
     * Se actualiza en cada consulta y en «Sincronizar con Dynamics».
     */
    val lastSyncedAt: String? = null,
    /** Compat legado; preferir cantidadAlmacen. */
    val cantidad: Double? = null,
    val uom: String? = null
)

data class ScanEventResponse(
    val id: String?,
    val lote: String?,
    val scannedBy: String?,
    val userDisplay: String?,
    val roleDisplay: String?,
    val createdAt: String?,
    val deviceId: String?
)

data class LoteCommentResponse(
    val id: String?,
    val lote: String?,
    val userId: String?,
    val username: String?,
    val displayName: String?,
    val role: String?,
    val createdAt: String?,
    val comment: String?
)

data class CreateLoteCommentRequest(
    val comment: String
)

data class AdminCorrectLabelRequest(
    val motivo: String,
    val tipoMaterial: String? = null,
    val nombre: String? = null,
    val codigo: String? = null,
    val fechaEntrada: String? = null,
    val caducidad: String? = null,
    val reanalisis: String? = null,
    val envaseNum: Int? = null,
    val envaseTotal: Int? = null,
    val cantidadPorEnvase: String? = null
)

data class AdminCorrectLabelResponse(
    val id: String?,
    val lote: String?
)

data class AdminCorrectStatusRequest(
    val status: String,
    val motivo: String
)

data class AdminCorrectStatusResponse(
    val id: String?,
    val lote: String?,
    val status: String?,
    val from: String?,
    val to: String?
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