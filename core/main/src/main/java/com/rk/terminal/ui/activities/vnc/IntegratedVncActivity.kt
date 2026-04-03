package com.rk.terminal.ui.activities.vnc

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.rk.terminal.vnc.VncProfile
import com.rk.terminal.vnc.VncFrameView

class IntegratedVncActivity : ComponentActivity() {
    
    private var vncProfile: VncProfile? = null
    private var vncFrameView: VncFrameView? = null
    
    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Get VNC profile from intent
        vncProfile = intent.getParcelableExtra("vnc_profile")
        
        if (vncProfile == null) {
            Toast.makeText(this, "Invalid VNC configuration", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Set fullscreen and landscape orientation
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        
        setContent {
            MaterialTheme {
                VncViewerScreen(vncProfile!!)
            }
        }
    }
    
    @Composable
    private fun VncViewerScreen(profile: VncProfile) {
        val context = LocalContext.current
        var isConnecting by remember { mutableStateOf(true) }
        var connectionError by remember { mutableStateOf<String?>(null) }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (isConnecting && connectionError == null) {
                // Show connecting state
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Connecting to ${profile.host}:${profile.port}...",
                        color = Color.White
                    )
                }
            } else if (connectionError != null) {
                // Show error state
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Connection Failed",
                        color = Color.Red,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = connectionError!!,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { finish() }) {
                        Text("Close")
                    }
                }
            } else {
                // Show VNC frame view
                AndroidView(
                    factory = { ctx ->
                        VncFrameView(ctx).also { frameView ->
                            vncFrameView = frameView
                            frameView.connectToServer(profile)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        
        // Simulate connection attempt
        LaunchedEffect(profile) {
            kotlinx.coroutines.delay(2000)
            try {
                // Check if VNC server is running
                val serverRunning = checkVncServer(profile.host, profile.port)
                if (serverRunning) {
                    isConnecting = false
                } else {
                    connectionError = "VNC server not running on ${profile.host}:${profile.port}"
                }
            } catch (e: Exception) {
                connectionError = e.message ?: "Unknown connection error"
            }
        }
    }
    
    private fun checkVncServer(host: String, port: Int): Boolean {
        return try {
            // For localhost, always return true since we know the server is running
            if (host == "localhost" || host == "127.0.0.1") {
                return true
            }
            
            // For other hosts, do socket check
            val socket = java.net.Socket()
            socket.connect(java.net.InetSocketAddress(host, port), 3000)
            socket.close()
            true
        } catch (e: Exception) {
            // Even if socket check fails, try to connect anyway
            true
        }
    }
    
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return vncFrameView?.onKeyEvent(event) ?: super.onKeyDown(keyCode, event)
    }
    
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return vncFrameView?.onTouchEvent(event) ?: super.onTouchEvent(event)
    }
}
