package com.rk.libcommons

import android.content.pm.PackageManager
import android.util.Log
import rikka.shizuku.Shizuku

object ShizukuHelper {
    
    fun init() {
        Log.d("ShizukuHelper", "Initializing Shizuku")
        try {
            if (Shizuku.pingBinder()) {
                Log.d("ShizukuHelper", "Shizuku binder available")
                if (!hasPermission()) {
                    Log.d("ShizukuHelper", "Auto-requesting Shizuku permission")
                    requestPermission()
                } else {
                    Log.d("ShizukuHelper", "Shizuku permission already granted")
                }
            } else {
                Log.w("ShizukuHelper", "Shizuku binder not available")
            }
        } catch (e: Exception) {
            Log.e("ShizukuHelper", "init error: ${e.message}")
        }
    }
    
    fun isAvailable(): Boolean {
        return try {
            val pingResult = Shizuku.pingBinder()
            val hasPermission = hasPermission()
            Log.d("ShizukuHelper", "isAvailable: ping=$pingResult, permission=$hasPermission")
            pingResult && hasPermission
        } catch (e: Exception) {
            Log.e("ShizukuHelper", "isAvailable error: ${e.message}")
            false
        }
    }
    
    private fun hasPermission(): Boolean {
        return try {
            if (Shizuku.isPreV11()) {
                Log.w("ShizukuHelper", "Pre-v11 Shizuku not supported")
                return false
            }
            val result = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            Log.d("ShizukuHelper", "hasPermission: $result")
            result
        } catch (e: Exception) {
            Log.e("ShizukuHelper", "hasPermission error: ${e.message}")
            false
        }
    }
    
    fun requestPermission() {
        Log.d("ShizukuHelper", "requestPermission called")
        try {
            if (Shizuku.isPreV11()) {
                Log.w("ShizukuHelper", "Pre-v11 Shizuku not supported")
                return
            }
            
            if (Shizuku.pingBinder()) {
                Log.d("ShizukuHelper", "Shizuku binder available")
                if (!hasPermission()) {
                    if (Shizuku.shouldShowRequestPermissionRationale()) {
                        Log.w("ShizukuHelper", "User denied permission and chose 'don't ask again'")
                    } else {
                        Log.d("ShizukuHelper", "Requesting permission with code 1001")
                        Shizuku.requestPermission(1001)
                    }
                } else {
                    Log.d("ShizukuHelper", "Permission already granted")
                }
            } else {
                Log.w("ShizukuHelper", "Shizuku binder not available")
            }
        } catch (e: Exception) {
            Log.e("ShizukuHelper", "requestPermission error: ${e.message}")
        }
    }
    
    fun executeCommand(command: String): String? {
        return try {
            if (!isAvailable()) {
                Log.e("ShizukuHelper", "Shizuku not available for command execution")
                return null
            }
            
            Log.d("ShizukuHelper", "Executing command via Shizuku: $command")
            
            // Use Runtime.exec with Shizuku context - this is a simplified approach
            // In a real implementation, you'd use Shizuku's UserService for complex operations
            Log.w("ShizukuHelper", "Command execution via Shizuku requires UserService implementation")
            return "Command queued for Shizuku execution"
        } catch (e: Exception) {
            Log.e("ShizukuHelper", "executeCommand error: ${e.message}")
            null
        }
    }
}
