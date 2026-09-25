package com.my.televip.xposed;

import android.app.Activity;
import android.os.Bundle;

import com.my.televip.ClientChecker;
import com.my.televip.Class.ClassLoad;
import com.my.televip.Class.ClassNames;
import com.my.televip.TeleVip;
import com.my.televip.base.AbstractMethodHook;
import com.my.televip.hooks.HMethod;
import com.my.televip.logging.Logger;
import com.my.televip.utils.Utils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The bootstrap the module entry point funnels into.
 *
 * <p>{@link TeleVipModule} ({@code META-INF/xposed/java_init.list}) calls {@link #attach} once per
 * target package. The {@link #ATTACHED} guard is kept because {@code onPackageReady} can legitimately
 * fire more than once for the same package — for example when a client spawns an extra process.</p>
 *
 * <p>Everything TeleVip does hangs off one hook: the first activity's {@code onCreate}. Nothing
 * else runs until it fires — not the features, not the settings screen, not even the hook health
 * report that would say why. So this bootstrap must not be able to fail quietly, and in particular
 * it must not depend on resolving a class name out of the client. On a fork whose build the
 * obfuscation tables were not generated against, {@code org.telegram.ui.LaunchActivity} may not
 * resolve, and the module would then attach, hook nothing, and look simply dead.</p>
 */
public final class ModuleEntry {

    private ModuleEntry() {
    }

    private static final Set<String> ATTACHED = Collections.synchronizedSet(new HashSet<String>());

    /**
     * Atomic rather than a plain boolean: the fallback below hooks {@link Activity} itself, so the
     * callback can run for several activities, and two of them can be created at once.
     */
    private static final AtomicBoolean STARTED = new AtomicBoolean();

    public static void attach(String packageName, ClassLoader classLoader) {
        try {
            if (packageName == null || classLoader == null) return;
            if (!ClientChecker.ClientType.containsPackage(packageName)) return;
            if (!ATTACHED.add(packageName)) return;

            Utils.pkgName = packageName;
            Utils.classLoader = classLoader;
            Utils.modulePath = XBridge.modulePath();

            Logger.l("attached to " + packageName
                    + " | backend=" + XBridge.backendId()
                    + " | framework=" + XBridge.frameworkName() + " " + XBridge.frameworkVersion()
                    + " | modulePath=" + Utils.modulePath);

            AbstractMethodHook bootstrap = new AbstractMethodHook() {
                @Override
                protected void beforeMethod(MethodHookParam param) {
                    if (!STARTED.compareAndSet(false, true)) return;
                    Activity activity = (Activity) param.thisObject;
                    ClientChecker.checkClientVersion(activity);
                    TeleVip.startHook(activity);
                }
            };

            Class<?> launchActivity = ClassLoad.getClass(ClassNames.LAUNCH_ACTIVITY);
            if (launchActivity != null) {
                HMethod.hookMethod(launchActivity, "onCreate", Bundle.class, bootstrap);
                return;
            }

            // The client does not have the class under the name we expect. Rather than give up -
            // which used to mean the module did nothing whatsoever and said nothing about it -
            // bootstrap off Activity#onCreate, which is a framework class and always resolves.
            // startHook only ever wanted a Context, so any activity in the target process will do,
            // and the guard above keeps it to the first one.
            Logger.w(ClassNames.LAUNCH_ACTIVITY + " did not resolve in " + packageName
                    + ", starting from the first activity instead. Features that depend on renamed"
                    + " symbols will still be inactive - see the hook health line below. "
                    + Utils.issue);
            HMethod.hookMethod(Activity.class, "onCreate", Bundle.class, bootstrap);

        } catch (Throwable throwable) {
            Logger.e(throwable);
        }
    }
}
