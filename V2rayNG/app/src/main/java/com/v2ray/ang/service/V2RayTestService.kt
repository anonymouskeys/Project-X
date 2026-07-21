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
        // Restore the old fast queue for both modes, with a safety cap for
        // high-core devices. Lifecycle ownership, not parallelism, was
        // terminating active DPI handshakes.
        private val MAX_PARALLEL_TESTS = Runtime.getRuntime()
            .availableProcessors()
            .coerceIn(2, 8)

        private const val DPI_IDLE_STOP_DELAY_MS = 1500L
    }

    private val testExecutor = Executors.newFixedThreadPool(MAX_PARALLEL_TESTS)
    private val testDispatcher = testExecutor.asCoroutineDispatcher()
    private val testScope = CoroutineScope(SupervisorJob() + testDispatcher)

    // Cleanup must not occupy a test worker while waiting for another request.
    private val maintenanceExecutor = Executors.newSingleThreadExecutor()
    private val maintenanceDispatcher = maintenanceExecutor.asCoroutineDispatcher()
    private val maintenanceScope = CoroutineScope(SupervisorJob() + maintenanceDispatcher)

    private val pendingDpiTests = AtomicInteger(0)
    private val cleanupGeneration = AtomicLong(0)

    override fun onCreate() {
        super.onCreate()
        Seq.setContext(this)
        Libv2ray.initV2Env(Utils.userAssetPath(this), Utils.getDeviceIdForXUDPBaseKey())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.getIntExtra("key", 0)) {
            MSG_MEASURE_CONFIG -> {
                val guid = intent.serializable<String>("content") ?: ""

                // Snapshot the setting when the request enters the queue.
                val dpiEnabled = MmkvManager.decodeSettingsBool(
                    AppConfig.PREF_DPI_ENABLED,
                    false
                )

                if (dpiEnabled) {
                    cleanupGeneration.incrementAndGet()
                    pendingDpiTests.incrementAndGet()
                }

                testScope.launch {
                    try {
                        val result = startRealPing(guid, dpiEnabled)
                        MessageUtil.sendMsg2UI(
                            this@V2RayTestService,
                            MSG_MEASURE_CONFIG_SUCCESS,
                            Pair(guid, result)
                        )
                    } finally {
                        if (dpiEnabled && pendingDpiTests.decrementAndGet() == 0) {
                            scheduleDpiCleanup()
                        }
                    }
                }
            }

            MSG_MEASURE_CONFIG_CANCEL -> {
                cleanupGeneration.incrementAndGet()
                testScope.coroutineContext[Job]?.cancelChildren()
                if (pendingDpiTests.get() == 0) scheduleDpiCleanup()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        cleanupGeneration.incrementAndGet()

        testScope.cancel()
        maintenanceScope.cancel()

        testDispatcher.close()
        maintenanceDispatcher.close()

        testExecutor.shutdownNow()
        maintenanceExecutor.shutdownNow()

        ByeDpiManager.release(ByeDpiManager.Owner.TEST_SERVICE)
        super.onDestroy()
    }

    private fun startRealPing(guid: String, dpiEnabled: Boolean): Long {
        val configItem = MmkvManager.decodeServerConfig(guid) ?: return -1L
        if (configItem.configType == EConfigType.HYSTERIA2) {
            return PluginUtil.realPingHy2(this, configItem)
        }

        if (dpiEnabled && !ByeDpiManager.acquire(this, ByeDpiManager.Owner.TEST_SERVICE)) {
            Log.w(AppConfig.ANG_PACKAGE, "Delay test: ByeDPI failed to start for $guid")
            return -1L
        }

        // Generate only after TEST_SERVICE owns a live ciadpi process, because
        // V2rayConfigManager inserts its SOCKS outbound only while it is running.
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
            if (cleanupGeneration.get() == generation && pendingDpiTests.get() == 0) {
                ByeDpiManager.release(ByeDpiManager.Owner.TEST_SERVICE)
            }
        }
    }
}
