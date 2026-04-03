package com.rk.terminal.vnc

import android.os.Parcel
import android.os.Parcelable

data class VncProfile(
    var ID: Long = 0,
    var name: String = "",
    var host: String = "",
    var port: Int = 5900,
    var username: String = "",
    var password: String = "",
    var securityType: Int = 0,
    var channelType: Int = CHANNEL_TCP,
    var colorLevel: Int = 7,
    var imageQuality: Int = 5,
    var useRawEncoding: Boolean = false,
    var zoom1: Float = 1f,
    var zoom2: Float = 1f,
    var viewMode: Int = VIEW_MODE_NORMAL,
    var useLocalCursor: Boolean = true,
    var gestureStyle: String = "auto",
    var screenOrientation: String = "auto",
    var useCount: Int = 0
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readByte() != 0.toByte(),
        parcel.readFloat(),
        parcel.readFloat(),
        parcel.readInt(),
        parcel.readByte() != 0.toByte(),
        parcel.readString() ?: "auto",
        parcel.readString() ?: "auto",
        parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(ID)
        parcel.writeString(name)
        parcel.writeString(host)
        parcel.writeInt(port)
        parcel.writeString(username)
        parcel.writeString(password)
        parcel.writeInt(securityType)
        parcel.writeInt(channelType)
        parcel.writeInt(colorLevel)
        parcel.writeInt(imageQuality)
        parcel.writeByte(if (useRawEncoding) 1 else 0)
        parcel.writeFloat(zoom1)
        parcel.writeFloat(zoom2)
        parcel.writeInt(viewMode)
        parcel.writeByte(if (useLocalCursor) 1 else 0)
        parcel.writeString(gestureStyle)
        parcel.writeString(screenOrientation)
        parcel.writeInt(useCount)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<VncProfile> {
        const val CHANNEL_TCP = 1
        const val CHANNEL_SSH_TUNNEL = 24
        const val VIEW_MODE_NORMAL = 0
        const val VIEW_MODE_NO_INPUT = 1
        const val VIEW_MODE_NO_VIDEO = 2

        override fun createFromParcel(parcel: Parcel): VncProfile {
            return VncProfile(parcel)
        }

        override fun newArray(size: Int): Array<VncProfile?> {
            return arrayOfNulls(size)
        }
    }
}
