package com.olnatura.qr.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Centra el contenido y limita el ancho en tablets / landscape
 * para que formularios no queden estirados de borde a borde.
 */
@Composable
fun TabletContent(
    maxWidth: Dp = 560.dp,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopCenter,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = contentAlignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .fillMaxWidth()
        ) {
            content()
        }
    }
}
