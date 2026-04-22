package com.sxybyte.mybattery

import android.content.Intent
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sxybyte.mybattery.ui.theme.MyBatteryTheme
import kotlinx.coroutines.delay
import kotlin.math.abs

private data class UiStrings(
    val dashboardTitle: String,
    val refreshLabel: String,
    val liveOverview: String,
    val batteryInfo: String,
    val completeInfo: String,
    val cycleLimited: String,
    val cycleNotPublic: String,
    val batteryChip: String,
    val noPackChip: String,
    val cycleCount: String,
    val currentLevel: String,
    val health: String,
    val temperature: String,
    val voltage: String,
    val status: String,
    val chargingSource: String,
    val designCapacity: String,
    val currentNow: String,
    val currentAverage: String,
    val chargeCounter: String,
    val technology: String,
    val powerLevel: String,
    val powerEvent: String,
    val chargerConnected: String,
    val chargerDisconnected: String,
    val languageToggle: String,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyBatteryTheme {
                BatteryDashboardApp()
            }
        }
    }
}

@Composable
private fun BatteryDashboardApp() {
    val context = LocalContext.current
    val (snapshot, refresh) = rememberBatterySnapshotState()
    val (language, setLanguage) = rememberAppLanguageState()
    val strings = remember(language) { stringsFor(language) }
    var chargerToast by remember { mutableStateOf<String?>(null) }
    var lastChargerConnected by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(snapshot.chargerConnected, language) {
        val previous = lastChargerConnected
        if (previous != null && previous != snapshot.chargerConnected) {
            chargerToast = if (snapshot.chargerConnected) {
                strings.chargerConnected
            } else {
                strings.chargerDisconnected
            }
        }
        lastChargerConnected = snapshot.chargerConnected
    }

    LaunchedEffect(chargerToast) {
        if (chargerToast != null) {
            delay(3000)
            chargerToast = null
        }
    }

    Scaffold(containerColor = Color.Transparent) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            Color(0xFF07111F),
                            Color(0xFF03060B),
                        )
                    )
                )
                .padding(innerPadding)
        ) {
            TechBackground()
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                item {
                    DashboardHeader(
                        strings = strings,
                        updatedAt = snapshot.updatedAt,
                        onRefresh = refresh,
                        language = language,
                        onToggleLanguage = {
                            setLanguage(if (language == AppLanguage.ZH) AppLanguage.EN else AppLanguage.ZH)
                        },
                    )
                }
                item { HeroPanel(snapshot, strings) }
                item { DetailPanel(snapshot, strings) }
                item {
                    FooterLink(
                        text = "Powered by J99",
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/mrjeye/Cycle-Pulse"))
                            )
                        }
                    )
                }
            }

            if (chargerToast != null) {
                ChargerToast(
                    title = strings.powerEvent,
                    message = chargerToast!!,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = 120.dp),
                )
            }
        }
    }
}

@Composable
private fun DashboardHeader(
    strings: UiStrings,
    updatedAt: String,
    onRefresh: () -> Unit,
    language: AppLanguage,
    onToggleLanguage: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = "BATTERY SURFACE",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 4.sp,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = strings.dashboardTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip(text = "${strings.refreshLabel}  $updatedAt", onClick = onRefresh)
            StatusChip(
                text = if (language == AppLanguage.ZH) "中文 / EN" else "EN / 中文",
                onClick = onToggleLanguage,
            )
        }
    }
}

