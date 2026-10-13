# PixelMiPush

PixelMiPush 是 [NihilityT/MiPush](https://github.com/NihilityT/MiPush) 的派生项目，需要配合我们维护的 [PixelMiPushFramework](https://github.com/huaxianyan/PixelMiPushFramework) 使用。项目基于 Xposed API 102，为非 MIUI 设备提供目标应用环境伪装和跨包通知发布能力，并针对 Pixel Android 16 的 QQ 会话通知进行适配。

当前 Pixel 版身份：

```text
应用名称：Pixel MiPush
包名：com.neko7ina.pixelmipush
```

## 主要功能

- 使用 Xposed API 102，不依赖 `de.robv.android.xposed` Legacy API
- 为 PixelMiPushFramework 提供以目标应用身份发布通知的能力
- 在 Android 16 限制反射写入 `Build.*` 时，继续提供 `SystemProperties` Hook 和 `Unsafe` 后备写入
- 适配 Pixel Android 16 的 QQ MessagingStyle 会话通知
- 保留联系人头像、群头像、`Person.icon` 和 `android.largeIcon`
- 使用 PixelMiPushFramework 提供的单色 `smallIcon` 作为会话头像右下角角标和顶层汇总图标
- 将 Android 16 的 40 dp 会话头像恢复为 48 dp，并使用 20 dp 角标和 4 dp 角标内边距
- 修复 QQ 顶层汇总通知重复显示「QQ」的问题，不修改普通子通知的副标题
- 仅处理 `packageName=com.tencent.mobileqq` 且 `tag=mipush_com.tencent.mobileqq` 的通知；接口不匹配时安全跳过

Pixel 版不包含 MIUI/HyperOS SystemUI 插件代码，也不会内置、替换或自动启用 QQ fallback 图标。通知使用的 `smallIcon` 始终由 PixelMiPushFramework 提供。

## 环境要求

- Pixel Android 16
- 支持 Xposed API 102 的 LSPosed
- 已完成初始化的 PixelMiPushFramework
- Root 环境

以下为历史版本使用原版 MiPushFramework 时的验证环境，不代表当前发布包的验收结果：

```text
Pixel 10 Pro
Android 16 / API 36
LSPosed 2.1.0 / API 102
QQ 9.1.50
MiPushFramework 0.3.11
```

## 安装

1. 安装并初始化 [PixelMiPushFramework](https://github.com/huaxianyan/PixelMiPushFramework/releases/latest)，通常选择 `normal` 版本。
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

### 包名迁移

历史版本（截至 v242）的包名是 `com.neko7ina.mipush.pixel`，后续版本改为 `com.neko7ina.pixelmipush`。新包独立安装，签名相同也不能覆盖旧包，模块私有配置需要重新设置。

迁移时先在 LSPosed 中关闭旧模块，再安装并启用新模块，重新选择作用域，最后按安装步骤重启。确认新模块工作正常后再移除旧包。框架及其配置文件保持原位。

### 通过 Obtainium 安装与更新

先安装 [Obtainium](https://obtainium.imranr.dev/)，再点击下方按钮导入本项目。入口跟踪采用新包名和新产物命名的正式版本。

[<img src="images/obtainium-badge.png" alt="通过 Obtainium 获取 Pixel MiPush" height="48">][obtainium]

模块从 v1.0.0 开始使用独立版本号。历史旧包用户需按上面的步骤手动迁移，迁移后可通过 Obtainium 跟踪后续正式版。

[obtainium]: https://apps.obtainium.imranr.dev/redirect?r=obtainium%3A%2F%2Fapp%2F%257B%2522id%2522%253A%2522com.neko7ina.pixelmipush%2522%252C%2522url%2522%253A%2522https%253A%2F%2Fgithub.com%2Fhuaxianyan%2FPixelMiPush%2522%252C%2522author%2522%253A%2522huaxianyan%2522%252C%2522name%2522%253A%2522Pixel%2520MiPush%2522%252C%2522additionalSettings%2522%253A%2522%257B%255C%2522apkFilterRegEx%255C%2522%253A%255C%2522%255EPixelMiPush-universal-.%252A%255C%255C%255C%255C.apk%2524%255C%2522%252C%255C%2522includePrereleases%255C%2522%253Afalse%252C%255C%2522versionDetection%255C%2522%253Atrue%252C%255C%2522appName%255C%2522%253A%255C%2522Pixel%2520MiPush%255C%2522%257D%2522%257D

## PixelMiPushFramework 配置

通知渠道、MessagingStyle、头像、会话 ID、点击意图和通知分组由 PixelMiPushFramework 配置负责。推荐从 [NihilityT/MiPushConfigurations](https://github.com/NihilityT/MiPushConfigurations) 按需选择配置，不要同时保留标记为互斥的白名单、黑名单或样式文件。

QQ 在 Pixel 上推荐启用：

- `com.tencent.mobileqq_QQ.json`
- `com.tencent.mobileqq_QQ_MessagingStyle.json`
- `com.tencent.mobileqq_QQ_意图重整.json`
- `2_后置配置_直接打开意图-白名单.json`，并将 `com.tencent.mobileqq` 加入白名单

「直接打开意图」会让新通知使用 Activity PendingIntent，可避免通过 XMSF Service 间接打开 QQ 时触发 SystemUI 启动动画超时。配置变更只影响之后创建的新通知。

## 使用边界

- Pixel SystemUI 适配依赖 Android 16 的内部实现，系统更新后可能需要重新验证。
- 模块提供环境伪装和跨包通知发布，PixelMiPushFramework 负责共享推送服务。QQ 服务端决定何时切换到厂商推送。
- 并非所有应用都提供可用的 MiPush 注册和离线推送能力。
- PixelMiPushFramework、XMSF 和 LSPosed 不应被后台管理工具冻结或限制网络。
- `master` 是唯一开发主线，仅维护 Pixel 版。通用版和 Legacy 版不再保留开发分支，历史源码通过 `archive/*` 标签及原有 Release 标签查阅。

## 构建

仓库使用 GitHub Actions、JDK 17 和 Android SDK 34 构建 APK。进入 **Actions → Build installable APK → Run workflow**，选择 `master` 分支并按需填写 `version_name` 和 `version_code`。

未配置签名 Secrets 时，工作流使用临时 Debug 签名，不能覆盖正式 Release。正式 Release 使用长期签名，并由 CI 校验包名、Xposed API 102 声明、APK 签名以及 DEX 中不存在 Legacy Xposed API 依赖。标签发布必须配置长期签名，不能使用临时 Debug 密钥。发布流程与产物命名见 [发布约定](docs/RELEASING.md)。

## 来源与许可证

本项目 Fork 自 [NihilityT/MiPush](https://github.com/NihilityT/MiPush)，原项目又派生自 [fei-ke/HMSPush](https://github.com/fei-ke/HMSPush)。这些代码继续保留 [GNU General Public License v3](LICENSE.MiPush-GPL-3.0) 的版权和授权声明。

设备属性伪装中的部分 `android.os.SystemProperties` Hook 和 MIUI 属性值改编自 [yin-ol/MiPushFaker](https://github.com/yin-ol/MiPushFaker)，对应代码遵循 [GNU Affero General Public License v3](LICENSE.MiPushFaker-AGPL-3.0)。

组合发行版本遵循仓库根目录中的 [GNU AGPL v3](LICENSE)。原有 GPLv3 代码继续保留其许可证、版权和来源说明，并依据 GPLv3 第 13 节与 AGPLv3 代码组合分发。详细来源、文件范围和署名参见 [NOTICE](NOTICE)。

本项目与上述上游作者不存在背书关系。
