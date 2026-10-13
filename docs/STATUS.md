# Pixel MiPush 交接

## STATUS-001：包名迁移与发布改造

- 日期：2026-10-13，分支 `chore/package-release-standard`
- 后续 applicationId 为 `com.neko7ina.pixelmipush`，长期签名、API 102 和通知实现保持原样。历史包 `com.neko7ina.mipush.pixel` 的安装与推送记录不作为新包验收
- README 和 PIXEL-API102 补充首次安装迁移：关闭旧模块，启用新模块，重新设置作用域与模块私有配置，最后手动重启
- CI 添加正式标签发布：必需版本化说明，长期证书与 v2 签名检查，规范附件名、同名 SHA-256 和构建信息，标题仅标签，不覆盖历史 Release
- README 添加 Obtainium 图标入口，仅匹配新命名的正式版 APK。首次新包发布前没有匹配版本，旧包不自动迁移
- 验证：发布说明门禁 5 项回归、工作流语法与 Obtainium 解码／筛选通过。整理脚本以实际已签名 v242 APK 在临时 Git 仓库验证签名、包信息、复制字节和 SHA-256 文件，临时文件已清理
- 构建限制：尝试仓库 Wrapper 的 `:app:assembleRelease`，停在 Gradle 7.5 发行版下载超时，尚未进入源码编译。未替换 Wrapper、升级工具链或重复构建
- 边界：新包 APK 尚未验证，未推送、创建标签、公开发布、安装或重启。历史发布及附件保持原样
- 下一步：补齐新包构建／产物检查，确认首次版本与发布说明，再单独安排设备迁移。流程见 `RELEASING.md`

## STATUS-002：Android 17 通知归属修复

- 日期：2026-10-13，分支 `fix/android17-notification`，尚未提交。此前包名与发布改造的未提交文件保留在工作区
- 现场版本：Android 17（SDK 37），模块 v242。模块已注入系统服务与 SystemUI，但就绪探测报 `NotificationManager#isSystemConditionProviderEnabled` 方法不存在，QQ 通知记录的归属为 `com.xiaomi.xmsf`
- 系统文件核对：实际 `INotificationManager` 仍有 `isSystemConditionProviderEnabled(String)`，跨包发布、取消和渠道查询方法的签名与现有调用一致。接口存在不代表运行权限已验收
- 修改：把就绪探测移入 `SystemNotificationManager`，复用已有通知 Binder 服务。`HookPushNC` 使用该探测结果，系统端的就绪标记 Hook 保持不变。保留异常返回 false 和原有缓存行为，增加探测结果日志
- 静态验证：代码差异检查通过。仓库 Wrapper 所需的 Gradle 7.5 已下载到共享缓存，并通过官方 SHA-256 校验，未替换 Wrapper 或升级工具链
- 构建限制：离线 APK 构建缺少 AGP 7.4.1 缓存。随后缩小为 `:xposed:compileReleaseKotlin`，依赖下载因 Kotlin stdlib 响应截断失败。两次均未进入源码编译，本轮没有可交付测试 APK
- 验收边界：真实消息由用户提供，尚未验证修复后的通知归属和图标。未安装、重启、推送或发布
- 下一步：恢复依赖下载后完成编译与测试 APK 打包，再单独确认安装。以真实 QQ 推送验证通知归属 `com.tencent.mobileqq`，并检查头像、消息整合及图标。测试安装前需决定新包迁移或旧包原位测试，不能同时启用两份模块

## STATUS-003：测试包构建与安装准备

- 日期：2026-10-13，用户授权编译模块并安装到设备，真实消息由用户提供。公开发布与重启仍未授权
- 本轮构建参数：`versionName=2026.10.13-android17-test`、`versionCode=243`，当前包名为 `com.neko7ina.pixelmipush`。测试包尚未生成，这些参数不代表已发布或已安装版本
- 依赖下载已通过先前 Kotlin stdlib 的阻塞点，构建进入 Android SDK 检查。AGP 7.4.1 默认需要 Build Tools 30.0.3，共享 SDK 已有 34、35 和 36，30.0.3 尚未安装完整
- APK 构建等待 SDK 自动下载超过 5 分钟，已终止本轮 Wrapper 客户端及对应 Gradle 7.5 daemon。单独 SDK 安装与直接官方归档下载也超时，归档未通过校验，未解压为有效 SDK 包
- 本轮没有源码编译结果、签名测试包或设备安装。设备只读连接成功，旧模块保留原样
- 下一步：补齐共享 Build Tools 30.0.3，或明确选择已安装的 Build Tools 34.0.0 再继续构建。安装测试仍需处理新旧包并存及 LSPosed 作用域，避免同时启用

