package com.olnatura.qr.ui.screen.report

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.olnatura.qr.ui.theme.OlnaturaColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportProblemScreen(vm: ReportProblemViewModel, lote: String, onBack: () -> Unit) {
    val s by vm.state.collectAsState()
    val ctx = LocalContext.current

    val motivos = listOf(
        "Etiqueta ilegible",
        "Datos no coinciden",
        "Producto no encontrado",
        "Otro"
    )
    var expanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reportar problema") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("←", color = MaterialTheme.colorScheme.onPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OlnaturaColors.Green,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(18.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("¿Cuál es el problema?", style = MaterialTheme.typography.titleLarge)

            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(
                    value = if (s.motivo.isBlank()) "Selecciona un motivo" else s.motivo,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    motivos.forEach { m ->
                        DropdownMenuItem(
                            text = { Text(m) },
                            onClick = { vm.setMotivo(m); expanded = false }
                        )
                    }
                }
            }

            Text("Comentarios", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = s.comentario,
                onValueChange = vm::setComentario,
                placeholder = { Text("Añade un comentario adicional.") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                shape = RoundedCornerShape(18.dp)
            )

            Button(
                onClick = {
                    Toast.makeText(ctx, "Enviado", Toast.LENGTH_SHORT).show()
                    onBack()
                },
                enabled = s.motivo.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OlnaturaColors.Green),
                shape = RoundedCornerShape(28.dp)
            ) { Text("Enviar", style = MaterialTheme.typography.titleMedium) }
        }
    }
}