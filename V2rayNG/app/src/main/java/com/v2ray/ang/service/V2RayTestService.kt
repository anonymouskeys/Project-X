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
        // Anti-DPI tests deliberately stay conservative: every test creates a
        // temporary Xray core and shares one stateful ciadpi auto-strategy.
        private const val MAX_PARALLEL_DPI_TESTS = 2

        // Without Anti-DPI there is no shared ciadpi bottleneck. Restore a fast
        // queue, but cap it to avoid the old unbounded one-core-per-CPU storm on
        // high-core devices.
        private val MAX_PARALLEL_DIRECT_TESTS = Runtime.getRuntime()
            .availableProcessors()
            .coerceIn(2, 8)

        private const val DPI_IDLE_STOP_DELAY_MS = 1500L
    }

    private val directExecutor = Executors.newFixedThreadPool(MAX_PARALLEL_DIRECT_TESTS)
    private val directDispatcher = directExecutor.asCoroutineDispatcher()
    private val directTestScope = CoroutineScope(SupervisorJob() + directDispatcher)

    private val dpiExecutor = Executors.newFixedThreadPool(MAX_PARALLEL_DPI_TESTS)
    private val dpiDispatcher = dpiExecutor.asCoroutineDispatcher()
    private val dpiTestScope = CoroutineScope(SupervisorJob() + dpiDispatcher)

    // Cleanup must not occupy one of the two DPI test slots while sleeping.
    private val maintenanceExecutor = Executors.newSingleThreadExecutor()
    private val maintenanceDispatcher = maintenanceExecutor.asCoroutineDispatcher()
    private val maintenanceScope = CoroutineScope(SupervisorJob() + maintenanceDispatcher)

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

                // Snapshot the mode when the request enters the queue. A later
                // settings toggle must not move an already queued test between
                // the fast and DPI paths halfway through configuration creation.
                val dpiEnabled = MmkvManager.decodeSettingsBool(
                    AppConfig.PREF_DPI_ENABLED,
                    false
                )
                val scope = if (dpiEnabled) dpiTestScope else directTestScope

                cleanupGeneration.incrementAndGet()
                pendingTests.incrementAndGet()

                scope.launch {
                    try {
                        val result = startRealPing(guid, dpiEnabled)
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
                directTestScope.coroutineContext[Job]?.cancelChildren()
                dpiTestScope.coroutineContext[Job]?.cancelChildren()
                if (pendingTests.get() == 0) scheduleDpiCleanup()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        cleanupGeneration.incrementAndGet()

        directTestScope.cancel()
        dpiTestScope.cancel()
        maintenanceScope.cancel()

        directDispatcher.close()
        dpiDispatcher.close()
        maintenanceDispatcher.close()

        directExecutor.shutdownNow()
        dpiExecutor.shutdownNow()
        maintenanceExecutor.shutdownNow()

        stopTestOwnedDpiIfSafe()
        super.onDestroy()
    }

    private fun startRealPing(guid: String, dpiEnabled: Boolean): Long {
        val configItem = MmkvManager.decodeServerConfig(guid) ?: return -1L
        if (configItem.configType == EConfigType.HYSTERIA2) {
            return PluginUtil.realPingHy2(this, configItem)
        }

        if (dpiEnabled) {
            val wasRunning = ByeDpiManager.isRunning()
            if (!ByeDpiManager.start(this)) {
                Log.w(AppConfig.ANG_PACKAGE, "Delay test: ByeDPI failed to start for $guid")
                return -1L
            }
            if (!wasRunning) dpiStartedByTestService = true
        }

        // Generate only after ciadpi is listening, because V2rayConfigManager
        // adds the local SOCKS outbound based on ByeDpiManager.isRunning().
        val generated = V2rayConfigManager.getV2rayConfig(this, guid)
        if (!generated.status) return -1L

        return SpeedtestUtil.realPing(
            generated.content,
            attempts = if (dpiEnabled) 2 else 1,
            retryDelayMs = if (dpiEnabled) 450L else 0L
        )
    }

    private fun scheduleDpiCleanup() {
        val generation = cleanupGeneration.incrementAndGet()
        maintenanceScope.launch {
            delay(DPI_IDLE_STOP_DELAY_MS)
            if (cleanupGeneration.get() == generation && pendingTests.get() == 0) {
                stopTestOwnedDpiIfSafe()
            }
        }
    }

    private fun stopTestOwnedDpiIfSafe() {
        if (!dpiStartedByTestService) return

        // If VPN became active during testing, it now owns the shared process.
        if (V2RayServiceManager.v2rayPoint.isRunning) {
            dpiStartedByTestService = false
            return
        }

        ByeDpiManager.stop()
        dpiStartedByTestService = false
    }
}
