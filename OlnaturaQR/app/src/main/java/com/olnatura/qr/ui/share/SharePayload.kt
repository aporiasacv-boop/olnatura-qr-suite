package com.olnatura.qr.ui.share

data class SharePayload(
    val lote: String,
    val status: String,
    val nombre: String = "—",
    val codigo: String = "—",
    val ubicacion: String = "—",
    val almacen: String = "—",
    val inventario: String = "—",
    val statusDynamics: String = "—",
    val fechaEntrada: String = "—",
    val caducidad: String = "—",
    val escaneadoHoy: String = "—"
) {
    fun asText(): String = buildString {
        appendLine("Olnatura QR — Consulta de lote")
        appendLine("Lote: $lote")
        appendLine("Estado: $status")
        appendLine("Nombre: $nombre")
        appendLine("Código: $codigo")
        appendLine("Ubicación: $ubicacion")
        appendLine("Almacén: $almacen")
        appendLine("Inventario disponible: $inventario")
        appendLine("Estado Dynamics: $statusDynamics")
        appendLine("Fecha de entrada: $fechaEntrada")
        appendLine("Fecha de caducidad: $caducidad")
        appendLine("Escaneado hoy: $escaneadoHoy")
    }
}
