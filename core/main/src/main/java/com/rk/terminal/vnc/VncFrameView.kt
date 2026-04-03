package com.rk.terminal.vnc

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlinx.coroutines.*
import java.io.*
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder

class VncFrameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback {

    private var vncSocket: Socket? = null
    private var vncProfile: VncProfile? = null
    private var framebuffer: Bitmap? = null
    private var isConnected = false
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // VNC protocol constants
    private val RFB_VERSION = "RFB 003.008\n"
    private val SECURITY_TYPE_NONE = 1
    private val SECURITY_TYPE_VNC_AUTH = 2
    
    init {
        holder.addCallback(this)
    }
    
    fun connectToServer(profile: VncProfile) {
        this.vncProfile = profile
        coroutineScope.launch {
            try {
                performVncHandshake(profile)
            } catch (e: Exception) {
                e.printStackTrace()
                // Handle connection error
            }
        }
    }
    
    private suspend fun performVncHandshake(profile: VncProfile) = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("VncFrameView", "Connecting to ${profile.host}:${profile.port}")
            
            // Connect to VNC server
            vncSocket = Socket(profile.host, profile.port)
            val input = DataInputStream(vncSocket!!.getInputStream())
            val output = DataOutputStream(vncSocket!!.getOutputStream())
            
            android.util.Log.d("VncFrameView", "Socket connected, starting handshake")
            
            // Protocol version handshake
            val serverVersion = ByteArray(12)
            input.readFully(serverVersion)
            android.util.Log.d("VncFrameView", "Server version: ${String(serverVersion)}")
            
            output.write(RFB_VERSION.toByteArray())
            output.flush()
            
            // Security handshake
            val securityTypesCount = input.readUnsignedByte()
            android.util.Log.d("VncFrameView", "Security types count: $securityTypesCount")
            
            val securityTypes = ByteArray(securityTypesCount)
            input.readFully(securityTypes)
            android.util.Log.d("VncFrameView", "Security types: ${securityTypes.joinToString()}")
            
            // Choose security type (prefer None, fallback to VNC Auth)
            val chosenSecurity = if (securityTypes.contains(SECURITY_TYPE_NONE.toByte())) {
                android.util.Log.d("VncFrameView", "Using no security")
                SECURITY_TYPE_NONE
            } else if (securityTypes.contains(SECURITY_TYPE_VNC_AUTH.toByte())) {
                android.util.Log.d("VncFrameView", "Using VNC auth")
                SECURITY_TYPE_VNC_AUTH
            } else {
                android.util.Log.e("VncFrameView", "No supported security type")
                throw Exception("No supported security type")
            }
            
            output.writeByte(chosenSecurity)
            output.flush()
            
            // Handle security result
            if (chosenSecurity == SECURITY_TYPE_VNC_AUTH) {
                android.util.Log.d("VncFrameView", "Handling VNC authentication")
                // VNC authentication
                val challenge = ByteArray(16)
                input.readFully(challenge)
                
                val response = vncAuth(challenge, profile.password)
                output.write(response)
                output.flush()
            }
            
            val securityResult = input.readInt()
            android.util.Log.d("VncFrameView", "Security result: $securityResult")
            if (securityResult != 0) {
                throw Exception("Authentication failed")
            }
            
            // Client initialization
            output.writeByte(1) // Shared flag
            output.flush()
            android.util.Log.d("VncFrameView", "Sent client init")
            
            // Server initialization
            val framebufferWidth = input.readUnsignedShort()
            val framebufferHeight = input.readUnsignedShort()
            android.util.Log.d("VncFrameView", "Framebuffer size: ${framebufferWidth}x${framebufferHeight}")
            
            val pixelFormat = ByteArray(16)
            input.readFully(pixelFormat)
            val nameLength = input.readInt()
            val serverName = ByteArray(nameLength)
            input.readFully(serverName)
            android.util.Log.d("VncFrameView", "Server name: ${String(serverName)}")
            
