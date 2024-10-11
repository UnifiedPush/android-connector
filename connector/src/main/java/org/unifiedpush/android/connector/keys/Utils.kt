package org.unifiedpush.android.connector.keys

import android.util.Base64
import com.google.crypto.tink.subtle.EllipticCurves
import java.security.KeyFactory
import java.security.interfaces.ECPublicKey
import java.security.spec.ECPublicKeySpec

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