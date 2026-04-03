package com.rk.terminal.ui.screens.terminal

import android.annotation.SuppressLint
import android.content.Intent
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.widget.ConstraintLayout
import vn.vhn.vhscode.chromebrowser.VSCodeBrowser
import vn.vhn.vhscode.chromebrowser.webclient.VSCodeWebChromeClient
import vn.vhn.vhscode.chromebrowser.webclient.VSCodeWebClient
import vn.vhn.vhscode.generic_dispatcher.BBKeyboardEventDispatcher
import vn.vhn.vhscode.generic_dispatcher.IGenericEventDispatcher
import com.ahk.webview.plugin.virtualmouse.VirtualMouse
import com.rk.settings.Settings
import com.rk.terminal.ui.activities.terminal.MainActivity
import com.rk.terminal.ui.screens.settings.WorkingMode
import com.rk.terminal.ui.screens.terminal.virtualkeys.SpecialButton
import com.rk.terminal.ui.screens.terminal.virtualkeys.VirtualKeyButton
import com.rk.terminal.ui.screens.terminal.virtualkeys.VirtualKeysConstants
import com.rk.terminal.ui.screens.terminal.virtualkeys.VirtualKeysInfo
import com.rk.terminal.ui.screens.terminal.virtualkeys.VirtualKeysView
import com.rk.terminal.ui.screens.terminal.WEBVIEW_VIRTUAL_KEYS

// Key mapping for JavaScript events
private data class JsKey(val key: String, val code: String, val keyCode: Int)

private fun resolveJsKey(termuxKey: String, shift: Boolean): JsKey? = when (termuxKey) {
    "A" -> JsKey(if (shift) "A" else "a", "KeyA", 65)
    "B" -> JsKey(if (shift) "B" else "b", "KeyB", 66)
    "C" -> JsKey(if (shift) "C" else "c", "KeyC", 67)
    "D" -> JsKey(if (shift) "D" else "d", "KeyD", 68)
    "E" -> JsKey(if (shift) "E" else "e", "KeyE", 69)
    "F" -> JsKey(if (shift) "F" else "f", "KeyF", 70)
    "G" -> JsKey(if (shift) "G" else "g", "KeyG", 71)
    "H" -> JsKey(if (shift) "H" else "h", "KeyH", 72)
    "I" -> JsKey(if (shift) "I" else "i", "KeyI", 73)
    "J" -> JsKey(if (shift) "J" else "j", "KeyJ", 74)
    "K" -> JsKey(if (shift) "K" else "k", "KeyK", 75)
    "L" -> JsKey(if (shift) "L" else "l", "KeyL", 76)
    "M" -> JsKey(if (shift) "M" else "m", "KeyM", 77)
    "N" -> JsKey(if (shift) "N" else "n", "KeyN", 78)
    "O" -> JsKey(if (shift) "O" else "o", "KeyO", 79)
    "P" -> JsKey(if (shift) "P" else "p", "KeyP", 80)
    "Q" -> JsKey(if (shift) "Q" else "q", "KeyQ", 81)
    "R" -> JsKey(if (shift) "R" else "r", "KeyR", 82)
    "S" -> JsKey(if (shift) "S" else "s", "KeyS", 83)
    "T" -> JsKey(if (shift) "T" else "t", "KeyT", 84)
    "U" -> JsKey(if (shift) "U" else "u", "KeyU", 85)
    "V" -> JsKey(if (shift) "V" else "v", "KeyV", 86)
    "W" -> JsKey(if (shift) "W" else "w", "KeyW", 87)
    "X" -> JsKey(if (shift) "X" else "x", "KeyX", 88)
    "Y" -> JsKey(if (shift) "Y" else "y", "KeyY", 89)
    "Z" -> JsKey(if (shift) "Z" else "z", "KeyZ", 90)
    "UP" -> JsKey("ArrowUp", "ArrowUp", 38)
    "DOWN" -> JsKey("ArrowDown", "ArrowDown", 40)
    "LEFT" -> JsKey("ArrowLeft", "ArrowLeft", 37)
    "RIGHT" -> JsKey("ArrowRight", "ArrowRight", 39)
    "TAB" -> JsKey("Tab", "Tab", 9)
    "ESC" -> JsKey("Escape", "Escape", 27)
    "ENTER" -> JsKey("Enter", "Enter", 13)
    "HOME" -> JsKey("Home", "Home", 36)
    "END" -> JsKey("End", "End", 35)
    "PGUP" -> JsKey("PageUp", "PageUp", 33)
    "PGDN" -> JsKey("PageDown", "PageDown", 34)
    "/" -> JsKey("/", "Slash", 191)
    "-" -> JsKey("-", "Minus", 189)
    else -> null
}

