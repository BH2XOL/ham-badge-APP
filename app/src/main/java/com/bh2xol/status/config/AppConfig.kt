package com.bh2xol.status.config

/**
 * 全局应用配置与常量。
 *
 * 默认服务地址对应公开部署实例，运行期间可通过设置页修改并持久化。
 */
object AppConfig {
    const val DEFAULT_AUTH_BASE_URL = "https://auth.bh2xol.com"
    const val DEFAULT_STATUS_BASE_URL = "https://status.bh2xol.com"

    const val MAX_FREQ_LEN = 16
    const val MAX_MODE_LEN = 12
    const val MAX_NOTE_LEN = 32

    const val CONNECT_TIMEOUT_MS = 8000
    const val READ_TIMEOUT_MS = 8000

    val QUICK_FREQS = listOf(
        "7.050", "14.074", "14.270", "21.074", "28.074", "145.000", "438.500"
    )
    val QUICK_MODES = listOf(
        "FT8", "FT4", "SSB", "CW", "FM", "AM", "RTTY", "SSTV"
    )
    val QUICK_NOTES = listOf(
        "CQWW", "POTA", "SOTA", "WAPC", "DXpedition"
    )

    @Volatile
    var authBaseUrl: String = DEFAULT_AUTH_BASE_URL
        private set

    @Volatile
    var statusBaseUrl: String = DEFAULT_STATUS_BASE_URL
        private set

    fun updateEndpoints(authUrl: String, statusUrl: String) {
        authBaseUrl = normalizeUrl(authUrl, DEFAULT_AUTH_BASE_URL)
        statusBaseUrl = normalizeUrl(statusUrl, DEFAULT_STATUS_BASE_URL)
    }

    fun normalizeUrl(raw: String, fallback: String): String {
        val trimmed = raw.trim().trimEnd('/')
        if (trimmed.startsWith("https://") || trimmed.startsWith("http://")) {
            return trimmed
        }
        return fallback
    }
}
