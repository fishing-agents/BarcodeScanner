package com.atharok.barcodescanner.domain.library.payment

import androidx.annotation.StringRes
import com.atharok.barcodescanner.R

enum class PaymentCode(@StringRes val appNameRes: Int, @StringRes val openActionRes: Int) {
    WECHAT_PAY(R.string.payment_app_wechat, R.string.action_open_in_wechat),
    WECHAT_LINK(R.string.payment_app_wechat, R.string.action_open_in_wechat),
    ALIPAY(R.string.payment_app_alipay, R.string.action_open_in_alipay)
}
