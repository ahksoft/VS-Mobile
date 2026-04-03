package com.rk.terminal.ui.activities.desktop

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class DesktopActivity : AppCompatActivity() {

    companion object {
        const val TERMUX_X11_PKG = "com.termux.x11"
        const val TERMUX_X11_ACTIVITY = "com.termux.x11.MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        when {
            isInstalled() -> startX11AndLaunch()
            !packageManager.canRequestPackageInstalls() -> askInstallPermission()
            else -> installApk()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isInstalled()) { startX11AndLaunch(); return }
        if (packageManager.canRequestPackageInstalls()) installApk()
    }

    private fun isInstalled() = try {
        packageManager.getPackageInfo(TERMUX_X11_PKG, 0); true
    } catch (e: Exception) { false }

    private fun startX11AndLaunch() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    // Get termux-x11 APK path for CLASSPATH
                    val apkPath = packageManager.getApplicationInfo(TERMUX_X11_PKG, 0).sourceDir

                    // Kill existing X server
                    Runtime.getRuntime().exec("kill -9 \$(pgrep -f termux.x11)").waitFor()
                    delay(500)

                    // Start X server on Android host (not inside proot)
                    val env = arrayOf(
                        "CLASSPATH=$apkPath",
                        "PATH=/system/bin:/system/xbin"
                    )
                    Runtime.getRuntime().exec(
                        arrayOf("/system/bin/app_process", "/", "com.termux.x11.CmdEntryPoint", ":0"),
                        env
                    )
                    delay(2000)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Launch termux-x11 display Activity
            try {
                startActivity(Intent().apply {
                    setClassName(TERMUX_X11_PKG, TERMUX_X11_ACTIVITY)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } catch (e: Exception) {
                Toast.makeText(this@DesktopActivity, "Launch failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
            finish()
        }
    }

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
            val apkFile = File(cacheDir, "termux-x11.apk")
            assets.open("termux-x11.apk").use { it.copyTo(apkFile.outputStream()) }
            val uri = androidx.core.content.FileProvider.getUriForFile(
                this, "$packageName.fileprovider", apkFile)
            startActivity(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            Toast.makeText(this, "Install failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
        finish()
    }
}
