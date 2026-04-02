# Quick Reference Card

## Installation (3 Steps)

### 1. Add Module
```groovy
// settings.gradle
include ':webview-virtualmouse-plugin'

// app/build.gradle
implementation project(':webview-virtualmouse-plugin')
```

### 2. Layout
```xml
<androidx.constraintlayout.widget.ConstraintLayout
    android:id="@+id/rootLayout" ...>
    <com.ahk.webview.plugin.browser.EnhancedWebView
        android:id="@+id/webView" ... />
</androidx.constraintlayout.widget.ConstraintLayout>
```

### 3. Code
```kotlin
val virtualMouse = VirtualMouse()
virtualMouse.enable(rootLayout, webView, 1.0f)
```

## API Cheat Sheet

### VirtualMouse

```kotlin
// Enable
virtualMouse.enable(rootView, webView, scaleFactor = 1.0f)

// Disable
virtualMouse.disable()

// Move cursor
virtualMouse.moveMouseTo(x, y)

// Set scale
virtualMouse.setMouseScale(1.5f)

// Check status
if (virtualMouse.isEnabled) { }
```

### EnhancedWebView

```kotlin
// Set listener
webView.setPointerIconChangedListener(object : PointerIconChangedListen.Listener {
    override fun onPointerIconChanged(pointerIcon: PointerIcon?) {
        // Handle icon change
    }
})
```

## Touch Gestures

| Gesture | Action |
|---------|--------|
| 1 tap | Left click |
| 2 taps | Double click |
| 2-finger tap | Right click |
| 1-finger drag | Move cursor |
| 2-finger drag | Scroll |
| 2-finger fling | Scroll momentum |
| Tap-tap-drag | Click & drag |

## WebView Setup

```kotlin
webView.settings.apply {
    javaScriptEnabled = true
    domStorageEnabled = true
    setSupportZoom(false)
    userAgentString = "Mozilla/5.0 (X11; Linux x86_64) ..."
}
webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
```

## Preferences Example

```kotlin
class Prefs(context: Context) {
    private val prefs = context.getSharedPreferences("prefs", 0)
    
    var mouseEnabled: Boolean
        get() = prefs.getBoolean("mouse", true)
        set(v) = prefs.edit().putBoolean("mouse", v).apply()
    
    var mouseScale: Float
        get() = prefs.getFloat("scale", 1.0f)
        set(v) = prefs.edit().putFloat("scale", v).apply()
}
```

## Common Patterns

### Toggle
```kotlin
fun toggle() {
    if (virtualMouse.isEnabled) {
        virtualMouse.disable()
    } else {
        virtualMouse.enable(rootLayout, webView)
    }
}
```

### With Settings
```kotlin
if (prefs.mouseEnabled) {
    virtualMouse.enable(rootLayout, webView, prefs.mouseScale)
}
```

### Update Scale
```kotlin
override fun onResume() {
    super.onResume()
    if (virtualMouse.isEnabled) {
        virtualMouse.setMouseScale(prefs.mouseScale)
    }
}
```

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Not showing | Check ConstraintLayout, Android 7.0+ |
| Poor performance | Enable hardware acceleration |
| Touch not working | Enable JavaScript |
| Cursor not changing | Use EnhancedWebView |

## Requirements

- Android 7.0+ (API 24+)
- Kotlin 1.8+
- AndroidX
- ConstraintLayout

## Manifest

```xml
<application
    android:hardwareAccelerated="true" ...>
```

## Dependencies

```groovy
implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
implementation 'androidx.webkit:webkit:1.8.0'
```

## Performance Tips

1. Enable hardware acceleration
2. Use appropriate scale for DPI
3. Disable WebView zoom
4. Use LAYER_TYPE_HARDWARE

## Links

- Full Guide: `INTEGRATION_GUIDE.md`
- Example: `EXAMPLE_PROJECT.md`
- README: `README.md`
