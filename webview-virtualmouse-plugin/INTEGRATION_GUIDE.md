# WebView Virtual Mouse Plugin - Integration Guide

## Overview

This plugin provides a virtual mouse cursor overlay for Android WebView applications. It's particularly useful for web-based IDEs, remote desktop applications, or any WebView-based app that needs precise mouse control.

## Features

- ✅ Virtual mouse cursor overlay on WebView
- ✅ Touch-to-mouse event translation
- ✅ Cursor icon changes (arrow, hand, text, etc.)
- ✅ Tap, double-tap, right-click support
- ✅ Scroll and fling gestures
- ✅ Customizable cursor scale
- ✅ Hardware-accelerated rendering
- ✅ Enhanced WebView with pointer icon notifications

## Requirements

- Android SDK 24+ (Android 7.0+)
- Kotlin 1.8+
- AndroidX libraries
- ConstraintLayout for virtual mouse overlay

## Installation

### Step 1: Add the Plugin Module

1. Copy the `webview-virtualmouse-plugin` folder to your project root
2. Add the module to your `settings.gradle`:

```groovy
include ':app', ':webview-virtualmouse-plugin'
```

### Step 2: Add Dependency

In your app's `build.gradle`:

```groovy
dependencies {
    implementation project(':webview-virtualmouse-plugin')
    
    // Required dependencies (if not already included)
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'androidx.webkit:webkit:1.8.0'
}
```

### Step 3: Sync Project

```bash
./gradlew sync
```

## Basic Usage

### 1. Update Your Layout XML

Replace standard `WebView` with `EnhancedWebView` and ensure your root layout is a `ConstraintLayout`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout 
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/rootLayout"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <com.ahk.webview.plugin.browser.EnhancedWebView
        android:id="@+id/webView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

### 2. Initialize in Your Activity/Fragment

```kotlin
import com.ahk.webview.plugin.browser.EnhancedWebView
import com.ahk.webview.plugin.virtualmouse.VirtualMouse

class MainActivity : AppCompatActivity() {
    private lateinit var webView: EnhancedWebView
    private lateinit var virtualMouse: VirtualMouse
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        val rootLayout = findViewById<ConstraintLayout>(R.id.rootLayout)
        webView = findViewById(R.id.webView)
        
        // Configure WebView
        setupWebView()
        
        // Initialize Virtual Mouse
        virtualMouse = VirtualMouse()
        virtualMouse.enable(rootLayout, webView, scaleFactor = 1.0f)
        
        // Load your web content
        webView.loadUrl("https://your-web-app.com")
    }
    
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            setSupportZoom(true)
            builtInZoomControls = false
            displayZoomControls = false
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        virtualMouse.disable()
    }
}
```

## Advanced Usage

### Custom Cursor Scale

```kotlin
// Set cursor scale (1.0 = normal, 2.0 = double size, 0.5 = half size)
virtualMouse.setMouseScale(1.5f)
```

### Toggle Virtual Mouse

```kotlin
// Enable
virtualMouse.enable(rootLayout, webView, 1.0f)

// Disable
virtualMouse.disable()

// Check if enabled
if (virtualMouse.isEnabled) {
    // Virtual mouse is active
}
```

### Move Cursor Programmatically

```kotlin
// Move cursor to specific coordinates
virtualMouse.moveMouseTo(x = 100f, y = 200f)
```

### With Preferences/Settings

```kotlin
class WebViewPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("webview_prefs", Context.MODE_PRIVATE)
    
    var virtualMouseEnabled: Boolean
        get() = prefs.getBoolean("virtual_mouse_enabled", true)
        set(value) = prefs.edit().putBoolean("virtual_mouse_enabled", value).apply()
    
    var virtualMouseScale: Float
        get() = prefs.getFloat("virtual_mouse_scale", 1.0f)
        set(value) = prefs.edit().putFloat("virtual_mouse_scale", value).apply()
}

// Usage
val preferences = WebViewPreferences(this)

if (preferences.virtualMouseEnabled) {
    virtualMouse.enable(rootLayout, webView, preferences.virtualMouseScale)
}
```

### Settings UI Example

```kotlin
// In your settings activity/fragment
class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        
        // Virtual Mouse Toggle
        findPreference<SwitchPreferenceCompat>("virtual_mouse_enabled")?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                val enabled = newValue as Boolean
                // Notify main activity to enable/disable virtual mouse
                true
            }
        }
        
        // Virtual Mouse Scale Slider
        findPreference<SeekBarPreference>("virtual_mouse_scale")?.apply {
            min = 50  // 0.5x
            max = 300 // 3.0x
            value = 100 // 1.0x
            setOnPreferenceChangeListener { _, newValue ->
                val scale = (newValue as Int) / 100f
                // Notify main activity to update scale
                true
            }
        }
    }
}
```

## Touch Gestures

The virtual mouse automatically translates touch gestures:

| Gesture | Mouse Action |
|---------|-------------|
| Single tap | Left click |
| Double tap | Double click |
| Two-finger tap | Right click |
| Drag (1 finger) | Move cursor |
| Drag (2 fingers) | Scroll |
| Fling (2 fingers) | Scroll with momentum |
| Tap-tap-drag | Click and drag |

