package org.unifiedpush.android.connector.data

import android.os.Parcel
import android.os.Parcelable

/**
 * Contains the push message. It has been correctly decrypted if [decrypted] is `true`.
 */
class PushMessage(
    /** Content of the push message. */
    val content: ByteArray,
    /** Whether it has been correctly decrypted. */
    val decrypted: Boolean,
) : Parcelable {
    override fun writeToParcel(
        parcel: Parcel,
        flags: Int,
    ) {
        parcel.writeInt(content.size)
        parcel.writeByteArray(content)
        parcel.writeInt(
            if (decrypted) {
                1
            } else {
                0
            },
        )
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<PushMessage> {
        override fun createFromParcel(parcel: Parcel): PushMessage? {
            val size = parcel.readInt()
            val content = ByteArray(size)
            parcel.readByteArray(content)
            val decrypted = parcel.readInt() == 1
            if (content.isEmpty()) {
                return null
            }
            return PushMessage(
                content,
                decrypted,
            )
        }

        override fun newArray(size: Int): Array<PushMessage?> {
            return arrayOfNulls(size)
        }
    }
}
