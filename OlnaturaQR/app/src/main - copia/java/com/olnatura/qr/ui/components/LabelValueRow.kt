package com.olnatura.qr.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.olnatura.qr.ui.theme.OlnTextMuted

@Composable
fun LabelValueRow(
    label: String,
    value: String,
    showDivider: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        Text(label, color = OlnTextMuted)
        Text(value)
        if (showDivider) {
            HorizontalDivider(modifier = Modifier.padding(top = 10.dp))
        }
    }
}