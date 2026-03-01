package com.olnatura.qr.ui.screen.requestaccess

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.olnatura.qr.ui.theme.OlnaturaColors


@Composable
fun RequestAccessScreen(
    vm: RequestAccessViewModel,
    onBackToLogin: () -> Unit
) {
    val s by vm.state.collectAsState()

    Surface(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Solicitar acceso", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "Completa el formulario. Un administrador revisará tu solicitud.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(24.dp))

            if (s.success) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = OlnaturaColors.Green.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Solicitud enviada", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text("Tu solicitud está pendiente. Un administrador la revisará pronto.")
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = onBackToLogin,
                            colors = ButtonDefaults.buttonColors(containerColor = OlnaturaColors.Green)
                        ) {
                            Text("Volver al login")
                        }
                    }
                }
                return@Surface
            }

            OutlinedTextField(
                value = s.username,
                onValueChange = vm::setUsername,
                label = { Text("Usuario") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = s.email,
                onValueChange = vm::setEmail,
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = s.password,
                onValueChange = vm::setPassword,
                label = { Text("Contraseña") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text("Rol solicitado", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = s.role == "ALMACEN",
                    onClick = { vm.setRole("ALMACEN") },
                    label = { Text("ALMACÉN") }
                )
                FilterChip(
                    selected = s.role == "INSPECCION",
                    onClick = { vm.setRole("INSPECCION") },
                    label = { Text("INSPECCIÓN") }
                )
            }

            if (s.error != null) {
                Spacer(Modifier.height(12.dp))
                AssistChip(
                    onClick = {},
                    label = { Text(s.error!!) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        labelColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                )
            }

            Spacer(Modifier.height(18.dp))
            Button(
                onClick = { vm.submit() },
                enabled = !s.busy && s.username.isNotBlank() && s.email.isNotBlank() && s.password.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OlnaturaColors.Green)
            ) {
                Text(if (s.busy) "Enviando…" else "Enviar solicitud", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onBackToLogin, modifier = Modifier.fillMaxWidth()) {
                Text("Volver al login")
            }
        }
    }
}
