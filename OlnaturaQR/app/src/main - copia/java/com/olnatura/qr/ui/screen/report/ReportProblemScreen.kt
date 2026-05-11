package com.olnatura.qr.ui.screen.report

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.olnatura.qr.ui.components.OlnTopBar
import com.olnatura.qr.ui.components.PillButton
import com.olnatura.qr.ui.theme.OlnCream
import com.olnatura.qr.ui.theme.OlnGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportProblemScreen(
    vm: ReportProblemViewModel,
    lote: String,
    onDone: () -> Unit
) {
    val motivos = listOf(
        "QR ilegible",
        "Producto no coincide",
        "Datos incompletos",
        "Producto vencido",
        "Otro"
    )

    var expanded by remember { mutableStateOf(false) }
    var motivo by remember { mutableStateOf<String?>(null) }
    var comentario by remember { mutableStateOf("") }
    val canSend = motivo != null

    Scaffold(
        topBar = { OlnTopBar("Reportar problema", onBack = onDone) },
        containerColor = OlnCream
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("¿Cuál es el problema?", style = MaterialTheme.typography.titleLarge)

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
                placeholder = { Text("Añade un comentario adicional.") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(Modifier.weight(1f))

            PillButton(
                text = "Enviar",
                enabled = canSend,
                onClick = {
                    // Aquí luego llamas vm.submit(lote, motivo, comentario)
                    onDone()
                },
                containerColor = OlnGreen
            )
        }
    }
}