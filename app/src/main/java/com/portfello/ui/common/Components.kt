package com.portfello.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
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

/** Compact ▲/▼ percent-change pill. Renders nothing when [pct] is null. */
@Composable
fun ChangeBadge(pct: Double?, label: String? = null, modifier: Modifier = Modifier) {
    if (pct == null) return
    val up = pct >= 0
    val color = if (up) GainGreen else LossRed
    val prefix = label?.let { "$it " } ?: ""
    Text(
        text = "%s%s%.1f%%".format(prefix, if (up) "▲" else "▼", kotlin.math.abs(pct)),
        color = color,
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}