@Composable
private fun HeroPanel(snapshot: BatterySnapshot, strings: UiStrings) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        border = panelBorder(),
        shadowElevation = 12.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column {
                    Text(
                        text = strings.liveOverview,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = overviewText(snapshot, strings),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StatusChip(text = if (snapshot.present) strings.batteryChip else strings.noPackChip)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BatteryGauge(
                    level = snapshot.level,
                    powerLevelLabel = strings.powerLevel,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(20.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    MetricDisplay(
                        label = strings.cycleCount,
                        value = snapshot.cycleCount?.toString() ?: "--",
                        suffix = "CYCLES",
                        accent = MaterialTheme.colorScheme.primary,
                    )
                    MetricDisplay(
                        label = strings.currentLevel,
                        value = snapshot.level?.toString() ?: "--",
                        suffix = "%",
                        accent = MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                SummaryStat(strings.health, healthText(snapshot.healthCode, language = stringsToLanguage(strings)))
                SummaryStat(strings.temperature, formatTemperature(snapshot.temperatureCelsius))
                SummaryStat(strings.voltage, formatVoltage(snapshot.voltageVolts))
                SummaryStat(strings.status, chargeStatusText(snapshot.batteryStatusCode, stringsToLanguage(strings)))
            }
        }
    }
}

@Composable
private fun MetricDisplay(label: String, value: String, suffix: String, accent: Color) {
    Column {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
        Text(text = suffix, style = MaterialTheme.typography.labelLarge, color = accent, letterSpacing = 3.sp)
    }
}

@Composable
private fun BatteryGauge(level: Int?, powerLevelLabel: String, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "battery-gauge")
    val sweepOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(5000, easing = LinearEasing), RepeatMode.Restart),
        label = "gauge-sweep",
    )
    val scanOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
        label = "gauge-scan",
    )
    val safeLevel = (level ?: 0).coerceIn(0, 100)
    val sweepAngle = safeLevel / 100f * 270f
    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
    val dashColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
    val gaugeColor = batteryLevelColor(safeLevel)
    val gaugeSoft = gaugeColor.copy(alpha = 0.25f)
    val gaugeAccent = batteryLevelAccentColor(safeLevel)

    Box(modifier = modifier.size(188.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            val radiusInset = strokeWidth / 2f + 10.dp.toPx()
            val arcSize = Size(size.width - radiusInset * 2, size.height - radiusInset * 2)
            val topLeft = Offset(radiusInset, radiusInset)

            drawArc(
                color = outlineColor,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            drawArc(
                brush = Brush.sweepGradient(listOf(gaugeSoft, gaugeColor, gaugeAccent, gaugeSoft), center = center),
                startAngle = 135f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(Color.Transparent, gaugeAccent.copy(alpha = 0.9f), Color.Transparent),
                    center = center
                ),
                startAngle = sweepOffset,
                sweepAngle = 30f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(gaugeColor.copy(alpha = 0.18f), Color.Transparent),
                    center = center,
                    radius = size.minDimension * 0.42f,
                ),
                radius = size.minDimension * 0.42f,
                center = center,
            )
            val scanY = size.height * (0.24f + scanOffset * 0.52f)
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        gaugeColor.copy(alpha = 0.1f),
                        gaugeColor.copy(alpha = 0.28f),
                        gaugeColor.copy(alpha = 0.1f),
                        Color.Transparent,
                    ),
                    startY = scanY - 18.dp.toPx(),
                    endY = scanY + 18.dp.toPx(),
                ),
                topLeft = Offset(size.width * 0.23f, size.height * 0.22f),
                size = Size(size.width * 0.54f, size.height * 0.56f),
                cornerRadius = CornerRadius(28.dp.toPx(), 28.dp.toPx()),
            )
            drawCircle(
                color = dashColor,
                radius = size.minDimension * 0.26f,
                center = center,
                style = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 8.dp.toPx())),
                ),
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = level?.toString() ?: "--",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "%",
                style = MaterialTheme.typography.titleMedium,
                color = gaugeColor,
                letterSpacing = 3.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = powerLevelLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 2.sp,
            )
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun DetailPanel(snapshot: BatterySnapshot, strings: UiStrings) {
    val language = stringsToLanguage(strings)
    val detailItems = listOf(
        strings.currentLevel to (snapshot.level?.let { "$it%" } ?: "--"),
        strings.cycleCount to (snapshot.cycleCount?.let { if (language == AppLanguage.ZH) "$it 次" else "$it" } ?: "--"),
        strings.health to healthText(snapshot.healthCode, language),
        strings.temperature to formatTemperature(snapshot.temperatureCelsius),
        strings.voltage to formatVoltage(snapshot.voltageVolts),
        strings.status to chargeStatusText(snapshot.batteryStatusCode, language),
        strings.chargingSource to powerSourceText(snapshot.powerSourceCode, language),
        strings.designCapacity to formatMah(snapshot.designCapacityMah),
        strings.currentNow to formatCurrent(snapshot.currentNowMa),
        strings.currentAverage to formatCurrent(snapshot.currentAverageMa),
        strings.chargeCounter to formatMah(snapshot.chargeCounterMah),
        strings.technology to (snapshot.technology ?: "--"),
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        border = panelBorder(),
        shadowElevation = 12.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                text = strings.batteryInfo,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )

            detailItems.chunked(2).forEach { rowItems ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowItems.forEach { (label, value) ->
                        DetailCell(label = label, value = value, modifier = Modifier.weight(1f))
                    }
                    if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DetailCell(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun ChargerToast(title: String, message: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        border = BorderStroke(
            1.dp,
            Brush.linearGradient(
                listOf(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f),
                )
            )
        ),
        shadowElevation = 16.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)
            )
            Column {
                Text(text = title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun StatusChip(text: String, onClick: (() -> Unit)? = null) {
    val modifier = Modifier
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), CircleShape)
        .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
        .padding(horizontal = 12.dp, vertical = 7.dp)

    Box(modifier = modifier) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.4.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun FooterLink(text: String, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            modifier = Modifier.clickable(onClick = onClick),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
            letterSpacing = 1.2.sp,
        )
    }
}

