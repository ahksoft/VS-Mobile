package com.rk.terminal.utils

import android.content.Context
import android.system.Os
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.io.*
import java.util.zip.GZIPInputStream

object CodeServerInstaller {
    
    suspend fun extractCodeServer(
        context: Context,
        onProgress: (Int, Int, String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val targetDir = File(context.filesDir.parentFile, "local/ubuntu/root/.local/share/code-server")
        
        // Remove old installation
        if (targetDir.exists()) {
            onProgress(0, 1, "Removing old installation...")
            targetDir.deleteRecursively()
        }
        
        // Create parent directories
        targetDir.parentFile?.mkdirs()
        
        // Get architecture
        val arch = getArchitecture()
        val assetName = "cs-$arch.tgz"
        
        onProgress(0, 1, "Loading $assetName...")
        
        // Open asset
        val inputStream = try {
            context.assets.open(assetName)
        } catch (e: Exception) {
            throw Exception("Asset not found: $assetName. Please download it first.")
        }
        
        // Extract
        onProgress(0, 1, "Extracting code-server...")
        extractTarGz(inputStream, targetDir, onProgress)
        
        // Set permissions
        onProgress(0, 1, "Setting permissions...")
        setPermissions(targetDir)
        
        // Copy libc++_shared.so from native libs
        onProgress(0, 1, "Copying native libraries...")
        copyNativeLibs(context, targetDir)
        
        // Patch ptyHostMain.js for Android support
        onProgress(0, 1, "Patching for Android...")
        patchPtyHost(targetDir)
        
        // Patch product.json to enable extension marketplace
        onProgress(0, 1, "Configuring extension marketplace...")
        patchProductJson(targetDir)
        
        // Apply mobile patches
        
        // Patch vsce-sign for extension installation
        
        // Write version marker
        File(targetDir, "INSTALLED").writeText("cs-2025.12.09-r1")
        
        onProgress(1, 1, "Installation complete")
    }
    
    private fun patchPtyHost(targetDir: File) {
        // Temporarily disabled - using globalinject.js instead
        /*
        try {
            val ptyHostFile = File(targetDir, "code-server/release-standalone/lib/vscode/out/vs/platform/terminal/node/ptyHostMain.js")
            if (ptyHostFile.exists()) {
                var content = ptyHostFile.readText()
                // Patch platform detection to always return "linux"
                content = content.replace("switch(process.platform)", "switch(\"linux\")")
                ptyHostFile.writeText(content)
            }
        } catch (e: Exception) {
            // Non-fatal
        }
        */
    }
    
    private fun patchProductJson(targetDir: File) {
        try {
            val productFile = File(targetDir, "code-server/release-standalone/lib/vscode/product.json")
            if (productFile.exists()) {
                val json = org.json.JSONObject(productFile.readText())
                
                // DELETE extensionsGallery to let VS Code use built-in Open VSX
                // This is more reliable than adding custom config
                if (json.has("extensionsGallery")) {
                    json.remove("extensionsGallery")
                }
                
                productFile.writeText(json.toString(2))
            }
        } catch (e: Exception) {
            // Non-fatal
            e.printStackTrace()
        }
    }
    
    private fun copyNativeLibs(context: Context, targetDir: File) {
        try {
            val nativeLibDir = File(context.applicationInfo.nativeLibraryDir)
            val libcppShared = File(nativeLibDir, "libc++_shared.so")
            if (libcppShared.exists()) {
                val destLib = File(targetDir, "libc++_shared.so")
                libcppShared.copyTo(destLib, overwrite = true)
            }
        } catch (e: Exception) {
            // Non-fatal, continue
        }
    }
    
    fun isCodeServerInstalled(context: Context): Boolean {
        val targetDir = File(context.filesDir.parentFile, "local/ubuntu/root/.local/share/code-server")
        return targetDir.exists() && File(targetDir, "INSTALLED").exists()
    }
    
    private fun getArchitecture(): String {
        val arch = System.getProperty("os.arch") ?: ""
        return when {
            arch.contains("aarch64") || arch.contains("arm64") -> "aarch64"
            arch.contains("arm") -> "arm"
            arch.contains("x86_64") || arch.contains("amd64") -> "x86_64"
            arch.contains("x86") || arch.contains("i686") -> "x86"
            else -> "aarch64" // default
        }
    }
    
    private fun extractTarGz(
        inputStream: InputStream,
        outputDir: File,
        onProgress: (Int, Int, String) -> Unit
    ) {
        val buffer = ByteArray(8192)
        var total = 0
        var current = 0
        
        // Count entries first
        val countStream = BufferedInputStream(inputStream)
        countStream.mark(Int.MAX_VALUE)
        var reader = TarArchiveInputStream(GZIPInputStream(countStream))
        while (reader.nextTarEntry != null) {
            total++
        }
        countStream.reset()
        
        // Extract
        reader = TarArchiveInputStream(GZIPInputStream(countStream))
        var entry = reader.nextTarEntry
        
        while (entry != null) {
            current++
            val fileName = entry.name.substringAfterLast('/')
            if (current % 100 == 0 || fileName.isNotEmpty()) {
                onProgress(current, total, fileName)
            }
            
            val outputFile = File(outputDir, entry.name)
            
            when {
                entry.isDirectory -> {
                    outputFile.mkdirs()
                }
                entry.isSymbolicLink -> {
                    outputFile.parentFile?.mkdirs()
                    try {
                        val linkName = entry.linkName ?: ""
                        Os.symlink(linkName, outputFile.absolutePath)
                    } catch (e: Exception) {
                        // Ignore symlink errors
                    }
                }
                else -> {
                    outputFile.parentFile?.mkdirs()
                    FileOutputStream(outputFile).use { out ->
                        var len: Int
                        while (reader.read(buffer).also { len = it } > 0) {
                            out.write(buffer, 0, len)
                        }
                    }
                }
            }
            
            entry = reader.nextTarEntry
        }
        
        reader.close()
    }
    
    private fun setPermissions(dir: File) {
        // Set executable permissions on scripts and binaries
        dir.walk().forEach { file ->
            when {
                file.name.endsWith(".sh") -> file.setExecutable(true)
                file.name == "node" -> file.setExecutable(true)
                file.name == "code-server" -> file.setExecutable(true)
                file.name == "rg" -> file.setExecutable(true)
                file.path.contains("/bin/") -> file.setExecutable(true)
            }
        }
    }
}
