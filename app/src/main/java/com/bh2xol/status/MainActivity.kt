package com.bh2xol.status

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialException
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HamStatusTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HamStatusApp()
                }
            }
        }
    }
}

@Composable
fun HamStatusApp() {
    val ctx = LocalContext.current
    val store = remember { TokenStore(ctx) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var token by remember { mutableStateOf(store.token) }
    var verifyingStartup by remember { mutableStateOf(token != null) }

    LaunchedEffect(Unit) {
        val saved = token
        if (saved != null) {
            val valid = StatusApi.checkToken(saved)
            if (!valid) {
                store.token = null
                token = null
                scope.launch { snackbarHostState.showSnackbar("登录已过期，请重新验证") }
            }
        }
        verifyingStartup = false
    }

    if (verifyingStartup) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (token == null) {
        LoginScreen(
            initialEmail = store.email,
            snackbarHostState = snackbarHostState,
            onLoggedIn = { email, newToken ->
                store.email = email
                store.token = newToken
                token = newToken
            },
        )
    } else {
        ControlScreen(
            token = token!!,
            store = store,
            snackbarHostState = snackbarHostState,
            onUnauthorized = {
                store.token = null
                token = null
                scope.launch { snackbarHostState.showSnackbar("Token 已失效，请重新登录") }
            },
            onLogout = {
                store.token = null
                token = null
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    initialEmail: String,
    snackbarHostState: SnackbarHostState,
    onLoggedIn: (email: String, token: String) -> Unit,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val credManager = remember { CredentialManager.create(ctx) }

    var usePasskey by remember { mutableStateOf(false) }
    var hasPasskey by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf(initialEmail) }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var otpCode by remember { mutableStateOf("") }
    var otpSent by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val exists = StatusApi.hasPasskey()
        hasPasskey = exists
        if (exists) usePasskey = true
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("QRZ 状态徽章") })
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
                text = "通过 auth.bh2xol.com 邮箱验证码登录",
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
                                "服务器尚未绑定 Passkey，请先切换到「邮箱验证码」登录后再绑定。"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                loading = true
                                scope.launch {
                                    when (val optRes = StatusApi.getPasskeyLoginOptions()) {
                                        is ApiResult.Success -> {
                                            val (challengeId, optJson) = optRes.data
                                            try {
                                                val req = GetCredentialRequest(
                                                    listOf(GetPublicKeyCredentialOption(optJson))
                                                )
                                                val result = credManager.getCredential(ctx, req)
                                                val cred = result.credential as? PublicKeyCredential
                                                if (cred != null) {
                                                    when (val vRes = StatusApi.verifyPasskeyLogin(challengeId, cred.authenticationResponseJson)) {
                                                        is ApiResult.Success -> onLoggedIn(email.trim(), vRes.data)
                                                        is ApiResult.Error -> snackbarHostState.showSnackbar(vRes.message)
                                                        ApiResult.Unauthorized -> Unit
                                                    }
                                                } else {
                                                    snackbarHostState.showSnackbar("未获取到 Passkey 凭证")
                                                }
                                            } catch (e: GetCredentialException) {
                                                snackbarHostState.showSnackbar(e.errorMessage?.toString() ?: "已取消 Passkey 验证")
                                            }
                                        }
                                        is ApiResult.Error -> snackbarHostState.showSnackbar(optRes.message)
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
                                when (val res = StatusApi.requestOtp(email, password)) {
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
                                    when (val res = StatusApi.verifyOtp(otpCode)) {
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ControlScreen(
    token: String,
    store: TokenStore,
    snackbarHostState: SnackbarHostState,
    onUnauthorized: () -> Unit,
    onLogout: () -> Unit,
) {
    val ctx = LocalContext.current
    val credManager = remember { CredentialManager.create(ctx) }
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf(StationStatus()) }
    var freqInput by remember { mutableStateOf(store.lastFreq) }
    var modeInput by remember { mutableStateOf(store.lastMode) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }

    val quickFreqs = remember { listOf("7.050", "14.074", "14.270", "21.074", "28.074", "145.000", "438.500") }
    val quickModes = remember { listOf("FT8", "FT4", "SSB", "CW", "FM", "AM", "RTTY", "SSTV") }

    suspend fun refresh() {
        loading = true
        when (val res = StatusApi.getStatus()) {
            is ApiResult.Success -> {
                status = res.data
                if (res.data.freq.isNotBlank()) freqInput = res.data.freq
                if (res.data.mode.isNotBlank()) modeInput = res.data.mode
            }
            is ApiResult.Error -> snackbarHostState.showSnackbar(res.message)
            ApiResult.Unauthorized -> Unit
        }
        loading = false
    }

    LaunchedEffect(Unit) { refresh() }

    val onAirContainer by animateColorAsState(
        targetValue = if (status.isOnAir) Color(0xFF1A7F37) else MaterialTheme.colorScheme.surfaceVariant,
        label = "statusCardBg",
    )
    val onAirContent by animateColorAsState(
        targetValue = if (status.isOnAir) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "statusCardFg",
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("QRZ 状态徽章") },
                actions = {
                    IconButton(
                        onClick = {
                            busy = true
                            scope.launch {
                                when (val optRes = StatusApi.getPasskeyRegisterOptions(token)) {
                                    is ApiResult.Success -> {
                                        val (challengeId, optJson) = optRes.data
                                        try {
                                            val req = CreatePublicKeyCredentialRequest(optJson)
                                            val result = credManager.createCredential(ctx, req) as CreatePublicKeyCredentialResponse
                                            val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
                                            when (val vRes = StatusApi.verifyPasskeyRegister(token, challengeId, result.registrationResponseJson, deviceName)) {
                                                is ApiResult.Success -> snackbarHostState.showSnackbar("Passkey 绑定成功！下次可直接指纹登录")
                                                is ApiResult.Unauthorized -> onUnauthorized()
                                                is ApiResult.Error -> snackbarHostState.showSnackbar(vRes.message)
                                            }
                                        } catch (e: CreateCredentialException) {
                                            snackbarHostState.showSnackbar(e.errorMessage?.toString() ?: "已取消绑定")
                                        }
                                    }
                                    is ApiResult.Unauthorized -> onUnauthorized()
                                    is ApiResult.Error -> snackbarHostState.showSnackbar(optRes.message)
                                }
                                busy = false
                            }
                        },
                        enabled = !busy,
                    ) {
                        Icon(Icons.Rounded.Fingerprint, contentDescription = "绑定 Passkey")
                    }
                    IconButton(onClick = { scope.launch { refresh() } }, enabled = !loading && !busy) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "刷新")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = "退出登录")
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

            // 1. 状态总览卡片
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
                            val fm = listOf(status.freq, status.mode).filter { it.isNotBlank() }.joinToString(" · ")
                            append(fm.ifBlank { "未指定频率 / 模式" })
                            val sinceFmt = formatSince(status.since)
                            if (sinceFmt.isNotBlank()) append("\n更新时间：$sinceFmt")
                        } else {
                            append("当前徽章显示 OFF AIR，开启后将同步至 status.bh2xol.com")
                        }
                    }
                    Text(
                        text = detailLine,
                        style = MaterialTheme.typography.bodyMedium,
                        color = onAirContent.copy(alpha = 0.9f),
                    )

                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            busy = true
                            val nextStatus = if (status.isOnAir) "close" else "onair"
                            if (nextStatus == "onair") {
                                store.lastFreq = freqInput.trim()
                                store.lastMode = modeInput.trim().uppercase()
                            }
                            scope.launch {
                                val res = if (nextStatus == "onair") {
                                    StatusApi.setStatus(
                                        token = token,
                                        status = "onair",
                                        freq = freqInput.trim(),
                                        mode = modeInput.trim().uppercase(),
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

            // 2. 频率与模式设置卡片
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "频率与模式",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = freqInput,
                        onValueChange = { if (it.length <= 16) freqInput = it },
                        label = { Text("频率 (MHz)") },
                        placeholder = { Text("例如 14.074 / 7.050 / 438.500") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Rounded.GraphicEq, null) },
                        trailingIcon = {
                            if (freqInput.isNotEmpty()) {
                                IconButton(onClick = { freqInput = "" }) {
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
                        quickFreqs.forEach { f ->
                            FilterChip(
                                selected = freqInput == f,
                                onClick = { freqInput = f },
                                label = { Text(f) },
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = modeInput,
                        onValueChange = { if (it.length <= 12) modeInput = it.uppercase() },
                        label = { Text("模式 (支持任意自定义)") },
                        placeholder = { Text("例如 FT8 / SSB / CW / FM / JS8") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Rounded.Tune, null) },
                        trailingIcon = {
                            if (modeInput.isNotEmpty()) {
                                IconButton(onClick = { modeInput = "" }) {
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
                        quickModes.forEach { m ->
                            FilterChip(
                                selected = modeInput.equals(m, ignoreCase = true),
                                onClick = { modeInput = m },
                                label = { Text(m) },
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    FilledTonalButton(
                        onClick = {
                            busy = true
                            val cleanFreq = freqInput.trim()
                            val cleanMode = modeInput.trim().uppercase()
                            store.lastFreq = cleanFreq
                            store.lastMode = cleanMode
                            scope.launch {
                                val res = if (status.isOnAir) {
                                    // ON AIR 期间只更新频率与模式，不重置上线时间
                                    StatusApi.setStatus(token = token, freq = cleanFreq, mode = cleanMode)
                                } else {
                                    StatusApi.setStatus(
                                        token = token,
                                        status = "onair",
                                        freq = cleanFreq,
                                        mode = cleanMode,
                                    )
                                }
                                when (res) {
                                    is ApiResult.Success -> {
                                        status = res.data
                                        snackbarHostState.showSnackbar("已更新：$cleanFreq $cleanMode".trim())
                                    }
                                    is ApiResult.Unauthorized -> onUnauthorized()
                                    is ApiResult.Error -> snackbarHostState.showSnackbar(res.message)
                                }
                                busy = false
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (status.isOnAir) "更新徽章频率与模式" else "以此参数开启 ON AIR")
                    }
                }
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

private fun formatSince(iso: String): String {
    if (iso.isBlank()) return ""
    return runCatching {
        val inst = Instant.parse(iso)
        val utc = DateTimeFormatter.ofPattern("HH:mm'Z'").withZone(ZoneOffset.UTC).format(inst)
        val local = DateTimeFormatter.ofPattern("MM-dd HH:mm").withZone(ZoneId.systemDefault()).format(inst)
        "$utc ($local)"
    }.getOrDefault("")
}
