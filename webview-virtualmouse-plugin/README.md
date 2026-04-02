# WebView Virtual Mouse Plugin

A standalone Android library that adds virtual mouse cursor functionality to WebView applications.

## Features

- 🖱️ Virtual mouse cursor overlay
- 👆 Touch-to-mouse event translation
- 🎯 Precise cursor control
- 🔄 Automatic cursor icon changes
- ⚡ Hardware-accelerated rendering
- 📱 Android 7.0+ support

## Quick Start

### 1. Add to Your Project

```groovy
// settings.gradle
include ':app', ':webview-virtualmouse-plugin'

// app/build.gradle
dependencies {
    implementation project(':webview-virtualmouse-plugin')
}
```

### 2. Update Layout

```xml
<androidx.constraintlayout.widget.ConstraintLayout
    android:id="@+id/rootLayout"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <com.ahk.webview.plugin.browser.EnhancedWebView
        android:id="@+id/webView"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

### 3. Initialize

```kotlin
val virtualMouse = VirtualMouse()
virtualMouse.enable(rootLayout, webView, scaleFactor = 1.0f)
```

## Documentation

See [INTEGRATION_GUIDE.md](INTEGRATION_GUIDE.md) for complete documentation.

## Components

### Core Classes

- **VirtualMouse**: Main API for enabling/disabling virtual mouse
- **MouseView**: Custom view that renders the cursor and handles touch events
- **EnhancedWebView**: WebView with pointer icon change notifications
- **PointerIconChangedListen**: Interface for cursor icon changes

### File Structure

```
webview-virtualmouse-plugin/
├── build.gradle
├── src/main/
│   ├── AndroidManifest.xml
│   └── java/com/ahk/webview/plugin/
│       ├── browser/
│       │   └── EnhancedWebView.kt
│       └── virtualmouse/
│           ├── VirtualMouse.kt
│           ├── MouseView.kt
│           └── PointerIconChangedListen.kt
├── INTEGRATION_GUIDE.md
└── README.md
```

## Requirements

- Android SDK 24+ (Android 7.0+)
- Kotlin 1.8+
- AndroidX
- ConstraintLayout

## Usage Examples

### Basic Usage

```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var virtualMouse: VirtualMouse
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        val rootLayout = findViewById<ConstraintLayout>(R.id.rootLayout)
        val webView = findViewById<EnhancedWebView>(R.id.webView)
        
        virtualMouse = VirtualMouse()
        virtualMouse.enable(rootLayout, webView)
        
        webView.loadUrl("https://vscode.dev")
    }
}
```

### With Settings

```kotlin
// Enable/disable
if (preferences.virtualMouseEnabled) {
    virtualMouse.enable(rootLayout, webView, preferences.mouseScale)
} else {
    virtualMouse.disable()
}

// Adjust scale
virtualMouse.setMouseScale(1.5f)
```

### Toggle Button

```kotlin
button.setOnClickListener {
    if (virtualMouse.isEnabled) {
        virtualMouse.disable()
    } else {
        virtualMouse.enable(rootLayout, webView)
    }
}
```

## Touch Gestures

| Gesture | Action |
|---------|--------|
| Single tap | Left click |
| Double tap | Double click |
| Two-finger tap | Right click |
| One-finger drag | Move cursor |
| Two-finger drag | Scroll |
| Two-finger fling | Scroll with momentum |

## API

### VirtualMouse

```kotlin
// Enable virtual mouse
fun enable(rootView: View, targetView: WebView, scaleFactor: Float = 1.0f)

// Disable virtual mouse
fun disable()

// Move cursor
fun moveMouseTo(x: Float, y: Float)

// Set scale
fun setMouseScale(factor: Float)

// Check status
val isEnabled: Boolean
```

### EnhancedWebView

```kotlin
// Set listener for cursor icon changes
fun setPointerIconChangedListener(listener: PointerIconChangedListen.Listener?)
```

## Performance Tips

1. Enable hardware acceleration
2. Use appropriate cursor scale for device DPI
3. Disable zoom controls (virtual mouse provides better control)
4. Use LAYER_TYPE_HARDWARE for both WebView and MouseView

## Troubleshooting

### Virtual Mouse Not Showing

- Ensure root layout is ConstraintLayout
- Check Android version (7.0+)
- Enable hardware acceleration
- Check logs for "actionButton offset" errors

### Poor Performance

- Enable hardware acceleration
- Reduce cursor scale
- Test on newer devices

### Touch Events Not Working

- Enable JavaScript in WebView
- Check web content compatibility
- Verify MouseView is on top

## License

Extracted from VSDevEditor-Android project.
Free to use and modify.

## Credits

Original implementation: VSDevEditor-Android
Modularized for reusability

## Support

For issues:
1. Check INTEGRATION_GUIDE.md
2. Review troubleshooting section
3. Test on Android 7.0+ devices
