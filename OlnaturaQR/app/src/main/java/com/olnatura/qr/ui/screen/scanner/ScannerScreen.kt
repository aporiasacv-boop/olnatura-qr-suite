package com.olnatura.qr.ui.screen.scanner

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
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
    val configuration = LocalConfiguration.current

    LaunchedEffect(Unit) {
        vm.resetForNewScan()
    }

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

    Surface(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp)
        ) {
            val landscape = maxWidth > maxHeight

            if (landscape) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 320.dp)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text("Escanea el código QR del producto", style = MaterialTheme.typography.titleLarge)
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
                    }

                    OlnaturaCard(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        CameraPreviewPane(
                            hasCameraPermission = hasCameraPermission,
                            orientationKey = configuration.orientation,
                            onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            onQrText = { raw -> vm.consumeQr(raw) { lote -> onLoteDetected(lote) } }
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Escanea el código QR del producto", style = MaterialTheme.typography.titleLarge)

                    OlnaturaCard(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        CameraPreviewPane(
                            hasCameraPermission = hasCameraPermission,
                            orientationKey = configuration.orientation,
                            onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            onQrText = { raw -> vm.consumeQr(raw) { lote -> onLoteDetected(lote) } }
                        )
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
                }
            }
        }
    }
}

@Composable
private fun CameraPreviewPane(
    hasCameraPermission: Boolean,
    orientationKey: Int,
    onRequestPermission: () -> Unit,
    onQrText: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        if (hasCameraPermission) {
            CameraPreview(
                orientationKey = orientationKey,
                onQrText = onQrText
            )
        } else {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(2.dp, OlnaturaColors.Green),
                color = MaterialTheme.colorScheme.surface
            ) {}
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Cámara no disponible", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text("Concede permiso para escanear QR.", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(containerColor = OlnaturaColors.Green),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) { Text("Conceder permiso") }
            }
        }
    }
}

@SuppressLint("UnsafeOptInUsageError")
@Composable
private fun CameraPreview(
    orientationKey: Int,
    onQrText: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember { BarcodeScanning.getClient() }
    val latestOnQrText by rememberUpdatedState(onQrText)

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    // Remonta la preview al girar la tablet (configChanges en el Manifest).
    key(orientationKey) {
        DisposableEffect(orientationKey) {
            onDispose {
                try {
                    ProcessCameraProvider.getInstance(context).get().unbindAll()
                } catch (_: Exception) {
                }
            }
        }

        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    try {
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
                                val image = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )
                                scanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        val raw = barcodes.firstOrNull()?.rawValue
                                        if (!raw.isNullOrBlank()) latestOnQrText(raw)
                                    }
                                    .addOnCompleteListener { imageProxy.close() }
                            } else {
                                imageProxy.close()
                            }
                        }

                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analysis
                        )
                    } catch (_: Exception) {
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
