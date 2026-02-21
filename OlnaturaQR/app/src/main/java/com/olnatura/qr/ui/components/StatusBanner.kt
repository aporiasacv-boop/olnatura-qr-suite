package com.olnatura.qr.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.olnatura.qr.ui.theme.OlnSuccessBg
import com.olnatura.qr.ui.theme.OlnSuccessText

@Composable
fun SuccessBanner(text: String, modifier: Modifier = Modifier) {
    StatusBanner(text = text, bgColor = OlnSuccessBg, textColor = OlnSuccessText, modifier = modifier)
}

/**
 * Muestra el estatus dinámico (APROBADO/LIBERADO, RECHAZADO, CUARENTENA, etc.) con color según tipo.
 */
@Composable
fun StatusBanner(
    text: String,
    modifier: Modifier = Modifier,
    bgColor: Color = OlnSuccessBg,
    textColor: Color = OlnSuccessText,
    icon: @Composable (() -> Unit)? = null
) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                icon()
                Spacer(Modifier.width(8.dp))
            }
            Text(text, color = textColor)
        }
    }
}

fun statusColors(status: String?): Pair<Color, Color> {
    val s = (status ?: "").trim().uppercase()
    return when {
        s in listOf("APROBADO", "LIBERADO", "VERIFICADO") -> OlnSuccessBg to OlnSuccessText
        s in listOf("RECHAZADO") -> Color(0xFFFEE2E2) to Color(0xFF991B1B)
        s in listOf("CUARENTENA", "PENDIENTE") -> Color(0xFFFEF3C7) to Color(0xFF92400E)
        else -> Color(0xFFF3F4F6) to Color(0xFF6B7280)
    }
}