package com.ahk.webview.plugin.virtualmouse

import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet

class VirtualMouse {
    companion object {
        private const val TAG = "VirtualMouse"
    }

    private var mRootView: View? = null
    private var mMouseView: MouseView? = null
    private var onRightClickCallback: (() -> Unit)? = null

    val isEnabled: Boolean
        get() = mMouseView?.parent != null

    fun setOnRightClickListener(callback: () -> Unit) {
        onRightClickCallback = callback
        mMouseView?.setOnRightClickListener(callback)
    }

    fun enable(rootView: View, targetView: WebView, scaleFactor: Float = 1.0f) {
        if (MouseView.actionButtonOffset < 0) {
            Toast.makeText(
                rootView.context,
                "Failed to obtain actionButton offset. Virtual mouse may not work on this device.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        
        mRootView = rootView
        val constraintLayout = mRootView as? ConstraintLayout ?: run {
            Toast.makeText(
                rootView.context,
                "Root view must be a ConstraintLayout",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        
        if (mMouseView != null && mMouseView!!.parent == constraintLayout) {
            return
        }
        
        disable()
        
        mMouseView = MouseView(constraintLayout.context, targetView).also { mouseView ->
            mouseView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            mouseView.cursorScale = scaleFactor
            mouseView.id = View.generateViewId()
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mouseView.focusable = View.NOT_FOCUSABLE
            }
            
            val params = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.MATCH_PARENT
            )
            constraintLayout.addView(mouseView, params)
            mouseView.initialize()
            
            val constraintSet = ConstraintSet()
            constraintSet.clone(constraintLayout)
            for (dir in listOf(
                ConstraintSet.LEFT,
                ConstraintSet.TOP,
                ConstraintSet.RIGHT,
                ConstraintSet.BOTTOM
            )) {
                constraintSet.connect(mouseView.id, dir, ConstraintSet.PARENT_ID, dir, 0)
            }
            constraintSet.applyTo(constraintLayout)
            
            onRightClickCallback?.let { mouseView.setOnRightClickListener(it) }
        }
    }

    fun disable() {
        mMouseView?.also { mouseView ->
            val parent = mouseView.parent as? ViewGroup
            parent?.removeView(mouseView)
            mMouseView = null
        }
    }

    fun moveMouseTo(x: Float, y: Float) {
        mMouseView?.update(x, y)
        mMouseView?.postInvalidate()
    }

    fun setMouseScale(factor: Float) {
        mMouseView?.cursorScale = factor
    }
}
