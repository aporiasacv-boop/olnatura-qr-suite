package com.olnatura.qr.ui.screen.scanner

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.olnatura.qr.ui.components.OlnaturaCard
import com.olnatura.qr.ui.theme.OlnaturaColors
import java.util.concurrent.Executors

@Composable
fun ScannerScreen(vm: ScannerViewModel, onLoteDetected: (String) -> Unit) {
    val s by vm.state.collectAsState()
    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    Surface(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Escanea el código QR del producto", style = MaterialTheme.typography.titleLarge)

            OlnaturaCard {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(320.dp)
                    ) {
                        if (hasCameraPermission) {
                            CameraPreview(onQrText = { raw ->
                                vm.consumeQr(raw) { lote -> onLoteDetected(lote) }
                            })
                        } else {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                shape = RoundedCornerShape(22.dp),
                                border = BorderStroke(2.dp, OlnaturaColors.Green),
                                color = MaterialTheme.colorScheme.surface
                            ) {}
                            Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.Center) {
                                Text("Cámara no disponible", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(6.dp))
                                Text("Concede permiso para escanear QR.", style = MaterialTheme.typography.bodyLarge)
                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                    colors = ButtonDefaults.buttonColors(containerColor = OlnaturaColors.Green),
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier.fillMaxWidth().height(52.dp)
                                ) { Text("Conceder permiso") }
                            }
                        }
                    }
                }
            }

            if (s.error != null) {
                AssistChip(
                    onClick = { vm.clearError() },
                    label = { Text(s.error!!) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        labelColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                )
            }

            SimulateVerification(onLote = onLoteDetected)
        }
    }
}

@Composable
private fun SimulateVerification(onLote: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    var lote by remember { mutableStateOf("") }

    Button(
        onClick = { open = true },
        modifier = Modifier.fillMaxWidth().height(54.dp),
        colors = ButtonDefaults.buttonColors(containerColor = OlnaturaColors.Green),
        shape = RoundedCornerShape(18.dp)
    ) {
        Text("Simular verificación", style = MaterialTheme.typography.titleMedium)
    }

    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text("Simular verificación") },
            text = {
                OutlinedTextField(
                    value = lote,
                    onValueChange = { lote = it },
                    label = { Text("Ingresa lote") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val t = lote.trim()
                    if (t.isNotBlank()) onLote(t)
                    open = false
                }) { Text("Continuar") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancelar") } }
        )
    }
}

@SuppressLint("UnsafeOptInUsageError")
@Composable
private fun CameraPreview(onQrText: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember { BarcodeScanning.getClient() }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                val raw = barcodes.firstOrNull()?.rawValue
                                if (!raw.isNullOrBlank()) onQrText(raw)
                            }
                            .addOnCompleteListener { imageProxy.close() }
                    } else {
                        imageProxy.close()
                    }
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis
                    )
                } catch (_: Exception) {}
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}