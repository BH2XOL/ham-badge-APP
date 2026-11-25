package com.bh2xol.status

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.bh2xol.status.data.local.TokenStore
import com.bh2xol.status.data.remote.AuthApi
import com.bh2xol.status.ui.control.ControlScreen
import com.bh2xol.status.ui.login.LoginScreen
import com.bh2xol.status.ui.settings.SettingsScreen
import com.bh2xol.status.ui.theme.HamStatusTheme
import kotlinx.coroutines.launch

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
    val store = remember { TokenStore(ctx).apply { syncEndpointsToConfig() } }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var token by remember { mutableStateOf(store.token) }
    var verifyingStartup by remember { mutableStateOf(token != null) }
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val saved = token
        if (saved != null) {
            val valid = AuthApi.checkToken(saved)
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

    val activeToken = token
    if (activeToken == null) {
        showSettings = false
        LoginScreen(
            store = store,
            snackbarHostState = snackbarHostState,
            onLoggedIn = { email, newToken ->
                if (email.isNotBlank()) store.email = email
                store.token = newToken
                token = newToken
            },
        )
    } else if (showSettings) {
        BackHandler { showSettings = false }
        SettingsScreen(
            token = activeToken,
            store = store,
            snackbarHostState = snackbarHostState,
            onBack = { showSettings = false },
            onUnauthorized = {
                showSettings = false
                store.token = null
                token = null
                scope.launch { snackbarHostState.showSnackbar("Token 已失效，请重新登录") }
            },
            onLogout = {
                val toRevoke = activeToken
                showSettings = false
                store.token = null
                token = null
                scope.launch {
                    AuthApi.logout(toRevoke)
                }
            },
        )
    } else {
        ControlScreen(
            token = activeToken,
            store = store,
            snackbarHostState = snackbarHostState,
            onUnauthorized = {
                store.token = null
                token = null
                scope.launch { snackbarHostState.showSnackbar("Token 已失效，请重新登录") }
            },
            onOpenSettings = { showSettings = true },
        )
    }
}
