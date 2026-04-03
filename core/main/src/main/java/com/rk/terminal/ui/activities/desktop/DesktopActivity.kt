package com.rk.terminal.ui.activities.desktop

import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DesktopActivity : AppCompatActivity() {

    companion object {
        const val TERMUX_X11_PKG = "com.termux.x11"
        const val TERMUX_X11_ACTIVITY = "com.termux.x11.MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isInstalled()) launchX11() else installAndLaunch()
    }

    private fun isInstalled() = try {
        packageManager.getPackageInfo(TERMUX_X11_PKG, 0); true
    } catch (e: Exception) { false }

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

    private fun installAndLaunch() {
        Toast.makeText(this, "Installing Termux:X11...", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) { installApk() }
            if (ok) {
                delay(3000) // wait for install to complete
                if (isInstalled()) launchX11()
                else Toast.makeText(this@DesktopActivity,
                    "Install Termux:X11 manually from Settings > Install unknown apps", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this@DesktopActivity,
                    "Install failed. Enable 'Install unknown apps' permission.", Toast.LENGTH_LONG).show()
            }
            finish()
        }
    }

    private fun installApk(): Boolean = try {
        val bytes = assets.open("termux-x11.apk").readBytes()
        val installer = packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            session.openWrite("termux-x11.apk", 0, bytes.size.toLong()).use { it.write(bytes); session.fsync(it) }
            val pi = PendingIntent.getActivity(this, 0,
                Intent(this, DesktopActivity::class.java),
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            session.commit(pi.intentSender)
        }
        true
    } catch (e: Exception) { e.printStackTrace(); false }
}
