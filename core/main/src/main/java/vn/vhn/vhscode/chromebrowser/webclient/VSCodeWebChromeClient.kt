package vn.vhn.vhscode.chromebrowser.webclient

import android.webkit.WebChromeClient
import android.webkit.WebView

class VSCodeWebChromeClient(
    private val onTitleReceived: ((String?) -> Unit)? = null
): WebChromeClient() {
    override fun onReceivedTitle(view: WebView?, title: String?) {
        super.onReceivedTitle(view, title)
        onTitleReceived?.invoke(title)
    }
}