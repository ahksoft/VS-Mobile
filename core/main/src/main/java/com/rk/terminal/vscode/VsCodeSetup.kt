package com.rk.terminal.vscode

import android.util.Log
import com.rk.libcommons.application
import com.rk.libcommons.child
import com.rk.libcommons.localBinDir
import com.rk.libcommons.ubuntuHomeDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object VsCodeSetup {
    private const val TAG = "VsCodeSetup"
    
    fun isSetupComplete(): Boolean {
        val ubuntuRoot = File(ubuntuHomeDir().parent, "ubuntu")
        val markerFile = File(ubuntuRoot, "root/.vscode_setup_done")
        return markerFile.exists()
    }
}