@Composable
private fun TechBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val gridColor = Color(0xFF3C536E).copy(alpha = 0.14f)
        val accent = Color(0xFF39D0FF).copy(alpha = 0.14f)
        val step = 56.dp.toPx()

        var x = 0f
        while (x < size.width) {
            drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
            x += step
        }

        var y = 0f
        while (y < size.height) {
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
            y += step
        }

        drawLine(
            brush = Brush.horizontalGradient(listOf(Color.Transparent, accent, Color.Transparent)),
            start = Offset(size.width * 0.06f, size.height * 0.16f),
            end = Offset(size.width * 0.94f, size.height * 0.16f),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round,
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF39D0FF).copy(alpha = 0.1f), Color.Transparent),
            ),
            radius = size.minDimension * 0.45f,
            center = Offset(size.width * 0.82f, size.height * 0.14f),
        )
    }
}

@Composable
private fun panelBorder(): BorderStroke {
    return BorderStroke(
        width = 1.dp,
        brush = Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.52f),
                MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
            )
        ),
    )
}

@Composable
private fun batteryLevelColor(level: Int): Color = when {
    level < 34 -> Color(0xFFFF5A5F)
    level < 67 -> Color(0xFFFFB547)
    else -> MaterialTheme.colorScheme.primary
}

@Composable
private fun batteryLevelAccentColor(level: Int): Color = when {
    level < 34 -> Color(0xFFFF7D66)
    level < 67 -> Color(0xFFFFD86B)
    else -> MaterialTheme.colorScheme.secondary
}

private fun overviewText(snapshot: BatterySnapshot, strings: UiStrings): String = when {
    snapshot.cycleCount != null -> strings.completeInfo
    !snapshot.cycleCountSupported -> strings.cycleLimited
    else -> strings.cycleNotPublic
}

private fun healthText(code: Int, language: AppLanguage): String = when (code) {
    BatteryManager.BATTERY_HEALTH_GOOD -> if (language == AppLanguage.ZH) "正常" else "Good"
    BatteryManager.BATTERY_HEALTH_OVERHEAT -> if (language == AppLanguage.ZH) "过热" else "Overheat"
    BatteryManager.BATTERY_HEALTH_DEAD -> if (language == AppLanguage.ZH) "损坏" else "Dead"
    BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> if (language == AppLanguage.ZH) "电压过高" else "Over voltage"
    BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> if (language == AppLanguage.ZH) "故障" else "Failure"
    BatteryManager.BATTERY_HEALTH_COLD -> if (language == AppLanguage.ZH) "低温" else "Cold"
    else -> if (language == AppLanguage.ZH) "未知" else "Unknown"
}

private fun chargeStatusText(code: Int, language: AppLanguage): String = when (code) {
    BatteryManager.BATTERY_STATUS_CHARGING -> if (language == AppLanguage.ZH) "充电中" else "Charging"
    BatteryManager.BATTERY_STATUS_DISCHARGING -> if (language == AppLanguage.ZH) "放电中" else "Discharging"
    BatteryManager.BATTERY_STATUS_FULL -> if (language == AppLanguage.ZH) "已充满" else "Full"
    BatteryManager.BATTERY_STATUS_NOT_CHARGING -> if (language == AppLanguage.ZH) "未充电" else "Not charging"
    else -> if (language == AppLanguage.ZH) "未知" else "Unknown"
}

private fun powerSourceText(code: Int, language: AppLanguage): String = when (code) {
    BatteryManager.BATTERY_PLUGGED_AC -> if (language == AppLanguage.ZH) "AC" else "AC"
    BatteryManager.BATTERY_PLUGGED_USB -> "USB"
    BatteryManager.BATTERY_PLUGGED_WIRELESS -> if (language == AppLanguage.ZH) "无线" else "Wireless"
    BatteryManager.BATTERY_PLUGGED_DOCK -> if (language == AppLanguage.ZH) "底座" else "Dock"
    0 -> if (language == AppLanguage.ZH) "未连接" else "Disconnected"
    else -> if (language == AppLanguage.ZH) "未知" else "Unknown"
}

