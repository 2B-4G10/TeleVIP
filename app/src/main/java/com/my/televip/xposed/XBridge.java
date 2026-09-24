package com.my.televip.xposed;

import android.util.Log;

import com.my.televip.base.AbstractMethodHook;
import com.my.televip.diagnostics.HookHealth;
import com.my.televip.reflect.XReflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * The single seam between TeleVip and the Xposed implementation hosting it.
 *
 * <p>The only supported backend is {@link ModernBackend} — the libxposed API 102 interceptor
 * model used by LSPosed 1.10+ and Vector 2.2 (JingMatrix), which is what runs under Zygisk Next
 * and NeoZygisk. The classic {@code de.robv.android.xposed} API 93 backend was removed; this
 * module no longer references {@code de.robv.*} anywhere and will not load on EdXposed, LSPatch
 * or LSPosed 1.9.x.</p>
 *
 * <p>The {@link Backend} indirection is kept deliberately: it is the seam that let the legacy API
 * be deleted without touching a single feature class, and it is what a future API revision would
 * plug into. Exactly one backend is installed per process and the first one to arrive wins.</p>
 */
public final class XBridge {

    private XBridge() {
    }

    public interface Backend {
        String id();

        void log(String message);

        void log(Throwable throwable);

        void hook(Member member, AbstractMethodHook callback);

        boolean deoptimize(Member member);

        String modulePath();

        String frameworkName();

        String frameworkVersion();
    }

    private static final String TAG = "TeleVip";

    private static volatile Backend backend;
    private static volatile String cachedModulePath;

    /** Installs a backend. The first backend to arrive wins; later calls are ignored. */
    public static synchronized boolean install(Backend candidate) {
        if (candidate == null) return false;
        if (backend != null) {
            if (!backend.id().equals(candidate.id())) {
                backend.log("[TeleVip] ignoring second backend '" + candidate.id()
                        + "', already running on '" + backend.id() + "'");
            }
            return false;
        }
        backend = candidate;
        return true;
    }

    public static boolean isInstalled() {
        return backend != null;
    }

    public static String backendId() {
        Backend current = backend;
        return current == null ? "none" : current.id();
    }

    public static String frameworkName() {
        Backend current = backend;
        return current == null ? "unknown" : current.frameworkName();
    }

    public static String frameworkVersion() {
        Backend current = backend;
        return current == null ? "unknown" : current.frameworkVersion();
    }

    // -------------------------------------------------------------------- log

    public static void log(String message) {
        Backend current = backend;
        if (current != null) {
            current.log(message);
        } else {
            Log.i(TAG, message);
        }
    }

    public static void log(Throwable throwable) {
        Backend current = backend;
        if (current != null) {
            current.log(throwable);
        } else {
            Log.e(TAG, "error", throwable);
        }
    }

    // ------------------------------------------------------------- module apk

    /**
     * Absolute path of the module APK.
     *
     * <p>The legacy path came from {@code initZygote}, which the modern API does not have at all —
     * and which is unreliable under Zygisk Next when the module is scoped to apps only. The value is
     * therefore taken from the backend when it can supply one, and otherwise recovered from our own
     * class loader.</p>
     */
    public static String modulePath() {
        String cached = cachedModulePath;
        if (cached != null) return cached;

        Backend current = backend;
        String path = current == null ? null : current.modulePath();
        if (path == null || path.isEmpty()) {
            path = deriveModulePath();
        }
        cachedModulePath = path;
        return path;
    }

    public static void setModulePath(String path) {
        if (path != null && !path.isEmpty()) {
            cachedModulePath = path;
        }
    }

    private static String deriveModulePath() {
        try {
            java.security.CodeSource source =
                    XBridge.class.getProtectionDomain().getCodeSource();
            if (source != null && source.getLocation() != null) {
                String location = source.getLocation().getPath();
                if (location != null && !location.isEmpty()) return location;
            }
        } catch (Throwable ignored) {
        }
        try {
            // dalvik.system.BaseDexClassLoader#toString() embeds the dex paths.
            String description = String.valueOf(XBridge.class.getClassLoader());
            int start = description.indexOf("/data/");
            if (start < 0) start = description.indexOf("/system/");
            if (start >= 0) {
                int end = description.indexOf(".apk", start);
                if (end > start) return description.substring(start, end + 4);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    // ------------------------------------------------------------------ hooks

    public static void hook(Member member, AbstractMethodHook callback) {
        Backend current = backend;
        if (member == null || callback == null) return;
        if (current == null) {
            Log.w(TAG, "no Xposed backend installed, skipping hook on " + member);
            return;
        }
        current.hook(member, callback);
    }

    public static boolean deoptimize(Member member) {
        Backend current = backend;
        return current != null && member != null && current.deoptimize(member);
    }

    /**
     * Legacy-compatible convenience: {@code parameterTypesAndCallback} is a list of parameter types
     * (as {@link Class} or class-name {@link String}) terminated by the {@link AbstractMethodHook}.
     */
    public static void findAndHookMethod(Class<?> clazz, String methodName,
                                         Object... parameterTypesAndCallback) {
        if (clazz == null || methodName == null) return;
        AbstractMethodHook callback = takeCallback(parameterTypesAndCallback);
        Class<?>[] parameterTypes = XReflect.resolveParameterTypes(
                clazz.getClassLoader(), dropLast(parameterTypesAndCallback));

        Method method = XReflect.findMethodExactIfExists(clazz, methodName, parameterTypes);
        if (method != null) {
            HookHealth.resolved();
        } else {
            // The exact signature is gone. Telegram renames and reshapes freely between releases,
            // so before giving the feature up entirely, see whether the method is still there
            // under the same name with a signature the call site can safely be attached to.
            method = XReflect.findMethodCompatibleIfExists(clazz, methodName, parameterTypes);
            String symbol = clazz.getSimpleName() + "#" + methodName;
            if (method == null) {
                HookHealth.missingMember(symbol);
                throw new NoSuchMethodError(symbol + " not found in " + clazz.getName());
            }
            HookHealth.drifted(symbol);
            log("[TeleVip] signature drift, hooking anyway: " + symbol + " is now " + method);
        }
        hook(method, callback);
    }

    public static void findAndHookConstructor(Class<?> clazz, Object... parameterTypesAndCallback) {
        if (clazz == null) return;
        AbstractMethodHook callback = takeCallback(parameterTypesAndCallback);
        Class<?>[] parameterTypes = XReflect.resolveParameterTypes(
                clazz.getClassLoader(), dropLast(parameterTypesAndCallback));
        Constructor<?> constructor = XReflect.findConstructorExact(clazz, parameterTypes);
        hook(constructor, callback);
    }

    private static AbstractMethodHook takeCallback(Object[] args) {
        if (args == null || args.length == 0
                || !(args[args.length - 1] instanceof AbstractMethodHook)) {
            throw new IllegalArgumentException(
                    "The last argument must be an AbstractMethodHook, got: "
                            + Arrays.toString(args));
        }
        return (AbstractMethodHook) args[args.length - 1];
    }

    private static Object[] dropLast(Object[] args) {
        return Arrays.copyOf(args, args.length - 1);
    }
}
