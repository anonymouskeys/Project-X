package com.v2ray.ang.service

import android.app.Service

interface ServiceControl {
    fun getService(): Service

    fun startService()

    fun stopService()

    /** True only while an established Android VPN interface is accepting traffic. */
    fun isVpnActive(): Boolean

    fun vpnProtect(socket: Int): Boolean
}
