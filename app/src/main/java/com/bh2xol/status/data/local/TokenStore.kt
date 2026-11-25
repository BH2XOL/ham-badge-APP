package com.bh2xol.status.data.local

import android.content.Context
import com.bh2xol.status.config.AppConfig

/**
 * 基于 SharedPreferences 的轻量持久化存储。
 */
class TokenStore(context: Context) {
    private val prefs = context.getSharedPreferences("bh2xol_status_prefs", Context.MODE_PRIVATE)

    var token: String?
        get() = prefs.getString("token", null)
        set(value) = prefs.edit().apply {
            if (value.isNullOrBlank()) remove("token") else putString("token", value)
        }.apply()

    var email: String
        get() = prefs.getString("email", "") ?: ""
        set(value) = prefs.edit().putString("email", value).apply()

    var lastFreq: String
        get() = prefs.getString("last_freq", "14.074") ?: "14.074"
        set(value) = prefs.edit().putString("last_freq", value).apply()

    var lastMode: String
        get() = prefs.getString("last_mode", "FT8") ?: "FT8"
        set(value) = prefs.edit().putString("last_mode", value).apply()

    var lastNote: String
        get() = prefs.getString("last_note", "") ?: ""
        set(value) = prefs.edit().putString("last_note", value).apply()

    var authBaseUrl: String
        get() = prefs.getString("auth_base_url", AppConfig.DEFAULT_AUTH_BASE_URL)
            ?: AppConfig.DEFAULT_AUTH_BASE_URL
        set(value) {
            val normalized = AppConfig.normalizeUrl(value, AppConfig.DEFAULT_AUTH_BASE_URL)
            prefs.edit().putString("auth_base_url", normalized).apply()
        }

    var statusBaseUrl: String
        get() = prefs.getString("status_base_url", AppConfig.DEFAULT_STATUS_BASE_URL)
            ?: AppConfig.DEFAULT_STATUS_BASE_URL
        set(value) {
            val normalized = AppConfig.normalizeUrl(value, AppConfig.DEFAULT_STATUS_BASE_URL)
            prefs.edit().putString("status_base_url", normalized).apply()
        }

    fun syncEndpointsToConfig() {
        AppConfig.updateEndpoints(authBaseUrl, statusBaseUrl)
    }

    fun resetEndpoints() {
        authBaseUrl = AppConfig.DEFAULT_AUTH_BASE_URL
        statusBaseUrl = AppConfig.DEFAULT_STATUS_BASE_URL
        syncEndpointsToConfig()
    }
}
