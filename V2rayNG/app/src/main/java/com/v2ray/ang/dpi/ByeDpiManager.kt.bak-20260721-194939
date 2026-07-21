package com.v2ray.ang.dpi

import android.content.Context
import android.util.Log
import com.v2ray.ang.AppConfig
import com.v2ray.ang.handler.MmkvManager
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit

/** Runtime wrapper around the official ciadpi local SOCKS proxy. */
data class ByeDpiSettings(
    val enabled: Boolean,
    val strategy: String,
    val splitPosition: String,
    val fakeTtl: Int,
    val fakeCount: Int,
    val delayMs: Int,
    val ports80And443Only: Boolean,
    val expertArgs: String,
) {
    companion object {
        fun load(): ByeDpiSettings = ByeDpiSettings(
            enabled = MmkvManager.decodeSettingsBool(AppConfig.PREF_DPI_ENABLED, false),
            strategy = MmkvManager.decodeSettingsString(AppConfig.PREF_DPI_STRATEGY, "auto") ?: "auto",
            splitPosition = MmkvManager.decodeSettingsString(AppConfig.PREF_DPI_SPLIT_POSITION, "1+s") ?: "1+s",
            fakeTtl = MmkvManager.decodeSettingsString(AppConfig.PREF_DPI_FAKE_TTL, "8")?.toIntOrNull()?.coerceIn(1, 255) ?: 8,
            fakeCount = MmkvManager.decodeSettingsString(AppConfig.PREF_DPI_FAKE_COUNT, "1")?.toIntOrNull()?.coerceIn(1, 8) ?: 1,
            delayMs = MmkvManager.decodeSettingsString(AppConfig.PREF_DPI_DELAY_MS, "0")?.toIntOrNull()?.coerceIn(0, 5000) ?: 0,
            ports80And443Only = MmkvManager.decodeSettingsBool(AppConfig.PREF_DPI_PORTS_80_443_ONLY, true),
            expertArgs = MmkvManager.decodeSettingsString(AppConfig.PREF_DPI_EXPERT_ARGS, "") ?: "",
        )
    }
}

object ByeDpiManager {
    enum class State { STOPPED, STARTING, RUNNING, FAILED }

    @Volatile var state: State = State.STOPPED
        private set
    @Volatile private var process: Process? = null

    fun isRunning(): Boolean = state == State.RUNNING && process?.isAlive == true

    @Synchronized
    fun start(context: Context): Boolean {
        val settings = ByeDpiSettings.load()
        if (!settings.enabled) {
            stop()
            return false
        }
        if (isRunning()) return true
        stop()

        val binary = File(context.applicationInfo.nativeLibraryDir, "libciadpi.so")
        if (!binary.isFile) {
            state = State.FAILED
            Log.e(AppConfig.ANG_PACKAGE, "ByeDPI binary missing: ${binary.absolutePath}")
            return false
        }

        val command = mutableListOf(
            binary.absolutePath,
            "--ip", AppConfig.LOOPBACK,
            "--port", AppConfig.PORT_BYEDPI.toString(),
            "--max-conn", "1024",
            "--timeout", "3",
            "--cache-ttl", "86400",
            "--auto-mode", "3",
            "--proto", "http,tls",
        )
        if (settings.ports80And443Only) command += listOf("--pf", "80-443")
        command += presetArguments(settings)
        command += splitCommandLine(settings.expertArgs)

        return try {
            state = State.STARTING
            Log.i(AppConfig.ANG_PACKAGE, "ByeDPI command: ${command.joinToString(" ")}")
            val created = ProcessBuilder(command)
                .directory(context.filesDir)
                .redirectErrorStream(true)
                .start()
            process = created
            Thread({
                created.inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { Log.i("ByeDPI", it) }
                }
            }, "ByeDPI-log").apply { isDaemon = true }.start()
            Thread({
                val code = created.waitFor()
                Log.w(AppConfig.ANG_PACKAGE, "ByeDPI exited with code $code")
                if (process === created) {
                    process = null
                    state = if (code == 0) State.STOPPED else State.FAILED
                }
            }, "ByeDPI-wait").apply { isDaemon = true }.start()

            val ready = waitForPort(2500)
            state = if (ready) State.RUNNING else State.FAILED
            if (!ready) stop()
            ready
        } catch (e: Exception) {
            Log.e(AppConfig.ANG_PACKAGE, "ByeDPI start failed", e)
            state = State.FAILED
            stop()
            false
        }
    }

    @Synchronized
    fun stop() {
        val current = process
        process = null
        if (current != null) {
            current.destroy()
            try {
                if (!current.waitFor(500, TimeUnit.MILLISECONDS)) current.destroyForcibly()
            } catch (_: Exception) {
                current.destroyForcibly()
            }
        }
        state = State.STOPPED
    }

    private fun waitForPort(timeoutMs: Long): Boolean {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            val p = process
            if (p == null || !p.isAlive) return false
            try {
                Socket().use { it.connect(InetSocketAddress(AppConfig.LOOPBACK, AppConfig.PORT_BYEDPI), 150) }
                return true
            } catch (_: Exception) {
                Thread.sleep(75)
            }
        }
        return false
    }

    private fun presetArguments(s: ByeDpiSettings): List<String> {
        val pos = s.splitPosition.ifBlank { "1+s" }
        val repeatedFake = mutableListOf<String>()
        repeat(s.fakeCount) { repeatedFake += listOf("--fake", "-1", "--ttl", s.fakeTtl.toString()) }
        return when (s.strategy) {
            "split" -> listOf("--split", pos)
            "disorder" -> listOf("--disorder", pos)
            "fake" -> repeatedFake
            "fake_split" -> listOf("--split", "1+s", "--disorder", "3+s") + repeatedFake
            "oob" -> listOf("--oob", pos, "--oob-data", "a")
            "tls_record_split" -> listOf("--tlsrec", pos)
            "auto" -> listOf(
                "--split", "1+s", "--disorder", "3+s",
                "--auto=torst", "--fake", "-1", "--ttl", s.fakeTtl.toString(), "--fake-tls-mod", "rand",
                "--auto=torst", "--tlsrec", "3+s",
                "--auto=torst", "--oob", "1+s",
                "--auto=torst", "--disorder", "1",
            )
            else -> listOf("--split", pos)
        }
    }

    /** Minimal shell-like tokenizer supporting quotes and backslash escapes. */
    private fun splitCommandLine(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        val out = mutableListOf<String>()
        val token = StringBuilder()
        var quote: Char? = null
        var escaped = false
        for (c in raw) {
            when {
                escaped -> { token.append(c); escaped = false }
                c == '\\' -> escaped = true
                quote != null && c == quote -> quote = null
                quote == null && (c == '\'' || c == '"') -> quote = c
                quote == null && c.isWhitespace() -> if (token.isNotEmpty()) { out += token.toString(); token.clear() }
                else -> token.append(c)
            }
        }
        if (escaped) token.append('\\')
        if (token.isNotEmpty()) out += token.toString()
        return out
    }
}
