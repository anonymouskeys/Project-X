package com.v2ray.ang.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.v2ray.ang.AppConfig
import com.v2ray.ang.AppConfig.MSG_MEASURE_CONFIG
import com.v2ray.ang.AppConfig.MSG_MEASURE_CONFIG_CANCEL
import com.v2ray.ang.AppConfig.MSG_MEASURE_CONFIG_SUCCESS
import com.v2ray.ang.dpi.ByeDpiManager
import com.v2ray.ang.dto.EConfigType
import com.v2ray.ang.extension.serializable
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.V2rayConfigManager
import com.v2ray.ang.util.MessageUtil
import com.v2ray.ang.util.PluginUtil
import com.v2ray.ang.util.SpeedtestUtil
import com.v2ray.ang.util.Utils
import go.Seq
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import libv2ray.Libv2ray
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class V2RayTestService : Service() {
    companion object {
        // measureOutboundDelay creates a complete temporary Xray core. Running one
        // core per CPU overloads TLS/DNS and makes healthy profiles randomly fail.
        private const val MAX_PARALLEL_REAL_TESTS = 2
        private const val DPI_IDLE_STOP_DELAY_MS = 1500L
    }

    private val testExecutor = Executors.newFixedThreadPool(MAX_PARALLEL_REAL_TESTS)
    private val testDispatcher = testExecutor.asCoroutineDispatcher()
    private val realTestScope = CoroutineScope(SupervisorJob() + testDispatcher)
    private val pendingTests = AtomicInteger(0)
    private val cleanupGeneration = AtomicLong(0)

    @Volatile
    private var dpiStartedByTestService = false

    override fun onCreate() {
        super.onCreate()
        Seq.setContext(this)
        Libv2ray.initV2Env(Utils.userAssetPath(this), Utils.getDeviceIdForXUDPBaseKey())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.getIntExtra("key", 0)) {
            MSG_MEASURE_CONFIG -> {
                val guid = intent.serializable<String>("content") ?: ""
                cleanupGeneration.incrementAndGet()
                pendingTests.incrementAndGet()

                realTestScope.launch {
                    try {
                        val result = startRealPing(guid)
                        MessageUtil.sendMsg2UI(
                            this@V2RayTestService,
                            MSG_MEASURE_CONFIG_SUCCESS,
                            Pair(guid, result)
                        )
                    } finally {
                        if (pendingTests.decrementAndGet() == 0) {
                            scheduleDpiCleanup()
                        }
                    }
                }
            }

            MSG_MEASURE_CONFIG_CANCEL -> {
                cleanupGeneration.incrementAndGet()
                realTestScope.coroutineContext[Job]?.cancelChildren()
                if (pendingTests.get() == 0) scheduleDpiCleanup()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        cleanupGeneration.incrementAndGet()
        realTestScope.cancel()
        testDispatcher.close()
        testExecutor.shutdownNow()
        stopTestOwnedDpiIfSafe()
        super.onDestroy()
    }

    private fun startRealPing(guid: String): Long {
        val configItem = MmkvManager.decodeServerConfig(guid) ?: return -1L
        if (configItem.configType == EConfigType.HYSTERIA2) {
            return PluginUtil.realPingHy2(this, configItem)
        }

        val dpiEnabled = MmkvManager.decodeSettingsBool(AppConfig.PREF_DPI_ENABLED, false)
        if (dpiEnabled) {
            val wasRunning = ByeDpiManager.isRunning()
            if (!ByeDpiManager.start(this)) {
                Log.w(AppConfig.ANG_PACKAGE, "Delay test: ByeDPI failed to start for $guid")
                return -1L
            }
            if (!wasRunning) dpiStartedByTestService = true
        }

        // Build the Xray config only after ByeDPI is ready. V2rayConfigManager
        // adds the local SOCKS outbound only while ByeDpiManager.isRunning().
        val generated = V2rayConfigManager.getV2rayConfig(this, guid)
        if (!generated.status) return -1L

        // Auto presets learn/cache a working group per destination. A second,
        // delayed attempt gives ciadpi time to move to the next group and avoids
        // marking a profile dead because of one transient TLS/EOF result.
        return SpeedtestUtil.realPing(
            generated.content,
            attempts = if (dpiEnabled) 2 else 1,
            retryDelayMs = if (dpiEnabled) 450L else 0L
        )
    }

    private fun scheduleDpiCleanup() {
        val generation = cleanupGeneration.incrementAndGet()
        realTestScope.launch {
            delay(DPI_IDLE_STOP_DELAY_MS)
            if (cleanupGeneration.get() == generation && pendingTests.get() == 0) {
                stopTestOwnedDpiIfSafe()
            }
        }
    }

    private fun stopTestOwnedDpiIfSafe() {
        if (!dpiStartedByTestService) return

        // Never stop a process that the active VPN is using. If VPN started while
        // the test batch was running, ownership effectively passes to VPN.
        if (V2RayServiceManager.v2rayPoint.isRunning) {
            dpiStartedByTestService = false
            return
        }

        ByeDpiManager.stop()
        dpiStartedByTestService = false
    }
}
