package com.my.televip.reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reflection helpers that used to come from {@code de.robv.android.xposed.XposedHelpers}.
 *
 * <p>The legacy helper class only exists inside a process when the framework decided to inject the
 * legacy Xposed bridge. A module that is loaded through the modern libxposed API (100/102) — which
 * is how LSPosed 1.10+/Vector load us now — gets no {@code de.robv.*} classes at all, so relying on
 * {@code XposedHelpers} for plain reflection would blow up with {@code NoClassDefFoundError}.</p>
 *
 * <p>Nothing here touches the Xposed framework: it is ordinary JVM reflection with the same
 * signatures, the same best-match argument resolution and the same unchecked-error behaviour the
 * call sites were written against.</p>
 */
public final class XReflect {

    private XReflect() {
    }

    private static final Map<String, Object> CACHE = new ConcurrentHashMap<>();
    /** Sentinel stored in {@link #CACHE} for lookups that are known to fail. */
    private static final Object MISS = new Object();

    private static final Map<Class<?>, Class<?>> BOXED = new HashMap<>();

    static {
        BOXED.put(boolean.class, Boolean.class);
        BOXED.put(byte.class, Byte.class);
        BOXED.put(char.class, Character.class);
        BOXED.put(short.class, Short.class);
        BOXED.put(int.class, Integer.class);
        BOXED.put(long.class, Long.class);
        BOXED.put(float.class, Float.class);
        BOXED.put(double.class, Double.class);
    }

    // ---------------------------------------------------------------- classes

    public static Class<?> findClass(String className, ClassLoader classLoader) {
        Class<?> clazz = findClassIfExists(className, classLoader);
        if (clazz == null) {
            throw new ClassNotFoundError("Class not found: " + className);
        }
        return clazz;
    }

    public static Class<?> findClassIfExists(String className, ClassLoader classLoader) {
        if (className == null) return null;
        ClassLoader loader = classLoader != null ? classLoader : XReflect.class.getClassLoader();
        String key = "c:" + System.identityHashCode(loader) + ":" + className;
        Object cached = CACHE.get(key);
        if (cached == MISS) return null;
        if (cached != null) return (Class<?>) cached;
        try {
            Class<?> clazz = Class.forName(className, false, loader);
            CACHE.put(key, clazz);
            return clazz;
        } catch (Throwable ignored) {
            CACHE.put(key, MISS);
            return null;
        }
    }

    /**
     * Resolves a mixed array of {@link Class} objects and fully-qualified class names into
     * parameter types, exactly like the legacy helpers accepted.
     */
    public static Class<?>[] resolveParameterTypes(ClassLoader classLoader, Object... specs) {
        Class<?>[] types = new Class<?>[specs == null ? 0 : specs.length];
        for (int i = 0; i < types.length; i++) {
            Object spec = specs[i];
            if (spec == null || spec instanceof Class) {
                // A null spec means the call site resolved this parameter type against the client
                // and came back empty, so the class has been renamed or removed. Keeping the
                // position null marks it as a wildcard for findMethodCompatibleIfExists instead of
                // failing the whole hook here, which is all that used to happen.
                types[i] = (Class<?>) spec;
            } else if (spec instanceof String) {
                types[i] = findClassIfExists((String) spec, classLoader);
            } else {
                throw new IllegalArgumentException(
                        "Parameter type must be a Class or a class name, got: " + spec);
            }
        }
        return types;
    }

    // ---------------------------------------------------------------- methods

    public static Method findMethodExactIfExists(Class<?> clazz, String name, Class<?>... parameterTypes) {
        if (clazz == null || name == null) return null;
        // A null entry means the call site asked for a parameter type that no longer resolves.
        // getDeclaredMethod cannot match that, so skip straight to the tolerant lookup.
        if (hasUnresolvedType(parameterTypes)) return null;
        for (Class<?> current = clazz; current != null; current = current.getSuperclass()) {
            try {
                Method method = current.getDeclaredMethod(name, parameterTypes);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
                // keep walking up
            }
        }
        return null;
    }