private fun injectKeyboardEvent(
    webView: VSCodeBrowser?,
    key: String,
    code: String,
    keyCode: Int,
    ctrl: Boolean = false,
    alt: Boolean = false,
    shift: Boolean = false,
    meta: Boolean = false
) {
    val js = """
        (function() {
            var target = document.activeElement || document.body;
            ['keydown', 'keyup'].forEach(function(type) {
                target.dispatchEvent(new KeyboardEvent(type, {
                    key: '$key',
                    code: '$code',
                    keyCode: $keyCode,
                    which: $keyCode,
                    ctrlKey: $ctrl,
                    altKey: $alt,
                    shiftKey: $shift,
                    metaKey: $meta,
                    bubbles: true,
                    cancelable: true
                }));
            });
        })();
    """.trimIndent()
    webView?.evaluateJavascript(js, null)
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewSession(modifier: Modifier = Modifier, mainActivity: MainActivity, reloadTrigger: Int = 0, overrideUrl: String? = null) {
    val context = LocalContext.current
    var webView by remember { mutableStateOf<VSCodeBrowser?>(null) }
    var virtualMouse by remember { mutableStateOf<VirtualMouse?>(null) }
    var rootLayout by remember { mutableStateOf<ConstraintLayout?>(null) }
    var currentVirtualKeysView by remember { mutableStateOf<VirtualKeysView?>(null) }
    
    // Reload WebView when trigger changes
    LaunchedEffect(reloadTrigger) {
        if (reloadTrigger > 0) {
            android.util.Log.d("WebViewSession", "Reload triggered: $reloadTrigger, webView=$webView")
            webView?.reload()
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            virtualMouse?.disable()
            webView?.destroy()
        }
    }
    
    Column(modifier = modifier.fillMaxSize().statusBarsPadding()) {
        // WebView with virtual mouse
        AndroidView(
            factory = { ctx ->
                ConstraintLayout(ctx).apply {
                    rootLayout = this
                    layoutParams = ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.MATCH_PARENT,
                        ConstraintLayout.LayoutParams.MATCH_PARENT
                    )
                    
                    val enhancedWebView = VSCodeBrowser(ctx, null).apply {
                        webView = this
                        id = View.generateViewId()
                        
                        // Create base JS interface for keyboard handling
                        val baseJsInterface = vn.vhn.vhscode.chromebrowser.VSCodeJSInterface(ctx)
                        val jsInterface = object {
                            @android.webkit.JavascriptInterface
                            fun copyToClipboard(text: String?) = baseJsInterface.copyToClipboard(text)
                            
                            @android.webkit.JavascriptInterface
                            fun getClipboard(): String = baseJsInterface.getClipboard()
                            
                            @android.webkit.JavascriptInterface
                            fun isCtrlPressed(): Boolean {
                                android.util.Log.d("WebViewSession", "isCtrlPressed() called, currentVirtualKeysView=${currentVirtualKeysView != null}")
                                val result = currentVirtualKeysView?.readSpecialButton(SpecialButton.CTRL, false) == true
                                android.util.Log.d("WebViewSession", "isCtrlPressed() result=$result")
                                return result
                            }
                            
                            @android.webkit.JavascriptInterface
                            fun isAltPressed(): Boolean {
                                val result = currentVirtualKeysView?.readSpecialButton(SpecialButton.ALT, false) == true
                                android.util.Log.d("WebViewSession", "isAltPressed() called, result=$result")
                                return result
                            }
                            
                            @android.webkit.JavascriptInterface
                            fun isShiftKeyPressed(): Boolean {
                                val result = currentVirtualKeysView?.readSpecialButton(SpecialButton.SHIFT, false) == true
                                android.util.Log.d("WebViewSession", "isShiftKeyPressed() called, result=$result")
                                return result
                            }
                        }
                        
                        android.util.Log.d("WebViewSession", "Device: ${android.os.Build.BRAND} ${android.os.Build.MODEL}")
                        
                        // Initialize generic keyboard dispatcher for BlackBerry/Unihertz devices
                        val genericDispatcher: IGenericEventDispatcher? = if (
                            android.os.Build.MODEL.matches(Regex("BB[FB]100-[0-9]+")) || // BlackBerry Key1, Key2
                            android.os.Build.MODEL.matches(Regex("STV100-[0-9]+")) || // BlackBerry Priv
                            (android.os.Build.BRAND == "Unihertz" && android.os.Build.MODEL.matches(Regex("Titan( pocket)?.*")))
                        ) {
                            android.util.Log.d("WebViewSession", "Found physical keyboard device - enabling BBKeyboardEventDispatcher")
                            BBKeyboardEventDispatcher(baseJsInterface).also {
                                it.initializeForTarget(mainActivity, this)
                            }
                        } else {
                            android.util.Log.d("WebViewSession", "No physical keyboard detected - using virtual keyboard modifiers")
                            null
                        }
                        
                        // Add JS interface to WebView
                        addJavascriptInterface(jsInterface, "_vscode_js_interface_")
                        android.util.Log.d("WebViewSession", "JS Interface added to WebView")
                        
                        // Wrap InputConnection to intercept text input with modifiers
                        inputConnectionWrapper = { baseConnection ->
                            if (baseConnection != null) {
                                vn.vhn.vhscode.chromebrowser.ModifierInputConnectionWrapper(baseConnection) {
                                    currentVirtualKeysView
                                }
                            } else {
                                baseConnection
                            }
                        }
                        
                        // Set key event interceptor to apply virtual keyboard modifiers
                        var isProcessingModifiedEvent = false
                        keyEventInterceptor = { event ->
                            if (isProcessingModifiedEvent) {
                                // Don't intercept our own modified events
                                false
                            } else {
                                android.util.Log.d("WebViewSession", "Key event: code=${event.keyCode}, action=${event.action}, meta=${event.metaState}")
                                
                                // Try generic dispatcher first (for physical keyboards)
                                if (genericDispatcher?.dispatchKeyEvent(event) == true) {
                                    android.util.Log.d("WebViewSession", "Event handled by generic dispatcher")
                                    true
                            } else if (event.action == android.view.KeyEvent.ACTION_DOWN) {
                                var metaState = event.metaState
                                var modified = false
                                
                                android.util.Log.d("WebViewSession", "currentVirtualKeysView is ${if (currentVirtualKeysView == null) "NULL" else "available"}")
                                
                                currentVirtualKeysView?.let { vk ->
                                    if (vk.readSpecialButton(SpecialButton.CTRL, false) == true) {
                                        android.util.Log.d("WebViewSession", "CTRL modifier detected")
                                        metaState = metaState or android.view.KeyEvent.META_CTRL_ON or android.view.KeyEvent.META_CTRL_LEFT_ON
                                        modified = true
                                    }
                                    if (vk.readSpecialButton(SpecialButton.ALT, false) == true) {
                                        android.util.Log.d("WebViewSession", "ALT modifier detected")
                                        metaState = metaState or android.view.KeyEvent.META_ALT_ON or android.view.KeyEvent.META_ALT_LEFT_ON
                                        modified = true
                                    }
                                    if (vk.readSpecialButton(SpecialButton.SHIFT, false) == true) {
                                        android.util.Log.d("WebViewSession", "SHIFT modifier detected")
                                        metaState = metaState or android.view.KeyEvent.META_SHIFT_ON or android.view.KeyEvent.META_SHIFT_LEFT_ON
                                        modified = true
                                    }
                                    if (vk.readSpecialButton(SpecialButton.FN, false) == true) {
                                        android.util.Log.d("WebViewSession", "FN modifier detected")
                                        metaState = metaState or android.view.KeyEvent.META_META_ON or android.view.KeyEvent.META_META_LEFT_ON
                                        modified = true
                                    }
                                }
                                
                                if (modified) {
                                    android.util.Log.d("WebViewSession", "Dispatching modified key event with metaState=$metaState")
                                    val newEvent = android.view.KeyEvent(
                                        event.downTime,
                                        event.eventTime,
                                        event.action,
                                        event.keyCode,
                                        event.repeatCount,
                                        metaState
                                    )
                                    isProcessingModifiedEvent = true
                                    this@apply.dispatchKeyEvent(newEvent)
                                    isProcessingModifiedEvent = false
                                    true
                                } else {
                                    false
                                }
                            } else {
                                false
                            }
                            }
                        }
                        
                        // Clear cache
                        clearCache(true)
                        clearFormData()
                        clearHistory()
                        
                        // Setup WebView
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            allowContentAccess = true
                            allowFileAccess = true
                            @Suppress("DEPRECATION")
                            allowFileAccessFromFileURLs = true
                            @Suppress("DEPRECATION")
                            allowUniversalAccessFromFileURLs = true
                            cacheMode = WebSettings.LOAD_DEFAULT
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            fixedFontFamily = "vscode-monospace"
                        }
                        
                        // Track if we're on offline page to disable zoom
                        var isOfflinePage = false
                        
                        // Set initial scale like VSDevEditor (uses zoom level setting)
                        // Don't set scale for offline page to prevent zoom loop
                        setInitialScale(Settings.webview_zoom_level)
                        
                        // Prevent keyboard from opening/closing repeatedly
                        isFocusable = true
                        isFocusableInTouchMode = true
                        
                        // Disable long click to prevent context menu keyboard issues
                        setOnLongClickListener { true }
                        isLongClickable = false
                        
                        // Add JavaScript interface for offline page
                        addJavascriptInterface(object {
                            @android.webkit.JavascriptInterface
                            fun startServer() {
                                (ctx as? android.app.Activity)?.runOnUiThread {
                                    startVSCodeServer(mainActivity)
                                }
                            }
                            
                            @android.webkit.JavascriptInterface
                            fun checkPackagesInstalled(): Boolean {
                                val packagesFile = java.io.File(ctx.filesDir.parentFile, "local/ubuntu/root/.packages_installed")
                                return packagesFile.exists()
                            }
                            
                            @android.webkit.JavascriptInterface
                            fun installResources() {
                                (ctx as? android.app.Activity)?.runOnUiThread {
                                    // Create Ubuntu terminal session for resource installation
                                    val prefs = ctx.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                                    prefs.edit().putBoolean("open_terminal_for_setup", true).apply()
                                    
                                    // Switch to terminal session (will trigger setup)
                                    val sessionId = "install-${System.currentTimeMillis()}"
                                    mainActivity.sessionBinder?.getService()?.currentSession?.value = Pair(sessionId, WorkingMode.UBUNTU)
                                }
                            }
                        }, "OfflinePageInterface")
                        
                        setLayerType(View.LAYER_TYPE_HARDWARE, null)
                        
                        isFocusable = true
                        isFocusableInTouchMode = true
                        requestFocus()
                        
                        // Prevent long click from stealing focus
                        setOnLongClickListener { true }
                        isLongClickable = false
                        
                        // Inject JavaScript keyboard bridge
                        webChromeClient = VSCodeWebChromeClient(activity = mainActivity)
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                
                                // Check if this is offline page
                                val isOffline = url?.contains("file:///android_asset/offline.html") == true
                                if (isOffline) {
                                    // Reset scale for offline page
                                    setInitialScale(0)
                                    isOfflinePage = true
                                } else if (isOfflinePage) {
                                    // Restore scale when leaving offline page
                                    setInitialScale(Settings.webview_zoom_level)
                                    isOfflinePage = false
                                }
                                
                                // Inject vsboot.js for keyboard handling
                                try {
                                    val vsbootJs = ctx.assets.open("vsboot.js").bufferedReader().use { it.readText() }
                                    val bootScript = """
                                        (function(){
                                            if(window.__vscode_boot_included__) return;
                                            window.__vscode_boot_included__ = true;
                                            $vsbootJs
                                            
                                            // Override keyboard event properties to include virtual modifiers
                                            if(!window.vscodeOrigKeyboardEventDescriptorCtrlKey) {
                                                window.vscodeOrigKeyboardEventDescriptorCtrlKey = Object.getOwnPropertyDescriptor(window.KeyboardEvent.prototype, 'ctrlKey');
                                            }
                                            if(!window.vscodeOrigKeyboardEventDescriptorAltKey) {
                                                window.vscodeOrigKeyboardEventDescriptorAltKey = Object.getOwnPropertyDescriptor(window.KeyboardEvent.prototype, 'altKey');
                                            }
                                            if(!window.vscodeOrigKeyboardEventDescriptorShiftKey) {
                                                window.vscodeOrigKeyboardEventDescriptorShiftKey = Object.getOwnPropertyDescriptor(window.KeyboardEvent.prototype, 'shiftKey');
                                            }
                                            
                                            var ctrlGetter = window.vscodeOrigKeyboardEventDescriptorCtrlKey.get;
                                            var altGetter = window.vscodeOrigKeyboardEventDescriptorAltKey.get;
                                            var shiftGetter = window.vscodeOrigKeyboardEventDescriptorShiftKey.get;
                                            
                                            Object.defineProperty(window.KeyboardEvent.prototype, 'ctrlKey', {
                                                get(){
                                                    let orig = ctrlGetter.apply(this);
                                                    if (orig) return true;
                                                    if (typeof _vscode_js_interface_ !== 'undefined') {
                                                        return _vscode_js_interface_.isCtrlPressed();
                                                    }
                                                    return false;
                                                }
                                            });
                                            
                                            Object.defineProperty(window.KeyboardEvent.prototype, 'altKey', {
                                                get(){
                                                    let orig = altGetter.apply(this);
                                                    if (orig) return true;
                                                    if (typeof _vscode_js_interface_ !== 'undefined') {
                                                        return _vscode_js_interface_.isAltPressed();
                                                    }
                                                    return false;
                                                }
                                            });
                                            
                                            Object.defineProperty(window.KeyboardEvent.prototype, 'shiftKey', {
                                                get(){
                                                    let orig = shiftGetter.apply(this);
                                                    if (orig) return true;
                                                    if (typeof _vscode_js_interface_ !== 'undefined') {
                                                        return _vscode_js_interface_.isShiftKeyPressed();
                                                    }
                                                    return false;
                                                }
                                            });
                                        })();
                                    """.trimIndent()
                                    view?.evaluateJavascript(bootScript, null)
                                    android.util.Log.d("WebViewSession", "vsboot.js injected successfully")
                                    
                                    // Inject viewport meta tag to fix popup positioning
                                    view?.evaluateJavascript("""
                                        (function(){
                                            if (!document.querySelector('meta[name="viewport"]')) {
                                                var meta = document.createElement('meta');
                                                meta.name = 'viewport';
                                                meta.content = 'width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no';
                                                document.head.appendChild(meta);
                                                console.log('Viewport meta tag added');
                                            }
                                        })();
                                    """.trimIndent(), null)
                                    
                                    // Test if JS interface is accessible
                                    view?.evaluateJavascript("""
                                        (function(){
                                            if (typeof _vscode_js_interface_ !== 'undefined') {
                                                console.log('JS Interface available');
                                                console.log('isCtrlPressed:', _vscode_js_interface_.isCtrlPressed());
                                            } else {
                                                console.log('JS Interface NOT available');
                                            }
                                        })();
                                    """.trimIndent(), null)
                                } catch (e: Exception) {
                                    android.util.Log.e("WebViewSession", "Failed to inject vsboot.js", e)
                                }
                                
                                // Inject keyboard bridge
                                val jsKeyboardBridge = """
                                    window.__sendKey = function(key, ctrl, alt, shift, meta) {
                                        const eventInit = {
                                            key: key,
                                            code: key,
                                            ctrlKey: ctrl,
                                            altKey: alt,
                                            shiftKey: shift,
                                            metaKey: meta,
                                            bubbles: true,
                                            cancelable: true
                                        };
                                        let target = document.activeElement || document.body;
                                        target.dispatchEvent(new KeyboardEvent('keydown', eventInit));
                                        target.dispatchEvent(new KeyboardEvent('keypress', eventInit));
                                        target.dispatchEvent(new KeyboardEvent('keyup', eventInit));
                                    };
                                """
                                evaluateJavascript(jsKeyboardBridge, null)
                                
                                // Original page finished logic
                                if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                                    if (Settings.webview_virtual_mouse_enabled && virtualMouse?.isEnabled != true) {
                                        rootLayout?.let { layout ->
                                            virtualMouse = VirtualMouse().apply {
                                                enable(layout, webView!!, Settings.webview_cursor_scale)
                                                setOnRightClickListener {
                                                    val imm = ctx.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                                                    imm?.hideSoftInputFromWindow(webView?.windowToken, 0)
                                                }
                                            }
                                        }
                                    } else if (!Settings.webview_virtual_mouse_enabled) {
                                        virtualMouse?.disable()
                                    }
                                } else if (url != null && url.startsWith("file://")) {
                                    virtualMouse?.disable()
                                }
                            }
                            
                            override fun onReceivedError(
                                view: android.webkit.WebView?,
                                request: android.webkit.WebResourceRequest?,
                                error: android.webkit.WebResourceError?
                            ) {
                                super.onReceivedError(view, request, error)
                                if (request?.isForMainFrame == true) {
                                    virtualMouse?.disable()
                                    view?.loadUrl("file:///android_asset/offline.html")
                                }
                            }
                        }
                        
                        // Load URL
                        loadUrl(overrideUrl ?: "http://${Settings.webview_url}:${Settings.webview_port}")
                    }
                    
                    addView(enhancedWebView, ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.MATCH_PARENT,
                        ConstraintLayout.LayoutParams.MATCH_PARENT
                    ))
                }
            },
            modifier = Modifier.weight(1f).fillMaxWidth()
        )
        
        // Virtual keys (same as terminal)
        val pagerState = rememberPagerState(pageCount = { 2 })
        val onSurfaceColor = MaterialTheme.colorScheme.onSurface.toArgb()
        
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(75.dp)
        ) { page ->
            when (page) {
                0 -> {
                    AndroidView(
                        factory = { ctx ->
                            VirtualKeysView(ctx, null).apply {
                                currentVirtualKeysView = this
                                
                                virtualKeysViewClient = object : VirtualKeysView.IVirtualKeysView {
                                    override fun onVirtualKeyButtonClick(
                                        view: android.view.View?,
                                        buttonInfo: VirtualKeyButton?,
                                        button: android.widget.Button?
                                    ) {
                                        val key = buttonInfo?.key ?: return
                                        
                                        // Handle BACK key - go back in webview
                                        if (key == "BACK") {
                                            if (webView?.canGoBack() == true) {
                                                webView?.goBack()
                                            }
                                            return
                                        }
                                        
                                        // Handle MOUSE key - toggle virtual mouse
                                        if (key == "MOUSE") {
                                            virtualMouse?.let { mouse ->
                                                if (mouse.isEnabled) {
                                                    mouse.disable()
                                                } else {
                                                    rootLayout?.let { layout ->
                                                        mouse.enable(layout, webView!!, Settings.webview_cursor_scale)
                                                    }
                                                }
                                            } ?: run {
                                                // Create virtual mouse if it doesn't exist
                                                rootLayout?.let { layout ->
                                                    virtualMouse = VirtualMouse().apply {
                                                        enable(layout, webView!!, Settings.webview_cursor_scale)
                                                    }
                                                }
                                            }
                                            return
                                        }
                                        
                                        // Handle special characters
                                        if (key == "~") {
                                            webView?.evaluateJavascript(
                                                """
                                                (function() {
                                                    var event = new KeyboardEvent('keydown', {
                                                        key: '$key',
                                                        code: 'Backquote',
                                                        keyCode: 192,
                                                        which: 192,
                                                        shiftKey: true,
                                                        bubbles: true,
                                                        cancelable: true
                                                    });
                                                    document.activeElement.dispatchEvent(event);
                                                    
                                                    var inputEvent = new InputEvent('input', {
                                                        data: '$key',
                                                        inputType: 'insertText',
                                                        bubbles: true,
                                                        cancelable: true
                                                    });
                                                    document.activeElement.dispatchEvent(inputEvent);
                                                })();
                                                """.trimIndent(),
                                                null
                                            )
                                            return
                                        }
                                        
                                        // Check modifier states
                                        var ctrlKey = false
                                        var altKey = false
                                        var shiftKey = false
                                        var metaKey = false
                                        
                                        currentVirtualKeysView?.let { vkView ->
                                            ctrlKey = vkView.readSpecialButton(SpecialButton.CTRL, true) == true
                                            altKey = vkView.readSpecialButton(SpecialButton.ALT, true) == true
                                            shiftKey = vkView.readSpecialButton(SpecialButton.SHIFT, true) == true
                                            metaKey = vkView.readSpecialButton(SpecialButton.FN, true) == true
                                        }
                                        
                                        when (key) {
                                            "CTRL", "ALT", "SHIFT" -> {
                                                // Modifier keys just toggle state
                                            }
                                            else -> {
                                                val keyCode = when (key) {
                                                    "UP" -> android.view.KeyEvent.KEYCODE_DPAD_UP
                                                    "DOWN" -> android.view.KeyEvent.KEYCODE_DPAD_DOWN
                                                    "LEFT" -> android.view.KeyEvent.KEYCODE_DPAD_LEFT
                                                    "RIGHT" -> android.view.KeyEvent.KEYCODE_DPAD_RIGHT
                                                    "ENTER" -> android.view.KeyEvent.KEYCODE_ENTER
                                                    "PGUP" -> android.view.KeyEvent.KEYCODE_PAGE_UP
                                                    "PGDN" -> android.view.KeyEvent.KEYCODE_PAGE_DOWN
                                                    "TAB" -> android.view.KeyEvent.KEYCODE_TAB
                                                    "HOME" -> android.view.KeyEvent.KEYCODE_MOVE_HOME
                                                    "END" -> android.view.KeyEvent.KEYCODE_MOVE_END
                                                    "ESC" -> android.view.KeyEvent.KEYCODE_ESCAPE
                                                    "-" -> android.view.KeyEvent.KEYCODE_MINUS
                                                    "/" -> android.view.KeyEvent.KEYCODE_SLASH
                                                    else -> {
                                                        if (key.length == 1) {
                                                            android.view.KeyEvent.keyCodeFromString("KEYCODE_${key.uppercase()}")
                                                        } else -1
                                                    }
                                                }
                                                
                                                if (keyCode != -1) {
                                                    var metaState = 0
                                                    if (ctrlKey) metaState = metaState or android.view.KeyEvent.META_CTRL_ON or android.view.KeyEvent.META_CTRL_LEFT_ON
                                                    if (altKey) metaState = metaState or android.view.KeyEvent.META_ALT_ON or android.view.KeyEvent.META_ALT_LEFT_ON
                                                    if (shiftKey) metaState = metaState or android.view.KeyEvent.META_SHIFT_ON or android.view.KeyEvent.META_SHIFT_LEFT_ON
                                                    if (metaKey) metaState = metaState or android.view.KeyEvent.META_META_ON or android.view.KeyEvent.META_META_LEFT_ON
                                                    
                                                    val downEvent = android.view.KeyEvent(
                                                        android.os.SystemClock.uptimeMillis(),
                                                        android.os.SystemClock.uptimeMillis(),
                                                        android.view.KeyEvent.ACTION_DOWN,
                                                        keyCode,
                                                        0,
                                                        metaState
                                                    )
                                                    val upEvent = android.view.KeyEvent(
                                                        android.os.SystemClock.uptimeMillis(),
                                                        android.os.SystemClock.uptimeMillis(),
                                                        android.view.KeyEvent.ACTION_UP,
                                                        keyCode,
                                                        0,
                                                        metaState
                                                    )
                                                    
                                                    webView?.dispatchKeyEvent(downEvent)
                                                    webView?.dispatchKeyEvent(upEvent)
                                                }
                                            }
                                        }
                                    }
                                    
                                    override fun performVirtualKeyButtonHapticFeedback(
                                        view: android.view.View?,
                                        buttonInfo: VirtualKeyButton?,
                                        button: android.widget.Button?
                                    ): Boolean {
                                        return false
                                    }
                                }
                                
                                buttonTextColor = onSurfaceColor
                                
                                reload(
                                    VirtualKeysInfo(
                                        WEBVIEW_VIRTUAL_KEYS,
                                        "",
                                        VirtualKeysConstants.CONTROL_CHARS_ALIASES
                                    )
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(75.dp)
                    )
                }
                1 -> {
                    // Custom command buttons
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(6),
                        modifier = Modifier.fillMaxSize().padding(2.dp),
                        contentPadding = PaddingValues(2.dp)
                    ) {
                        items(12) { index ->
                            val (label, action) = when (index) {
                                // Row 1 - Icons only
                                0 -> Pair("⌘", { sendKeys(webView, "P", ctrl = true, shift = true) })
                                1 -> Pair("▤", { sendKeys(webView, "B", ctrl = true) })
                                2 -> Pair("⚡", { sendKeys(webView, "P", ctrl = true) })
                                3 -> Pair("Select All", { sendKeys(webView, "A", ctrl = true) })
                                4 -> Pair("↶", { sendKeys(webView, "Z", ctrl = true) })
                                5 -> Pair("↷", { sendKeys(webView, "Y", ctrl = true) })
                                // Row 2 - Text shortcuts
                                6 -> Pair("❯_", { sendKeys(webView, "`", ctrl = true) })
                                7 -> Pair("Ctrl+X", { sendKeys(webView, "X", ctrl = true) })
                                8 -> Pair("Ctrl+C", { sendKeys(webView, "C", ctrl = true) })
                                9 -> Pair("Ctrl+V", { sendKeys(webView, "V", ctrl = true) })
                                10 -> Pair("Ctrl+F", { sendKeys(webView, "F", ctrl = true) })
                                11 -> Pair("Ctrl+E", { sendKeys(webView, "E", ctrl = true) })
                                else -> Pair("", {})
                            }
                            
                            androidx.compose.material3.OutlinedButton(
                                onClick = { action() },
                                modifier = Modifier
                                    .padding(1.dp)
                                    .height(28.dp),
                                contentPadding = PaddingValues(2.dp),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                            ) {
                                androidx.compose.material3.Text(
                                    text = label,
                                    fontSize = if (label.contains("Ctrl")) 10.sp else 16.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun sendKeys(
    webView: VSCodeBrowser?,
    key: String,
    ctrl: Boolean = false,
    alt: Boolean = false,
    shift: Boolean = false,
    meta: Boolean = false
) {
    val keyCode = when (key) {
        "P" -> android.view.KeyEvent.KEYCODE_P
        "B" -> android.view.KeyEvent.KEYCODE_B
        "`" -> android.view.KeyEvent.KEYCODE_GRAVE
        "A" -> android.view.KeyEvent.KEYCODE_A
        "C" -> android.view.KeyEvent.KEYCODE_C
        "X" -> android.view.KeyEvent.KEYCODE_X
        "V" -> android.view.KeyEvent.KEYCODE_V
        "F" -> android.view.KeyEvent.KEYCODE_F
        "E" -> android.view.KeyEvent.KEYCODE_E
        "Z" -> android.view.KeyEvent.KEYCODE_Z
        "Y" -> android.view.KeyEvent.KEYCODE_Y
        else -> return
    }
    
    var metaState = 0
    if (ctrl) metaState = metaState or android.view.KeyEvent.META_CTRL_ON or android.view.KeyEvent.META_CTRL_LEFT_ON
    if (alt) metaState = metaState or android.view.KeyEvent.META_ALT_ON or android.view.KeyEvent.META_ALT_LEFT_ON
    if (shift) metaState = metaState or android.view.KeyEvent.META_SHIFT_ON or android.view.KeyEvent.META_SHIFT_LEFT_ON
    if (meta) metaState = metaState or android.view.KeyEvent.META_META_ON or android.view.KeyEvent.META_META_LEFT_ON
    
    val downEvent = android.view.KeyEvent(
        android.os.SystemClock.uptimeMillis(),
        android.os.SystemClock.uptimeMillis(),
        android.view.KeyEvent.ACTION_DOWN,
        keyCode,
        0,
        metaState
    )
    val upEvent = android.view.KeyEvent(
        android.os.SystemClock.uptimeMillis(),
        android.os.SystemClock.uptimeMillis(),
        android.view.KeyEvent.ACTION_UP,
        keyCode,
        0,
        metaState
    )
    
    webView?.dispatchKeyEvent(downEvent)
    webView?.dispatchKeyEvent(upEvent)
}

private fun startVSCodeServer(mainActivity: MainActivity) {
    android.widget.Toast.makeText(mainActivity, "Starting server session...", android.widget.Toast.LENGTH_SHORT).show()
    
    try {
        val serverScriptPath = "${mainActivity.filesDir.parentFile!!.path}/local/bin/server"
        val newSessionId = "server-${System.currentTimeMillis()}"
        
        com.rk.libcommons.pendingCommand = com.rk.libcommons.TerminalCommand(
            ubuntu = true,
            shell = "/system/bin/sh",
            args = arrayOf("-c", serverScriptPath),
            id = newSessionId,
            workingMode = 0,
            workingDir = mainActivity.filesDir.parentFile!!.path
        )
        
        // Switch to terminal session first, then it will create the session
        mainActivity.runOnUiThread {
            // Create a terminal session if terminalView exists
            if (terminalView.get() != null) {
                val client = TerminalBackEnd(terminalView.get()!!, mainActivity)
                val session = mainActivity.sessionBinder!!.createSession(
                    newSessionId,
                    client,
                    mainActivity,
                    workingMode = 0
                )
                
                terminalView.get()?.apply {
                    session.updateTerminalSessionClient(client)
                    attachSession(session)
                    setTerminalViewClient(client)
                }
                
                mainActivity.sessionBinder?.getService()?.currentSession?.value = Pair(newSessionId, 0)
            } else {
                // If no terminal view yet, just set current session and it will be created
                mainActivity.sessionBinder?.getService()?.currentSession?.value = Pair(newSessionId, 0)
            }
            
            // Auto-switch back to webview after 5 seconds
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                changeSession(mainActivity, "webview")
                android.widget.Toast.makeText(mainActivity, "Server started, switched back to browser", android.widget.Toast.LENGTH_SHORT).show()
            }, 5000)
        }
    } catch (e: Exception) {
        android.widget.Toast.makeText(mainActivity, "Failed to start server: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        android.util.Log.e("WebViewSession", "Error starting server", e)
    }
}
