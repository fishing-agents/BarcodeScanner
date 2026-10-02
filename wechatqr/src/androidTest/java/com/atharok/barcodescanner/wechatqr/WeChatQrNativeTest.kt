package com.atharok.barcodescanner.wechatqr

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer

@RunWith(AndroidJUnit4::class)
class WeChatQrNativeTest {

    @Test fun nativeLibraryLoadsAndRunsWithoutModels() {
        val native = WeChatQrNativeJni()
        assertTrue(native.nativeInit(modelDir = null))

        // 32x32 all-white frame (WeChatQRCode rejects images <= 20px): no QR present, must not crash.
        val buffer = ByteBuffer.allocateDirect(32 * 32).apply { repeat(32 * 32) { put(0xFF.toByte()) }; rewind() }
        val result = native.nativeDetectAndDecode(buffer, width = 32, height = 32, rowStride = 32)

        assertTrue(result.texts.isEmpty())
    }
}
