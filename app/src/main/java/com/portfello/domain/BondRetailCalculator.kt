package com.portfello.domain

import com.portfello.data.db.entity.BondRetailDetails

object BondRetailCalculator {

    fun estimatedValue(
        details: BondRetailDetails,
        purchaseDate: Long,
        nowMs: Long = System.currentTimeMillis()
    ): Double {
        val nominal = details.nominal
        val rate = details.interestRate / 100.0
        val yearsHeld = (nowMs - purchaseDate).toDouble() / (365.25 * 24 * 3600 * 1000)
        if (yearsHeld <= 0) return nominal

        return when (details.capitalizationType) {
            "ANNUAL" -> {
                val fullYears = yearsHeld.toInt()
                val fraction = yearsHeld - fullYears
                // ponytail: compound for full years, simple accrual for partial year
                val compounded = nominal * Math.pow(1.0 + rate, fullYears.toDouble())
                compounded * (1.0 + rate * fraction)
            }
            "END" -> {
                nominal * (1.0 + rate * yearsHeld)
            }
            else -> nominal * (1.0 + rate * yearsHeld)
        }
    }
}
