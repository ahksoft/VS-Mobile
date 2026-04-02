package com.ahk.webview.plugin.browser

import android.content.Context
import android.util.AttributeSet
import android.view.PointerIcon
import android.webkit.WebView
import com.ahk.webview.plugin.virtualmouse.PointerIconChangedListen
import java.lang.reflect.Method

/**
 * Enhanced WebView with pointer icon change notifications
 * This WebView notifies listeners when the cursor icon changes (e.g., from arrow to hand)
 */
class EnhancedWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr), PointerIconChangedListen {
    
    companion object {
        private const val TAG = "EnhancedWebView"
        private lateinit var mWebviewProviderMethod: Method

        init {
            try {
                mWebviewProviderMethod = WebView::class.java.getDeclaredMethod("getWebViewProvider")
                mWebviewProviderMethod.isAccessible = true
            } catch (e: Exception) {
                // Method not available on this Android version
            }
        }
    }

    private var mPointerIconChangedListener: PointerIconChangedListen.Listener? = null

    override fun setPointerIconChangedListener(listener: PointerIconChangedListen.Listener?) {
        mPointerIconChangedListener = listener
    }

    override fun setPointerIcon(pointerIcon: PointerIcon?) {
        super.setPointerIcon(pointerIcon)
        mPointerIconChangedListener?.onPointerIconChanged(pointerIcon)
    }
}
