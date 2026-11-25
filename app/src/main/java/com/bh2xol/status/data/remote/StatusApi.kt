package com.bh2xol.status.data.remote

import com.bh2xol.status.config.AppConfig
import com.bh2xol.status.data.model.ApiResult
import com.bh2xol.status.data.model.StationStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * 对接 BH2XOL/ham-badge 的台站状态服务接口。
 */
object StatusApi {
    private val baseUrl: String get() = AppConfig.statusBaseUrl

    suspend fun getStatus(): ApiResult<StationStatus> = withContext(Dispatchers.IO) {
        val (code, json) = HttpClient.request("GET", "$baseUrl/api/status")
        if (code in 200..299) {
            ApiResult.Success(parseStatus(json))
        } else {
            ApiResult.Error(json.optString("error").ifBlank { "获取状态失败 ($code)" })
        }
    }

    suspend fun setStatus(
        token: String,
        status: String? = null,
        freq: String? = null,
        mode: String? = null,
        note: String? = null,
    ): ApiResult<StationStatus> = withContext(Dispatchers.IO) {
        val obj = JSONObject()
        if (status != null) obj.put("status", status)
        if (freq != null) obj.put("freq", freq.trim().take(AppConfig.MAX_FREQ_LEN))
        if (mode != null) obj.put("mode", mode.trim().take(AppConfig.MAX_MODE_LEN))
        if (note != null) obj.put("note", note.trim().take(AppConfig.MAX_NOTE_LEN))

        val (code, json) = HttpClient.request(
            method = "POST",
            urlStr = "$baseUrl/api/status",
            body = obj.toString(),
            bearerToken = token,
        )
        when {
            code == 401 -> ApiResult.Unauthorized
            code in 200..299 && json.optBoolean("ok") -> ApiResult.Success(parseStatus(json))
            else -> ApiResult.Error(json.optString("error").ifBlank { "操作失败 ($code)" })
        }
    }

    private fun parseStatus(json: JSONObject): StationStatus = StationStatus(
        status = json.optString("status", "close"),
        since = json.optString("since", ""),
        mode = json.optString("mode", ""),
        freq = json.optString("freq", ""),
        note = json.optString("note", ""),
    )
}
