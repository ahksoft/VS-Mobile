package com.rk.terminal.ui.screens.terminal

import android.os.Environment
import com.rk.libcommons.ubuntuDir
import com.rk.libcommons.ubuntuHomeDir
import com.rk.libcommons.ShizukuHelper
import com.rk.libcommons.application
import com.rk.libcommons.child
import com.rk.libcommons.createFileIfNot
import com.rk.libcommons.localBinDir
import com.rk.libcommons.localDir
import com.rk.libcommons.localLibDir
import com.rk.libcommons.pendingCommand
import com.rk.settings.Settings
import com.rk.terminal.App
import com.rk.terminal.App.Companion.getTempDir
import com.rk.terminal.BuildConfig
import com.rk.terminal.ui.activities.terminal.MainActivity
import com.rk.terminal.ui.screens.settings.WorkingMode
import com.termux.terminal.TerminalEmulator
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import java.io.File
import java.io.FileOutputStream

object MkSession {
    fun createSession(
        activity: MainActivity, sessionClient: TerminalSessionClient, session_id: String,workingMode:Int
    ): TerminalSession {
        with(activity) {
            val envVariables = mapOf(
                "ANDROID_ART_ROOT" to System.getenv("ANDROID_ART_ROOT"),
                "ANDROID_DATA" to System.getenv("ANDROID_DATA"),
                "ANDROID_I18N_ROOT" to System.getenv("ANDROID_I18N_ROOT"),
                "ANDROID_ROOT" to System.getenv("ANDROID_ROOT"),
                "ANDROID_RUNTIME_ROOT" to System.getenv("ANDROID_RUNTIME_ROOT"),
                "ANDROID_TZDATA_ROOT" to System.getenv("ANDROID_TZDATA_ROOT"),
                "BOOTCLASSPATH" to System.getenv("BOOTCLASSPATH"),
                "DEX2OATBOOTCLASSPATH" to System.getenv("DEX2OATBOOTCLASSPATH"),
                "EXTERNAL_STORAGE" to System.getenv("EXTERNAL_STORAGE")
            )

            val workingDir = pendingCommand?.workingDir ?: ubuntuHomeDir().path

            val initFile: File = localBinDir().child("init-host")

            if (initFile.exists().not()){
                initFile.createFileIfNot()
                initFile.writeText(assets.open("init-host.sh").bufferedReader().use { it.readText() })
            }


            localBinDir().child("init").apply {
                if (exists().not()){
                    createFileIfNot()
                    writeText(assets.open("init.sh").bufferedReader().use { it.readText() })
                }
            }

            // Copy Shizuku wrapper script
            localBinDir().child("shizuku").apply {
                if (exists().not()){
                    createFileIfNot()
                    writeText(assets.open("shizuku").bufferedReader().use { it.readText() })
                    setExecutable(true)
                }
            }

            // Copy vscode lounch script
            localBinDir().child("vsc.sh").apply {
                if (exists().not()){
                    createFileIfNot()
                    writeText(assets.open("vsc.sh").bufferedReader().use { it.readText() })
                    setExecutable(true)
                }
            }

            // Copy server script
            localBinDir().child("server").apply {
                if (exists().not()){
                    createFileIfNot()
                    writeText(assets.open("server.sh").bufferedReader().use { it.readText() })
                    setExecutable(true)
                }
            }

            // Copy globalinject.js
            localDir().child("globalinject.js").apply {
                createFileIfNot()
                writeText(assets.open("globalinject.js").bufferedReader().use { it.readText() })
            }

            // Copy vscode-config.js script
            localDir().child("vscode-config.js").apply {
                if (exists().not()){
                    createFileIfNot()
                    writeText(assets.open("vscode-config.js").bufferedReader().use { it.readText() })
                }
            }

            // Copy shell wrapper for code-server terminals
            localBinDir().child("shell").apply {
                if (exists().not()){
                    createFileIfNot()
                    writeText(assets.open("shell.sh").bufferedReader().use { it.readText() })
                    setExecutable(true)
                }
            }

            // Copy desktop launcher
            localBinDir().child("desktop").apply {
                createFileIfNot()
                writeText(assets.open("desktop.sh").bufferedReader().use { it.readText() })
                setExecutable(true)
            }



            val env = mutableListOf(
                "PATH=${System.getenv("PATH")}:/sbin:${localBinDir().absolutePath}",
                "HOME=/root",
                "PUBLIC_HOME=${getExternalFilesDir(null)?.absolutePath}",
                "COLORTERM=truecolor",
                "TERM=xterm-256color",
                "LANG=C.UTF-8",
                "BIN=${localBinDir()}",
                "DEBUG=${BuildConfig.DEBUG}",
                "PREFIX=${filesDir.parentFile!!.path}",
                "LD_LIBRARY_PATH=${localLibDir().absolutePath}",
                "LINKER=${if(File("/system/bin/linker64").exists()){"/system/bin/linker64"}else{"/system/bin/linker"}}",
                "NATIVE_LIB_DIR=${applicationInfo.nativeLibraryDir}",
                "PKG=${packageName}",
                "RISH_APPLICATION_ID=${packageName}",
                "PKG_PATH=${applicationInfo.sourceDir}",
                "PROOT_TMP_DIR=${getTempDir().child(session_id).also { if (it.exists().not()){it.mkdirs()} }}",
                "TMPDIR=${getTempDir().absolutePath}"
            )

            if (File(applicationInfo.nativeLibraryDir).child("libproot-loader32.so").exists()){
                env.add("PROOT_LOADER32=${applicationInfo.nativeLibraryDir}/libproot-loader32.so")
            }

            if (File(applicationInfo.nativeLibraryDir).child("libproot-loader.so").exists()){
                env.add("PROOT_LOADER=${applicationInfo.nativeLibraryDir}/libproot-loader.so")
            }

            if (Settings.seccomp) {
                env.add("SECCOMP=1")
            }

            // Add shell preference
            env.add("DEFAULT_SHELL=${Settings.default_shell}")
            
            // Add GUI installation preference
            env.add("INSTALL_GUI=${Settings.install_gui}")
            
            // Add environment type preference
            env.add("ENVIRONMENT_TYPE=${Settings.environment_type}")
            
            // Add Shizuku availability
            env.add("SHIZUKU_AVAILABLE=${if (ShizukuHelper.isAvailable()) "1" else "0"}")




            env.addAll(envVariables.map { "${it.key}=${it.value}" })

            localDir().child("stat").apply {
                if (exists().not()){
                    writeText(stat)
                }
            }

            localDir().child("vmstat").apply {
                if (exists().not()){
                    writeText(vmstat)
                }
            }

            pendingCommand?.env?.let {
                env.addAll(it)
            }

            val args: Array<String>

            val shell = if (pendingCommand == null) {
                args = if (workingMode == WorkingMode.UBUNTU){
                    arrayOf("-c",initFile.absolutePath)
                }else{
                    arrayOf()
                }
                "/system/bin/sh"
            } else{
                args = pendingCommand!!.args
                pendingCommand!!.shell
            }

            pendingCommand = null
            return TerminalSession(
                shell,
                workingDir,
                args,
                env.toTypedArray(),
                TerminalEmulator.DEFAULT_TERMINAL_TRANSCRIPT_ROWS,
                sessionClient,
            )
        }

    }
}
