# Example Project Structure

This document shows how to integrate the WebView Virtual Mouse Plugin into a new Android project.

## Project Structure

```
MyWebViewApp/
├── settings.gradle
├── build.gradle (project)
├── app/
│   ├── build.gradle
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   ├── activity_main.xml
│   │   │   │   └── activity_settings.xml
│   │   │   ├── menu/
│   │   │   │   └── webview_menu.xml
│   │   │   ├── xml/
│   │   │   │   └── preferences.xml
│   │   │   └── values/
│   │   │       ├── strings.xml
│   │   │       └── themes.xml
│   │   └── java/com/example/myapp/
│   │       ├── MainActivity.kt
│   │       ├── SettingsActivity.kt
│   │       └── WebViewPreferences.kt
│   └── proguard-rules.pro
└── webview-virtualmouse-plugin/
    └── (plugin files)
```

## Step-by-Step Integration

### 1. settings.gradle

```groovy
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MyWebViewApp"
include ':app'
include ':webview-virtualmouse-plugin'  // Add this line
```

### 2. app/build.gradle

```groovy
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
}

android {
    namespace 'com.example.myapp'
    compileSdk 34

    defaultConfig {
        applicationId "com.example.myapp"
        minSdk 24
        targetSdk 34
        versionCode 1
        versionName "1.0"
    }

    buildTypes {
        release {
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = '17'
    }

    buildFeatures {
        viewBinding true
    }
}

dependencies {
    // Plugin dependency
    implementation project(':webview-virtualmouse-plugin')
    
    // AndroidX
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'androidx.preference:preference-ktx:1.2.1'
    
    // WebView
    implementation 'androidx.webkit:webkit:1.8.0'
}
```

### 3. AndroidManifest.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <application
        android:allowBackup="true"
        android:hardwareAccelerated="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/Theme.MyWebViewApp"
        android:usesCleartextTraffic="true">
        
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:configChanges="orientation|screenSize|keyboardHidden"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        
        <activity
            android:name=".SettingsActivity"
            android:label="@string/settings"
            android:parentActivityName=".MainActivity" />
    </application>

</manifest>
```

### 4. activity_main.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout 
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/rootLayout"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".MainActivity">

    <com.google.android.material.appbar.MaterialToolbar
        android:id="@+id/toolbar"
        android:layout_width="match_parent"
        android:layout_height="?attr/actionBarSize"
        android:background="?attr/colorPrimary"
        android:elevation="4dp"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        app:title="@string/app_name"
        app:titleTextColor="@android:color/white" />

    <com.ahk.webview.plugin.browser.EnhancedWebView
        android:id="@+id/webView"
        android:layout_width="0dp"
        android:layout_height="0dp"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toBottomOf="@id/toolbar" />

    <ProgressBar
        android:id="@+id/progressBar"
        style="?android:attr/progressBarStyleHorizontal"
        android:layout_width="0dp"
        android:layout_height="4dp"
        android:indeterminate="true"
        android:visibility="gone"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toBottomOf="@id/toolbar" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

### 5. webview_menu.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<menu xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto">

    <item
        android:id="@+id/action_toggle_mouse"
        android:icon="@android:drawable/ic_menu_cursor"
        android:title="@string/toggle_virtual_mouse"
        app:showAsAction="ifRoom" />

    <item
        android:id="@+id/action_refresh"
        android:icon="@android:drawable/ic_menu_rotate"
        android:title="@string/refresh"
        app:showAsAction="ifRoom" />

    <item
        android:id="@+id/action_settings"
        android:icon="@android:drawable/ic_menu_preferences"
        android:title="@string/settings"
        app:showAsAction="never" />

</menu>
```

### 6. MainActivity.kt

