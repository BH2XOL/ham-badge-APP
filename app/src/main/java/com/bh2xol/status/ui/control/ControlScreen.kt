package com.bh2xol.status.ui.control

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bh2xol.status.data.local.TokenStore
import com.bh2xol.status.data.model.ApiResult
import com.bh2xol.status.data.model.StationStatus
import com.bh2xol.status.data.remote.StatusApi
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlScreen(
    token: String,
    store: TokenStore,
    snackbarHostState: SnackbarHostState,
    onUnauthorized: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf(StationStatus()) }
    var freqInput by remember { mutableStateOf(store.lastFreq) }
    var modeInput by remember { mutableStateOf(store.lastMode) }
    var noteInput by remember { mutableStateOf(store.lastNote) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }

    suspend fun refresh() {
        loading = true
        when (val res = StatusApi.getStatus()) {
            is ApiResult.Success -> {
                status = res.data
                if (res.data.freq.isNotBlank()) freqInput = res.data.freq
                if (res.data.mode.isNotBlank()) modeInput = res.data.mode
                if (res.data.isOnAir) noteInput = res.data.note
            }
            is ApiResult.Error -> snackbarHostState.showSnackbar(res.message)
            ApiResult.Unauthorized -> Unit
        }
        loading = false
    }

    LaunchedEffect(Unit) { refresh() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("QRZ 状态徽章") },
                actions = {
                    IconButton(onClick = { scope.launch { refresh() } }, enabled = !loading && !busy) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "刷新")
                    }
                    IconButton(onClick = onOpenSettings, enabled = !busy) {
                        Icon(Icons.Rounded.Settings, contentDescription = "设置")
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Spacer(Modifier.height(4.dp))

                StatusOverviewCard(
                    status = status,
                    loading = loading,
                    busy = busy,
                    onToggle = {
                        busy = true
                        val nextStatus = if (status.isOnAir) "close" else "onair"
                        val cleanFreq = freqInput.trim()
                        val cleanMode = modeInput.trim().uppercase()
                        val cleanNote = noteInput.trim()
                        if (nextStatus == "onair") {
                            store.lastFreq = cleanFreq
                            store.lastMode = cleanMode
                            store.lastNote = cleanNote
                        }
                        scope.launch {
                            val res = if (nextStatus == "onair") {
                                StatusApi.setStatus(
                                    token = token,
                                    status = "onair",
                                    freq = cleanFreq,
                                    mode = cleanMode,
                                    note = cleanNote,
                                )
                            } else {
                                StatusApi.setStatus(token = token, status = "close")
                            }
                            when (res) {
                                is ApiResult.Success -> {
                                    status = res.data
                                    snackbarHostState.showSnackbar(
                                        if (res.data.isOnAir) "已切换为 ON AIR" else "已切换为 OFF AIR"
                                    )
                                }
                                is ApiResult.Unauthorized -> onUnauthorized()
                                is ApiResult.Error -> snackbarHostState.showSnackbar(res.message)
                            }
                            busy = false
                        }
                    },
                )

                RadioParamsCard(
                    isOnAir = status.isOnAir,
                    busy = busy,
                    freqInput = freqInput,
                    modeInput = modeInput,
                    noteInput = noteInput,
                    onFreqChange = { freqInput = it },
                    onModeChange = { modeInput = it },
                    onNoteChange = { noteInput = it },
                    onSubmit = {
                        busy = true
                        val cleanFreq = freqInput.trim()
                        val cleanMode = modeInput.trim().uppercase()
                        val cleanNote = noteInput.trim()
                        store.lastFreq = cleanFreq
                        store.lastMode = cleanMode
                        store.lastNote = cleanNote
                        scope.launch {
                            val res = if (status.isOnAir) {
                                // ON AIR 期间只更新频率、模式与备注，不重置上线时间
                                StatusApi.setStatus(
                                    token = token,
                                    freq = cleanFreq,
                                    mode = cleanMode,
                                    note = cleanNote,
                                )
                            } else {
                                StatusApi.setStatus(
                                    token = token,
                                    status = "onair",
                                    freq = cleanFreq,
                                    mode = cleanMode,
                                    note = cleanNote,
                                )
                            }
                            when (res) {
                                is ApiResult.Success -> {
                                    status = res.data
                                    val summary = listOf(cleanFreq, cleanMode, cleanNote)
                                        .filter { it.isNotBlank() }
                                        .joinToString(" ")
                                    snackbarHostState.showSnackbar(
                                        if (summary.isNotBlank()) "已更新：$summary" else "已更新状态"
                                    )
                                }
                                is ApiResult.Unauthorized -> onUnauthorized()
                                is ApiResult.Error -> snackbarHostState.showSnackbar(res.message)
                            }
                            busy = false
                        }
                    },
                )

                Spacer(Modifier.height(16.dp))
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp),
            )
        }
    }
}
