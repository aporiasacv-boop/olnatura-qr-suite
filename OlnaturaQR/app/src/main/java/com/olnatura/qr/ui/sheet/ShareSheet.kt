package com.olnatura.qr.ui.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.olnatura.qr.ui.share.ConsultationImage
import com.olnatura.qr.ui.share.SharePayload

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareBottomSheet(
    payload: SharePayload,
    onDismiss: () -> Unit
) {
    val ctx = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Compartir", style = MaterialTheme.typography.titleLarge)

            ShareRow("WhatsApp") {
                ConsultationImage.shareImage(ctx, payload, "com.whatsapp")
                onDismiss()
            }
            ShareRow("Gmail") {
                ConsultationImage.shareImage(ctx, payload, "com.google.android.gm")
                onDismiss()
            }
            ShareRow("Guardar imagen") {
                if (ConsultationImage.saveToGallery(ctx, payload)) {
                    onDismiss()
                }
            }

            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun ShareRow(title: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(22.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
    }
}
