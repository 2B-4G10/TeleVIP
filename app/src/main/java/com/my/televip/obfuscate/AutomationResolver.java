package com.my.televip.obfuscate;


import com.my.televip.ClientChecker;
import com.my.televip.base.AbstractMethodHook;
import com.my.televip.obfuscate.struct.ResolverRegistry;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;



public class AutomationResolver {

    public static ResolverRegistry resolverRegistry = new ResolverRegistry(ResolverRegistry.getResolverClass());

    /** Method names that came out of an obfuscated client's mapping - R8's, not the source's. */
    private static final Set<String> OBFUSCATED_NAMES = Collections.synchronizedSet(new HashSet<String>());

    /**
     * True for a method name this resolver produced from an obfuscation mapping. Such names carry
     * no meaning - "d" exists in most classes - so nothing may treat a same-named method with a
     * different signature as the same method.
     */
    public static boolean isObfuscatedName(String methodName) {
        return methodName != null && OBFUSCATED_NAMES.contains(methodName);
    }

    public static String resolve(String className)
    {
        if (resolverRegistry.hasClass(className)) {
            return resolverRegistry.resolveClass(className);
        }

        return className;
    }

    public static Class<?>[] resolveObject(String name, Class<?>[] classes) {

        if (resolverRegistry.hasParameter(name)) {
            return resolverRegistry.resolveParameter(name);
        }

        return classes;
    }

    public static String resolve(String className, String name, ResolverType type) {

        if (type == ResolverType.Field && resolverRegistry.hasField(className, name)) {
            return resolverRegistry.resolveField(className, name);
        }
        String nameMethod = resolverRegistry.resolveMethodName(className, name);
        if (type == ResolverType.Method && resolverRegistry.hasMethod(className, nameMethod)) {
            String resolved = resolverRegistry.resolveMethod(className, nameMethod);
            if (resolved != null && !resolved.equals(name) && ClientChecker.isTgnetObfuscated()) {
                OBFUSCATED_NAMES.add(resolved);
            }
            return resolved;
        }

        name = name.replace("storyEntitiesAllowed2", "storyEntitiesAllowed");
        name = name.replace("hasStories2", "hasStories");

        return name;
    }


    public static Object[] merge(Class<?>[] classes, AbstractMethodHook hook)
    {
        if (classes != null) {
            Object[] result = new Object[classes.length + 1];
            System.arraycopy(classes, 0, result, 0, classes.length);
            result[classes.length] = hook;
            return result;
        }
        return null;
    }

    public enum ResolverType
    {
        Field,
        Method
    }
}