## WebView Configuration

### Recommended Settings

```kotlin
webView.settings.apply {
    // Enable JavaScript
    javaScriptEnabled = true
    
    // Enable DOM storage
    domStorageEnabled = true
    databaseEnabled = true
    
    // Disable zoom controls (virtual mouse provides better control)
    setSupportZoom(false)
    builtInZoomControls = false
    displayZoomControls = false
    
    // Enable hardware acceleration
    setLayerType(View.LAYER_TYPE_HARDWARE, null)
    
    // User agent (optional - set to desktop for better compatibility)
    userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36"
    
    // Mixed content (if needed)
    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
    
    // Cache
    cacheMode = WebSettings.LOAD_DEFAULT
    setAppCacheEnabled(true)
}
```

### WebViewClient Example

```kotlin
webView.webViewClient = object : WebViewClient() {
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        return false
    }
    
    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        // Show loading indicator
    }
    
    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        // Hide loading indicator
    }
}
```

## Troubleshooting

### Virtual Mouse Not Appearing

1. **Check root layout**: Ensure your root layout is a `ConstraintLayout`
2. **Check Android version**: Requires Android 7.0+ (API 24+)
3. **Check logs**: Look for "Failed to obtain actionButton offset" message
4. **Hardware acceleration**: Ensure hardware acceleration is enabled

```kotlin
// Enable hardware acceleration in manifest
<application
    android:hardwareAccelerated="true"
    ...>
```

### Cursor Not Moving Smoothly

1. **Enable hardware acceleration** on both WebView and MouseView
2. **Reduce cursor scale** if performance is poor
3. **Check device performance** - older devices may struggle

### Touch Events Not Working

1. **Check WebView settings**: Ensure JavaScript is enabled
2. **Check web content**: Some web apps may block touch events
3. **Check z-order**: Ensure MouseView is on top of WebView

### Cursor Icon Not Changing

1. **Use EnhancedWebView**: Standard WebView doesn't support pointer icon notifications
2. **Check Android version**: Pointer icon support requires Android 7.0+
3. **Check web content**: Web app must set cursor styles

## Performance Optimization

### 1. Hardware Acceleration

```kotlin
// Enable for WebView
webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

// Enable for MouseView (done automatically)
```

### 2. Reduce Overdraw

```kotlin
// Make WebView background transparent if needed
webView.setBackgroundColor(Color.TRANSPARENT)
```

### 3. Optimize Cursor Scale

```kotlin
// Use smaller cursor on high-DPI devices
val scale = when {
    resources.displayMetrics.densityDpi >= DisplayMetrics.DENSITY_XXHIGH -> 0.8f
    resources.displayMetrics.densityDpi >= DisplayMetrics.DENSITY_XHIGH -> 1.0f
    else -> 1.2f
}
virtualMouse.setMouseScale(scale)
```

## Example: Complete Implementation

```kotlin
class WebViewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWebViewBinding
    private lateinit var virtualMouse: VirtualMouse
    private lateinit var preferences: WebViewPreferences
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        preferences = WebViewPreferences(this)
        
        setupWebView()
        setupVirtualMouse()
        setupToolbar()
        
        binding.webView.loadUrl("https://vscode.dev")
    }
    
    private fun setupWebView() {
        binding.webView.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.setSupportZoom(false)
            settings.userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36"
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    // Inject custom CSS/JS if needed
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
    
    private fun setupToolbar() {
        binding.toolbar.apply {
            inflateMenu(R.menu.webview_menu)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_toggle_mouse -> {
                        toggleVirtualMouse()
                        true
                    }
                    R.id.action_settings -> {
                        startActivity(Intent(this@WebViewActivity, SettingsActivity::class.java))
                        true
                    }
                    else -> false
                }
            }
        }
    }
    
    private fun toggleVirtualMouse() {
        if (virtualMouse.isEnabled) {
            virtualMouse.disable()
            preferences.virtualMouseEnabled = false
            Toast.makeText(this, "Virtual mouse disabled", Toast.LENGTH_SHORT).show()
        } else {
            virtualMouse.enable(binding.rootLayout, binding.webView, preferences.virtualMouseScale)
            preferences.virtualMouseEnabled = true
            Toast.makeText(this, "Virtual mouse enabled", Toast.LENGTH_SHORT).show()
        }
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

## API Reference

### VirtualMouse

| Method | Description |
|--------|-------------|
| `enable(rootView, targetView, scaleFactor)` | Enable virtual mouse overlay |
| `disable()` | Disable and remove virtual mouse |
| `moveMouseTo(x, y)` | Move cursor to coordinates |
| `setMouseScale(factor)` | Set cursor scale factor |
| `isEnabled` | Check if virtual mouse is active |

### EnhancedWebView

| Method | Description |
|--------|-------------|
| `setPointerIconChangedListener(listener)` | Set pointer icon change listener |

All standard WebView methods are available.

## License

This plugin is extracted from VSDevEditor-Android project.
Feel free to use and modify for your projects.

## Support

For issues and questions:
1. Check this integration guide
2. Review the troubleshooting section
3. Check device compatibility (Android 7.0+)
4. Test on different devices

## Credits

Original implementation from VSDevEditor-Android project.
Extracted and modularized for reusability.
