package com.rk.terminal.ui.activities.webview

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.webkit.WebSettings
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.ahk.webview.plugin.browser.EnhancedWebView
import com.ahk.webview.plugin.virtualmouse.VirtualMouse
import com.rk.settings.Settings
import com.rk.terminal.R
import com.rk.terminal.ui.activities.terminal.MainActivity
import com.rk.libcommons.pendingCommand
import com.rk.libcommons.TerminalCommand

class WebViewActivity : AppCompatActivity() {
    
    private lateinit var rootLayout: ConstraintLayout
    private lateinit var webView: EnhancedWebView
    private lateinit var virtualMouse: VirtualMouse
    private var backPressedTime: Long = 0
    
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        
        setContentView(R.layout.activity_webview)
        
        // Handle back button
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    // Double back to open terminal
                    if (backPressedTime + 2000 > System.currentTimeMillis()) {
                        val intent = Intent(this@WebViewActivity, MainActivity::class.java)
                        startActivity(intent)
                        backPressedTime = 0
                    } else {
                        android.widget.Toast.makeText(this@WebViewActivity, "Press back again to open Terminal", android.widget.Toast.LENGTH_SHORT).show()
                        backPressedTime = System.currentTimeMillis()
                    }
                }
            }
        })
        
        rootLayout = findViewById(R.id.rootLayout)
        webView = findViewById(R.id.webView)
        
        // Clear WebView cache on every app open
        webView.clearCache(true)
        webView.clearFormData()
        webView.clearHistory()
        
        setupWebView()
        
        virtualMouse = VirtualMouse()
        // Don't enable virtual mouse here - it will be enabled when page loads successfully
        
        val url = intent.getStringExtra("url") ?: "http://${Settings.webview_url}:${Settings.webview_port}"
        webView.loadUrl(url)
    }
    
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            useWideViewPort = true
            loadWithOverviewMode = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            textZoom = 75
        }
        
        webView.addJavascriptInterface(object {
            @android.webkit.JavascriptInterface
            fun startServer() {
                runOnUiThread {
                    startVSCodeServer()
                }
            }
            
            @android.webkit.JavascriptInterface
            fun openTerminal() {
                runOnUiThread {
                    val intent = Intent(this@WebViewActivity, MainActivity::class.java)
                    startActivity(intent)
                }
            }
        }, "OfflinePageInterface")
        
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(
                view: android.webkit.WebView?,
                request: android.webkit.WebResourceRequest?,
                error: android.webkit.WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    // Disable virtual mouse when showing offline page
                    virtualMouse.disable()
                    view?.loadUrl("file:///android_asset/offline.html")
                }
            }
            
            override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Enable virtual mouse only for http/https pages, not file:// (offline page)
                if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                    if (!virtualMouse.isEnabled) {
                        virtualMouse.enable(rootLayout, webView, Settings.webview_cursor_scale)
                        virtualMouse.setOnRightClickListener {
                            val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                            imm?.hideSoftInputFromWindow(webView.windowToken, 0)
                        }
                    }
                } else if (url != null && url.startsWith("file://")) {
                    // Ensure virtual mouse is disabled for offline page
                    virtualMouse.disable()
                }
            }
        }
        webView.isFocusable = true
        webView.isFocusableInTouchMode = true
        webView.requestFocus()
    }
    
    private fun startVSCodeServer() {
        runOnUiThread {
            android.widget.Toast.makeText(this, "Starting server session...", android.widget.Toast.LENGTH_SHORT).show()
            
            try {
                // Set pendingCommand for server script
                val serverScriptPath = "${filesDir.parentFile!!.path}/local/bin/server"
                
                com.rk.libcommons.pendingCommand = com.rk.libcommons.TerminalCommand(
                    ubuntu = true,
                    shell = "/system/bin/sh",
                    args = arrayOf("-c", serverScriptPath),
                    id = "vscode-server-${System.currentTimeMillis()}",
                    workingMode = 0,
                    workingDir = filesDir.parentFile!!.path
                )
                
                // Open Terminal which will create the session
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                
                // Wait 3 seconds then return to WebView
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    val webViewIntent = Intent(this, WebViewActivity::class.java)
                    webViewIntent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(webViewIntent)
                }, 4500)
            } catch (e: Exception) {
                android.widget.Toast.makeText(this, "Failed to start server: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                android.util.Log.e("WebViewActivity", "Error starting server", e)
            }
        }
    }
    
    private fun openTerminal() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
    
    override fun onDestroy() {
        virtualMouse.disable()
        webView.destroy()
        super.onDestroy()
    }
}