    /**
     * Finds a method whose signature has drifted since the call site was written.
     *
     * <p>Telegram is not obfuscated, so hooks address it by plain name — but a method keeping its
     * name while gaining a parameter, or having a parameter type renamed, is routine across client
     * releases and defeats the exact lookup above. This is the fallback for that case, and it is
     * only ever consulted once the exact match has already failed, so it cannot change the
     * behaviour of a hook that still resolves normally.</p>
     *
     * <p>It refuses to guess. A candidate is accepted only when it is the single possibility:</p>
     * <ul>
     *   <li>No parameters requested — the call site was written against a no-argument method, so
     *       its callback cannot be reading {@code param.args} and any signature is safe to take.
     *       Accepted only when exactly one method carries the name.</li>
     *   <li>Parameters requested — the arity must still match, so the argument positions the
     *       callback indexes stay aligned. Types are compared where the call site could still
     *       resolve them; a {@code null} entry is treated as a wildcard. Accepted only when
     *       exactly one candidate fits.</li>
     * </ul>
     *
     * @return the drifted method, or {@code null} when there is no unambiguous answer
     */
    public static Method findMethodCompatibleIfExists(Class<?> clazz, String name,
                                                      Class<?>... parameterTypes) {
        if (clazz == null || name == null) return null;

        List<Method> named = findMethodsNamed(clazz, name);
        if (named.isEmpty()) return null;

        int requested = parameterTypes == null ? 0 : parameterTypes.length;
        if (requested == 0) {
            return named.size() == 1 ? accessible(named.get(0)) : null;
        }

        Method match = null;
        for (Method candidate : named) {
            Class<?>[] actual = candidate.getParameterTypes();
            if (actual.length != requested) continue;
            boolean fits = true;
            for (int i = 0; i < requested; i++) {
                Class<?> wanted = parameterTypes[i];
                if (wanted != null && !wanted.equals(actual[i])) {
                    fits = false;
                    break;
                }
            }
            if (!fits) continue;
            if (match != null) return null; // more than one fits, so any pick would be a guess
            match = candidate;
        }
        return match == null ? null : accessible(match);
    }

