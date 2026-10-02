package com.atharok.barcodescanner.domain.library.scan

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.atharok.barcodescanner.domain.library.payment.PaymentCode
import com.atharok.barcodescanner.domain.library.payment.PaymentCodeClassifier
import com.atharok.barcodescanner.wechatqr.WeChatQrModelInstaller
import com.atharok.barcodescanner.wechatqr.WeChatQrNativeJni
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Real native decoders against fixture images (generated + calibrated off-device with the same OpenCV 4.14.0 and models). */
@RunWith(AndroidJUnit4::class)
class TieredScanEngineInstrumentedTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    /** Fixtures live in src/androidTest/assets, i.e. the test APK — read via the instrumentation context. */
    private fun fixture(name: String) = instrumentation.context.assets.open("scan_fixtures/$name").use(BitmapFactory::decodeStream)

    private fun texts(engine: ScanEngine, name: String) = engine.scan(fixture(name)).codes.map { it.text }

    @Test fun zxingCppDecodesTheCommonFormats() {
        val engine = ZxingCppEngine()
        assertEquals(listOf("https://example.com/"), texts(engine, "normal_qr.png"))
        assertEquals(listOf("5901234123457"), texts(engine, "ean13.png"))
        assertEquals(listOf("DataMatrix fixture"), texts(engine, "data_matrix.png"))
        assertEquals(listOf("inverted fixture"), texts(engine, "inverted_qr.png"))
        assertEquals(setOf("first fixture", "second fixture"), texts(engine, "two_qr.png").toSet())
    }

    @Test fun paymentFixturesClassifyEndToEnd() {
        val engine = ZxingCppEngine()
        assertEquals(PaymentCode.WECHAT_PAY, PaymentCodeClassifier.classify(texts(engine, "wechat_pay_qr.png").single()))
        assertEquals(PaymentCode.ALIPAY, PaymentCodeClassifier.classify(texts(engine, "alipay_qr.png").single()))
    }

    @Test fun cnnFallbackRecoversABlurredQrZxingCppCannotRead() {
        // Real models, installed the way production does — modelDir = null would test the no-CNN path instead.
        val modelDir = WeChatQrModelInstaller.ensureInstalled(instrumentation.targetContext)
        assertNotNull("WeChatQRCode models must be bundled in the app's assets", modelDir)

        // Guard: if zxing-cpp alone reads it, this test would prove nothing about the fallback.
        assertTrue("fixture is too easy", texts(ZxingCppEngine(), "small_blurred_qr.png").isEmpty())

        val tiered = TieredScanEngine(primary = ZxingCppEngine(), fallback = WeChatQrEngine(WeChatQrNativeJni(), modelDir))
        assertEquals(listOf("https://example.com/blurred-fixture"), texts(tiered, "small_blurred_qr.png"))
    }
}
