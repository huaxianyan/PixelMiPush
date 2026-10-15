package one.yufz.hmspush.app

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.os.Parcel
import android.os.Process
import com.neko7ina.pixelmipush.api.ModuleContract as Api
import java.util.concurrent.ConcurrentHashMap
import one.yufz.hmspush.common.HMS_PACKAGE_NAME

/** Only the framework may change options; injected hosts may register their own live endpoint. */
class ModuleControlProvider : ContentProvider() {
    private val endpoints = ConcurrentHashMap<String, IBinder>()
    private val prefs by lazy { requireNotNull(context).getSharedPreferences("module_control", 0) }

    override fun onCreate() = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
        val ctx = requireNotNull(context)
        val uid = Binder.getCallingUid()
        val packages = ctx.packageManager.getPackagesForUid(uid).orEmpty()
        val framework = HMS_PACKAGE_NAME in packages
        val systemUi = Api.SYSTEM_UI_PACKAGE in packages
        val own = uid == Process.myUid()
        when (method) {
            Api.REGISTER -> {
                val host = when {
                    framework -> HMS_PACKAGE_NAME
                    systemUi -> Api.SYSTEM_UI_PACKAGE
                    else -> throw SecurityException("Only injected hosts may register")
                }
                endpoints[host] = requireNotNull(extras?.getBinder(Api.ENDPOINT))
                changed()
                return Bundle.EMPTY
            }
            Api.SET -> {
                checkCaller(framework)
                require(arg in Api.OPTIONS)
                requireNotNull(extras)
                require(extras.containsKey(requireNotNull(arg)))
                synchronized(prefs) {
                    check(prefs.edit().putBoolean(arg, extras.getBoolean(arg))
                        .putLong(Api.CONFIG_REVISION, prefs.getLong(Api.CONFIG_REVISION, 0L) + 1)
                        .commit()) { "Unable to save module options" }
                }
                changed()
            }
            Api.REFRESH -> {
                checkCaller(framework)
                // Read-only discovery also restores registrations after this provider restarts.
                listOf(HMS_PACKAGE_NAME, Api.SYSTEM_UI_PACKAGE).forEach { host ->
                    ctx.sendBroadcast(Intent(Api.REFRESH_ACTION).setPackage(host))
                }
            }
            Api.APPLIED -> {
                checkCaller(framework || systemUi)
                changed()
                return Bundle.EMPTY
            }
            Api.STATUS -> checkCaller(framework || systemUi || own)
            else -> throw IllegalArgumentException("Unknown module operation")
        }
        return snapshot()
    }

    private fun checkCaller(allowed: Boolean) {
        if (!allowed) throw SecurityException("Caller cannot access module settings")
    }

    private fun changed() {
        requireNotNull(context).contentResolver.notifyChange(Uri.parse(Api.URI), null)
    }

    private fun snapshot() = Bundle().apply {
        putInt(Api.API_VERSION, Api.VERSION)
        putInt(Api.MODULE_UID, Process.myUid())
        val revision = synchronized(prefs) {
            Api.OPTIONS.forEach { putBoolean(it, prefs.getBoolean(it, true)) }
            prefs.getLong(Api.CONFIG_REVISION, 0L).also { putLong(Api.CONFIG_REVISION, it) }
        }
        val (xmsf, xmsfRevision) = probe(HMS_PACKAGE_NAME)
        val (systemUi, systemUiRevision) = probe(Api.SYSTEM_UI_PACKAGE)
        putBoolean(Api.XMSF_LOADED, xmsf and Api.BRIDGE_INSTALLED != 0)
        putBoolean(Api.SYSTEM_READY, xmsf and Api.SYSTEM_HOOK_READY != 0)
        putBoolean(Api.BADGE_READY, systemUi and Api.BADGE_INSTALLED != 0)
        putBoolean(Api.GEOMETRY_READY, systemUi and Api.GEOMETRY_INSTALLED != 0)
        putBoolean(Api.XMSF_CONFIG_READY, xmsf and Api.CONFIG_LOADED != 0 && xmsfRevision == revision)
        putBoolean(Api.SYSTEM_UI_CONFIG_READY, systemUi and Api.CONFIG_LOADED != 0 && systemUiRevision == revision)
    }

    private fun probe(host: String): Pair<Int, Long> {
        val endpoint = endpoints[host] ?: return 0 to -1L
        val request = Parcel.obtain()
        val reply = Parcel.obtain()
        // Clear the original caller identity: runtime endpoints accept only the module's UID.
        val identity = Binder.clearCallingIdentity()
        return try {
            request.writeInterfaceToken(Api.DESCRIPTOR)
            if (!endpoint.transact(Api.PROBE, request, reply, 0)) return 0 to -1L
            reply.readException()
            reply.readInt() to reply.readLong()
        } catch (_: android.os.RemoteException) {
            endpoints.remove(host, endpoint)
            0 to -1L
        } finally {
            Binder.restoreCallingIdentity(identity)
            request.recycle()
            reply.recycle()
        }
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = throw UnsupportedOperationException()
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = throw UnsupportedOperationException()
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = throw UnsupportedOperationException()
}
