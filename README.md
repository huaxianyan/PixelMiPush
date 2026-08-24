# PixelMiPush

PixelMiPush 是 [NihilityT/MiPush](https://github.com/NihilityT/MiPush) 的派生项目，需要配合 [MiPushFramework](https://github.com/NihilityT/MiPushFramework) 使用。项目基于 Xposed API 102，为非 MIUI 设备提供目标应用环境伪装和跨包通知发布能力，并针对 Pixel Android 16 的 QQ 会话通知进行适配。

当前 Pixel 版身份：

```text
应用名称：Pixel MiPush
包名：com.neko7ina.mipush.pixel
```

## 主要功能

- 使用 Xposed API 102，不依赖 `de.robv.android.xposed` Legacy API
- 为 MiPushFramework 提供以目标应用身份发布通知的能力
- 在 Android 16 限制反射写入 `Build.*` 时，继续提供 `SystemProperties` Hook 和 `Unsafe` 后备写入
- 适配 Pixel Android 16 的 QQ MessagingStyle 会话通知
- 保留联系人头像、群头像、`Person.icon` 和 `android.largeIcon`
- 使用 MiPushFramework 提供的单色 `smallIcon` 作为会话头像右下角角标和顶层汇总图标
- 将 Android 16 的 40 dp 会话头像恢复为 48 dp，并使用 20 dp 角标和 4 dp 角标内边距
- 修复 QQ 顶层汇总通知重复显示「QQ」的问题，不修改普通子通知的副标题
- 仅处理 `packageName=com.tencent.mobileqq` 且 `tag=mipush_com.tencent.mobileqq` 的通知；接口不匹配时安全跳过

Pixel 版不包含 MIUI/HyperOS SystemUI 插件代码，也不会内置、替换或自动启用 QQ fallback 图标。通知使用的 `smallIcon` 始终由 MiPushFramework 提供。

## 环境要求

- Pixel Android 16
- 支持 Xposed API 102 的 LSPosed
- 已完成初始化的 MiPushFramework
- Root 环境

当前版本已在以下环境完成验证：

```text
Pixel 10 Pro
Android 16 / API 36
LSPosed 2.1.0 / API 102
QQ 9.1.50
MiPushFramework 0.3.11
```

## 安装

1. 安装并初始化 [MiPushFramework](https://github.com/NihilityT/MiPushFramework/releases/latest)。
2. 从 [Releases](https://github.com/huaxianyan/PixelMiPush/releases/latest) 下载最新的 Pixel APK。
3. 在 LSPosed 中启用 PixelMiPush，并设置以下作用域：

   ```text
   系统框架
   系统界面（com.android.systemui）
   推送服务（com.xiaomi.xmsf）
   需要设备属性伪装的目标应用
   ```

4. 重启设备，使 `system_server`、SystemUI 和 XMSF 中的 Hook 完整生效。
5. 启动目标应用，使其完成 MiPush 注册，再测试真实服务端推送。

不要在相同 LSPosed 作用域中同时启用其他 MiPush 模块版本，否则可能产生重复 Hook 和不可预测的通知行为。

## MiPushFramework 配置

通知渠道、MessagingStyle、头像、会话 ID、点击意图和通知分组由 MiPushFramework 配置负责。推荐从 [NihilityT/MiPushConfigurations](https://github.com/NihilityT/MiPushConfigurations) 按需选择配置，不要同时保留标记为互斥的白名单、黑名单或样式文件。

QQ 在 Pixel 上推荐启用：

- `com.tencent.mobileqq_QQ.json`
- `com.tencent.mobileqq_QQ_MessagingStyle.json`
- `com.tencent.mobileqq_QQ_意图重整.json`
- `2_后置配置_直接打开意图-白名单.json`，并将 `com.tencent.mobileqq` 加入白名单

「直接打开意图」会让新通知使用 Activity PendingIntent，可避免通过 XMSF Service 间接打开 QQ 时触发 SystemUI 启动动画超时。配置变更只影响之后创建的新通知。

## 使用边界

- Pixel SystemUI 适配依赖 Android 16 的内部实现，系统更新后可能需要重新验证。
- 模块不会替代 MiPushFramework，也不负责决定 QQ 服务端何时切换到厂商推送。
- 并非所有应用都提供可用的 MiPush 注册和离线推送能力。
- MiPushFramework、XMSF 和 LSPosed 不应被后台管理工具冻结或限制网络。
- Pixel 专用源码位于 `modern-api-102-pixel-android16` 分支；`master` 保留 API 102 通用实现。

## 构建

仓库使用 GitHub Actions、JDK 17 和 Android SDK 34 构建 APK。进入 **Actions → Build installable APK → Run workflow**，选择 Pixel 分支并按需填写 `version_name` 和 `version_code`。

未配置签名 Secrets 时，工作流使用临时 Debug 签名，不能覆盖正式 Release。正式 Release 使用长期签名，并由 CI 校验包名、Xposed API 102 声明、APK 签名以及 DEX 中不存在 Legacy Xposed API 依赖。

## 来源与许可证

本项目 Fork 自 [NihilityT/MiPush](https://github.com/NihilityT/MiPush)，原项目又派生自 [fei-ke/HMSPush](https://github.com/fei-ke/HMSPush)。这些代码继续保留 [GNU General Public License v3](LICENSE.MiPush-GPL-3.0) 的版权和授权声明。

设备属性伪装中的部分 `android.os.SystemProperties` Hook 和 MIUI 属性值改编自 [yin-ol/MiPushFaker](https://github.com/yin-ol/MiPushFaker)，对应代码遵循 [GNU Affero General Public License v3](LICENSE.MiPushFaker-AGPL-3.0)。

组合发行版本遵循仓库根目录中的 [GNU AGPL v3](LICENSE)。原有 GPLv3 代码继续保留其许可证、版权和来源说明，并依据 GPLv3 第 13 节与 AGPLv3 代码组合分发。详细来源、文件范围和署名参见 [NOTICE](NOTICE)。

本项目与上述上游作者不存在背书关系。
