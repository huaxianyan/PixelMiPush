<div align="center">
  <img src="docs/images/icon.svg" width="120" alt="Pixel MiPush 应用图标">

# Pixel MiPush

[![Release](https://img.shields.io/github/v/release/huaxianyan/PixelMiPush)](https://github.com/huaxianyan/PixelMiPush/releases/latest)
[![Build](https://github.com/huaxianyan/PixelMiPush/actions/workflows/ci.yml/badge.svg?branch=master)](https://github.com/huaxianyan/PixelMiPush/actions/workflows/ci.yml)
[![License AGPL-3.0](https://img.shields.io/badge/license-AGPLv3.0-blue.svg)](LICENSE)

</div>

面向 Pixel 的小米推送增强模块，配合 [Pixel MiPushFramework](https://github.com/huaxianyan/PixelMiPushFramework) 使用，提供设备识别适配、系统通知增强和 QQ 通知定制。

框架负责接收消息、记录与通知配置，模块提供需要 LSPosed 的系统和应用增强。模块启用与作用域在 LSPosed 中管理，个性化选项在配套框架中设置。

> 社区维护的非官方项目，以 Pixel 的 Android 16 和 Android 17 为主要适配环境。其他系统的通知模板和增强效果需要自行验证。

## 特性

- 设备识别适配：在所选应用中提供小米设备相关属性，帮助支持小米推送的应用识别接入环境。
- 系统通知增强：让框架代发的通知归属到对应应用，便于在系统设置中按应用管理通知。
- QQ 通知定制：保留联系人和群头像，提供单色通知角标、会话头像布局和汇总副标题清理，三个选项独立控制。
- 框架内管理：查看模块加载、系统通知增强和设置同步状态，按个性化项目汇总就绪数量，关闭选项不计为故障。

具体版本的功能和验证范围见 [Release 说明](https://github.com/huaxianyan/PixelMiPush/releases)，通知适配原理见 [技术说明](PIXEL-API102.md)。

## 快速开始

### 下载安装

需要 Root、支持 Xposed API 102 的 LSPosed，以及已完成初始化的 Pixel MiPushFramework。

**使用历史旧包的用户，先阅读 [旧包迁移](docs/USAGE.md#旧包迁移)。** 同一作用域只启用一个 MiPush 模块版本。

1. 安装并初始化 [Pixel MiPushFramework](https://github.com/huaxianyan/PixelMiPushFramework/releases/latest)，通常选择 `normal` 版本。
2. 从 [最新正式版](https://github.com/huaxianyan/PixelMiPush/releases/latest) 下载模块 APK，在 LSPosed 中启用 Pixel MiPush。
3. 选择以下作用域，并按需要加入目标应用：

   ```text
   系统框架
   系统界面（com.android.systemui）
   推送服务（com.xiaomi.xmsf）
   需要设备识别适配的目标应用
   ```

4. 重启设备，使所选作用域加载模块。
5. 打开目标应用完成小米推送注册，再用一条真实消息检查接收与通知效果。

### 通过 Obtainium 更新

先安装 [Obtainium](https://obtainium.imranr.dev/)，再点击下方入口导入更新源。默认跟踪本项目采用新包名的正式版 APK，旧包迁移需先按使用说明完成。

[<img src="images/obtainium-badge.png" alt="通过 Obtainium 获取 Pixel MiPush" height="48">][obtainium]

## 使用与配置

在框架的「设置 → 模块设置」中检查运行状态，再从个性化设置分组进入「QQ 通知定制」，按需调整角标、会话头像和汇总通知选项。

通知标题、渠道、分组、点击行为和会话样式由框架规则配置。模块处理对应的设备识别与通知增强，规则选择和 QQ 配置示例见 [使用说明](docs/USAGE.md#通知规则)。

状态就绪表示相关增强已加载、设置已同步。应用是否接入小米推送、服务端何时发送消息，还需结合框架中的注册与消息记录判断。

遇到样式或接收问题时，先检查作用域、重启后的加载状态、框架连接和目标应用注册。系统更新后的通知适配限制见 [兼容与排查](docs/USAGE.md#兼容与排查)。

## 与上游的区别

本项目源自 [NihilityT/MiPush](https://github.com/NihilityT/MiPush)，继续维护 Pixel 所需的设备识别和通知增强，并适配 Xposed API 102、Android 17 通知归属及 Pixel 会话通知模板。

模块与配套框架共享设置协议，支持运行状态检查和 QQ 独立选项。框架管理共享推送服务及用户设置，模块分别在系统和目标应用中提供增强。

包名为 `com.neko7ina.pixelmipush`，使用独立版本线、长期签名和更新源。历史通用版、Legacy 版及旧包的说明与验收结果，按对应标签和技术记录查阅。

## 文档

- [使用与兼容说明](docs/USAGE.md)：旧包迁移、QQ 规则、常见问题和源码构建。
- [模块状态与个性化设置](docs/MODULE-CONTROL.md)：状态含义、QQ 设置入口与共享协议。
- [Pixel 通知适配](PIXEL-API102.md)：通知实现、平台适配和历史验证边界。
- [发布约定](docs/RELEASING.md)：版本、长期签名与正式附件规则。
- [开发记录](docs/STATUS.md)：功能进度、验证证据与未完成事项。
- [来源与署名](NOTICE)：上游项目、改编文件及许可证范围。

## 许可证

组合发行版本遵循 [GNU Affero General Public License v3](LICENSE)。继承自 MiPush 和 HMSPush 的代码保留 [GPLv3](LICENSE.MiPush-GPL-3.0) 声明，设备属性适配中的 MiPushFaker 改编代码保留 [AGPLv3](LICENSE.MiPushFaker-AGPL-3.0) 声明。

原项目来源、版权与授权范围见 [NOTICE](NOTICE)。本项目独立维护，与上游作者不存在背书关系。

[obtainium]: https://apps.obtainium.imranr.dev/redirect?r=obtainium%3A%2F%2Fapp%2F%257B%2522id%2522%253A%2522com.neko7ina.pixelmipush%2522%252C%2522url%2522%253A%2522https%253A%2F%2Fgithub.com%2Fhuaxianyan%2FPixelMiPush%2522%252C%2522author%2522%253A%2522huaxianyan%2522%252C%2522name%2522%253A%2522Pixel%2520MiPush%2522%252C%2522additionalSettings%2522%253A%2522%257B%255C%2522apkFilterRegEx%255C%2522%253A%255C%2522%255EPixelMiPush-universal-.%252A%255C%255C%255C%255C.apk%2524%255C%2522%252C%255C%2522includePrereleases%255C%2522%253Afalse%252C%255C%2522versionDetection%255C%2522%253Atrue%252C%255C%2522appName%255C%2522%253A%255C%2522Pixel%2520MiPush%255C%2522%257D%2522%257D
