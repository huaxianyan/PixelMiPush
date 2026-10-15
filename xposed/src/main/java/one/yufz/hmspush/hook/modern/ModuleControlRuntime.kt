package one.yufz.hmspush.hook.modern

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Parcel
import com.neko7ina.pixelmipush.api.ModuleContract as Api
import one.yufz.hmspush.hook.XLog
import java.util.concurrent.atomic.AtomicInteger

/** One runtime per injected host. Options are cached, not fetched while drawing notifications. */
object ModuleControlRuntime {
    @Volatile private var options = Bundle()
    @Volatile private var configLoaded = false

    fun enabled(key: String): Boolean = configLoaded && options.getBoolean(key, true)

    fun attach(context: Context, readiness: () -> Int) {
        val handler = Handler(HandlerThread("PixelMiPush-control").apply { start() }.looper)
        val uri = Uri.parse(Api.URI)
        val moduleUid = AtomicInteger(-1)
        val endpoint = object : Binder() {
            override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
                if (code != Api.PROBE) return super.onTransact(code, data, reply, flags)
                if (moduleUid.get() < 0 || getCallingUid() != moduleUid.get()) throw SecurityException("Only the module may probe")
                data.enforceInterface(Api.DESCRIPTOR)
                requireNotNull(reply).writeNoException()
                reply.writeInt(readiness() or if (configLoaded) Api.CONFIG_LOADED else 0)
                reply.writeLong(options.getLong(Api.CONFIG_REVISION, -1L))
                return true
            }
        }
        fun load(): Boolean {
            return try {
                val result = requireNotNull(context.contentResolver.call(uri, Api.STATUS, null, null))
                check(result.getInt(Api.API_VERSION) == Api.VERSION)
                moduleUid.set(result.getInt(Api.MODULE_UID, -1))
                val changed = !configLoaded || options.getLong(Api.CONFIG_REVISION, -1L) !=
                    result.getLong(Api.CONFIG_REVISION)
                options = result
                configLoaded = true
                if (changed) context.contentResolver.call(uri, Api.APPLIED, null, null)
                true
            } catch (error: Exception) {
                configLoaded = false
                XLog.e("ModuleControl", "Unable to read module options", error)
                false
            }
        }
        fun register() {
            if (!load()) return
            try {
                context.contentResolver.call(uri, Api.REGISTER, null, Bundle().apply {
                    putBinder(Api.ENDPOINT, endpoint)
                })
            } catch (error: Exception) {
                XLog.e("ModuleControl", "Unable to register runtime status", error)
            }
        }
        context.contentResolver.registerContentObserver(uri, false, object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) { load() }
        })
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Api.REFRESH_ACTION) handler.post { register() }
            }
        }
        // This receiver only refreshes our own registration; it accepts no configuration input.
        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(receiver, IntentFilter(Api.REFRESH_ACTION), Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, IntentFilter(Api.REFRESH_ACTION))
        }
        handler.post { register() }
    }
}
