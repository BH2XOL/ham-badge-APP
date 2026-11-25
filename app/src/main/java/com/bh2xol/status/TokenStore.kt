package com.bh2xol.status

import android.content.Context

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
}
