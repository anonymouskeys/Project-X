package com.v2ray.ang.dpi

import com.v2ray.ang.AppConfig
import com.v2ray.ang.handler.MmkvManager

/**
 * Configuration scaffold for the future native ByeDPI process.
 *
 * Phase 1 is deliberately runtime-neutral: no process is started and no Xray
 * configuration is modified. This keeps the current VPN path unchanged.
 */
data class ByeDpiSettings(
    val enabled: Boolean,
    val strategy: String,
    val splitPosition: Int,
    val fakeTtl: Int,
    val fakeCount: Int,
    val delayMs: Int,
    val ports80And443Only: Boolean,
    val expertArgs: String,
) {
    companion object {
        fun load(): ByeDpiSettings = ByeDpiSettings(
            enabled = MmkvManager.decodeSettingsBool(AppConfig.PREF_DPI_ENABLED, false),
            strategy = MmkvManager.decodeSettingsString(AppConfig.PREF_DPI_STRATEGY, "split") ?: "split",
            splitPosition = MmkvManager.decodeSettingsString(AppConfig.PREF_DPI_SPLIT_POSITION, "1")?.toIntOrNull() ?: 1,
            fakeTtl = MmkvManager.decodeSettingsString(AppConfig.PREF_DPI_FAKE_TTL, "3")?.toIntOrNull()?.coerceIn(1, 255) ?: 3,
            fakeCount = MmkvManager.decodeSettingsString(AppConfig.PREF_DPI_FAKE_COUNT, "1")?.toIntOrNull()?.coerceIn(1, 16) ?: 1,
            delayMs = MmkvManager.decodeSettingsString(AppConfig.PREF_DPI_DELAY_MS, "0")?.toIntOrNull()?.coerceIn(0, 10_000) ?: 0,
            ports80And443Only = MmkvManager.decodeSettingsBool(AppConfig.PREF_DPI_PORTS_80_443_ONLY, true),
            expertArgs = MmkvManager.decodeSettingsString(AppConfig.PREF_DPI_EXPERT_ARGS, "") ?: "",
        )
    }
}

object ByeDpiManager {
    enum class State { NOT_INSTALLED, STOPPED, STARTING, RUNNING, FAILED }

    @Volatile
    var state: State = State.NOT_INSTALLED
        private set

    fun isAvailable(): Boolean = false

    fun stop() {
        state = State.STOPPED
    }
}
