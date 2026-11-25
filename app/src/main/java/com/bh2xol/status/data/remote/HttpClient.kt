package com.bh2xol.status.data.remote

import com.bh2xol.status.config.AppConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * 轻量级 HTTP JSON 客户端封装。
 */
internal object HttpClient {
    fun request(
        method: String,
        urlStr: String,
        body: String? = null,
        bearerToken: String? = null,
    ): Pair<Int, JSONObject> {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = AppConfig.CONNECT_TIMEOUT_MS
                readTimeout = AppConfig.READ_TIMEOUT_MS
                setRequestProperty("Accept", "application/json")
                if (bearerToken != null) {
                    setRequestProperty("Authorization", "Bearer $bearerToken")
                }
                if (body != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: "{}"
            val json = runCatching { JSONObject(text) }.getOrElse { JSONObject() }
            code to json
        } catch (e: Exception) {
            -1 to JSONObject().put("error", e.message ?: "网络异常")
        } finally {
            conn?.disconnect()
        }
    }
}