            // Create framebuffer
            withContext(Dispatchers.Main) {
                framebuffer = Bitmap.createBitmap(
                    framebufferWidth, 
                    framebufferHeight, 
                    Bitmap.Config.ARGB_8888  // Use ARGB_8888 instead of RGB_565
                )
                isConnected = true
                android.util.Log.d("VncFrameView", "Framebuffer created, connection established")
            }
            
            // Set pixel format to RGB565
            setPixelFormat(output)
            
            // Request full screen update
            requestFramebufferUpdate(output, 0, 0, framebufferWidth, framebufferHeight)
            android.util.Log.d("VncFrameView", "Requested initial framebuffer update")
            
            // Start message loop
            messageLoop(input, output)
            
        } catch (e: Exception) {
            android.util.Log.e("VncFrameView", "VNC connection failed", e)
            withContext(Dispatchers.Main) {
                isConnected = false
            }
        }
    }
    
    private fun vncAuth(challenge: ByteArray, password: String): ByteArray {
        // Simple VNC authentication (DES encryption)
        // This is a simplified implementation
        val key = password.padEnd(8, '\u0000').take(8).toByteArray()
        return challenge // Simplified - should use DES encryption
    }
    
    private fun setPixelFormat(output: DataOutputStream) {
        output.writeByte(0) // Message type: SetPixelFormat
        output.writeByte(0) // Padding
        output.writeShort(0) // Padding
        
        // Pixel format for 32-bit BGRA (server default)
        output.writeByte(32) // bits-per-pixel
        output.writeByte(24) // depth
        output.writeByte(0)  // big-endian-flag
        output.writeByte(1)  // true-colour-flag
        output.writeShort(255) // red-max
        output.writeShort(255) // green-max
        output.writeShort(255) // blue-max
        output.writeByte(16)  // red-shift
        output.writeByte(8)   // green-shift
        output.writeByte(0)   // blue-shift
        output.writeByte(0)   // padding
        output.writeShort(0)  // padding
        output.flush()
        android.util.Log.d("VncFrameView", "Set pixel format to 32-bit BGRA")
    }
    
    private fun requestFramebufferUpdate(output: DataOutputStream, x: Int, y: Int, w: Int, h: Int) {
        output.writeByte(3) // Message type: FramebufferUpdateRequest
        output.writeByte(0) // incremental
        output.writeShort(x)
        output.writeShort(y)
        output.writeShort(w)
        output.writeShort(h)
        output.flush()
    }
    
    private suspend fun messageLoop(input: DataInputStream, output: DataOutputStream) {
        android.util.Log.d("VncFrameView", "Starting message loop")
        while (isConnected) {
            try {
                val messageType = input.readUnsignedByte()
                android.util.Log.d("VncFrameView", "Received message type: $messageType")
                when (messageType) {
                    0 -> handleFramebufferUpdate(input)
                    // Add other message types as needed
                    else -> android.util.Log.w("VncFrameView", "Unknown message type: $messageType")
                }
            } catch (e: Exception) {
                android.util.Log.e("VncFrameView", "Message loop error", e)
                break
            }
        }
        android.util.Log.d("VncFrameView", "Message loop ended")
    }
    
    private suspend fun handleFramebufferUpdate(input: DataInputStream) {
        android.util.Log.d("VncFrameView", "Handling framebuffer update")
        input.readByte() // padding
        val numberOfRectangles = input.readUnsignedShort()
        android.util.Log.d("VncFrameView", "Number of rectangles: $numberOfRectangles")
        
        for (i in 0 until numberOfRectangles) {
            val x = input.readUnsignedShort()
            val y = input.readUnsignedShort()
            val width = input.readUnsignedShort()
            val height = input.readUnsignedShort()
            val encoding = input.readInt()
            
            android.util.Log.d("VncFrameView", "Rectangle $i: ${x}x${y} ${width}x${height} encoding=$encoding")
            
            when (encoding) {
                0 -> handleRawEncoding(input, x, y, width, height)
                // Add other encodings as needed
                else -> {
                    android.util.Log.w("VncFrameView", "Unsupported encoding: $encoding")
                    // Skip this rectangle data
                    val bytesToSkip = width * height * 2 // Assuming RGB565
                    input.skipBytes(bytesToSkip)
                }
            }
        }
        
        // Draw framebuffer to surface
        withContext(Dispatchers.Main) {
            drawFramebuffer()
        }
        android.util.Log.d("VncFrameView", "Framebuffer update complete")
    }
    
    private fun handleRawEncoding(input: DataInputStream, x: Int, y: Int, width: Int, height: Int) {
        android.util.Log.d("VncFrameView", "Handling raw encoding: ${x},${y} ${width}x${height}")
        
        framebuffer?.let { bitmap ->
            // Read pixel data based on server's pixel format (usually 32-bit BGRA)
            val pixelData = ByteArray(width * height * 4) // 4 bytes per pixel for BGRA
            input.readFully(pixelData)
            
            val pixels = IntArray(width * height)
            val buffer = java.nio.ByteBuffer.wrap(pixelData).order(java.nio.ByteOrder.LITTLE_ENDIAN)
            
            for (i in pixels.indices) {
                // Read BGRA and convert to ARGB
                val b = buffer.get().toInt() and 0xFF
                val g = buffer.get().toInt() and 0xFF  
                val r = buffer.get().toInt() and 0xFF
                val a = buffer.get().toInt() and 0xFF
                pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }
            
            bitmap.setPixels(pixels, 0, width, x, y, width, height)
            android.util.Log.d("VncFrameView", "Updated bitmap region: ${x},${y} ${width}x${height}")
        }
    }
    
    private fun drawFramebuffer() {
        android.util.Log.d("VncFrameView", "Drawing framebuffer")
        val canvas = holder.lockCanvas()
        canvas?.let { c ->
            framebuffer?.let { bitmap ->
                val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
                val dstRect = Rect(0, 0, width, height)
                c.drawBitmap(bitmap, srcRect, dstRect, null)
                android.util.Log.d("VncFrameView", "Drew bitmap ${bitmap.width}x${bitmap.height} to ${width}x${height}")
            } ?: android.util.Log.w("VncFrameView", "No framebuffer to draw")
            holder.unlockCanvasAndPost(c)
        } ?: android.util.Log.w("VncFrameView", "Could not lock canvas")
    }
    
    fun onKeyEvent(event: KeyEvent?): Boolean {
        if (!isConnected || event == null) return false
        
        coroutineScope.launch {
            try {
                val output = DataOutputStream(vncSocket!!.getOutputStream())
                output.writeByte(4) // KeyEvent message
                output.writeByte(if (event.action == KeyEvent.ACTION_DOWN) 1 else 0)
                output.writeShort(0) // padding
                output.writeInt(event.keyCode)
                output.flush()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return true
    }
    
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (!isConnected || event == null) return false
        
        coroutineScope.launch {
            try {
                val output = DataOutputStream(vncSocket!!.getOutputStream())
                output.writeByte(5) // PointerEvent message
                
                val buttonMask = when (event.action) {
                    MotionEvent.ACTION_DOWN -> 1
                    MotionEvent.ACTION_UP -> 0
                    else -> 0
                }
                
                output.writeByte(buttonMask)
                output.writeShort(event.x.toInt())
                output.writeShort(event.y.toInt())
                output.flush()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return true
    }
    
    fun disconnect() {
        isConnected = false
        vncSocket?.close()
        vncSocket = null
        coroutineScope.cancel()
    }
    
    override fun surfaceCreated(holder: SurfaceHolder) {}
    
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
    
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isConnected = false
        vncSocket?.close()
        coroutineScope.cancel()
    }
}
