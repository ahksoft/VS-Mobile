package vn.vhn.vhscode.chromebrowser

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.PointerIcon
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.webkit.WebView
import com.ahk.webview.plugin.virtualmouse.PointerIconChangedListen
import java.lang.reflect.Method

class VSCodeBrowser(context: Context, attrs: AttributeSet?) : WebView(context, attrs),
    PointerIconChangedListen {
    companion object {
        val TAG = "VSCodeBrowser"
        lateinit var mWebviewProviderMethod: Method

        init {
            mWebviewProviderMethod = WebView::class.java.getDeclaredMethod("getWebViewProvider")
            mWebviewProviderMethod.isAccessible = true
        }
    }

    private var mPointerIconChangedListener: PointerIconChangedListen.Listener? = null
    var keyEventInterceptor: ((KeyEvent) -> Boolean)? = null
    var inputConnectionWrapper: ((InputConnection?) -> InputConnection?)? = null

    override fun setPointerIconChangedListener(listener: PointerIconChangedListen.Listener?) {
        mPointerIconChangedListener = listener
    }

    override fun setPointerIcon(pointerIcon: PointerIcon?) {
        super.setPointerIcon(pointerIcon)
        mPointerIconChangedListener?.onPointerIconChanged(pointerIcon)
    }
    
    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        event?.let {
            if (keyEventInterceptor?.invoke(it) == true) {
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }
    
    override fun onCreateInputConnection(outAttrs: EditorInfo?): InputConnection? {
        Log.d(TAG, "onCreateInputConnection called")
        
        // Prevent keyboard from auto-showing by setting IME options
        outAttrs?.let {
            it.imeOptions = it.imeOptions or android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI
            it.imeOptions = it.imeOptions or android.view.inputmethod.EditorInfo.IME_FLAG_NO_FULLSCREEN
        }
        
        val baseInputConnection = super.onCreateInputConnection(outAttrs)
        Log.d(TAG, "baseInputConnection: $baseInputConnection")
        val wrapped = inputConnectionWrapper?.invoke(baseInputConnection)
        Log.d(TAG, "wrapped: $wrapped")
        return wrapped ?: baseInputConnection
    }
}