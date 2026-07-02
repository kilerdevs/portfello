package com.portfello.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import com.portfello.ui.theme.GainGreen
import com.portfello.ui.theme.LossRed

/** Signed profit/loss with optional percentage, colored by sign. Renders nothing when [profitLoss] is null. */
@Composable
fun ProfitLossText(
    profitLoss: Double?,
    profitLossPct: Double?,
    currency: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodySmall
) {
    if (profitLoss == null) return
    val sign = if (profitLoss >= 0) "+" else ""
    val pctPart = profitLossPct?.let { " (%s%.1f%%)".format(sign, it) } ?: ""
    Text(
        text = "%s%,.2f %s%s".format(sign, profitLoss, currency, pctPart),
        color = if (profitLoss >= 0) GainGreen else LossRed,
        style = style,
        modifier = modifier
    )
}