    /**
     * Every method with this name in the hierarchy, most-derived first, one entry per signature so
     * an override does not read as a second candidate.
     */
    private static List<Method> findMethodsNamed(Class<?> clazz, String name) {
        List<Method> found = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Class<?> current = clazz; current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (!method.getName().equals(name)) continue;
                if (seen.add(Arrays.toString(method.getParameterTypes()))) {
                    found.add(method);
                }
            }
        }
        return found;
    }

    private static boolean hasUnresolvedType(Class<?>[] parameterTypes) {
        if (parameterTypes == null) return false;
        for (Class<?> parameterType : parameterTypes) {
            if (parameterType == null) return true;
        }
        return false;
    }

    private static Method accessible(Method method) {
        method.setAccessible(true);
        return method;
    }

    /**
     * Picks the most specific method callable with {@code args}. Mirrors the legacy
     * {@code findMethodBestMatch} semantics: exact runtime types win, then widening / boxing /
     * {@code null}-compatible candidates ranked by specificity.
     */
    public static Method findMethodBestMatch(Class<?> clazz, String name, Object... args) {
        Class<?>[] argTypes = argumentTypes(args);

        Method exact = findMethodExactIfExists(clazz, name, argTypes);
        if (exact != null) return exact;

        Method best = null;
        for (Class<?> current = clazz; current != null; current = current.getSuperclass()) {
            best = bestOf(best, current.getDeclaredMethods(), name, argTypes);
            for (Class<?> iface : current.getInterfaces()) {
                best = bestOf(best, iface.getDeclaredMethods(), name, argTypes);
            }
        }
        if (best == null) {
            throw new NoSuchMethodError(descriptor(clazz, name, argTypes));
        }
        best.setAccessible(true);
        return best;
    }

    private static Method bestOf(Method best, Method[] candidates, String name, Class<?>[] argTypes) {
        for (Method candidate : candidates) {
            if (!candidate.getName().equals(name)) continue;
            if (!accepts(candidate.getParameterTypes(), argTypes)) continue;
            if (best == null || moreSpecific(candidate.getParameterTypes(), best.getParameterTypes())) {
                best = candidate;
            }
        }
        return best;
    }

    public static Constructor<?> findConstructorExact(Class<?> clazz, Class<?>... parameterTypes) {
        Constructor<?> constructor = findConstructorExactIfExists(clazz, parameterTypes);
        if (constructor == null) {
            throw new NoSuchMethodError(descriptor(clazz, "<init>", parameterTypes));
        }
        return constructor;
    }

    public static Constructor<?> findConstructorExactIfExists(Class<?> clazz, Class<?>... parameterTypes) {
        if (clazz == null) return null;
        try {
            Constructor<?> constructor = clazz.getDeclaredConstructor(parameterTypes);
            constructor.setAccessible(true);
            return constructor;
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    public static Constructor<?> findConstructorBestMatch(Class<?> clazz, Object... args) {
        Class<?>[] argTypes = argumentTypes(args);
        Constructor<?> exact = findConstructorExactIfExists(clazz, argTypes);
        if (exact != null) return exact;

        Constructor<?> best = null;
        for (Constructor<?> candidate : clazz.getDeclaredConstructors()) {
            if (!accepts(candidate.getParameterTypes(), argTypes)) continue;
            if (best == null || moreSpecific(candidate.getParameterTypes(), best.getParameterTypes())) {
                best = candidate;
            }
        }
        if (best == null) {
            throw new NoSuchMethodError(descriptor(clazz, "<init>", argTypes));
        }
        best.setAccessible(true);
        return best;
    }

    // ------------------------------------------------------------ invocation

    public static Object callMethod(Object obj, String methodName, Object... args) {
        if (obj == null) throw new NullPointerException("callMethod on null receiver: " + methodName);
        try {
            return findMethodBestMatch(obj.getClass(), methodName, args).invoke(obj, args);
        } catch (Throwable t) {
            throw unwrap(t);
        }
    }

    public static Object callStaticMethod(Class<?> clazz, String methodName, Object... args) {
        if (clazz == null) throw new NullPointerException("callStaticMethod on null class: " + methodName);
        try {
            return findMethodBestMatch(clazz, methodName, args).invoke(null, args);
        } catch (Throwable t) {
            throw unwrap(t);
        }
    }

    public static Object newInstance(Class<?> clazz, Object... args) {
        if (clazz == null) throw new NullPointerException("newInstance on null class");
        try {
            return findConstructorBestMatch(clazz, args).newInstance(args);
        } catch (Throwable t) {
            throw unwrap(t);
        }
    }

    // ----------------------------------------------------------------- fields

    public static Field findField(Class<?> clazz, String fieldName) {
        Field field = findFieldIfExists(clazz, fieldName);
        if (field == null) {
            throw new NoSuchFieldError((clazz == null ? "null" : clazz.getName()) + "#" + fieldName);
        }
        return field;
    }

    public static Field findFieldIfExists(Class<?> clazz, String fieldName) {
        if (clazz == null || fieldName == null) return null;
        String key = "f:" + clazz.getName() + "#" + fieldName + "@" + System.identityHashCode(clazz);
        Object cached = CACHE.get(key);
        if (cached == MISS) return null;
        if (cached != null) return (Field) cached;
        for (Class<?> current = clazz; current != null; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                CACHE.put(key, field);
                return field;
            } catch (NoSuchFieldException ignored) {
                // keep walking up
            }
        }
        CACHE.put(key, MISS);
        return null;
    }

    private static Field instanceField(Object obj, String name) {
        if (obj == null) throw new NullPointerException("Field access on null object: " + name);
        return findField(obj.getClass(), name);
    }

    public static Object getObjectField(Object obj, String fieldName) {
        try {
            return instanceField(obj, fieldName).get(obj);
        } catch (Throwable t) {
            throw unwrap(t);
        }
    }

    public static void setObjectField(Object obj, String fieldName, Object value) {
        try {
            instanceField(obj, fieldName).set(obj, value);
        } catch (Throwable t) {
            throw unwrap(t);
        }
    }

    public static boolean getBooleanField(Object obj, String fieldName) {
        try {
            return instanceField(obj, fieldName).getBoolean(obj);
        } catch (Throwable t) {
            throw unwrap(t);
        }
    }

    public static void setBooleanField(Object obj, String fieldName, boolean value) {
        try {
            instanceField(obj, fieldName).setBoolean(obj, value);
        } catch (Throwable t) {
            throw unwrap(t);
        }
    }

    public static int getIntField(Object obj, String fieldName) {
        try {
            return instanceField(obj, fieldName).getInt(obj);
        } catch (Throwable t) {
            throw unwrap(t);
        }
    }

    public static void setIntField(Object obj, String fieldName, int value) {
        try {
            instanceField(obj, fieldName).setInt(obj, value);
        } catch (Throwable t) {
            throw unwrap(t);
        }
    }

    public static long getLongField(Object obj, String fieldName) {
        try {
            return instanceField(obj, fieldName).getLong(obj);
        } catch (Throwable t) {
            throw unwrap(t);
        }
    }

    public static void setLongField(Object obj, String fieldName, long value) {
        try {
            instanceField(obj, fieldName).setLong(obj, value);
        } catch (Throwable t) {
            throw unwrap(t);
        }
    }

    public static float getFloatField(Object obj, String fieldName) {
        try {
            return instanceField(obj, fieldName).getFloat(obj);
        } catch (Throwable t) {
            throw unwrap(t);
        }
    }

    public static void setFloatField(Object obj, String fieldName, float value) {
        try {
            instanceField(obj, fieldName).setFloat(obj, value);
        } catch (Throwable t) {
            throw unwrap(t);
        }
    }

    public static Object getStaticObjectField(Class<?> clazz, String fieldName) {
        try {
            return findField(clazz, fieldName).get(null);
        } catch (Throwable t) {
            throw unwrap(t);
        }
    }

    public static void setStaticObjectField(Class<?> clazz, String fieldName, Object value) {
        try {
            findField(clazz, fieldName).set(null, value);
        } catch (Throwable t) {
            throw unwrap(t);
        }
    }

    public static boolean getStaticBooleanField(Class<?> clazz, String fieldName) {
        try {
            return findField(clazz, fieldName).getBoolean(null);
        } catch (Throwable t) {
            throw unwrap(t);
        }
    }

    public static void setStaticBooleanField(Class<?> clazz, String fieldName, boolean value) {
        try {
            findField(clazz, fieldName).setBoolean(null, value);
        } catch (Throwable t) {
            throw unwrap(t);
        }
    }

    public static int getStaticIntField(Class<?> clazz, String fieldName) {
        try {
            return findField(clazz, fieldName).getInt(null);
        } catch (Throwable t) {
            throw unwrap(t);
        }
    }

    public static void setStaticIntField(Class<?> clazz, String fieldName, int value) {
        try {
            findField(clazz, fieldName).setInt(null, value);
        } catch (Throwable t) {
            throw unwrap(t);
        }
    }

    public static long getStaticLongField(Class<?> clazz, String fieldName) {
        try {
            return findField(clazz, fieldName).getLong(null);
        } catch (Throwable t) {
            throw unwrap(t);
        }
    }

    public static void setStaticLongField(Class<?> clazz, String fieldName, long value) {
        try {
            findField(clazz, fieldName).setLong(null, value);
        } catch (Throwable t) {
            throw unwrap(t);
        }
    }

    // ---------------------------------------------------------------- helpers

    private static Class<?>[] argumentTypes(Object[] args) {
        Class<?>[] types = new Class<?>[args == null ? 0 : args.length];
        for (int i = 0; i < types.length; i++) {
            types[i] = args[i] == null ? null : args[i].getClass();
        }
        return types;
    }

    /** {@code argTypes[i] == null} means the caller passed {@code null} for that argument. */
    private static boolean accepts(Class<?>[] parameterTypes, Class<?>[] argTypes) {
        if (parameterTypes.length != argTypes.length) return false;
        for (int i = 0; i < parameterTypes.length; i++) {
            if (!isCompatible(parameterTypes[i], argTypes[i])) return false;
        }
        return true;
    }

    private static boolean isCompatible(Class<?> parameterType, Class<?> argType) {
        if (argType == null) return !parameterType.isPrimitive();
        Class<?> target = parameterType.isPrimitive() ? BOXED.get(parameterType) : parameterType;
        Class<?> source = argType.isPrimitive() ? BOXED.get(argType) : argType;
        if (target.isAssignableFrom(source)) return true;
        return parameterType.isPrimitive() && widens(source, parameterType);
    }

    private static boolean widens(Class<?> boxedSource, Class<?> primitiveTarget) {
        if (boxedSource == Byte.class) {
            return primitiveTarget == short.class || primitiveTarget == int.class
                    || primitiveTarget == long.class || primitiveTarget == float.class
                    || primitiveTarget == double.class;
        }
        if (boxedSource == Short.class || boxedSource == Character.class) {
            return primitiveTarget == int.class || primitiveTarget == long.class
                    || primitiveTarget == float.class || primitiveTarget == double.class;
        }
        if (boxedSource == Integer.class) {
            return primitiveTarget == long.class || primitiveTarget == float.class
                    || primitiveTarget == double.class;
        }
        if (boxedSource == Long.class) {
            return primitiveTarget == float.class || primitiveTarget == double.class;
        }
        if (boxedSource == Float.class) {
            return primitiveTarget == double.class;
        }
        return false;
    }

    /** True when {@code candidate} is at least as specific as {@code current} in every position. */
    private static boolean moreSpecific(Class<?>[] candidate, Class<?>[] current) {
        for (int i = 0; i < candidate.length; i++) {
            if (candidate[i] == current[i]) continue;
            if (!isCompatible(current[i], candidate[i])) return false;
        }
        return true;
    }

    private static String descriptor(Class<?> clazz, String name, Class<?>[] parameterTypes) {
        StringBuilder sb = new StringBuilder();
        sb.append(clazz == null ? "null" : clazz.getName()).append('#').append(name).append('(');
        for (int i = 0; i < parameterTypes.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(parameterTypes[i] == null ? "null" : parameterTypes[i].getName());
        }
        return sb.append(')').toString();
    }

    private static RuntimeException unwrap(Throwable t) {
        Throwable cause = t instanceof java.lang.reflect.InvocationTargetException && t.getCause() != null
                ? t.getCause()
                : t;
        if (cause instanceof RuntimeException) return (RuntimeException) cause;
        if (cause instanceof Error) throw (Error) cause;
        return new InvocationError(cause);
    }

    public static boolean isStatic(java.lang.reflect.Member member) {
        return member != null && Modifier.isStatic(member.getModifiers());
    }

    public static class ClassNotFoundError extends Error {
        public ClassNotFoundError(String message) {
            super(message);
        }
    }

    public static class InvocationError extends RuntimeException {
        public InvocationError(Throwable cause) {
            super(cause);
        }
    }
}
