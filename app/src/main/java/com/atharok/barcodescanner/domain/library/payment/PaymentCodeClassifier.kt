package com.atharok.barcodescanner.domain.library.payment

import java.net.URLEncoder

/**
 * Recognizes WeChat Pay / WeChat link / Alipay QR codes among ordinary URIs, so the
 * existing URI content/actions screens can special-case them without a new BarcodeType.
 */
object PaymentCodeClassifier {

    private val WECHAT_PAY_SCHEME = Regex("^wxp://", RegexOption.IGNORE_CASE)
    private val WECHAT_LINK_SCHEME = Regex("^weixin://", RegexOption.IGNORE_CASE)
    private val WECHAT_LINK_HOST = Regex("^https?://(u\\.wechat\\.com|weixin\\.qq\\.com)(/|$|\\?)", RegexOption.IGNORE_CASE)
    private val ALIPAY_SCHEME = Regex("^alipays?://", RegexOption.IGNORE_CASE)
    private val ALIPAY_HOST = Regex("^https?://qr\\.alipay\\.com(/|$|\\?)", RegexOption.IGNORE_CASE)

    fun classify(uri: String): PaymentCode? = when {
        WECHAT_PAY_SCHEME.containsMatchIn(uri) -> PaymentCode.WECHAT_PAY
        WECHAT_LINK_SCHEME.containsMatchIn(uri) || WECHAT_LINK_HOST.containsMatchIn(uri) -> PaymentCode.WECHAT_LINK
        ALIPAY_SCHEME.containsMatchIn(uri) || ALIPAY_HOST.containsMatchIn(uri) -> PaymentCode.ALIPAY
        else -> null
    }

    /**
     * URI to hand to ACTION_VIEW. `wxp://`/`weixin://` deep-link directly. Alipay's
     * `https://qr.alipay.com/...` web link isn't guaranteed to open the Alipay app, so it is
     * wrapped into Alipay's scan-intent scheme; `alipays://` codes pass through unchanged.
     */
    fun handoffUri(uri: String, code: PaymentCode): String =
        if (code == PaymentCode.ALIPAY && ALIPAY_HOST.containsMatchIn(uri)) {
            "alipays://platformapi/startapp?saId=10000007&qrcode=" + URLEncoder.encode(uri, "UTF-8")
        } else {
            uri
        }
}
