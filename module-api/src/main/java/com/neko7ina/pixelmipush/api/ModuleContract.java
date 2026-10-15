package com.neko7ina.pixelmipush.api;

/** Wire contract shared by the module, injected processes and the framework app. */
public final class ModuleContract {
    private ModuleContract() {}

    public static final String MODULE_PACKAGE = "com.neko7ina.pixelmipush";
    public static final String SYSTEM_UI_PACKAGE = "com.android.systemui";
    public static final String QQ_PACKAGE = "com.tencent.mobileqq";
    public static final String QQ_NOTIFICATION_TAG = "mipush_" + QQ_PACKAGE;
    public static final String AUTHORITY = MODULE_PACKAGE + ".control";
    public static final String URI = "content://" + AUTHORITY;
    public static final int VERSION = 1;
    public static final String API_VERSION = "apiVersion";
    public static final String MODULE_UID = "moduleUid";
    public static final String STATUS = "status";
    public static final String REFRESH = "refresh";
    public static final String REGISTER = "register";
    public static final String SET = "set";
    public static final String APPLIED = "applied";
    public static final String CONFIG_REVISION = "configRevision";
    public static final String REFRESH_ACTION = MODULE_PACKAGE + ".REFRESH_RUNTIME";
    public static final String ENDPOINT = "endpoint";
    public static final String QQ_BADGE = "qqBadge";
    public static final String QQ_GEOMETRY = "qqGeometry";
    public static final String QQ_SUMMARY = "qqSummary";
    public static final String[] OPTIONS = {QQ_BADGE, QQ_GEOMETRY, QQ_SUMMARY};

    public static final String XMSF_LOADED = "xmsfLoaded";
    public static final String SYSTEM_READY = "systemReady";
    public static final String BADGE_READY = "badgeReady";
    public static final String GEOMETRY_READY = "geometryReady";
    public static final String XMSF_CONFIG_READY = "xmsfConfigReady";
    public static final String SYSTEM_UI_CONFIG_READY = "systemUiConfigReady";

    public static final String DESCRIPTOR = MODULE_PACKAGE + ".Runtime";
    public static final int PROBE = 1;
    public static final int BRIDGE_INSTALLED = 1;
    public static final int SYSTEM_HOOK_READY = 2;
    public static final int BADGE_INSTALLED = 4;
    public static final int GEOMETRY_INSTALLED = 8;
    public static final int CONFIG_LOADED = 16;
}
