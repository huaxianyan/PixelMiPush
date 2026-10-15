package one.yufz.hmspush.hook.hms

import one.yufz.xposed.*

class HookHMS {
    companion object {
        private const val TAG = "HookHMS"
    }

    fun hook(classLoader: ClassLoader): Boolean {
        if (!HookPushNC.canHook(classLoader)) return false
        HookPushNC.hook(classLoader)
        return true
    }

}
