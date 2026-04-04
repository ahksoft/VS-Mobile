package com.rk.settings

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import com.rk.libcommons.application
import com.rk.terminal.ui.screens.settings.WorkingMode

object Settings {
    //Boolean
    var seccomp
        get() = Preference.getBoolean(key = "seccomp", default = false)
        set(value) = Preference.setBoolean(key = "seccomp",value)
    var amoled
        get() = Preference.getBoolean(key = "oled", default = false)
        set(value) = Preference.setBoolean(key = "oled",value)
    var monet
        get() = Preference.getBoolean(
            key = "monet",
            default = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        )
        set(value) = Preference.setBoolean(key = "monet",value)
    var ignore_storage_permission
        get() = Preference.getBoolean(key = "ignore_storage_permission",default = false)
        set(value) = Preference.setBoolean(key = "ignore_storage_permission",value)
    var github
        get() = Preference.getBoolean(key = "github", default = true)
        set(value) = Preference.setBoolean(key = "github",value)


   var default_night_mode
        get() = Preference.getInt(key = "default_night_mode", default = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        set(value) = Preference.setInt(key = "default_night_mode",value)

    var terminal_font_size
        get() = Preference.getInt(key = "terminal_font_size", default = 12)
        set(value) = Preference.setInt(key = "terminal_font_size",value)

    var wallTransparency
        get() = Preference.getFloat(key = "wallTransparency", default = 0f)
        set(value) = Preference.setFloat(key = "wallTransparency",value)

    var working_Mode
        get() = Preference.getInt(key = "workingMode", default = WorkingMode.UBUNTU)
        set(value) = Preference.setInt(key = "workingMode",value)

    var default_shell
        get() = Preference.getInt(key = "defaultShell", default = 0) // 0 = bash, 1 = zsh
        set(value) = Preference.setInt(key = "defaultShell", value)

    var install_gui
        get() = Preference.getInt(key = "installGui", default = 0) // 0 = no, 1 = yes
        set(value) = Preference.setInt(key = "installGui", value)

    var environment_type
        get() = Preference.getInt(key = "environmentType", default = 0) // 0 = proot, 1 = chroot
        set(value) = Preference.setInt(key = "environmentType", value)

    var webview_cursor_scale
        get() = Preference.getFloat(key = "webviewCursorScale", default = 1.0f)
        set(value) = Preference.setFloat(key = "webviewCursorScale", value)
    
    var webview_virtual_mouse_enabled
        get() = Preference.getBoolean(key = "webviewVirtualMouseEnabled", default = true)
        set(value) = Preference.setBoolean(key = "webviewVirtualMouseEnabled", value)

    var webview_zoom_level
        get() = Preference.getInt(key = "webviewZoomLevel", default = 200).coerceIn(100, 300)
        set(value) = Preference.setInt(key = "webviewZoomLevel", value.coerceIn(100, 300))

    var webview_url
        get() = Preference.getString(key = "webviewUrl", default = "localhost")
        set(value) = Preference.setString(key = "webviewUrl", value)

    var webview_port
        get() = Preference.getInt(key = "webviewPort", default = 6862)
        set(value) = Preference.setInt(key = "webviewPort", value)

    var custom_background_name
        get() = Preference.getString(key = "custom_bg_name", default = "No Image Selected")
        set(value) = Preference.setString(key = "custom_bg_name",value)
    var custom_font_name
        get() = Preference.getString(key = "custom_ttf_name", default = "No Font Selected")
        set(value) = Preference.setString(key = "custom_ttf_name",value)

    var blackTextColor
        get() = Preference.getBoolean(key = "blackText", default = false)
        set(value) = Preference.setBoolean(key = "blackText",value)

    var bell
        get() = Preference.getBoolean(key = "bell", default = false)
        set(value) = Preference.setBoolean(key = "bell",value)

    var vibrate
        get() = Preference.getBoolean(key = "vibrate", default = true)
        set(value) = Preference.setBoolean(key = "vibrate",value)

    var toolbar
        get() = Preference.getBoolean(key = "toolbar", default = true)
        set(value) = Preference.setBoolean(key = "toolbar",value)

    var statusBar
        get() = Preference.getBoolean(key = "statusBar", default = true)
        set(value) = Preference.setBoolean(key = "statusBar",value)

    var horizontal_statusBar
        get() = Preference.getBoolean(key = "horizontal_statusBar", default = true)
        set(value) = Preference.setBoolean(key = "horizontal_statusBar",value)

    var toolbar_in_horizontal
        get() = Preference.getBoolean(key = "toolbar_h", default = true)
        set(value) = Preference.setBoolean(key = "toolbar_h",value)

    var virtualKeys
        get() = Preference.getBoolean(key = "virtualKeys", default = true)
        set(value) = Preference.setBoolean(key = "virtualKeys",value)

    var hide_soft_keyboard_if_hwd
        get() = Preference.getBoolean(key = "force_soft_keyboard", default = true)
        set(value) = Preference.setBoolean(key = "force_soft_keyboard",value)

    var desktop_enabled
        get() = Preference.getBoolean(key = "desktop_enabled", default = false)
        set(value) = Preference.setBoolean(key = "desktop_enabled", value)

    var vnc_host
        get() = Preference.getString(key = "vnc_host", default = "localhost")
        set(value) = Preference.setString(key = "vnc_host", value)

    var vnc_port
        get() = Preference.getInt(key = "vnc_port", default = 5905)
        set(value) = Preference.setInt(key = "vnc_port", value)

    var vnc_password
        get() = Preference.getString(key = "vnc_password", default = "123456")
        set(value) = Preference.setString(key = "vnc_password", value)

    var vnc_gesture_style
        get() = Preference.getString(key = "vnc_gesture_style", default = "touchpad")
        set(value) = Preference.setString(key = "vnc_gesture_style", value)

    var vnc_orientation
        get() = Preference.getString(key = "vnc_orientation", default = "portrait")
        set(value) = Preference.setString(key = "vnc_orientation", value)

    var vnc_resize_remote
        get() = Preference.getBoolean(key = "vnc_resize_remote", default = true)
        set(value) = Preference.setBoolean(key = "vnc_resize_remote", value)

    var vnc_image_quality
        get() = Preference.getInt(key = "vnc_image_quality", default = 5)
        set(value) = Preference.setInt(key = "vnc_image_quality", value)

    var vnc_show_nav_buttons
        get() = Preference.getBoolean(key = "vnc_show_nav_buttons", default = true)
        set(value) = Preference.setBoolean(key = "vnc_show_nav_buttons", value)

    var vnc_screen_width
        get() = Preference.getInt(key = "vnc_screen_width", default = 1280)
        set(value) = Preference.setInt(key = "vnc_screen_width", value)

    var vnc_screen_height
        get() = Preference.getInt(key = "vnc_screen_height", default = 720)
        set(value) = Preference.setInt(key = "vnc_screen_height", value)



}

object Preference {
    private var sharedPreferences: SharedPreferences = application!!.getSharedPreferences("Settings", Context.MODE_PRIVATE)

