package com.bh2xol.status.auth

import android.content.Context
import android.os.Build
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialException
import com.bh2xol.status.data.model.ApiResult
import com.bh2xol.status.data.remote.AuthApi

/**
 * 封装 Android CredentialManager 与 auth-service 的 WebAuthn / Passkey 交互。
 */
class PasskeyManager(private val context: Context) {
    private val credentialManager = CredentialManager.create(context)

    suspend fun loginWithPasskey(): ApiResult<String> {
        val optRes = AuthApi.getPasskeyLoginOptions()
        if (optRes !is ApiResult.Success) return optRes as ApiResult<String>

        val (challengeId, optJson) = optRes.data
        return try {
            val req = GetCredentialRequest(listOf(GetPublicKeyCredentialOption(optJson)))
            val result = credentialManager.getCredential(context, req)
            val cred = result.credential as? PublicKeyCredential
                ?: return ApiResult.Error("未获取到 Passkey 凭证")
            AuthApi.verifyPasskeyLogin(challengeId, cred.authenticationResponseJson)
        } catch (e: GetCredentialException) {
            ApiResult.Error(e.errorMessage?.toString() ?: "已取消 Passkey 验证")
        }
    }

    suspend fun registerPasskey(token: String): ApiResult<Unit> {
        val optRes = AuthApi.getPasskeyRegisterOptions(token)
        if (optRes !is ApiResult.Success) return optRes as ApiResult<Unit>

        val (challengeId, optJson) = optRes.data
        return try {
            val req = CreatePublicKeyCredentialRequest(optJson)
            val result = credentialManager.createCredential(context, req) as CreatePublicKeyCredentialResponse
            val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
            AuthApi.verifyPasskeyRegister(token, challengeId, result.registrationResponseJson, deviceName)
        } catch (e: CreateCredentialException) {
            ApiResult.Error(e.errorMessage?.toString() ?: "已取消绑定")
        }
    }
}
