package com.example.whatif.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.whatif.util.formatCompact
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.Zoom
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarkerVisibilityListener
import com.patrykandpatrick.vico.core.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisGuidelineComponent

data class ChartSeries(
    val label: String,
    val values: List<Double>,
    val color: Color,
    val filled: Boolean = false,
    val dashed: Boolean = false,
)

@Composable
fun ProjectionChart(
    series: List<ChartSeries>,
    selectedIndex: Int?,
    onIndexSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 220.dp,
) {
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(series) {
        modelProducer.runTransaction {
            lineSeries {
                series.forEach { line -> series(line.values) }
            }
        }
    }

    val lines = series.map { line ->
        LineCartesianLayer.rememberLine(
            fill = LineCartesianLayer.LineFill.single(fill(line.color)),
            areaFill = if (line.filled) {
                LineCartesianLayer.AreaFill.single(fill(line.color.copy(alpha = 0.06f)))
            } else null
        )
    }

    val markerListener = remember(onIndexSelected) {
        object : CartesianMarkerVisibilityListener {
            override fun onShown(marker: CartesianMarker, targets: List<CartesianMarker.Target>) {
                targets.firstOrNull()?.let { onIndexSelected(it.x.toInt()) }
            }

            override fun onUpdated(marker: CartesianMarker, targets: List<CartesianMarker.Target>) {
                targets.firstOrNull()?.let { onIndexSelected(it.x.toInt()) }
            }

            override fun onHidden(marker: CartesianMarker) {}
        }
    }

    val marker = rememberDefaultCartesianMarker(
        label = rememberTextComponent(),
        valueFormatter = DefaultCartesianMarker.ValueFormatter { _, targets -> "" },
        guideline = rememberAxisGuidelineComponent(
            fill = fill(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)),
            thickness = 2.dp,
        ),
    )

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                lineProvider = LineCartesianLayer.LineProvider.series(lines),
            ),
            startAxis = VerticalAxis.rememberStart(
                line = null,
                tick = null,
                valueFormatter = CartesianValueFormatter { _, value, _ -> formatCompact(value) },
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                guideline = null,
                line = null,
                tick = null,
                valueFormatter = CartesianValueFormatter { _, value, _ -> "${value.toInt()}y" },
            ),
            marker = marker,
            markerVisibilityListener = markerListener,
            persistentMarkers = {
                selectedIndex?.let { marker at it }
            },
        ),
        modelProducer = modelProducer,
        scrollState = rememberVicoScrollState(scrollEnabled = false),
        zoomState = rememberVicoZoomState(zoomEnabled = false, initialZoom = remember { Zoom.Content }),
        modifier = modifier
            .fillMaxWidth()
            .height(height),
    )
}