package com.olnatura.qr.ui.sheet

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareBottomSheet(
    lote: String,
    status: String,
    onDismiss: () -> Unit
) {
    val ctx = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val text = remember(lote, status) { "Olnatura QR\nLote: $lote\nStatus: $status" }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Compartir", style = MaterialTheme.typography.titleLarge)

            ShareRow("WhatsApp") { shareViaPackageOrChooser(ctx, text, "com.whatsapp") }
            ShareRow("Mensajes") { shareViaChooser(ctx, text) }
            ShareRow("Correo") { shareViaEmail(ctx, text) }
            ShareRow("Copiar…") { copyToClipboard(ctx, text) }
            ShareRow("Más…") { shareViaChooser(ctx, text) }

            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun ShareRow(title: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(22.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
    }
}

private fun shareViaChooser(ctx: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    ctx.startActivity(Intent.createChooser(intent, "Compartir"))
}

private fun shareViaPackageOrChooser(ctx: Context, text: String, pkg: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        setPackage(pkg)
    }
    if (intent.resolveActivity(ctx.packageManager) != null) {
        ctx.startActivity(intent)
    } else {
        shareViaChooser(ctx, text)
    }
}

private fun shareViaEmail(ctx: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "message/rfc822"
        putExtra(Intent.EXTRA_SUBJECT, "Olnatura QR")
        putExtra(Intent.EXTRA_TEXT, text)
    }
    val chooser = Intent.createChooser(intent, "Enviar correo")
    ctx.startActivity(chooser)
}

private fun copyToClipboard(ctx: Context, text: String) {
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("Olnatura QR", text))
    Toast.makeText(ctx, "Copiado", Toast.LENGTH_SHORT).show()
}