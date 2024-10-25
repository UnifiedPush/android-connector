package org.unifiedpush.android.connector.data

import android.os.Parcel
import android.os.Parcelable

/**
 * Contains Web Push (public) keys information necessary for the application server
 * to encrypt notification for this instance, following [RFC8291](https://www.rfc-editor.org/rfc/rfc8291)
 */
class PublicKeySet(
    /** P-256 Public key, in uncompressed format, base64url encoded without padding. */
    val pubKey: String,
    /** Auth secret, base64url encoded without padding. */
    val auth: String,
): Parcelable {
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(pubKey)
        parcel.writeString(auth)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<PublicKeySet> {
        override fun createFromParcel(parcel: Parcel): PublicKeySet? {
            return PublicKeySet(
                parcel.readString() ?: return null,
                parcel.readString() ?: return null
            )
        }

        override fun newArray(size: Int): Array<PublicKeySet?> {
            return arrayOfNulls(size)
        }
    }
}
