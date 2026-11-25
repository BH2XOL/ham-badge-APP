package com.bh2xol.status.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bh2xol.status.auth.PasskeyManager
import com.bh2xol.status.data.local.TokenStore
import com.bh2xol.status.data.model.ApiResult
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    token: String,
    store: TokenStore,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onUnauthorized: () -> Unit,
    onLogout: () -> Unit,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val passkeyManager = remember { PasskeyManager(ctx) }

    var authUrlInput by remember { mutableStateOf(store.authBaseUrl) }
    var statusUrlInput by remember { mutableStateOf(store.statusBaseUrl) }
    var busy by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !busy) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
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

                // 1. 账号与安全（Passkey / 指纹绑定）
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "账号与安全",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "当前账号",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = store.email.ifBlank { "Passkey 会话" },
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = "绑定本机 Passkey 后，下次可直接使用系统指纹或面容一键登录。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        FilledTonalButton(
                            onClick = {
                                busy = true
                                scope.launch {
                                    when (val res = passkeyManager.registerPasskey(token)) {
                                        is ApiResult.Success ->
                                            snackbarHostState.showSnackbar("Passkey 绑定成功！下次可直接指纹登录")
                                        is ApiResult.Unauthorized -> onUnauthorized()
                                        is ApiResult.Error -> snackbarHostState.showSnackbar(res.message)
                                    }
                                    busy = false
                                }
                            },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            if (busy) {
                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                            } else {
                                Icon(Icons.Rounded.Fingerprint, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                            }
                            Text("绑定 Passkey / 指纹")
                        }
                    }
                }

                // 2. 服务地址配置
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Dns,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "服务地址配置",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "自定义 auth-service 与 ham-badge 后端地址（需以 http:// 或 https:// 开头）。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = authUrlInput,
                            onValueChange = { authUrlInput = it },
                            label = { Text("认证服务 (auth-service)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = statusUrlInput,
                            onValueChange = { statusUrlInput = it },
                            label = { Text("状态服务 (ham-badge)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            OutlinedButton(
                                onClick = {
                                    store.resetEndpoints()
                                    authUrlInput = store.authBaseUrl
                                    statusUrlInput = store.statusBaseUrl
                                    scope.launch { snackbarHostState.showSnackbar("已恢复默认服务地址") }
                                },
                                enabled = !busy,
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("恢复默认")
                            }
                            Button(
                                onClick = {
                                    store.authBaseUrl = authUrlInput
                                    store.statusBaseUrl = statusUrlInput
                                    store.syncEndpointsToConfig()
                                    authUrlInput = store.authBaseUrl
                                    statusUrlInput = store.statusBaseUrl
                                    scope.launch { snackbarHostState.showSnackbar("服务地址已保存") }
                                },
                                enabled = !busy,
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("保存地址")
                            }
                        }
                    }
                }

                // 3. 退出登录
                OutlinedButton(
                    onClick = onLogout,
                    enabled = !busy,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("退出登录")
                }

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
