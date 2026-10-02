package com.atharok.barcodescanner.domain.library.payment

import com.google.zxing.BarcodeFormat
import com.google.zxing.Result
import com.google.zxing.client.result.ParsedResultType
import com.google.zxing.client.result.ResultParser
import com.google.zxing.client.result.URIParsedResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PaymentCodeClassifierTest {

    @Test fun `wechat pay scheme classified`() {
        assertEquals(PaymentCode.WECHAT_PAY, PaymentCodeClassifier.classify("wxp://f2f0account/abc123"))
        assertEquals(PaymentCode.WECHAT_PAY, PaymentCodeClassifier.classify("WXP://F2F0ACCOUNT/ABC123"))
    }

    @Test fun `wechat link scheme and host classified`() {
        assertEquals(PaymentCode.WECHAT_LINK, PaymentCodeClassifier.classify("weixin://dl/business/?t=abc"))
        assertEquals(PaymentCode.WECHAT_LINK, PaymentCodeClassifier.classify("https://u.wechat.com/abcdef"))
        assertEquals(PaymentCode.WECHAT_LINK, PaymentCodeClassifier.classify("https://weixin.qq.com/g/xyz"))
    }

    @Test fun `alipay scheme and host classified`() {
        assertEquals(PaymentCode.ALIPAY, PaymentCodeClassifier.classify("https://qr.alipay.com/abc123"))
        assertEquals(PaymentCode.ALIPAY, PaymentCodeClassifier.classify("alipays://platformapi/startapp?saId=10000007"))
        assertEquals(PaymentCode.ALIPAY, PaymentCodeClassifier.classify("alipay://platformapi/startapp?saId=10000007"))
    }

    @Test fun `lookalike hosts are rejected`() {
        assertNull(PaymentCodeClassifier.classify("https://qr.alipay.com.evil.example/steal"))
        assertNull(PaymentCodeClassifier.classify("https://example.com/?u=wxp://x"))
        assertNull(PaymentCodeClassifier.classify("https://notu.wechat.com/abc"))
    }

    @Test fun `ordinary urls and text are not payment codes`() {
        assertNull(PaymentCodeClassifier.classify("https://example.com/"))
        assertNull(PaymentCodeClassifier.classify("Hello world"))
        assertNull(PaymentCodeClassifier.classify("upi://pay?pa=foo@bank"))
    }

    @Test fun `alipay https link is wrapped into the alipays scan intent`() {
        val handoff = PaymentCodeClassifier.handoffUri("https://qr.alipay.com/abc123?x=1", PaymentCode.ALIPAY)
        assertEquals(
            "alipays://platformapi/startapp?saId=10000007&qrcode=https%3A%2F%2Fqr.alipay.com%2Fabc123%3Fx%3D1",
            handoff
        )
    }

    @Test fun `alipay code already using the alipays scheme passes through unchanged`() {
        val uri = "alipays://platformapi/startapp?saId=10000007"
        assertEquals(uri, PaymentCodeClassifier.handoffUri(uri, PaymentCode.ALIPAY))
    }

    @Test fun `wechat codes pass through unchanged, they already deep-link directly`() {
        assertEquals("wxp://abc", PaymentCodeClassifier.handoffUri("wxp://abc", PaymentCode.WECHAT_PAY))
        assertEquals("weixin://abc", PaymentCodeClassifier.handoffUri("weixin://abc", PaymentCode.WECHAT_LINK))
    }

    @Test fun `ZXing parses payment-app codes as URIs, so they reach the URI content and action screens`() {
        for (raw in listOf(
            "wxp://f2f0eGmDYTyUj2nyz6vJMZeUOTW08oe39TEF",
            "weixin://dl/business/?t=abc",
            "https://qr.alipay.com/fkx12345abcde",
            "alipays://platformapi/startapp?saId=10000007"
        )) {
            val parsed = ResultParser.parseResult(Result(raw, null, null, BarcodeFormat.QR_CODE))
            assertEquals(raw, ParsedResultType.URI, parsed.type)
            assertEquals(raw, PaymentCodeClassifier.classify((parsed as URIParsedResult).uri), PaymentCodeClassifier.classify(raw))
        }
    }
}
