package com.rk.terminal.ui.activities.desktop

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
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

        if (isTermuxX11Installed()) {
            launchX11()
        } else {
            installAndLaunch()
        }
    }

    private fun isTermuxX11Installed(): Boolean {
        return try {
            packageManager.getPackageInfo(TERMUX_X11_PKG, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun launchX11() {
        try {
            val intent = Intent().apply {
                setClassName(TERMUX_X11_PKG, TERMUX_X11_ACTIVITY)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to launch X11: ${e.message}", Toast.LENGTH_LONG).show()
        }
        finish()
    }

    private fun installAndLaunch() {
        Toast.makeText(this, "Installing X11 display server...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            val success = withContext(Dispatchers.IO) {
                installApk()
            }
            if (success) {
                // Wait a moment for install to complete
                kotlinx.coroutines.delay(2000)
                launchX11()
            } else {
                Toast.makeText(this@DesktopActivity,
                    "X11 install failed. Install termux-x11 manually.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun installApk(): Boolean {
        return try {
            val apkBytes = assets.open("termux-x11.apk").readBytes()
            val installer = packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            val sessionId = installer.createSession(params)
            val session = installer.openSession(sessionId)

            session.openWrite("termux-x11.apk", 0, apkBytes.size.toLong()).use { out ->
                out.write(apkBytes)
                session.fsync(out)
            }

            val intent = Intent(this, DesktopActivity::class.java)
            val pi = android.app.PendingIntent.getActivity(
                this, 0, intent,
                android.app.PendingIntent.FLAG_MUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
            )
            session.commit(pi.intentSender)
            session.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
