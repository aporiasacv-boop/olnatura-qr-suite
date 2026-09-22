package com.olnatura.qr.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.olnatura.qr.ui.theme.OlnGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OlnTopBar(
    title: String,
    onBack: (() -> Unit)? = null
) {
    TopAppBar(
        title = { Text(title, color = Color.White) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }
        },
        windowInsets = WindowInsets.statusBars,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = OlnGreen,
            scrolledContainerColor = OlnGreen
        )
    )
}
