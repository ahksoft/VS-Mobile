package com.rk.terminal.ui.activities.vnc

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.gaurav.avnc.VncLauncher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.Socket

class VncViewerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check VNC server and launch viewer
        checkVncServerAndLaunch()
    }

    private fun checkVncServerAndLaunch() {
        lifecycleScope.launch {
            val isVncRunning = withContext(Dispatchers.IO) {
                checkVncServer("localhost", 5905)
            }
            
            if (isVncRunning) {
                // VNC server is running, launch AVNC viewer
                launchVncViewer()
            } else {
                // VNC server not running, show message
                Toast.makeText(
                    this@VncViewerActivity,
                    "VNC server not running. Please run 'vncstart' first.",
                    Toast.LENGTH_LONG
                ).show()
            }
            
            // Close this activity
            finish()
        }
    }

    private fun checkVncServer(host: String, port: Int): Boolean {
        return try {
            Socket(host, port).use { socket ->
                socket.isConnected
            }
        } catch (e: IOException) {
            false
        }
    }

    private fun launchVncViewer() {
        try {
            VncLauncher.launch(
                context = this,
                host = "localhost",
                port = 5905,
                password = "123456"
            )
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Failed to launch VNC viewer: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
