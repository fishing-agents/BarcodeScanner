package com.atharok.barcodescanner.wechatqr

import java.nio.ByteBuffer

/** One native call's output: decoded texts with their quads (parallel arrays), plus every detector candidate quad. */
class WeChatQrNativeResult(
    val texts: Array<String>,
    val decodedQuads: Array<FloatArray>,
    val candidateQuads: Array<FloatArray>
)

/** Native-boundary contract. An interface so JVM unit tests can mock it — Mockito cannot stub `external` methods. */
interface WeChatQrNative {
    /** @param modelDir directory with detect/sr prototxt+caffemodel, or null to run without the CNN models. */
    fun nativeInit(modelDir: String?): Boolean

    fun nativeDetectAndDecode(yBuffer: ByteBuffer, width: Int, height: Int, rowStride: Int): WeChatQrNativeResult
}

/** JNI implementation over `libwechatqr_android` (OpenCV + opencv_contrib wechat_qrcode, built from source). */
class WeChatQrNativeJni : WeChatQrNative {

    init {
        System.loadLibrary("wechatqr_android")
    }

    override fun nativeInit(modelDir: String?): Boolean = jniInit(modelDir)

    // The native side holds one global WeChatQRCode / dnn::Net, which is not safe to run
    // concurrently; camera and gallery scans share this instance through the ScanEngine single.
    @Synchronized
    override fun nativeDetectAndDecode(yBuffer: ByteBuffer, width: Int, height: Int, rowStride: Int): WeChatQrNativeResult =
        jniDetectAndDecode(yBuffer, width, height, rowStride)

    private external fun jniInit(modelDir: String?): Boolean

    private external fun jniDetectAndDecode(yBuffer: ByteBuffer, width: Int, height: Int, rowStride: Int): WeChatQrNativeResult
}
