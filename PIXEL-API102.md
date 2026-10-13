# Pixel Android 16 · Modern Xposed API 102

The `master` branch is the sole development line for the Pixel-specific API 102 edition. Historical General and Legacy sources are retained through `archive/*` tags and release tags, not development branches.

## Identity and update path

- Package for future releases: `com.neko7ina.pixelmipush`
- Historical package through v242: `com.neko7ina.mipush.pixel`
- Modern API dependency: `io.github.libxposed:api:102.0.0`
- Frozen predecessor: tag `archive/legacy-api82-pixel` at `110f396`

The new package installs separately even with the same long-term signing certificate. Disable the historical module, install and enable the new module, and reselect its LSPosed scopes. Keep only one MiPush edition enabled. Existing configuration under the old package is not migrated automatically.

## Recommended scope

- `system`
- `com.android.systemui`
- `com.xiaomi.xmsf`
- Selected target applications that need Xiaomi property spoofing

Do not enable another MiPush module edition in the same LSPosed scopes.

## Pixel-only SystemUI behavior

The hook activates only on Android 16 or newer and targets Pixel's concrete notification icon provider:

```text
com.android.systemui.statusbar.notification.row.icon.
NotificationRowIconViewInflaterFactory$createIconProvider$2.getIconType()
```

It is further restricted to:

```text
packageName=com.tencent.mobileqq
tag=mipush_com.tencent.mobileqq
```

For matching QQ MiPush conversation notifications it:

1. Selects `ICON_TYPE_SMALL_ICON` for the lower-right badge without rebuilding the notification.
2. Preserves `android.largeIcon`, `Person.icon`, contact avatars, and group avatars.
3. Changes only Android 16's 40dp conversation template to the tested Pixel 5-like geometry:
   - avatar: 48dp
   - badge: 20dp
   - badge position: 30dp
   - badge icon padding: 4dp
4. Fails safely when Pixel's implementation classes or fields do not match.

No fallback QQ icon is bundled or enabled. MiPushFramework remains the sole source of the notification `smallIcon`.

## Android 17 notification attribution

Android 17 removed the `NotificationManager.isSystemConditionProviderEnabled` wrapper used by the old readiness check. The module now probes the retained Binder method through the same service used for cross-package notification operations. Android 8.0 and Android 16 wrappers delegate to that same Binder method; this change does not add a new Android version requirement. Lower-version devices have not been retested.

The new package's signed API 102 test build was installed on Android 17. After the user configured scopes and rebooted, the readiness probe returned true and QQ conversation notifications belonged to `com.tencent.mobileqq`. Reposting a historical message exercised this path successfully. This checks notification publishing, not new server delivery or every notification operation.

The independent module release line starts at `v1.0.0`. Its internal versionCode remains monotonic and is maintained in the root build configuration.

The refreshed `v1.0.0` artifact targets Android 17 (SDK 37) and uses versionCode 245 to update the original code-244 artifact. compileSdk remains 34: no new platform API or toolchain is introduced. The device validation above used targetSdk 34 and does not establish targetSdk 37 runtime compatibility. The module declares no Activity, widget, or native library. Its target change does not change the target SDK of QQ or MiPushFramework processes where hooks run.

## Validation gates

The checks below describe the historical package. New-package Android 17 validation is recorded above.

- [x] Port the Pixel provider and geometry hooks from Legacy callbacks to API 102 interceptors.
- [x] Remove Legacy Xposed API references and entry declarations.
- [x] Keep the Pixel package identity and recommended SystemUI scope.
- [x] Build and inspect the signed Pixel API 102 APK.
- [x] Install with all other MiPush editions disabled in LSPosed.
- [x] Reboot and confirm the Pixel provider and geometry hooks install without errors.
- [x] Verify a QQ MiPush conversation notification retains its avatar and uses the custom monochrome `smallIcon` badge.
- [x] Verify the 48dp avatar and 4dp badge padding visually.
- [x] Verify genuine server-side delivery while QQ:MSF remains absent.

## Device validation

Validated on Pixel 10 Pro, Android 16 / API 36, with LSPosed 2.1.0 / API 102. The historical-notification test exercised both Pixel paths and logged:

```text
Adjusted QQ conversation avatar to 48dp and badge icon padding to 4dp
Using QQ small icon via Pixel NotificationIconProvider.getIconType
```

A separate live test received `Api102 pixel 测试` from the Xiaomi server while Thanox kept `com.tencent.mobileqq:MSF` absent. XMSF created the QQ conversation channel and published the notification as `com.tencent.mobileqq` with `opPkg=android`; the 68x68 small icon, 100x100 large icon, valid conversation shortcut, and `MessagingStyle` payload remained intact.
