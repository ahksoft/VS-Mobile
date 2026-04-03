package com.rk.terminal.ui.activities.desktop

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File

class DesktopActivity : AppCompatActivity() {

    companion object {
        const val TERMUX_X11_PKG = "com.termux.x11"
        const val TERMUX_X11_ACTIVITY = "com.termux.x11.MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        when {
            // Already installed — launch it
            isInstalled() -> launchX11()

            // Need permission to install unknown apps
            !packageManager.canRequestPackageInstalls() -> askInstallPermission()

            // Has permission — install
            else -> installApk()
        }
    }

    override fun onResume() {
        super.onResume()
        // Check again after returning from permission settings
        if (isInstalled()) { launchX11(); return }
        if (packageManager.canRequestPackageInstalls()) installApk()
    }

    private fun isInstalled() = try {
        packageManager.getPackageInfo(TERMUX_X11_PKG, 0); true
    } catch (e: Exception) { false }

    private fun askInstallPermission() {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("VS Mobile needs permission to install Termux:X11 (the X11 display server).")
            .setPositiveButton("Allow") { _, _ ->
                startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:$packageName")))
            }
            .setNegativeButton("Cancel") { _, _ -> finish() }
            .show()
    }

    private fun installApk() {
        try {
            // Copy APK from assets to cache
            val apkFile = File(cacheDir, "termux-x11.apk")
            assets.open("termux-x11.apk").use { input ->
                apkFile.outputStream().use { input.copyTo(it) }
            }

            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", apkFile)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Install failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
        finish()
    }

    private fun launchX11() {
        try {
            startActivity(Intent().apply {
                setClassName(TERMUX_X11_PKG, TERMUX_X11_ACTIVITY)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to launch X11: ${e.message}", Toast.LENGTH_LONG).show()
        }
        finish()
    }
}
