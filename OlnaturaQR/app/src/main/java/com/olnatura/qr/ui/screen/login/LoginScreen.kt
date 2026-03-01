package com.olnatura.qr.ui.screen.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.olnatura.qr.ui.theme.OlnaturaColors

@Composable
fun LoginScreen(
    vm: LoginViewModel,
    onRequestAccess: () -> Unit = {},
    onLoggedIn: () -> Unit
) {
    val s by vm.state.collectAsState()

    Surface(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(18.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("Olnatura QR", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text("Inicia sesión para escanear y registrar.", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = s.username,
                onValueChange = vm::onUsername,
                label = { Text("Usuario") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = s.password,
                onValueChange = vm::onPassword,
                label = { Text("Contraseña") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

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
                onClick = { vm.login(onLoggedIn) },
                enabled = !s.loading && s.username.isNotBlank() && s.password.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OlnaturaColors.Green)
            ) {
                Text(if (s.loading) "Iniciando..." else "Iniciar sesión", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onRequestAccess, modifier = Modifier.fillMaxWidth()) {
                Text("Solicitar acceso")
            }
        }
    }
}