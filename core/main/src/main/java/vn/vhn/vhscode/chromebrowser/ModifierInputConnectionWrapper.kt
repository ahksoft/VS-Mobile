package vn.vhn.vhscode.chromebrowser

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.CompletionInfo
import android.view.inputmethod.CorrectionInfo
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputContentInfo

class ModifierInputConnectionWrapper(
    private val base: InputConnection,
    private val getVirtualKeysView: () -> Any?
) : InputConnection {
    
    companion object {
        const val TAG = "ModifierInputWrapper"
    }
    
    init {
        Log.d(TAG, "ModifierInputConnectionWrapper created")
    }
    
    private fun getModifierMetaState(): Int {
        var metaState = 0
        val vk = getVirtualKeysView()
        if (vk != null) {
            try {
                val readMethod = vk.javaClass.getMethod("readSpecialButton", Class.forName("com.rk.terminal.ui.screens.terminal.SpecialButton"), Boolean::class.javaPrimitiveType)
                val specialButtonClass = Class.forName("com.rk.terminal.ui.screens.terminal.SpecialButton")
                val ctrlButton = specialButtonClass.getField("CTRL").get(null)
                val altButton = specialButtonClass.getField("ALT").get(null)
                val shiftButton = specialButtonClass.getField("SHIFT").get(null)
                
                if (readMethod.invoke(vk, ctrlButton, false) == true) {
                    metaState = metaState or KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
                }
                if (readMethod.invoke(vk, altButton, false) == true) {
                    metaState = metaState or KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
                }
                if (readMethod.invoke(vk, shiftButton, false) == true) {
                    metaState = metaState or KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read virtual keys", e)
            }
        }
        return metaState
    }
    
    override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
        Log.d(TAG, "commitText called: '$text'")
        val metaState = getModifierMetaState()
        Log.d(TAG, "metaState: $metaState")
        if (metaState != 0 && text?.length == 1) {
            Log.d(TAG, "commitText with modifiers: '$text', meta=$metaState")
            val char = text[0]
            val keyCode = when {
                char in 'a'..'z' -> KeyEvent.KEYCODE_A + (char - 'a')
                char in 'A'..'Z' -> KeyEvent.KEYCODE_A + (char - 'A')
                else -> 0
            }
            if (keyCode != 0) {
                val downTime = System.currentTimeMillis()
                base.sendKeyEvent(KeyEvent(downTime, downTime, KeyEvent.ACTION_DOWN, keyCode, 0, metaState))
                base.sendKeyEvent(KeyEvent(downTime, downTime, KeyEvent.ACTION_UP, keyCode, 0, metaState))
                return true
            }
        }
        return base.commitText(text, newCursorPosition)
    }
    
    override fun getTextBeforeCursor(n: Int, flags: Int) = base.getTextBeforeCursor(n, flags)
    override fun getTextAfterCursor(n: Int, flags: Int) = base.getTextAfterCursor(n, flags)
    override fun getSelectedText(flags: Int) = base.getSelectedText(flags)
    override fun getCursorCapsMode(reqModes: Int) = base.getCursorCapsMode(reqModes)
    override fun getExtractedText(request: ExtractedTextRequest?, flags: Int) = base.getExtractedText(request, flags)
    override fun deleteSurroundingText(beforeLength: Int, afterLength: Int) = base.deleteSurroundingText(beforeLength, afterLength)
    override fun setComposingText(text: CharSequence?, newCursorPosition: Int) = base.setComposingText(text, newCursorPosition)
    override fun setComposingRegion(start: Int, end: Int) = base.setComposingRegion(start, end)
    override fun finishComposingText() = base.finishComposingText()
    override fun setSelection(start: Int, end: Int) = base.setSelection(start, end)
    override fun performEditorAction(editorAction: Int) = base.performEditorAction(editorAction)
    override fun performContextMenuAction(id: Int) = base.performContextMenuAction(id)
    override fun beginBatchEdit() = base.beginBatchEdit()
    override fun endBatchEdit() = base.endBatchEdit()
    override fun sendKeyEvent(event: KeyEvent?) = base.sendKeyEvent(event)
    override fun clearMetaKeyStates(states: Int) = base.clearMetaKeyStates(states)
    override fun reportFullscreenMode(enabled: Boolean) = base.reportFullscreenMode(enabled)
    override fun performPrivateCommand(action: String?, data: Bundle?) = base.performPrivateCommand(action, data)
    override fun requestCursorUpdates(cursorUpdateMode: Int) = base.requestCursorUpdates(cursorUpdateMode)
    override fun getHandler() = base.handler
    override fun closeConnection() = base.closeConnection()
    override fun commitCompletion(text: CompletionInfo?) = base.commitCompletion(text)
    override fun commitCorrection(correctionInfo: CorrectionInfo?) = base.commitCorrection(correctionInfo)
    override fun deleteSurroundingTextInCodePoints(beforeLength: Int, afterLength: Int) = base.deleteSurroundingTextInCodePoints(beforeLength, afterLength)
    override fun commitContent(inputContentInfo: InputContentInfo, flags: Int, opts: Bundle?) = base.commitContent(inputContentInfo, flags, opts)
}
