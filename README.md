# ham-badge-APP

业余无线电（HAM）台站通联状态控制 Android 客户端（`QRZ 徽章`），基于 **Kotlin + Jetpack Compose + Material 3** 构建。

配套服务端项目：
- 状态徽章服务：[BH2XOL/ham-badge](https://github.com/BH2XOL/ham-badge)
- 统一认证服务：[BH2XOL/auth-service](https://github.com/BH2XOL/auth-service)

## 功能特性

- **双模式安全登录**：支持邮箱密码 + 6 位一次性验证码（OTP），以及系统级 WebAuthn / Passkey 生物识别登录。
- **实时台站状态控制**：一键切换 `ON AIR` / `OFF AIR` 状态，展示 UTC 上线时间与当前工作参数。
- **工作参数与活动备注**：支持设置频率（`freq`）、模式（`mode`）与比赛/活动备注（`note`，最多 32 个字符），`ON AIR` 期间更新参数不重置上线时间。
- **独立设置页面**：集中管理 Passkey / 指纹绑定、自定义 `auth-service` / `ham-badge` 服务地址（支持恢复默认）以及服务端会话注销退出。
- **Material 3 动态配色**：支持 Android 12+ 动态取色与自动跟随系统明暗主题。

## 项目结构

```text
app/src/main/java/com/bh2xol/status/
├── MainActivity.kt                  # 入口与顶层认证状态路由
├── config/
│   └── AppConfig.kt                 # 默认服务地址、预设参数与字段长度限制
├── data/
│   ├── model/
│   │   └── StationModels.kt         # StationStatus 与 ApiResult
│   ├── local/
│   │   └── TokenStore.kt            # SharedPreferences 持久化存储
│   └── remote/
│       ├── HttpClient.kt            # HttpURLConnection 请求封装
│       ├── AuthApi.kt               # auth-service 认证接口
│       └── StatusApi.kt             # ham-badge 状态接口
├── auth/
│   └── PasskeyManager.kt            # Android CredentialManager 封装
├── ui/
│   ├── theme/
│   │   └── Theme.kt                 # Material 3 主题
│   ├── login/
│   │   └── LoginScreen.kt           # 登录页面
│   ├── control/
│   │   ├── ControlScreen.kt         # 主控制页面
│   │   ├── StatusOverviewCard.kt    # 状态总览卡片
│   │   └── RadioParamsCard.kt       # 频率、模式与备注设置卡片
│   └── settings/
│       └── SettingsScreen.kt        # 设置二级页面
└── util/
    └── TimeFormat.kt                # UTC / 本地时间格式化
```

## 构建与配置

1. 默认服务端地址定义在 `app/src/main/java/com/bh2xol/status/config/AppConfig.kt` 中，也可在应用内「设置 → 服务地址配置」中直接修改：
   - `DEFAULT_AUTH_BASE_URL`：`https://auth.bh2xol.com`
   - `DEFAULT_STATUS_BASE_URL`：`https://status.bh2xol.com`
2. 在 `android/` 根目录创建 `local.properties` 指定 `sdk.dir` 后，使用 Gradle 构建：

```bash
gradle assembleDebug
```

如需构建签名 Release 包，可在根目录配置 `keystore.properties`（已被 `.gitignore` 忽略）。

## 开源协议

[MIT](./LICENSE)
