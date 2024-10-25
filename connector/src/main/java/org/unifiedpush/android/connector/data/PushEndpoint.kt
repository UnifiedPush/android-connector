package org.unifiedpush.android.connector.data

import android.os.Parcel
import android.os.Parcelable

/**
 * Contains the push endpoint and the associated [PublicKeySet].
 */
class PushEndpoint(
    /** URL to push notifications to. */
    val url: String,
    /** Web Push public key set. */
    val pubKeySet: PublicKeySet?,
): Parcelable {
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(url)
        pubKeySet?.writeToParcel(parcel, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<PushEndpoint> {
        override fun createFromParcel(parcel: Parcel): PushEndpoint? {
            return PushEndpoint(
                parcel.readString() ?: return null,
                PublicKeySet.createFromParcel(parcel)
            )
        }

        override fun newArray(size: Int): Array<PushEndpoint?> {
            return arrayOfNulls(size)
        }
    }
}
