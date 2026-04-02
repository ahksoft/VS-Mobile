package com.ahk.webview.plugin.virtualmouse

import android.view.PointerIcon

/**
 * Interface for listening to pointer icon changes in WebView
 */
interface PointerIconChangedListen {
    interface Listener {
        fun onPointerIconChanged(pointerIcon: PointerIcon?)
    }

    fun setPointerIconChangedListener(listener: Listener?)
}
