package com.sxybyte.mybattery

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import java.lang.reflect.Method
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val BatteryCycleCountExtra = "android.os.extra.CYCLE_COUNT"
private const val LanguagePrefs = "ui_prefs"
private const val LanguageKey = "app_language"

enum class AppLanguage(val tag: String) {
    ZH("zh"),
    EN("en");

    companion object {
        fun fromTag(tag: String?): AppLanguage {
            return entries.firstOrNull { it.tag == tag } ?: defaultFromSystem()
        }

        fun defaultFromSystem(): AppLanguage {
            return if (Locale.getDefault().language.startsWith("zh")) ZH else EN
        }
    }
}

data class BatterySnapshot(
    val level: Int? = null,
    val cycleCount: Int? = null,
    val healthCode: Int = BatteryManager.BATTERY_HEALTH_UNKNOWN,
    val temperatureCelsius: Float? = null,
    val voltageVolts: Float? = null,
    val batteryStatusCode: Int = BatteryManager.BATTERY_STATUS_UNKNOWN,
    val powerSourceCode: Int = 0,
    val designCapacityMah: Int? = null,
    val capacityPercent: Int? = null,
    val chargeCounterMah: Int? = null,
    val currentNowMa: Int? = null,
    val currentAverageMa: Int? = null,
    val energyCounterWh: Float? = null,
    val technology: String? = null,
    val present: Boolean = true,
    val chargerConnected: Boolean = false,
    val cycleCountSupported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
    val updatedAt: String = "",
)

@Composable
fun rememberAppLanguageState(): Pair<AppLanguage, (AppLanguage) -> Unit> {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(LanguagePrefs, Context.MODE_PRIVATE) }
    var language by remember {
        mutableStateOf(AppLanguage.fromTag(prefs.getString(LanguageKey, null)))
    }

    return language to { newLanguage ->
        language = newLanguage
        prefs.edit().putString(LanguageKey, newLanguage.tag).apply()
    }
}

@Composable
fun rememberBatterySnapshotState(): Pair<BatterySnapshot, () -> Unit> {
    val context = LocalContext.current
    var refreshToken by remember { mutableIntStateOf(0) }
    var snapshot by remember(refreshToken) { mutableStateOf(readBatterySnapshot(context)) }

    DisposableEffect(context, refreshToken) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                snapshot = readBatterySnapshot(context ?: return)
            }
        }

        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(receiver, filter)
        snapshot = readBatterySnapshot(context)

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    LaunchedEffect(context, refreshToken) {
        while (true) {
            delay(1000)
            snapshot = readBatterySnapshot(context)
        }
    }

    return snapshot to { refreshToken++ }
}

fun readBatterySnapshot(context: Context): BatterySnapshot {
    val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        ?: return BatterySnapshot(updatedAt = currentTimeString())

    val batteryManager = context.getSystemService(BatteryManager::class.java)
        ?: return BatterySnapshot(updatedAt = currentTimeString())

    val level = readLevelPercent(batteryIntent)
    val chargeCounterUaH = readIntPropertyOrNull(batteryManager, BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
    val currentNowUa = readIntPropertyOrNull(batteryManager, BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
    val currentAverageUa = readIntPropertyOrNull(batteryManager, BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)
    val capacityPercent = readIntPropertyOrNull(batteryManager, BatteryManager.BATTERY_PROPERTY_CAPACITY)
    val energyCounterNWh = readLongPropertyOrNull(batteryManager, BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER)

    return BatterySnapshot(
        level = level,
        cycleCount = readCycleCount(batteryIntent),
        healthCode = batteryIntent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN),
        temperatureCelsius = batteryIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
            .takeIf { it >= 0 }
            ?.div(10f),
        voltageVolts = batteryIntent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
            .takeIf { it >= 0 }
            ?.div(1000f),
        batteryStatusCode = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN),
        powerSourceCode = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0),
        designCapacityMah = readDesignCapacityMah(context),
        capacityPercent = capacityPercent ?: level,
        chargeCounterMah = chargeCounterUaH?.div(1000),
        currentNowMa = currentNowUa?.div(1000),
        currentAverageMa = currentAverageUa?.div(1000),
        energyCounterWh = energyCounterNWh?.div(1_000_000_000f),
        technology = batteryIntent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY),
        present = batteryIntent.getBooleanExtra(BatteryManager.EXTRA_PRESENT, true),
        chargerConnected = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0,
        updatedAt = currentTimeString(),
    )
}

private fun readLevelPercent(intent: Intent): Int? {
    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    return if (level >= 0 && scale > 0) ((level * 100f) / scale).toInt() else null
}

private fun readCycleCount(intent: Intent): Int? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return null
    return intent.getIntExtra(BatteryCycleCountExtra, -1).takeIf { it >= 0 }
}

private fun readIntPropertyOrNull(manager: BatteryManager, id: Int): Int? {
    val value = runCatching { manager.getIntProperty(id) }.getOrNull() ?: Int.MIN_VALUE
    return value.takeUnless { it == Int.MIN_VALUE || it == 0 }
}

private fun readLongPropertyOrNull(manager: BatteryManager, id: Int): Long? {
    val value = runCatching { manager.getLongProperty(id) }.getOrNull() ?: Long.MIN_VALUE
    return value.takeUnless { it == Long.MIN_VALUE || it == 0L }
}

private fun readDesignCapacityMah(context: Context): Int? {
    return runCatching {
        val clazz = Class.forName("com.android.internal.os.PowerProfile")
        val constructor = clazz.getConstructor(Context::class.java)
        val instance = constructor.newInstance(context)
        val method: Method = clazz.getMethod("getBatteryCapacity")
        val value = method.invoke(instance) as Double
        value.toInt()
    }.getOrNull()
}

private fun currentTimeString(): String {
    val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return formatter.format(Date())
}
