@file:Suppress("ktlint:standard:no-wildcard-imports")

package org.unifiedpush.android.connector

import android.util.Base64
import com.google.crypto.tink.subtle.EllipticCurves
import java.security.*
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECPublicKeySpec
import java.security.spec.PKCS8EncodedKeySpec

/**
 * Contains Web Push (public) keys information necessary for the application server
 * to encrypt notification for this instance, following [RFC8291](https://www.rfc-editor.org/rfc/rfc8291)
 */
class PublicKeySet(
    val pubKey: String,
    val auth: String,
)

/**
 * Contains Web Push keys information necessary to decrypt the messages
 */
internal class WebPushKeys(
    val keyPair: KeyPair,
    val auth: ByteArray,
) {
    val publicKeySet: PublicKeySet
        get() = PublicKeySet(
            (keyPair.public as ECPublicKey).serialize(),
            auth.b64encode(),
        )
}

internal class SerializedKeyPair(
    val private: String,
    val public: String,
) {
    fun deserialize(): KeyPair {
        val kf = KeyFactory.getInstance("EC")

        val privateBytes = this.private.b64decode()
        val privateSpec = PKCS8EncodedKeySpec(privateBytes)
        val privateKey = kf.generatePrivate(privateSpec)

        val publicKey = this.public.deserializePubKey()
        return KeyPair(publicKey, privateKey)
    }
}

internal fun KeyPair.serialize(): SerializedKeyPair {
    return SerializedKeyPair(
        private = this.private.encoded.b64encode(),
        public = (this.public as ECPublicKey).serialize(),
    )
}

internal fun ECPublicKey.serialize(): String {
    return EllipticCurves.pointEncode(
        EllipticCurves.CurveType.NIST_P256,
        EllipticCurves.PointFormatType.UNCOMPRESSED,
        this.w
    ).b64encode()
}

internal fun String.deserializePubKey(): ECPublicKey {
    val point = EllipticCurves.pointDecode(
        EllipticCurves.CurveType.NIST_P256,
        EllipticCurves.PointFormatType.UNCOMPRESSED, this.b64decode()
    )
    val spec = EllipticCurves.getCurveSpec(EllipticCurves.CurveType.NIST_P256)
    return KeyFactory.getInstance("EC").generatePublic(ECPublicKeySpec(point, spec)) as ECPublicKey
}

internal fun ByteArray.b64encode(): String {
    return Base64.encodeToString(
        this,
        Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
    )
}

internal fun String.b64decode(): ByteArray {
    return Base64.decode(
        this,
        Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
    )
}

internal object WebPush {
    fun generateKeyPair(): KeyPair {
        return KeyPairGenerator.getInstance("EC").apply {
            initialize(
                ECGenParameterSpec("secp256r1"),
            )
        }.generateKeyPair()
    }

    fun generateAuthSecret(): ByteArray {
        return ByteArray(16).apply {
            SecureRandom().nextBytes(this)
        }
    }
}