private fun stringsFor(language: AppLanguage): UiStrings = when (language) {
    AppLanguage.ZH -> UiStrings(
        dashboardTitle = "电池仪表盘",
        refreshLabel = "刷新",
        liveOverview = "实时概览",
        batteryInfo = "电池信息",
        completeInfo = "系统已返回完整电池信息",
        cycleLimited = "已读取电池信息，循环次数受系统版本限制",
        cycleNotPublic = "已读取电池信息，循环次数未公开",
        batteryChip = "BATTERY",
        noPackChip = "NO PACK",
        cycleCount = "循环次数",
        currentLevel = "当前电量",
        health = "健康",
        temperature = "温度",
        voltage = "电压",
        status = "状态",
        chargingSource = "供电方式",
        designCapacity = "设计容量",
        currentNow = "电流大小",
        currentAverage = "平均电流",
        chargeCounter = "电量计数器",
        technology = "技术类型",
        powerLevel = "POWER LEVEL",
        powerEvent = "POWER EVENT",
        chargerConnected = "已连接充电器",
        chargerDisconnected = "已断开充电器",
        languageToggle = "中文 / EN",
    )
    AppLanguage.EN -> UiStrings(
        dashboardTitle = "Battery Dashboard",
        refreshLabel = "Refresh",
        liveOverview = "Live Overview",
        batteryInfo = "Battery Info",
        completeInfo = "The system returned complete battery information.",
        cycleLimited = "Battery data is available, but cycle count is limited by system version.",
        cycleNotPublic = "Battery data is available, but cycle count is not exposed by the ROM.",
        batteryChip = "BATTERY",
        noPackChip = "NO PACK",
        cycleCount = "Cycle Count",
        currentLevel = "Battery Level",
        health = "Health",
        temperature = "Temp",
        voltage = "Voltage",
        status = "Status",
        chargingSource = "Source",
        designCapacity = "Design Capacity",
        currentNow = "Current",
        currentAverage = "Avg Current",
        chargeCounter = "Charge Counter",
        technology = "Technology",
        powerLevel = "POWER LEVEL",
        powerEvent = "POWER EVENT",
        chargerConnected = "Charger connected",
        chargerDisconnected = "Charger disconnected",
        languageToggle = "EN / 中文",
    )
}

private fun stringsToLanguage(strings: UiStrings): AppLanguage {
    return if (strings.dashboardTitle == "电池仪表盘") AppLanguage.ZH else AppLanguage.EN
}

private fun formatTemperature(value: Float?): String = value?.let { String.format("%.1f°C", it) } ?: "--"
private fun formatVoltage(value: Float?): String = value?.let { String.format("%.2fV", it) } ?: "--"
private fun formatMah(value: Int?): String = value?.let { "$it mAh" } ?: "--"
private fun formatCurrent(value: Int?): String = value?.let { "${if (it > 0) "+" else ""}$it mA" } ?: "--"
private fun formatEnergy(value: Float?): String = value?.let { String.format("%.2f Wh", abs(it)) } ?: "--"

@Preview(showBackground = true, backgroundColor = 0xFF03060B, showSystemUi = true)
@Composable
private fun BatteryDashboardPreview() {
    MyBatteryTheme {
        val strings = stringsFor(AppLanguage.ZH)
        val snapshot = BatterySnapshot(
            level = 33,
            cycleCount = 1282,
            healthCode = BatteryManager.BATTERY_HEALTH_GOOD,
            temperatureCelsius = 33.5f,
            voltageVolts = 3.83f,
            batteryStatusCode = BatteryManager.BATTERY_STATUS_CHARGING,
            powerSourceCode = BatteryManager.BATTERY_PLUGGED_USB,
            designCapacityMah = 5000,
            chargeCounterMah = 1353,
            technology = "Li-poly",
            updatedAt = "14:54:52",
        )

        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            TechBackground()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                item {
                    DashboardHeader(
                        strings = strings,
                        updatedAt = snapshot.updatedAt,
                        onRefresh = {},
                        language = AppLanguage.ZH,
                        onToggleLanguage = {},
                    )
                }
                item { HeroPanel(snapshot, strings) }
                item { DetailPanel(snapshot, strings) }
                item { FooterLink(text = "Powered by J99", onClick = {}) }
            }
        }
    }
}
