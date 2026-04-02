package vn.vhn.vhscode.chromebrowser.webclient

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.util.Log
import android.webkit.*
import java.net.URI

class VSCodeWebClient(
    val rootUrl: String,
    private val onPageStarted: ((WebView?, String?, Bitmap?) -> Unit)? = null,
    private val onPageFinished: ((WebView?, String?) -> Unit)? = null
) : WebViewClient() {
    companion object {
        val TAG = "VSCodeWebClient"
    }

    private val rootUri: Uri = Uri.parse(rootUrl)
    private val resource_html = Regex("\\.html?(\\?|\$)")

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        Log.d(TAG, "Page started: $url")
        onPageStarted?.invoke(view, url, favicon)
    }

    override fun onLoadResource(view: WebView?, url: String?) {
        super.onLoadResource(view, url)
        if (url != null && url.contains(resource_html)) {
            Log.d(TAG, "Loading HTML resource: $url")
        }
    }

    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
        Log.d(TAG, "SSL error ${URI.create(error?.url)?.host} vs ${rootUri.host}: $error")
        if (error != null && URI.create(error.url)?.host != rootUri.host) {
            Log.d(TAG, "SSL -> cancel")
            handler?.cancel()
            return
        }
        handler?.proceed()
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        if (request != null && request.isForMainFrame) {
            if (request.url.host != rootUri.host) {
                Log.d(TAG, "External URL: ${request.url.host}, opening in browser")
                view?.context?.startActivity(Intent(Intent.ACTION_VIEW, request.url))
                return true
            }
        }
        return super.shouldOverrideUrlLoading(view, request)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        Log.d(TAG, "Page finished: $url")
        onPageFinished?.invoke(view, url)
    }
}