package com.rk.terminal.ui.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.webkit.*
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import okhttp3.Headers.Companion.toHeaders
import okhttp3.OkHttpClient
import okhttp3.Request
import com.rk.terminal.R
import java.io.UnsupportedEncodingException
import java.net.URLEncoder

class BrowserActivity : Activity() {
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var textUrl: EditText
    private lateinit var btnStart: ImageView
    private lateinit var btnGoBack: ImageView
    private lateinit var btnGoForward: ImageView
    private lateinit var favicon: ImageView
    private lateinit var manager: InputMethodManager
    private val TAG = "BrowserActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browser)

        manager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager

        initView()
        initWebView()
    }
    
    override fun onPause() {
        super.onPause()
        // Save current URL
        val prefs = getSharedPreferences("browser_prefs", MODE_PRIVATE)
        prefs.edit().putString("last_url", webView.url).apply()
    }

    private fun initView() {
        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        textUrl = findViewById(R.id.textUrl)
        favicon = findViewById(R.id.webIcon)
        btnStart = findViewById(R.id.btnStart)
        btnGoBack = findViewById(R.id.goBack)
        btnGoForward = findViewById(R.id.goForward)

        btnStart.setOnClickListener {
            if (textUrl.hasFocus()) {
                if (manager.isActive) {
                    manager.hideSoftInputFromWindow(textUrl.applicationWindowToken, 0)
                }
                var input = textUrl.text.toString()
                if (!isHttpUrl(input)) {
                    if (mayBeUrl(input)) {
                        input = "https://${input}"
                    } else {
                        try {
                            input = URLEncoder.encode(input, "utf-8")
                        } catch (e: UnsupportedEncodingException) {
                            Log.e(TAG, e.message.toString())
                        }
                        input = "https://www.google.com/search?q=${input}"
                    }
                }
                webView.loadUrl(input)
                textUrl.clearFocus()
            } else {
                webView.reload()
            }
        }

        btnGoBack.setOnClickListener {
            webView.goBack()
        }

        btnGoForward.setOnClickListener {
            webView.goForward()
        }

        textUrl.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                textUrl.setText(webView.url)
                textUrl.setSelection(textUrl.text.length)
                btnStart.setImageResource(R.drawable.arrow_right)
            } else {
                textUrl.setText(webView.title)
                btnStart.setImageResource(R.drawable.refresh)
            }
        }
        
        textUrl.setOnKeyListener { _, keyCode, keyEvent ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && keyEvent.action == KeyEvent.ACTION_DOWN) {
                btnStart.callOnClick()
                textUrl.clearFocus()
            }
            return@setOnKeyListener false
        }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                if (request.isForMainFrame) {
                    val url = request.url.toString()
                    if (!isHttpUrl(url)) {
                        return null
                    }

                    var headers = request.requestHeaders.toHeaders()
                    val cookie = CookieManager.getInstance().getCookie(url)
                    if (cookie != null) {
                        headers = (headers.toMap() + Pair("cookie", cookie)).toHeaders()
                    }

                    val client = OkHttpClient.Builder().followRedirects(false).build()
                    val req = Request.Builder()
                        .url(url)
                        .headers(headers)
                        .build()

                    return try {
                        val response = client.newCall(req).execute()
                        if (response.headers["content-security-policy"] == null) {
                            return null
                        }
                        val resHeaders =
                            response.headers.toMap().filter { it.key != "content-security-policy" }

                        WebResourceResponse(
                            "text/html",
                            response.header("content-encoding", "utf-8"),
                            response.code,
                            "ok",
                            resHeaders,
                            response.body?.byteStream()
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, e.message.toString())
                        null
                    }
                }
                return null
            }

            override fun onPageStarted(view: WebView?, url: String, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.progress = 0
                progressBar.visibility = View.VISIBLE
                setTextUrl("Loading...")
                this@BrowserActivity.favicon.setImageResource(R.drawable.tool)
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.INVISIBLE
                title = view.title
                setTextUrl(view.title)

                val script = """
                    (function () {
                        if (window.eruda) return;
                        var define;
                        if (window.define) {
                            define = window.define;
                            window.define = null;
                        }
                        var script = document.createElement('script'); 
                        script.src = '//cdn.jsdelivr.net/npm/eruda'; 
                        document.body.appendChild(script); 
                        script.onload = function () { 
                            eruda.init({ theme: 'Dark' });
                            if (define) {
                                window.define = define;
                            }
                        }
                    })();
                """
                webView.evaluateJavascript(script) {}
            }
            
            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                if (request?.isForMainFrame == true) {
                    val errorHtml = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <style>
                                body { 
                                    background: #111; 
                                    color: #fff; 
                                    font-family: monospace; 
                                    padding: 20px;
                                }
                                h2 { color: #f48771; }
                                #msg { 
                                    background: #222; 
                                    padding: 15px; 
                                    border-radius: 8px; 
                                    border-left: 4px solid #f48771;
                                }
                            </style>
                        </head>
                        <body>
                            <h2>Failed to Load</h2>
                            <div id="msg">
                                <strong>URL:</strong> ${request.url}<br><br>
                                <strong>Error:</strong> ${error?.description ?: "Unknown"}
                            </div>
                            <script src="https://cdn.jsdelivr.net/npm/eruda"></script>
                            <script>eruda.init({ theme: 'Dark' });</script>
                        </body>
                        </html>
                    """.trimIndent()
                    
                    view?.loadDataWithBaseURL(null, errorHtml, "text/html", "UTF-8", null)
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                progressBar.progress = newProgress
            }

            override fun onReceivedIcon(view: WebView, icon: Bitmap) {
                super.onReceivedIcon(view, icon)
                favicon.setImageBitmap(icon)
            }
        }

        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        // Load last URL or default
        val prefs = getSharedPreferences("browser_prefs", MODE_PRIVATE)
        val lastUrl = prefs.getString("last_url", null)
        val url = intent.getStringExtra("url") ?: lastUrl ?: "https://github.com/ahksoft"
        webView.loadUrl(url)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v is EditText) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    v.clearFocus()
                    if (manager.isActive) {
                        manager.hideSoftInputFromWindow(textUrl.applicationWindowToken, 0)
                    }
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    private fun setTextUrl(text: String?) {
        if (!textUrl.hasFocus() && text != null) {
            textUrl.setText(text)
        }
    }

    private fun isHttpUrl(url: String): Boolean {
        return url.startsWith("http:") || url.startsWith("https:")
    }

    private fun mayBeUrl(text: String): Boolean {
        val domains = arrayOf(".com", ".io", ".me", ".org", ".net", ".tv", ".cn")
        return domains.any { text.contains(it) }
    }
}
