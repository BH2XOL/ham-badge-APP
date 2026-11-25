package com.bh2xol.status

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class StationStatus(
    val status: String = "close",
    val since: String = "",
    val mode: String = "",
    val freq: String = "",
) {
    val isOnAir: Boolean get() = status == "onair"
}

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data object Unauthorized : ApiResult<Nothing>()
    data class Error(val message: String) : ApiResult<Nothing>()
}

object StatusApi {
    private const val AUTH_BASE = "https://auth.bh2xol.com"
    private const val STATUS_BASE = "https://status.bh2xol.com"

    suspend fun requestOtp(email: String, password: String): ApiResult<Boolean> = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("email", email.trim())
            .put("password", password)
            .toString()
        val (code, json) = request("POST", "$AUTH_BASE/login", body)
        when {
            code in 200..299 && json.optBoolean("ok") -> ApiResult.Success(json.optBoolean("reuse", false))
            else -> ApiResult.Error(json.optString("error").ifBlank { "请求失败 ($code)" })
        }
    }

    suspend fun verifyOtp(otp: String): ApiResult<String> = withContext(Dispatchers.IO) {
        val body = JSONObject().put("code", otp.trim()).toString()
        val (code, json) = request("POST", "$AUTH_BASE/verify", body)
        val token = json.optString("token")
        when {
            code in 200..299 && json.optBoolean("ok") && token.isNotBlank() -> ApiResult.Success(token)
            else -> ApiResult.Error(json.optString("error").ifBlank { "验证失败 ($code)" })
        }
    }

    suspend fun checkToken(token: String): Boolean = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(token, "UTF-8")
        val (code, json) = request("GET", "$AUTH_BASE/verify-token?token=$encoded")
        code in 200..299 && json.optBoolean("ok")
    }

    suspend fun hasPasskey(): Boolean = withContext(Dispatchers.IO) {
        val (code, json) = request("GET", "$AUTH_BASE/webauthn/status")
        code in 200..299 && json.optBoolean("hasPasskey", false)
    }

    suspend fun getPasskeyLoginOptions(): ApiResult<Pair<String, String>> = withContext(Dispatchers.IO) {
        val (code, json) = request("POST", "$AUTH_BASE/webauthn/login/options", "{}")
        val challengeId = json.optString("challengeId")
        if (code in 200..299 && challengeId.isNotBlank()) {
            val clean = JSONObject(json.toString()).apply { remove("challengeId") }.toString()
            ApiResult.Success(challengeId to clean)
        } else {
            ApiResult.Error(json.optString("error").ifBlank { "获取 Passkey 挑战失败 ($code)" })
        }
    }

    suspend fun verifyPasskeyLogin(challengeId: String, responseJson: String): ApiResult<String> = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("challengeId", challengeId)
            .put("response", JSONObject(responseJson))
            .toString()
        val (code, json) = request("POST", "$AUTH_BASE/webauthn/login/verify", body)
        val token = json.optString("token")
        if (code in 200..299 && json.optBoolean("ok") && token.isNotBlank()) {
            ApiResult.Success(token)
        } else {
            ApiResult.Error(json.optString("error").ifBlank { "Passkey 验证失败 ($code)" })
        }
    }

    suspend fun getPasskeyRegisterOptions(token: String): ApiResult<Pair<String, String>> = withContext(Dispatchers.IO) {
        val (code, json) = request("POST", "$AUTH_BASE/webauthn/register/options", "{}", bearerToken = token)
        val challengeId = json.optString("challengeId")
        when {
            code == 401 -> ApiResult.Unauthorized
            code in 200..299 && challengeId.isNotBlank() -> {
                val clean = JSONObject(json.toString()).apply { remove("challengeId") }.toString()
                ApiResult.Success(challengeId to clean)
            }
            else -> ApiResult.Error(json.optString("error").ifBlank { "获取注册参数失败 ($code)" })
        }
    }

    suspend fun verifyPasskeyRegister(
        token: String,
        challengeId: String,
        responseJson: String,
        deviceName: String,
    ): ApiResult<Unit> = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("challengeId", challengeId)
            .put("name", deviceName)
            .put("response", JSONObject(responseJson))
            .toString()
        val (code, json) = request("POST", "$AUTH_BASE/webauthn/register/verify", body, bearerToken = token)
        when {
            code == 401 -> ApiResult.Unauthorized
            code in 200..299 && json.optBoolean("ok") -> ApiResult.Success(Unit)
            else -> ApiResult.Error(json.optString("error").ifBlank { "绑定失败 ($code)" })
        }
    }

    suspend fun getStatus(): ApiResult<StationStatus> = withContext(Dispatchers.IO) {
        val (code, json) = request("GET", "$STATUS_BASE/api/status")
        if (code in 200..299) {
            ApiResult.Success(
                StationStatus(
                    status = json.optString("status", "close"),
                    since = json.optString("since", ""),
                    mode = json.optString("mode", ""),
                    freq = json.optString("freq", ""),
                )
            )
        } else {
            ApiResult.Error(json.optString("error").ifBlank { "获取状态失败 ($code)" })
        }
    }

    suspend fun setStatus(
        token: String,
        status: String? = null,
        freq: String? = null,
        mode: String? = null,
    ): ApiResult<StationStatus> = withContext(Dispatchers.IO) {
        val obj = JSONObject()
        if (status != null) obj.put("status", status)
        if (freq != null) obj.put("freq", freq.trim())
        if (mode != null) obj.put("mode", mode.trim())

        val (code, json) = request(
            method = "POST",
            urlStr = "$STATUS_BASE/api/status",
            body = obj.toString(),
            bearerToken = token,
        )
        when {
            code == 401 -> ApiResult.Unauthorized
            code in 200..299 && json.optBoolean("ok") -> ApiResult.Success(
                StationStatus(
                    status = json.optString("status", "close"),
                    since = json.optString("since", ""),
                    mode = json.optString("mode", ""),
                    freq = json.optString("freq", ""),
                )
            )
            else -> ApiResult.Error(json.optString("error").ifBlank { "操作失败 ($code)" })
        }
    }

    private fun request(
        method: String,
        urlStr: String,
        body: String? = null,
        bearerToken: String? = null,
    ): Pair<Int, JSONObject> {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 8000
                readTimeout = 8000
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
