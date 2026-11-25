package com.bh2xol.status.data.remote

import com.bh2xol.status.config.AppConfig
import com.bh2xol.status.data.model.ApiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URLEncoder

/**
 * 对接 BH2XOL/auth-service 的身份认证接口。
 */
object AuthApi {
    private val baseUrl: String get() = AppConfig.authBaseUrl

    suspend fun requestOtp(email: String, password: String): ApiResult<Boolean> = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("email", email.trim())
            .put("password", password)
            .toString()
        val (code, json) = HttpClient.request("POST", "$baseUrl/login", body)
        when {
            code in 200..299 && json.optBoolean("ok") -> ApiResult.Success(json.optBoolean("reuse", false))
            else -> ApiResult.Error(json.optString("error").ifBlank { "请求失败 ($code)" })
        }
    }

    suspend fun verifyOtp(otp: String): ApiResult<String> = withContext(Dispatchers.IO) {
        val body = JSONObject().put("code", otp.trim()).toString()
        val (code, json) = HttpClient.request("POST", "$baseUrl/verify", body)
        val token = json.optString("token")
        when {
            code in 200..299 && json.optBoolean("ok") && token.isNotBlank() -> ApiResult.Success(token)
            else -> ApiResult.Error(json.optString("error").ifBlank { "验证失败 ($code)" })
        }
    }

    suspend fun checkToken(token: String): Boolean = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(token, "UTF-8")
        val (code, json) = HttpClient.request("GET", "$baseUrl/verify-token?token=$encoded")
        code in 200..299 && json.optBoolean("ok")
    }

    suspend fun logout(token: String): ApiResult<Unit> = withContext(Dispatchers.IO) {
        val (code, json) = HttpClient.request("POST", "$baseUrl/logout", "{}", bearerToken = token)
        when {
            code == 401 -> ApiResult.Unauthorized
            code in 200..299 -> ApiResult.Success(Unit)
            else -> ApiResult.Error(json.optString("error").ifBlank { "注销失败 ($code)" })
        }
    }

    suspend fun hasPasskey(): Boolean = withContext(Dispatchers.IO) {
        val (code, json) = HttpClient.request("GET", "$baseUrl/webauthn/status")
        code in 200..299 && json.optBoolean("hasPasskey", false)
    }

    suspend fun getPasskeyLoginOptions(): ApiResult<Pair<String, String>> = withContext(Dispatchers.IO) {
        val (code, json) = HttpClient.request("POST", "$baseUrl/webauthn/login/options", "{}")
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
        val (code, json) = HttpClient.request("POST", "$baseUrl/webauthn/login/verify", body)
        val token = json.optString("token")
        if (code in 200..299 && json.optBoolean("ok") && token.isNotBlank()) {
            ApiResult.Success(token)
        } else {
            ApiResult.Error(json.optString("error").ifBlank { "Passkey 验证失败 ($code)" })
        }
    }

    suspend fun getPasskeyRegisterOptions(token: String): ApiResult<Pair<String, String>> = withContext(Dispatchers.IO) {
        val (code, json) = HttpClient.request("POST", "$baseUrl/webauthn/register/options", "{}", bearerToken = token)
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
        val (code, json) = HttpClient.request("POST", "$baseUrl/webauthn/register/verify", body, bearerToken = token)
        when {
            code == 401 -> ApiResult.Unauthorized
            code in 200..299 && json.optBoolean("ok") -> ApiResult.Success(Unit)
            else -> ApiResult.Error(json.optString("error").ifBlank { "绑定失败 ($code)" })
        }
    }
}
