package one.yufz.hmspush.hook.system

import android.content.Context
import android.os.Binder
import one.yufz.hmspush.hook.modern.ProcessContext
import one.yufz.hmspush.common.IS_SYSTEM_HOOK_READY
import one.yufz.hmspush.hook.XLog
import one.yufz.xposed.callMethod
import one.yufz.xposed.findClass
import one.yufz.xposed.get
import one.yufz.xposed.hookMethod

class HookSystemService {
    companion object {
        private const val TAG = "HookSystemService"

    }

    fun hook(classLoader: ClassLoader) {
        val classNotificationManagerService = classLoader.findClass("com.android.server.notification.NotificationManagerService")

        classNotificationManagerService.hookMethod("onStart") {
            doAfter {
                XLog.d(TAG, "onStart invoked")
                val context = thisObject.callMethod("getContext") as Context
                ProcessContext.attach(context)
                //KeepHmsAlive(context).start()
                val stubClass = thisObject.get<Any>("mService").javaClass
                hookPermission(stubClass)
                hookSystemReadyFlag(stubClass)
            }
        }

        //private boolean isPackageSuspendedForUser(String pkg, int uid)
        classNotificationManagerService.hookMethod("isPackageSuspendedForUser", String::class.java, Int::class.java) {
            doBefore {
                if (Binder.getCallingUid() == 1000) {
                    //suspend app can not show notification, fake its state
                    result = false
                }
            }
        }


        val classShortcutService = classLoader.findClass("com.android.server.pm.ShortcutService")
        ShortcutPermissionHooker.hook(classShortcutService)
    }

    private fun hookSystemReadyFlag(stubClass: Class<Any>) {
        stubClass.hookMethod("isSystemConditionProviderEnabled", String::class.java) {
            doBefore {
                if (args[0] == IS_SYSTEM_HOOK_READY) {
                    result = true
                }
            }
        }
    }

    private fun hookPermission(stubClass: Class<Any>) {
        NmsPermissionHooker.hook(stubClass)
    }
}