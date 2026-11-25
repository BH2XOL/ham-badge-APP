package com.bh2xol.status.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.bh2xol.status.auth.PasskeyManager
import com.bh2xol.status.config.AppConfig
import com.bh2xol.status.data.local.TokenStore
import com.bh2xol.status.data.model.ApiResult
import com.bh2xol.status.data.remote.AuthApi
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    store: TokenStore,
    snackbarHostState: SnackbarHostState,
    onLoggedIn: (email: String, token: String) -> Unit,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val passkeyManager = remember { PasskeyManager(ctx) }

    var usePasskey by remember { mutableStateOf(false) }
    var hasPasskey by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf(store.email) }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var otpCode by remember { mutableStateOf("") }
    var otpSent by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var showEndpointDialog by remember { mutableStateOf(false) }

    LaunchedEffect(AppConfig.authBaseUrl) {
        val exists = AuthApi.hasPasskey()
        hasPasskey = exists
        if (exists) usePasskey = true
    }

    if (showEndpointDialog) {
        EndpointConfigDialog(
            store = store,
            onDismiss = { showEndpointDialog = false },
            onSaved = {
                showEndpointDialog = false
                scope.launch {
                    val exists = AuthApi.hasPasskey()
                    hasPasskey = exists
                    snackbarHostState.showSnackbar("服务地址已更新")
                }
            },
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("QRZ 状态徽章") },
                actions = {
                    IconButton(onClick = { showEndpointDialog = true }) {
                        Icon(Icons.Rounded.Settings, contentDescription = "服务地址设置")
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
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(24.dp))
                Icon(
                    imageVector = Icons.Rounded.Badge,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "登录 QRZ 徽章控制",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "通过 ${AppConfig.authBaseUrl.removePrefix("https://").removePrefix("http://")} 认证登录",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(20.dp))

                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = usePasskey,
                                onClick = { usePasskey = true },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                icon = { Icon(Icons.Rounded.Fingerprint, null, Modifier.size(18.dp)) },
                            ) {
                                Text("Passkey")
                            }
                            SegmentedButton(
                                selected = !usePasskey,
                                onClick = { usePasskey = false },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                icon = { Icon(Icons.Rounded.Email, null, Modifier.size(18.dp)) },
                            ) {
                                Text("邮箱验证码")
                            }
                        }
                        Spacer(Modifier.height(16.dp))

                        if (usePasskey) {
                            Text(
                                text = if (hasPasskey) {
                                    "使用系统生物识别（指纹 / 面容）或通行密钥一键登录。"
                                } else {
                                    "服务器尚未绑定 Passkey，请先切换到「邮箱验证码」登录后在设置中绑定。"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    loading = true
                                    scope.launch {
                                        when (val res = passkeyManager.loginWithPasskey()) {
                                            is ApiResult.Success -> onLoggedIn(email.trim(), res.data)
                                            is ApiResult.Error -> snackbarHostState.showSnackbar(res.message)
                                            ApiResult.Unauthorized -> Unit
                                        }
                                        loading = false
                                    }
                                },
                                enabled = !loading,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                if (loading) {
                                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(8.dp))
                                } else {
                                    Icon(Icons.Rounded.Fingerprint, null)
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text("使用 Passkey / 指纹登录")
                            }
                        } else {
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("邮箱") },
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Rounded.Email, null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("密码") },
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Rounded.Lock, null) },
                                trailingIcon = {
                                    IconButton(onClick = { showPassword = !showPassword }) {
                                        Icon(
                                            if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                            contentDescription = "显示密码",
                                        )
                                    }
                                },
                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    if (email.isBlank() || password.isBlank()) {
                                        scope.launch { snackbarHostState.showSnackbar("请输入邮箱和密码") }
                                        return@Button
                                    }
                                    loading = true
                                    scope.launch {
                                        when (val res = AuthApi.requestOtp(email, password)) {
                                            is ApiResult.Success -> {
                                                otpSent = true
                                                val msg = if (res.data) "验证码仍在有效期内，请直接输入" else "验证码已发送到邮箱"
                                                snackbarHostState.showSnackbar(msg)
                                            }
                                            is ApiResult.Error -> snackbarHostState.showSnackbar(res.message)
                                            ApiResult.Unauthorized -> Unit
                                        }
                                        loading = false
                                    }
                                },
                                enabled = !loading,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                if (loading && !otpSent) {
                                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text(if (otpSent) "重新发送验证码" else "发送验证码")
                            }

                            if (otpSent) {
                                Spacer(Modifier.height(20.dp))
                                HorizontalDivider()
                                Spacer(Modifier.height(16.dp))
                                OutlinedTextField(
                                    value = otpCode,
                                    onValueChange = { if (it.length <= 6) otpCode = it.filter(Char::isDigit) },
                                    label = { Text("6 位邮箱验证码") },
                                    singleLine = true,
                                    leadingIcon = { Icon(Icons.Rounded.Pin, null) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Spacer(Modifier.height(14.dp))
                                Button(
                                    onClick = {
                                        if (otpCode.length < 4) {
                                            scope.launch { snackbarHostState.showSnackbar("请输入验证码") }
                                            return@Button
                                        }
                                        loading = true
                                        scope.launch {
                                            when (val res = AuthApi.verifyOtp(otpCode)) {
                                                is ApiResult.Success -> onLoggedIn(email.trim(), res.data)
                                                is ApiResult.Error -> snackbarHostState.showSnackbar(res.message)
                                                ApiResult.Unauthorized -> Unit
                                            }
                                            loading = false
                                        }
                                    },
                                    enabled = !loading,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    if (loading) {
                                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    Text("验证并登录")
                                }
                            }
                        }
                    }
                }
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

@Composable
private fun EndpointConfigDialog(
    store: TokenStore,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    var authUrl by remember { mutableStateOf(store.authBaseUrl) }
    var statusUrl by remember { mutableStateOf(store.statusBaseUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("服务地址配置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = authUrl,
                    onValueChange = { authUrl = it },
                    label = { Text("认证服务 (auth-service)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = statusUrl,
                    onValueChange = { statusUrl = it },
                    label = { Text("状态服务 (ham-badge)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    store.resetEndpoints()
                    onSaved()
                },
            ) {
                Text("恢复默认")
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    store.authBaseUrl = authUrl
                    store.statusBaseUrl = statusUrl
                    store.syncEndpointsToConfig()
                    onSaved()
                },
            ) {
                Text("保存")
            }
        },
    )
}
