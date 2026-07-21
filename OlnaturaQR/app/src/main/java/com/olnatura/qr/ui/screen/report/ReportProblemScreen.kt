package com.olnatura.qr.ui.screen.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.olnatura.qr.ui.components.OlnTopBar
import com.olnatura.qr.ui.components.PillButton
import com.olnatura.qr.ui.components.TabletContent
import com.olnatura.qr.ui.theme.OlnCream
import com.olnatura.qr.ui.theme.OlnGreen

enum class ReportMode {
    /** Problemas al escanear / datos del lote. */
    SCAN,
    /** Problemas de acceso desde la pantalla de login. */
    ACCESS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportProblemScreen(
    vm: ReportProblemViewModel,
    lote: String,
    mode: ReportMode = ReportMode.SCAN,
    onDone: () -> Unit
) {
    val motivos = when (mode) {
        ReportMode.SCAN -> listOf(
            "QR ilegible",
            "Producto no coincide",
            "Datos incompletos",
            "Producto vencido",
            "Otro"
        )
        ReportMode.ACCESS -> listOf(
            "No reconoce mi usuario",
            "No me deja acceder a mi cuenta",
            "Contraseña incorrecta / no puedo entrar",
            "Cuenta bloqueada o pendiente de aprobación",
            "Otro"
        )
    }

    var expanded by remember { mutableStateOf(false) }
    var motivo by remember { mutableStateOf<String?>(null) }
    var comentario by remember { mutableStateOf("") }
    val canSend = motivo != null

    val title = when (mode) {
        ReportMode.SCAN -> "Reportar problema"
        ReportMode.ACCESS -> "Reportar problema de acceso"
    }

    Scaffold(
        topBar = { OlnTopBar(title, onBack = onDone) },
        containerColor = OlnCream,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        TabletContent {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    if (mode == ReportMode.ACCESS) {
                        "¿Qué ocurre con tu acceso?"
                    } else {
                        "¿Cuál es el problema?"
                    },
                    style = MaterialTheme.typography.titleLarge
                )

                if (mode == ReportMode.SCAN && lote.isNotBlank()) {
                    Text("Lote: $lote", style = MaterialTheme.typography.bodyMedium)
                }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = motivo ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Selecciona un motivo") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        motivos.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item) },
                                onClick = {
                                    motivo = item
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Text("Comentarios", style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(
                    value = comentario,
                    onValueChange = { comentario = it },
                    placeholder = {
                        Text(
                            if (mode == ReportMode.ACCESS) {
                                "Indica tu usuario o correo y detalla el problema."
                            } else {
                                "Añade un comentario adicional."
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp),
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(Modifier.height(8.dp))

                PillButton(
                    text = "Enviar",
                    enabled = canSend,
                    onClick = { onDone() },
                    containerColor = OlnGreen
                )
            }
        }
    }
}
