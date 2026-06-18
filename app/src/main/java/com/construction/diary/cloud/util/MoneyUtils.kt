package com.construction.diary.cloud.util

import java.text.DecimalFormat

object MoneyUtils {
    private val fmt = DecimalFormat("#,##0.00")

    fun format(amount: Double): String = "¥${fmt.format(amount)}"

    fun formatInt(amount: Double): String {
        return if (amount == amount.toLong().toDouble()) {
            "¥${DecimalFormat("#,##0").format(amount.toLong())}"
        } else {
            "¥${fmt.format(amount)}"
        }
    }
}
