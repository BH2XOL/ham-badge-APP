package com.bh2xol.status.ui.control

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CellTower
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.SensorsOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bh2xol.status.config.AppConfig
import com.bh2xol.status.data.model.StationStatus
import com.bh2xol.status.util.formatSince

@Composable
fun StatusOverviewCard(
    status: StationStatus,
    loading: Boolean,
    busy: Boolean,
    onToggle: () -> Unit,
) {
    val onAirContainer by animateColorAsState(
        targetValue = if (status.isOnAir) Color(0xFF1A7F37) else MaterialTheme.colorScheme.surfaceVariant,
        label = "statusCardBg",
    )
    val onAirContent by animateColorAsState(
        targetValue = if (status.isOnAir) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "statusCardFg",
    )

    Card(
        colors = CardDefaults.cardColors(
            containerColor = onAirContainer,
            contentColor = onAirContent,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (status.isOnAir) Icons.Rounded.Sensors else Icons.Rounded.SensorsOff,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = if (status.isOnAir) "ON AIR" else "OFF AIR",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (loading || busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = onAirContent,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            val detailLine = buildString {
                if (status.isOnAir) {
                    val fm = listOf(status.freq, status.mode, status.note)
                        .filter { it.isNotBlank() }
                        .joinToString(" · ")
                    append(fm.ifBlank { "未指定频率 / 模式" })
                    val sinceFmt = formatSince(status.since)
                    if (sinceFmt.isNotBlank()) append("\n更新时间：$sinceFmt")
                } else {
                    val host = AppConfig.statusBaseUrl.removePrefix("https://").removePrefix("http://")
                    append("当前徽章显示 OFF AIR，开启后将同步至 $host")
                }
            }
            Text(
                text = detailLine,
                style = MaterialTheme.typography.bodyMedium,
                color = onAirContent.copy(alpha = 0.9f),
            )

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onToggle,
                enabled = !busy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (status.isOnAir) Color.White else MaterialTheme.colorScheme.primary,
                    contentColor = if (status.isOnAir) Color(0xFF1A7F37) else MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = if (status.isOnAir) Icons.Rounded.PowerSettingsNew else Icons.Rounded.CellTower,
                    contentDescription = null,
                )
                Spacer(Modifier.width(8.dp))
                Text(if (status.isOnAir) "切换为 OFF AIR" else "开启 ON AIR 徽章")
            }
        }
    }
}
