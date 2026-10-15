package one.yufz.hmspush.hook.systemui

import android.os.Build
import com.neko7ina.pixelmipush.api.ModuleContract as Api
import one.yufz.hmspush.hook.modern.ModuleControlRuntime
import android.service.notification.StatusBarNotification
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import one.yufz.hmspush.hook.XLog
import one.yufz.xposed.findClass
import one.yufz.xposed.getField
import one.yufz.xposed.hookAllMethods
import one.yufz.xposed.hookMethod
import java.util.concurrent.atomic.AtomicBoolean
import java.lang.ref.WeakReference
import java.util.WeakHashMap

/** Pixel Android 16-specific notification row icon adaptation. */
class HookPixelSystemUI {
    companion object {
        private const val TAG = "HookPixelSystemUI"
        private const val ICON_TYPE_SMALL_ICON = 0
        private const val REDESIGN_AVATAR_SIZE_DP = 40
        private const val PIXEL_5_AVATAR_SIZE_DP = 48
        private const val BADGE_SIZE_DP = 20
        private const val BADGE_PROTRUSION_DP = 2
        private const val BADGE_ICON_PADDING_DP = 4
    }

    private val originalLayouts = WeakHashMap<View, List<SavedLayout>>()

    private class SavedLayout(view: View) {
        private val reference = WeakReference(view)
        private val width = view.layoutParams.width
        private val height = view.layoutParams.height
        private val margins = (view.layoutParams as? ViewGroup.MarginLayoutParams)?.let {
            intArrayOf(it.leftMargin, it.topMargin, it.rightMargin, it.bottomMargin, it.marginStart, it.marginEnd)
        }

        fun restore() {
            val view = reference.get() ?: return
            view.layoutParams = view.layoutParams.apply {
                width = this@SavedLayout.width
                height = this@SavedLayout.height
                if (this is ViewGroup.MarginLayoutParams && margins != null) {
                    setMargins(margins[0], margins[1], margins[2], margins[3])
                    marginStart = margins[4]
                    marginEnd = margins[5]
                }
            }
        }
    }

    private val loggedQqBadgeOverride = AtomicBoolean(false)
    private val loggedQqGeometryAdjustment = AtomicBoolean(false)

    fun hook(classLoader: ClassLoader): Int {
        if (Build.VERSION.SDK_INT < 36) return 0
        var installed = 0
        try {
            val providerClass = classLoader.findClass(
                "com.android.systemui.statusbar.notification.row.icon." +
                    "NotificationRowIconViewInflaterFactory\$createIconProvider\$2"
            )
            providerClass.hookMethod("getIconType") {
                doBefore {
                    val sbn = findStatusBarNotification(thisObject)
                    if (ModuleControlRuntime.enabled(Api.QQ_BADGE) &&
                        sbn?.packageName == Api.QQ_PACKAGE &&
                        sbn.tag == Api.QQ_NOTIFICATION_TAG
                    ) {
                        result = ICON_TYPE_SMALL_ICON
                        if (loggedQqBadgeOverride.compareAndSet(false, true)) {
                            XLog.d(
                                TAG,
                                "Using QQ small icon via Pixel NotificationIconProvider.getIconType"
                            )
                        }
                    }
                }
            }
            installed = installed or Api.BADGE_INSTALLED
            XLog.d(TAG, "Installed Pixel Android 16 NotificationIconProvider.getIconType hook")

            val rowIconClass = classLoader.findClass(
                "com.android.internal.widget.NotificationRowIconView"
            )
            listOf("setImageIcon", "setImageIconAsync").forEach { methodName ->
                rowIconClass.hookAllMethods(methodName) {
                    doBefore {
                        val iconView = thisObject as? View ?: return@doBefore
                        synchronized(originalLayouts) { originalLayouts.remove(iconView) }
                            ?.forEach { it.restore() }
                        val provider = try {
                            thisObject.getField("mIconProvider", Any::class.java)
                        } catch (_: Throwable) {
                            null
                        } ?: return@doBefore
                        val sbn = findStatusBarNotification(provider)
                        if (ModuleControlRuntime.enabled(Api.QQ_GEOMETRY) &&
                            sbn?.packageName == Api.QQ_PACKAGE &&
                            sbn.tag == Api.QQ_NOTIFICATION_TAG
                        ) {
                            adjustConversationIconGeometry(iconView)
                        }
                    }
                }
            }
            installed = installed or Api.GEOMETRY_INSTALLED
            XLog.d(TAG, "Installed Pixel Android 16 QQ conversation icon geometry hooks")
        } catch (t: Throwable) {
            // This class is Pixel/SystemUI-build-specific. Never destabilize SystemUI on mismatch.
            XLog.e(TAG, "Unable to install Pixel Android 16 notification icon hook", t)
        }
        return installed
    }

    private fun adjustConversationIconGeometry(iconView: View) {
        val badge = iconView.parent as? View ?: return
        val iconGroup = badge.parent as? ViewGroup ?: return
        val resources = iconView.resources
        val avatarId = resources.getIdentifier("conversation_icon", "id", "android")
        val facePileId = resources.getIdentifier("conversation_face_pile", "id", "android")
        val avatarSize = dp(iconView, PIXEL_5_AVATAR_SIZE_DP)
        val redesignAvatarSize = dp(iconView, REDESIGN_AVATAR_SIZE_DP)
        val badgeSize = dp(iconView, BADGE_SIZE_DP)
        val badgePosition = avatarSize - badgeSize + dp(iconView, BADGE_PROTRUSION_DP)
        val badgeIconPadding = dp(iconView, BADGE_ICON_PADDING_DP)

        // Apply this only to Android 16's 2025 conversation layout. Its avatar is 40dp; the
        // legacy Pixel 5 layout already uses 48dp and must not be changed again.
        val avatars = (0 until iconGroup.childCount)
            .map(iconGroup::getChildAt)
            .filter { it.id == avatarId || it.id == facePileId }
        if (avatars.none { it.layoutParams?.width == redesignAvatarSize }) return

        synchronized(originalLayouts) {
            originalLayouts[iconView] = (avatars + badge + iconView).map(::SavedLayout)
        }
        avatars.forEach { avatar ->
            avatar.layoutParams = avatar.layoutParams.apply {
                width = avatarSize
                height = avatarSize
            }
        }

        (badge.layoutParams as? ViewGroup.MarginLayoutParams)?.let { params ->
            params.width = badgeSize
            params.height = badgeSize
            params.topMargin = badgePosition
            params.marginStart = badgePosition
            badge.layoutParams = params
        }
        (iconView.layoutParams as? ViewGroup.MarginLayoutParams)?.let { params ->
            params.setMargins(
                badgeIconPadding,
                badgeIconPadding,
                badgeIconPadding,
                badgeIconPadding
            )
            iconView.layoutParams = params
        }

        if (loggedQqGeometryAdjustment.compareAndSet(false, true)) {
            XLog.d(
                TAG,
                "Adjusted QQ conversation avatar to 48dp and badge icon padding to 4dp"
            )
        }
    }

    private fun dp(view: View, value: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            view.resources.displayMetrics
        ).toInt()

    private fun findStatusBarNotification(provider: Any): StatusBarNotification? {
        return provider.javaClass.declaredFields.firstNotNullOfOrNull { field ->
            if (!StatusBarNotification::class.java.isAssignableFrom(field.type)) {
                null
            } else {
                try {
                    field.isAccessible = true
                    field.get(provider) as? StatusBarNotification
                } catch (_: Throwable) {
                    null
                }
            }
        }
    }
}
