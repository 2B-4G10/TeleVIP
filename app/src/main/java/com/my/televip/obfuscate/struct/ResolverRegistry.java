package com.my.televip.obfuscate.struct;

import com.my.televip.ClientChecker;
import com.my.televip.Class.ClassLoad;
import com.my.televip.obfuscate.RuntimeMappings;
import com.my.televip.obfuscate.resolve.Mapping;
import com.my.televip.utils.Utils;

public class ResolverRegistry {

    Class<?> finalField;
    Class<?> finalMethod;
    Class<?> finalParameter;
    Class<?> finalClass;
    Class<?> clazz;

    public ResolverRegistry(Class<?> clazz){
        this.clazz = clazz;
        try {
            Class<?> fieldResolverClass = null;
            Class<?> methodResolverClass = null;
            Class<?> parameterResolverClass = null;
            Class<?> classResolverClass = null;
            for (Class<?> inner : clazz.getDeclaredClasses()) {
                if (inner.getSimpleName().equals("FieldResolver")) fieldResolverClass = inner;
                if (inner.getSimpleName().equals("MethodResolver")) methodResolverClass = inner;
                if (inner.getSimpleName().equals("ParameterResolver")) parameterResolverClass = inner;
                if (inner.getSimpleName().equals("ClassResolver")) classResolverClass = inner;
            }

            finalField = fieldResolverClass;
            finalMethod = methodResolverClass;
            finalParameter = parameterResolverClass;
            finalClass = classResolverClass;

        } catch (Throwable ignored) {}
    }

    public String resolveMethodName(String className, String name) {
        try {
            return (String) clazz.getMethod("resolveMethodName", String.class, String.class).invoke(null, className, name);
        } catch (Throwable e){
            return name;
        }
    }

    public boolean hasClass(String className){
        Mapping runtime = RuntimeMappings.active();
        if (runtime != null) return runtime.resolveClass(className) != null;
        try {
            return (boolean)finalClass.getMethod("has", String.class).invoke(null, className);
        } catch (Throwable e){
            return false;
        }
    }

    public String resolveClass(String className){
        Mapping runtime = RuntimeMappings.active();
        if (runtime != null) return runtime.resolveClass(className);
        try {
            return (String) finalClass.getMethod("resolve", String.class).invoke(null, className);
        } catch (Throwable e){
            return null;
        }
    }

    public boolean hasField(String className, String name){
        Mapping runtime = RuntimeMappings.active();
        if (runtime != null) return runtime.resolveField(className, name) != null;
        try {
            return (boolean)finalField.getMethod("has", String.class,String.class).invoke(null, className ,name);
        } catch (Throwable e){
            return false;
        }
    }

    public String resolveField(String className, String name){
        Mapping runtime = RuntimeMappings.active();
        if (runtime != null) return runtime.resolveField(className, name);
        try {
            return (String) finalField.getMethod("resolve", String.class,String.class).invoke(null, className ,name);
        } catch (Throwable e){
            return null;
        }
    }

    public boolean hasMethod(String className, String name){
        Mapping runtime = RuntimeMappings.active();
        if (runtime != null) return runtime.resolveMethod(className, name) != null;
        try {
            return (boolean)finalMethod.getMethod("has", String.class,String.class).invoke(null, className ,name);
        } catch (Throwable e){
            return false;
        }
    }

    public String resolveMethod(String className, String name){
        Mapping runtime = RuntimeMappings.active();
        if (runtime != null) return runtime.resolveMethod(className, name);
        try {
            return (String) finalMethod.getMethod("resolve", String.class,String.class).invoke(null, className ,name);
        } catch (Throwable e){
            return null;
        }
    }

    public boolean hasParameter(String name){
        Mapping runtime = RuntimeMappings.active();
        if (runtime != null) return runtime.resolveParameters(name) != null;
        try {
            return (boolean)finalParameter.getMethod("has", String.class).invoke(null, name);
        } catch (Throwable e){
            return false;
        }
    }

    public Class<?>[] resolveParameter(String name){
        Mapping runtime = RuntimeMappings.active();
        if (runtime != null) return toClasses(runtime.resolveParameters(name));
        try {
            return (Class<?>[]) finalParameter.getMethod("resolve", String.class).invoke(null, name);
        } catch (Throwable e){
            return null;
        }
    }

    public void loadParameter(){
        try {
            clazz.getMethod("loadParameter").invoke(null);
        } catch (Throwable ignored){}
    }

    /** Real parameter types from a runtime mapping, loaded through the client's class loader. */
    private static Class<?>[] toClasses(String[] names) {
        if (names == null) return null;
        Class<?>[] classes = new Class<?>[names.length];
        for (int i = 0; i < names.length; i++) {
            classes[i] = primitive(names[i]);
            if (classes[i] == null) classes[i] = ClassLoad.getClass(names[i]);
            if (classes[i] == null) return null;   // cannot hook it faithfully, so do not try
        }
        return classes;
    }

    private static Class<?> primitive(String name) {
        switch (name) {
            case "Z": return boolean.class;
            case "B": return byte.class;
            case "S": return short.class;
            case "C": return char.class;
            case "I": return int.class;
            case "J": return long.class;
            case "F": return float.class;
            case "D": return double.class;
            default: return null;
        }
    }

    public static Class<?> getResolverClass() {
        ClientChecker.ClientType clientType = ClientChecker.ClientType.fromPackage(Utils.pkgName);
        if (clientType == null) return null;
        return clientType.getResolverClass();
    }
}