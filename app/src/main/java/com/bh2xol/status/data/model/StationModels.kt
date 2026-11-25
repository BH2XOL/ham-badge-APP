package com.bh2xol.status.data.model

/**
 * 对应 ham-badge 服务 GET/POST /api/status 的台站状态。
 */
data class StationStatus(
    val status: String = "close",
    val since: String = "",
    val mode: String = "",
    val freq: String = "",
    val note: String = "",
) {
    val isOnAir: Boolean get() = status == "onair"
}

/**
 * 统一网络请求结果封装。
 */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data object Unauthorized : ApiResult<Nothing>()
    data class Error(val message: String) : ApiResult<Nothing>()
}
