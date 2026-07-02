package com.portfello.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.Zoom
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.shader.ShaderProvider
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val yAxisFormat = CartesianValueFormatter { _, value, _ ->
    when {
        value >= 1_000_000 -> "%.1fM".format(value / 1_000_000)
        value >= 1_000 -> "%.0fk".format(value / 1_000)
        else -> DecimalFormat("#.##").format(value)
    }
}

/** Day-resolution line chart of (timestampMs, value) points, styled to the theme palette. */
@Composable
fun TimeLineChart(points: List<Pair<Long, Double>>, modifier: Modifier = Modifier) {
    val snapshot = remember(points) { points.toList() }
    val modelProducer = remember { CartesianChartModelProducer() }

    val dayMs = 24L * 3600 * 1000
    val baseDay = remember(snapshot) {
        if (snapshot.isNotEmpty()) snapshot.first().first / dayMs else 0L
    }
    val spanDays = remember(snapshot) {
        if (snapshot.size < 2) 1L
        else (snapshot.last().first - snapshot.first().first) / dayMs
    }

    LaunchedEffect(snapshot) {
        if (snapshot.size >= 2) {
            modelProducer.runTransaction {
                lineSeries {
                    series(
                        x = snapshot.map { ((it.first / dayMs) - baseDay).toDouble() },
                        y = snapshot.map { it.second },
                    )
                }
            }
        }
    }

    val xAxisFormat = remember(baseDay, spanDays) {
        val fmt = SimpleDateFormat(if (spanDays > 90) "MM.yy" else "dd.MM", Locale.getDefault())
        val today = System.currentTimeMillis() / dayMs
        CartesianValueFormatter { _, value, _ ->
            val day = baseDay + Math.round(value)
            if (day >= today) "Dziś" else fmt.format(Date(day * dayMs))
        }
    }

    // ponytail: getXStep controls label/tick density — one label per spanDays/5 days
    val xStep: (CartesianChartModel) -> Double = remember(spanDays) {
        { _ -> (spanDays / 5.0).coerceAtLeast(1.0) }
    }

    val lineColor = MaterialTheme.colorScheme.primary
    val line = LineCartesianLayer.rememberLine(
        fill = LineCartesianLayer.LineFill.single(fill(lineColor)),
        areaFill = remember(lineColor) {
            LineCartesianLayer.AreaFill.single(
                fill(
                    ShaderProvider.verticalGradient(
                        lineColor.copy(alpha = 0.30f).toArgb(),
                        Color.Transparent.toArgb()
                    )
                )
            )
        }
    )

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(LineCartesianLayer.LineProvider.series(line)),
            startAxis = VerticalAxis.rememberStart(valueFormatter = yAxisFormat),
            bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = xAxisFormat),
            getXStep = xStep,
        ),
        modelProducer = modelProducer,
        scrollState = rememberVicoScrollState(scrollEnabled = false),
        zoomState = rememberVicoZoomState(zoomEnabled = false, initialZoom = Zoom.Content),
        modifier = modifier
    )
}
