package com.olnatura.qr.ui.screen.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.olnatura.qr.ui.components.PillButton
import com.olnatura.qr.ui.components.TabletContent
import com.olnatura.qr.ui.theme.OlnaturaColors
import com.olnatura.qr.ui.theme.OlnGreen

@Composable
fun LoginScreen(
    vm: LoginViewModel,
    onRequestAccess: () -> Unit = {},
    onReport: () -> Unit = {},
    onLoggedIn: () -> Unit
) {
    val s by vm.state.collectAsState()

    Surface(modifier = Modifier.fillMaxSize()) {
        TabletContent(contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OlnaturaColors.Green)
                ) {
                    Text(
                        if (s.loading) "Iniciando..." else "Iniciar sesión",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onRequestAccess, modifier = Modifier.fillMaxWidth()) {
                    Text("Solicitar acceso")
                }

                Spacer(Modifier.height(8.dp))
                PillButton(
                    text = "Reportar",
                    onClick = onReport,
                    containerColor = OlnGreen,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
