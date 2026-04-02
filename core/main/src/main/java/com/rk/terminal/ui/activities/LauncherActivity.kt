package com.rk.terminal.ui.activities

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.rk.terminal.ui.activities.terminal.MainActivity
import com.rk.terminal.ui.activities.webview.WebViewActivity
import com.rk.terminal.utils.CodeServerInstaller
import kotlinx.coroutines.launch

class LauncherActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen
        installSplashScreen()
        
        super.onCreate(savedInstanceState)
        
        // Check if this is first launch
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val isFirstLaunch = prefs.getBoolean("is_first_launch", true)
        val codeServerExtracted = prefs.getBoolean("code_server_extracted", false)
        
        if (isFirstLaunch) {
            // First launch - check if code-server needs extraction
            if (!codeServerExtracted && !CodeServerInstaller.isCodeServerInstalled(this)) {
                // Show extraction dialog
                showExtractionDialog()
            } else {
                // Code-server already extracted, open Terminal for Ubuntu setup
                // Mark first launch complete and set flag to open terminal session
                prefs.edit()
                    .putBoolean("is_first_launch", false)
                    .putBoolean("open_terminal_for_setup", true)
                    .apply()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        } else {
            // Not first launch - open MainActivity (webview is default session)
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
    
    private fun showExtractionDialog() {
        val dialog = ProgressDialog(this).apply {
            setTitle("Extracting Code Server")
            setMessage("Please wait...")
            setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            setCancelable(false)
            show()
        }
        
        lifecycleScope.launch {
            try {
                CodeServerInstaller.extractCodeServer(this@LauncherActivity) { current, total, file ->
                    runOnUiThread {
                        dialog.max = total
                        dialog.progress = current
                        if (file.isNotEmpty()) {
                            dialog.setMessage("Extracting: $file")
                        }
                    }
                }
                
                getSharedPreferences("app_prefs", MODE_PRIVATE)
                    .edit()
                    .putBoolean("code_server_extracted", true)
                    .putBoolean("open_terminal_for_setup", true)
                    .apply()
                
                dialog.dismiss()
                
                // Now open Terminal for Ubuntu setup
                startActivity(Intent(this@LauncherActivity, MainActivity::class.java))
                finish()
            } catch (e: Exception) {
                dialog.dismiss()
                AlertDialog.Builder(this@LauncherActivity)
                    .setTitle("Extraction Failed")
                    .setMessage(e.message)
                    .setPositiveButton("Retry") { _, _ ->
                        showExtractionDialog()
                    }
                    .setNegativeButton("Skip") { _, _ ->
                        // Continue without embedded code-server (will download instead)
                        getSharedPreferences("app_prefs", MODE_PRIVATE)
                            .edit()
                            .putBoolean("code_server_extracted", true)
                            .putBoolean("open_terminal_for_setup", true)
                            .apply()
                        startActivity(Intent(this@LauncherActivity, MainActivity::class.java))
                        finish()
                    }
                    .show()
            }
        }
    }
}
