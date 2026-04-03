package vn.vhn.vhscode.chromebrowser.webclient

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView

class VSCodeWebChromeClient(
    private val onTitleReceived: ((String?) -> Unit)? = null,
    private val activity: Activity? = null
): WebChromeClient() {
    private var customView: View? = null
    private var customViewCallback: CustomViewCallback? = null

    override fun onReceivedTitle(view: WebView?, title: String?) {
        super.onReceivedTitle(view, title)
        onTitleReceived?.invoke(title)
    }

    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
        customView?.let { onHideCustomView(); return }
        customView = view
        customViewCallback = callback
        activity?.window?.decorView?.let {
            (it as? ViewGroup)?.addView(view, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ))
            it.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        }
    }

    override fun onHideCustomView() {
        activity?.window?.decorView?.let {
            (it as? ViewGroup)?.removeView(customView)
            it.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
        customViewCallback?.onCustomViewHidden()
        customView = null
        customViewCallback = null
    }
}
