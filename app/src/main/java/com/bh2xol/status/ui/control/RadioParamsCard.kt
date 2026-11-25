package com.bh2xol.status.ui.control

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bh2xol.status.config.AppConfig

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RadioParamsCard(
    isOnAir: Boolean,
    busy: Boolean,
    freqInput: String,
    modeInput: String,
    noteInput: String,
    onFreqChange: (String) -> Unit,
    onModeChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "频率、模式与活动备注",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = freqInput,
                onValueChange = { if (it.length <= AppConfig.MAX_FREQ_LEN) onFreqChange(it) },
                label = { Text("频率 (MHz)") },
                placeholder = { Text("例如 14.074 / 7.050 / 438.500") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.GraphicEq, null) },
                trailingIcon = {
                    if (freqInput.isNotEmpty()) {
                        IconButton(onClick = { onFreqChange("") }) {
                            Icon(Icons.Rounded.Clear, contentDescription = "清空")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                AppConfig.QUICK_FREQS.forEach { f ->
                    FilterChip(
                        selected = freqInput == f,
                        onClick = { onFreqChange(f) },
                        label = { Text(f) },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = modeInput,
                onValueChange = { if (it.length <= AppConfig.MAX_MODE_LEN) onModeChange(it.uppercase()) },
                label = { Text("模式 (支持任意自定义)") },
                placeholder = { Text("例如 FT8 / SSB / CW / FM / JS8") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Tune, null) },
                trailingIcon = {
                    if (modeInput.isNotEmpty()) {
                        IconButton(onClick = { onModeChange("") }) {
                            Icon(Icons.Rounded.Clear, contentDescription = "清空")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                AppConfig.QUICK_MODES.forEach { m ->
                    FilterChip(
                        selected = modeInput.equals(m, ignoreCase = true),
                        onClick = { onModeChange(m) },
                        label = { Text(m) },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = noteInput,
                onValueChange = { if (it.length <= AppConfig.MAX_NOTE_LEN) onNoteChange(it) },
                label = { Text("比赛 / 活动备注 (最多 ${AppConfig.MAX_NOTE_LEN} 字)") },
                placeholder = { Text("例如 CQWW / POTA CN-0001") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.EditNote, null) },
                trailingIcon = {
                    if (noteInput.isNotEmpty()) {
                        IconButton(onClick = { onNoteChange("") }) {
                            Icon(Icons.Rounded.Clear, contentDescription = "清空")
                        }
                    }
                },
                supportingText = {
                    Text("${noteInput.length} / ${AppConfig.MAX_NOTE_LEN}")
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                AppConfig.QUICK_NOTES.forEach { n ->
                    FilterChip(
                        selected = noteInput.equals(n, ignoreCase = true),
                        onClick = { onNoteChange(n) },
                        label = { Text(n) },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            FilledTonalButton(
                onClick = onSubmit,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (isOnAir) "更新徽章参数" else "以此参数开启 ON AIR")
            }
        }
    }
}