```kotlin
package com.example.myapp

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ahk.webview.plugin.browser.EnhancedWebView
import com.ahk.webview.plugin.virtualmouse.VirtualMouse
import com.example.myapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var virtualMouse: VirtualMouse
    private lateinit var preferences: WebViewPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferences = WebViewPreferences(this)

        setupToolbar()
        setupWebView()
        setupVirtualMouse()

        // Load initial URL
        val url = intent.getStringExtra("url") ?: "https://vscode.dev"
        binding.webView.loadUrl(url)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.inflateMenu(R.menu.webview_menu)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_toggle_mouse -> {
                    toggleVirtualMouse()
                    true
                }
                R.id.action_refresh -> {
                    binding.webView.reload()
                    true
                }
                R.id.action_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun setupWebView() {
        binding.webView.apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                setSupportZoom(false)
                builtInZoomControls = false
                displayZoomControls = false
                useWideViewPort = true
                loadWithOverviewMode = true
                
                // Desktop user agent for better compatibility
                userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            }

            setLayerType(View.LAYER_TYPE_HARDWARE, null)

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    return false
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    binding.progressBar.visibility = View.VISIBLE
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun setupVirtualMouse() {
        virtualMouse = VirtualMouse()

        if (preferences.virtualMouseEnabled) {
            virtualMouse.enable(
                binding.rootLayout,
                binding.webView,
                preferences.virtualMouseScale
            )
        }
    }

    private fun toggleVirtualMouse() {
        if (virtualMouse.isEnabled) {
            virtualMouse.disable()
            preferences.virtualMouseEnabled = false
            Toast.makeText(this, "Virtual mouse disabled", Toast.LENGTH_SHORT).show()
        } else {
            virtualMouse.enable(
                binding.rootLayout,
                binding.webView,
                preferences.virtualMouseScale
            )
            preferences.virtualMouseEnabled = true
            Toast.makeText(this, "Virtual mouse enabled", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.webView.onResume()
        
        // Update virtual mouse scale if changed in settings
        if (virtualMouse.isEnabled) {
            virtualMouse.setMouseScale(preferences.virtualMouseScale)
        }
    }

    override fun onPause() {
        super.onPause()
        binding.webView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        virtualMouse.disable()
        binding.webView.destroy()
    }

    override fun onBackPressed() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
```

### 7. WebViewPreferences.kt

```kotlin
package com.example.myapp

import android.content.Context
import android.content.SharedPreferences

class WebViewPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "webview_prefs",
        Context.MODE_PRIVATE
    )

    var virtualMouseEnabled: Boolean
        get() = prefs.getBoolean(KEY_VIRTUAL_MOUSE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_VIRTUAL_MOUSE_ENABLED, value).apply()

    var virtualMouseScale: Float
        get() = prefs.getFloat(KEY_VIRTUAL_MOUSE_SCALE, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_VIRTUAL_MOUSE_SCALE, value).apply()

    var hardwareAcceleration: Boolean
        get() = prefs.getBoolean(KEY_HARDWARE_ACCELERATION, true)
        set(value) = prefs.edit().putBoolean(KEY_HARDWARE_ACCELERATION, value).apply()

    var defaultUrl: String
        get() = prefs.getString(KEY_DEFAULT_URL, "https://vscode.dev") ?: "https://vscode.dev"
        set(value) = prefs.edit().putString(KEY_DEFAULT_URL, value).apply()

    companion object {
        private const val KEY_VIRTUAL_MOUSE_ENABLED = "virtual_mouse_enabled"
        private const val KEY_VIRTUAL_MOUSE_SCALE = "virtual_mouse_scale"
        private const val KEY_HARDWARE_ACCELERATION = "hardware_acceleration"
        private const val KEY_DEFAULT_URL = "default_url"
    }
}
```

### 8. SettingsActivity.kt