## STATUS-004：Android 17 测试模块已安装

- 日期：2026-10-13，用户授权通过本地代理重试下载，并在新包安装后卸载旧包。作用域和重启由用户处理
- 下载与构建：官方 Build Tools 30.0.3 归档通过代理分段续传，按官方 SHA-1 校验后安装到共享 SDK。仓库 Wrapper 的 `:app:assembleRelease` 成功，用时 1 分 43 秒，105 项任务执行。AGP、Wrapper 和目标 SDK 均保持不变
- 测试产物：`PixelMiPush-universal-2026.10.13-android17-test.apk`，applicationId `com.neko7ina.pixelmipush`，versionCode 243。使用既有长期证书签名，v2 签名、ZIP 对齐及 API 102 模块元数据检查通过，旧式 `assets/xposed_init` 入口不存在
- APK SHA-256：`deabf6e4a6956252aefbfee486472b3e54f6ef5d9657e2a6a65db3c1e1fbec31`
- 设备操作：新包安装成功，已读取安装包信息确认版本。随后旧包 `com.neko7ina.mipush.pixel` 卸载成功。未调整 LSPosed 作用域，未重启，未修改推送服务或 QQ 数据
- 此条更新 STATUS-002 和 STATUS-003 的构建阻塞与未安装状态。代码仍在 `fix/android17-notification`，尚未提交、推送或公开发布
- 下一步：用户启用新模块，设置系统框架、SystemUI、推送服务及所需接入应用的作用域，然后重启并产生真实 QQ 消息。检查就绪探测结果、通知归属、图标、头像和消息整合，当前尚未完成运行验收

## STATUS-005：Android 17 通知归属验收

- 日期：2026-10-13，用户已配置新模块作用域并重启。用户报告同一条历史消息修复前重新发送失败，修复后重新发送成功
- 设备证据：新包的 API 102 系统服务 Hook 已加载，就绪探测日志为 `System notification hook ready: true`。QQ 单聊及群聊的通知记录均归属 `com.tencent.mobileqq`，推送服务自身状态通知仍归属 `com.xiaomi.xmsf`
- 结论：本轮就绪探测及 QQ 跨包通知归属修复通过设备验收。历史消息重新发送可以验证此路径，撤销此前必须使用新消息的限定。此结果不单独证明新的服务器消息接收或所有通知操作均已回归
- 低版本核对：AOSP Android 8.0 与 Android 16 的原包装方法均转发到相同的 `INotificationManager.isSystemConditionProviderEnabled(String)`。新实现复用既有 Binder 服务，未引入 Android 17 专属 API，保留低版本调用语义。未在低版本设备重新运行测试
- 验收范围：通知归属有系统记录证据，显示效果由用户报告，未读取截图。未执行额外安装、卸载或重启，尚未提交、推送或发布

## STATUS-006：独立版本线首版发布准备

- 日期：2026-10-13，用户授权提交、推送此前 README 与发布改造，并发布模块正式版。模块版本线从 `v1.0.0` 开始，历史日期标签与附件保持原样
- 正式包 versionCode 为 244，高于测试包 243。原首父提交计数仅为 237，已改为根 `build.gradle` 显式维护递增版本号，构建参数仍可覆盖
- 发布内容：新包身份、Obtainium 入口、版本化发布说明与实际 APK 构建信息，以及已验收的 Android 17 通知归属修复。详细行为与验收边界见 `PIXEL-API102.md`
- 配套框架本轮只提交并推送 README、Obtainium 和后续发布流程改造，不发布新框架 APK
- 发布顺序：本地说明门禁、合入并推送主分支、等待 CI 成功，再推送注解标签触发正式签名构建。线上附件须核对名称、版本、长期签名与 SHA-256；本条不代表发布已完成
- 主分支首次 CI 在 SDK 初始化失败：`setup-android@v3` 默认请求已从仓库移除的 `tools` 包。已按该 Action 的正式输入显式选择 `platform-tools`，后续步骤仍安装项目所需平台与 Build Tools，未改变应用工具链
