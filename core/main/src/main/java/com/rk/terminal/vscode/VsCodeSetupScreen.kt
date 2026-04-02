package com.rk.terminal.vscode

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun VsCodeSetupScreen(onSetupComplete: () -> Unit) {
    var progress by remember { mutableFloatStateOf(0f) }
    
    LaunchedEffect(Unit) {
        // Show loading progress for 2 seconds
        val steps = 20
        for (i in 1..steps) {
            kotlinx.coroutines.delay(100)
            progress = i.toFloat() / steps
        }
        
        onSetupComplete()
    }
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                progress = progress,
                modifier = Modifier.size(64.dp)
            )
            Text("Starting VS Code...")
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.width(200.dp)
            )
            Text("${(progress * 100).toInt()}%")
        }
    }
}
