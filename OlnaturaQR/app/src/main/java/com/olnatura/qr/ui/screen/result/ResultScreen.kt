package com.olnatura.qr.ui.screen.result

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.olnatura.qr.ui.components.OlnaturaCard
import com.olnatura.qr.ui.theme.OlnaturaColors

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ResultScreen(
    vm: ResultViewModel,
    lote: String,
    onReport: (String) -> Unit,
    onShare: (String, String) -> Unit
) {
    val s by vm.state.collectAsState()

    LaunchedEffect(lote) { vm.load(lote) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Materia prima verificada") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OlnaturaColors.Green,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Surface(Modifier.fillMaxSize()) {
            Column(
                Modifier.padding(padding).padding(18.dp).fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (s.loading) LinearProgressIndicator(Modifier.fillMaxWidth())

                // GATE: NO logeado => solo mensaje, sin datos, sin compartir
                if (s.gate is GateState.Unauthorized) {
                    OlnaturaCard {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Acceso restringido", style = MaterialTheme.typography.titleLarge)
                            Text(
                                "Espera autorización para ver la información. Un administrador debe darte de alta.",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    return@Column
                }

                if (s.notFound) {
                    OlnaturaCard {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("No encontrado", style = MaterialTheme.typography.titleLarge)
                            Text("El lote “$lote” no existe en el sistema.", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    return@Column
                }

                if (s.error != null) {
                    OlnaturaCard {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Text("Error", style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(6.dp))
                            Text(s.error!!, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }

                val qr = s.qr
                if (qr != null && s.gate is GateState.Authorized) {
                    val label = qr.label
                    val status = qr.dynamic?.status ?: "—" // permitido mostrar SOLO status

                    OlnaturaCard {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(label.nombre ?: "—", style = MaterialTheme.typography.titleLarge)
                            InfoRow("Lote", label.lote ?: "—")
                            InfoRow("Código", label.codigo ?: "—")
                            InfoRow("Escaneado hoy", "V: ${s.todayCount}")
                            InfoRow("Status", status)
                            InfoRow("Lote en el que se usará", "—")
                        }
                    }

                    Surface(
                        color = OlnaturaColors.GreenLight,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.fillMaxWidth().padding(14.dp)) {
                            Text("✓ Producto verificado", style = MaterialTheme.typography.titleMedium)
                        }
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { onShare(label.lote ?: lote, status) },
                            modifier = Modifier.weight(1f).height(54.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OlnaturaColors.Green),
                            shape = RoundedCornerShape(24.dp)
                        ) { Text("Compartir", style = MaterialTheme.typography.titleMedium) }

                        OutlinedButton(
                            onClick = { onReport(label.lote ?: lote) },
                            modifier = Modifier.weight(1f).height(54.dp),
                            shape = RoundedCornerShape(24.dp)
                        ) { Text("Reportar problema", style = MaterialTheme.typography.titleMedium) }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(title: String, value: String) {
    Column {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}