package com.bh2xol.status.util

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * 将服务端返回的 ISO-8601 UTC 时间格式化为 `HH:mmZ (MM-dd HH:mm)`。
 */
fun formatSince(iso: String): String {
    if (iso.isBlank()) return ""
    return runCatching {
        val inst = Instant.parse(iso)
        val utc = DateTimeFormatter.ofPattern("HH:mm'Z'").withZone(ZoneOffset.UTC).format(inst)
        val local = DateTimeFormatter.ofPattern("MM-dd HH:mm").withZone(ZoneId.systemDefault()).format(inst)
        "$utc ($local)"
    }.getOrDefault("")
}
