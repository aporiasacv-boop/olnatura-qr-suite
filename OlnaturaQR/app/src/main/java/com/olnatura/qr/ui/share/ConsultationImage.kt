package com.olnatura.qr.ui.share

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Genera una ficha de consulta legible (no es un screenshot de la app).
 * Estilo informe: encabezado de marca, filas etiqueta|valor y estado destacado.
 */
object ConsultationImage {

    private const val BG = 0xFFF3F6EF.toInt()
    private const val CARD = 0xFFFFFFFF.toInt()
    private const val GREEN = 0xFF6F8B3A.toInt()
    private const val GREEN_DARK = 0xFF4F6328.toInt()
    private const val TEXT = 0xFF1F2A1C.toInt()
    private const val MUTED = 0xFF6B7568.toInt()
    private const val DIVIDER = 0xFFE4EBDD.toInt()
    private const val WIDTH = 1080

    fun render(payload: SharePayload): Bitmap {
        val margin = 48f
        val cardPad = 44f
        val contentW = (WIDTH - margin * 2 - cardPad * 2).toInt()

        val brandPaint = textPaint(44f, GREEN_DARK, bold = true)
        val titlePaint = textPaint(30f, MUTED, bold = false)
        val productPaint = textPaint(46f, TEXT, bold = true)
        val lotePaint = textPaint(34f, GREEN_DARK, bold = true)
        val labelPaint = textPaint(26f, MUTED, bold = false)
        val valuePaint = textPaint(32f, TEXT, bold = true)
        val footerPaint = textPaint(24f, MUTED, bold = false)
        val statusPaint = textPaint(36f, statusColors(payload.status).second, bold = true)

        val productLayout = staticLayout(payload.nombre.ifBlank { "—" }, productPaint, contentW)
        val rows = listOf(
            "Lote" to payload.lote,
            "Código" to payload.codigo,
            "Escaneado hoy" to payload.escaneadoHoy,
            "Ubicación" to payload.ubicacion,
            "Almacén" to payload.almacen,
            "Inventario disponible" to payload.inventario,
            "Estado Dynamics" to payload.statusDynamics,
            "Fecha de entrada" to payload.fechaEntrada,
            "Fecha de caducidad" to payload.caducidad
        )

        val labelColW = (contentW * 0.42f).toInt()
        val valueColW = contentW - labelColW - 24
        val rowLayouts = rows.map { (label, value) ->
            val l = staticLayout(label, labelPaint, labelColW)
            val v = staticLayout(value.ifBlank { "—" }, valuePaint, valueColW)
            Triple(l, v, maxOf(l.height, v.height).toFloat())
        }

        val headerBarH = 18f
        val topBlock =
            36f + // brand
                10f +
                34f + // subtitle
                28f +
                productLayout.height +
                18f +
                40f + // lote line
                28f

        val rowsH = rowLayouts.sumOf { it.third.toDouble() }.toFloat() +
            (rowLayouts.size - 1) * 28f + // spacing between rows
            rowLayouts.size * 1f // dividers

        val statusH = 88f
        val footerH = 40f
        val cardInnerH = topBlock + rowsH + 36f + statusH + 28f + footerH
        val height = (margin + headerBarH + 28f + cardInnerH + cardPad * 2 + margin).toInt()

        val bmp = Bitmap.createBitmap(WIDTH, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(BG)

        // Barra superior de marca
        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = GREEN }
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), headerBarH, barPaint)

        // Tarjeta
        val cardTop = margin + headerBarH
        val cardRect = RectF(margin, cardTop, WIDTH - margin, cardTop + cardInnerH + cardPad * 2)
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = CARD }
        canvas.drawRoundRect(cardRect, 28f, 28f, cardPaint)

        // Sutil borde
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = DIVIDER
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(cardRect, 28f, 28f, borderPaint)

        var y = cardTop + cardPad

        // Encabezado
        canvas.drawText("OLNATURA QR", margin + cardPad, y + 36f, brandPaint)
        y += 36f + 14f
        canvas.drawText("Ficha de consulta de lote", margin + cardPad, y + 28f, titlePaint)
        y += 28f + 26f

        // Producto (hero)
        canvas.save()
        canvas.translate(margin + cardPad, y)
        productLayout.draw(canvas)
        canvas.restore()
        y += productLayout.height + 16f

        canvas.drawText("Lote  ${payload.lote.ifBlank { "—" }}", margin + cardPad, y + 30f, lotePaint)
        y += 30f + 26f

        // Separador
        drawDivider(canvas, margin + cardPad, WIDTH - margin - cardPad, y)
        y += 22f

        // Filas etiqueta | valor
        rowLayouts.forEachIndexed { index, (labelLayout, valueLayout, rowH) ->
            canvas.save()
            canvas.translate(margin + cardPad, y)
            labelLayout.draw(canvas)
            canvas.restore()

            canvas.save()
            canvas.translate(margin + cardPad + labelColW + 24f, y)
            valueLayout.draw(canvas)
            canvas.restore()

            y += rowH + 14f
            if (index < rowLayouts.lastIndex) {
                drawDivider(canvas, margin + cardPad, WIDTH - margin - cardPad, y)
                y += 14f
            }
        }

        y += 28f

        // Estado
        val (statusBg, statusFg) = statusColors(payload.status)
        statusPaint.color = statusFg
        val statusRect = RectF(
            margin + cardPad,
            y,
            WIDTH - margin - cardPad,
            y + statusH
        )
        val statusBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = statusBg }
        canvas.drawRoundRect(statusRect, 20f, 20f, statusBgPaint)

        val statusText = payload.status.ifBlank { "DESCONOCIDO" }.uppercase(Locale.getDefault())
        val statusW = statusPaint.measureText(statusText)
        canvas.drawText(
            statusText,
            statusRect.centerX() - statusW / 2f,
            statusRect.centerY() + 12f,
            statusPaint
        )

        y += statusH + 24f

        val stamped = SimpleDateFormat("d MMM yyyy · HH:mm", Locale("es", "MX")).format(Date())
        canvas.drawText("Generado desde Olnatura QR · $stamped", margin + cardPad, y + 22f, footerPaint)

        return bmp
    }

    private fun drawDivider(canvas: Canvas, left: Float, right: Float, y: Float) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = DIVIDER
            strokeWidth = 2f
        }
        canvas.drawLine(left, y, right, y, p)
    }

    private fun textPaint(size: Float, color: Int, bold: Boolean): TextPaint {
        return TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            typeface = Typeface.create(
                Typeface.SANS_SERIF,
                if (bold) Typeface.BOLD else Typeface.NORMAL
            )
        }
    }

    private fun staticLayout(text: String, paint: TextPaint, width: Int): StaticLayout {
        val safeW = width.coerceAtLeast(1)
        return StaticLayout.Builder
            .obtain(text, 0, text.length, paint, safeW)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.15f)
            .setIncludePad(false)
            .build()
    }

    private fun statusColors(status: String): Pair<Int, Int> {
        val s = status.trim().uppercase(Locale.getDefault())
        return when {
            s in listOf("APROBADO", "LIBERADO", "VERIFICADO", "INSUMO VERIFICADO") ->
                0xFFDDF2D7.toInt() to 0xFF2E6B2E.toInt()
            s == "RECHAZADO" ->
                0xFFFEE2E2.toInt() to 0xFF991B1B.toInt()
            s in listOf("CUARENTENA", "PENDIENTE") ->
                0xFFFEF3C7.toInt() to 0xFF92400E.toInt()
            else ->
                0xFFF3F4F6.toInt() to 0xFF6B7280.toInt()
        }
    }

    fun saveToGallery(context: Context, payload: SharePayload): Boolean {
        return try {
            val bitmap = render(payload)
            val safeLote = payload.lote.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val name = "olnatura_lote_${safeLote}_${System.currentTimeMillis()}.png"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/OlnaturaQR")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return false
            resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            } ?: return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            bitmap.recycle()
            Toast.makeText(context, "Imagen guardada en Galería / OlnaturaQR", Toast.LENGTH_LONG).show()
            true
        } catch (_: Exception) {
            Toast.makeText(context, "No se pudo guardar la imagen", Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun cacheShareUri(context: Context, payload: SharePayload): Uri? {
        return try {
            val dir = File(context.cacheDir, "share").apply { mkdirs() }
            val safeLote = payload.lote.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val file = File(dir, "consulta_${safeLote}.png")
            FileOutputStream(file).use { out ->
                render(payload).compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (_: Exception) {
            null
        }
    }

    fun shareImage(context: Context, payload: SharePayload, packageName: String?) {
        val uri = cacheShareUri(context, payload) ?: run {
            Toast.makeText(context, "No se pudo preparar la imagen", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, payload.asText())
            putExtra(Intent.EXTRA_SUBJECT, "Olnatura QR — Lote ${payload.lote}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (!packageName.isNullOrBlank()) setPackage(packageName)
        }
        if (packageName != null && intent.resolveActivity(context.packageManager) == null) {
            intent.setPackage(null)
            context.startActivity(Intent.createChooser(intent, "Compartir"))
        } else {
            context.startActivity(intent)
        }
    }
}