```kotlin
package com.example.myapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreferenceCompat

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, SettingsFragment())
            .commit()
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        private lateinit var preferences: WebViewPreferences

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
            
            preferences = WebViewPreferences(requireContext())

            // Virtual Mouse Toggle
            findPreference<SwitchPreferenceCompat>("virtual_mouse_enabled")?.apply {
                isChecked = preferences.virtualMouseEnabled
                setOnPreferenceChangeListener { _, newValue ->
                    preferences.virtualMouseEnabled = newValue as Boolean
                    true
                }
            }

            // Virtual Mouse Scale
            findPreference<SeekBarPreference>("virtual_mouse_scale")?.apply {
                min = 50  // 0.5x
                max = 300 // 3.0x
                value = (preferences.virtualMouseScale * 100).toInt()
                setOnPreferenceChangeListener { _, newValue ->
                    preferences.virtualMouseScale = (newValue as Int) / 100f
                    true
                }
            }

            // Hardware Acceleration
            findPreference<SwitchPreferenceCompat>("hardware_acceleration")?.apply {
                isChecked = preferences.hardwareAcceleration
                setOnPreferenceChangeListener { _, newValue ->
                    preferences.hardwareAcceleration = newValue as Boolean
                    true
                }
            }
        }
    }
}
```

### 9. preferences.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<PreferenceScreen xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto">

    <PreferenceCategory
        android:title="Virtual Mouse"
        android:key="category_virtual_mouse">

        <SwitchPreferenceCompat
            android:key="virtual_mouse_enabled"
            android:title="Enable Virtual Mouse"
            android:summary="Show virtual mouse cursor on WebView"
            android:defaultValue="true" />

        <SeekBarPreference
            android:key="virtual_mouse_scale"
            android:title="Mouse Cursor Scale"
            android:summary="Adjust cursor size"
            android:defaultValue="100"
            android:min="50"
            android:max="300"
            app:showSeekBarValue="true"
            app:seekBarIncrement="10" />

    </PreferenceCategory>

    <PreferenceCategory
        android:title="Performance"
        android:key="category_performance">

        <SwitchPreferenceCompat
            android:key="hardware_acceleration"
            android:title="Hardware Acceleration"
            android:summary="Use GPU for rendering (recommended)"
            android:defaultValue="true" />

    </PreferenceCategory>

</PreferenceScreen>
```

### 10. strings.xml

```xml
<resources>
    <string name="app_name">My WebView App</string>
    <string name="settings">Settings</string>
    <string name="toggle_virtual_mouse">Toggle Virtual Mouse</string>
    <string name="refresh">Refresh</string>
</resources>
```

## Building and Running

### 1. Sync Project

```bash
./gradlew sync
```

### 2. Build APK

```bash
./gradlew assembleDebug
```

### 3. Install on Device

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 4. Run

```bash
adb shell am start -n com.example.myapp/.MainActivity
```

## Testing

### Test Virtual Mouse

1. Launch app
2. Touch screen with one finger - cursor should move
3. Tap once - should click
4. Tap twice quickly - should double click
5. Tap with two fingers - should right click
6. Drag with two fingers - should scroll

### Test Settings

1. Open settings from menu
2. Toggle virtual mouse - cursor should appear/disappear
3. Adjust scale - cursor size should change
4. Return to main screen - settings should persist

## Customization

### Change Default URL

In `WebViewPreferences.kt`:

```kotlin
var defaultUrl: String
    get() = prefs.getString(KEY_DEFAULT_URL, "https://your-url.com") ?: "https://your-url.com"
```

### Add More Settings

1. Add preference to `preferences.xml`
2. Add property to `WebViewPreferences.kt`
3. Handle in `SettingsFragment`

### Custom Cursor

Modify `MouseView.kt` in the plugin to use custom cursor images.

## Troubleshooting

### Build Errors

```bash
# Clean and rebuild
./gradlew clean
./gradlew assembleDebug
```

### Plugin Not Found

Check `settings.gradle` includes the plugin module.

### Virtual Mouse Not Working

1. Check Android version (7.0+)
2. Enable hardware acceleration in manifest
3. Check logs: `adb logcat | grep VirtualMouse`

## Next Steps

1. Add more WebView features (downloads, file upload, etc.)
2. Implement custom WebChromeClient
3. Add bookmarks/history
4. Implement fullscreen mode
5. Add developer tools

## Resources

- [WebView Documentation](https://developer.android.com/reference/android/webkit/WebView)
- [ConstraintLayout Guide](https://developer.android.com/training/constraint-layout)
- [Preferences Guide](https://developer.android.com/guide/topics/ui/settings)