    //store the result into memory for faster access
    private val stringCache = hashMapOf<String, String?>()
    private val boolCache = hashMapOf<String, Boolean>()
    private val intCache = hashMapOf<String, Int>()
    private val longCache = hashMapOf<String, Long>()
    private val floatCache = hashMapOf<String, Float>()

    @SuppressLint("ApplySharedPref")
    fun clearData(){
        sharedPreferences.edit().clear().commit()
    }

    fun removeKey(key: String){
        if (sharedPreferences.contains(key).not()){
            return
        }

        sharedPreferences.edit().remove(key).apply()

        if (stringCache.containsKey(key)){
            stringCache.remove(key)
            return
        }

        if (boolCache.containsKey(key)){
            boolCache.remove(key)
            return
        }

        if (intCache.containsKey(key)){
            intCache.remove(key)
            return
        }

        if (longCache.containsKey(key)){
            longCache.remove(key)
            return
        }

        if (floatCache.containsKey(key)){
            floatCache.remove(key)
            return
        }
    }

    fun getBoolean(key: String, default: Boolean): Boolean {
        runCatching {
            return boolCache[key] ?: sharedPreferences.getBoolean(key, default)
                .also { boolCache[key] = it }
        }.onFailure {
            it.printStackTrace()
            setBoolean(key, default)
        }
        return default
    }

    fun setBoolean(key: String, value: Boolean) {
        boolCache[key] = value
        runCatching {
            val editor = sharedPreferences.edit()
            editor.putBoolean(key, value)
            editor.apply()
        }.onFailure { it.printStackTrace() }
    }



    fun getString(key: String, default: String): String {
        runCatching {
            return stringCache[key] ?: sharedPreferences.getString(key, default)!!
                .also { stringCache[key] = it }
        }.onFailure {
            it.printStackTrace()
            setString(key, default)
        }
        return default
    }
    fun setString(key: String, value: String?) {
        stringCache[key] = value
        runCatching {
            val editor = sharedPreferences.edit()
            editor.putString(key, value)
            editor.apply()
        }.onFailure {
            it.printStackTrace()
        }

    }

    fun getInt(key: String, default: Int): Int {
        runCatching {
            return intCache[key] ?: sharedPreferences.getInt(key, default)
                .also { intCache[key] = it }
        }.onFailure {
            it.printStackTrace()
            setInt(key, default)
        }
        return default
    }

    fun setInt(key: String, value: Int) {
        intCache[key] = value
        runCatching {
            val editor = sharedPreferences.edit()
            editor.putInt(key, value)
            editor.apply()
        }.onFailure {
            it.printStackTrace()
        }

    }

    fun getLong(key: String, default: Long): Long {
        runCatching {
            return longCache[key] ?: sharedPreferences.getLong(key, default)
                .also { longCache[key] = it }
        }.onFailure {
            it.printStackTrace()
            setLong(key, default)
        }
        return default
    }

    fun setLong(key: String, value: Long) {
        longCache[key] = value
        runCatching {
            val editor = sharedPreferences.edit()
            editor.putLong(key,value)
            editor.apply()
        }.onFailure {
            it.printStackTrace()
        }
    }

    fun getFloat(key: String, default: Float): Float {
        runCatching {
            return floatCache[key] ?: sharedPreferences.getFloat(key, default)
                .also { floatCache[key] = it }
        }.onFailure {
            it.printStackTrace()
            setFloat(key, default)
        }
        return default
    }

    fun setFloat(key: String, value: Float) {
        floatCache[key] = value
        runCatching {
            val editor = sharedPreferences.edit()
            editor.putFloat(key,value)
            editor.apply()
        }.onFailure {
            it.printStackTrace()
        }
    }

}
