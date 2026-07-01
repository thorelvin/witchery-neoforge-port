package com.emoniph.witchery.protection;

import com.emoniph.witchery.WitcheryMod;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

abstract class ReflectiveProtectionAdapter implements WitcheryProtectionAdapter {
    private Boolean available;

    @Override
    public final boolean isAvailable() {
        if (available != null) {
            return available;
        }
        try {
            available = initialize();
        } catch (ClassNotFoundException ex) {
            available = false;
        } catch (ReflectiveOperationException | LinkageError ex) {
            available = false;
            WitcheryMod.LOGGER.warn("Witchery protection adapter '{}' is unavailable: {}", id(), ex.toString());
        }
        return available;
    }

    protected abstract boolean initialize() throws ReflectiveOperationException;

    protected static Class<?> loadClass(String className) throws ClassNotFoundException {
        return Class.forName(className, false, ReflectiveProtectionAdapter.class.getClassLoader());
    }

    protected static Method method(Class<?> owner, String name, Class<?>... parameters) throws NoSuchMethodException {
        Method method = owner.getMethod(name, parameters);
        method.setAccessible(true);
        return method;
    }

    protected static boolean invokeBoolean(Method method, Object owner, Object... args) throws ReflectiveOperationException {
        try {
            return Boolean.TRUE.equals(method.invoke(owner, args));
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw ex;
        }
    }

    protected static Decision fromProtectedResult(boolean protectedAgainstAction) {
        return protectedAgainstAction ? Decision.DENY : Decision.ALLOW;
    }

    protected static Decision fromAllowedResult(boolean allowed) {
        return allowed ? Decision.ALLOW : Decision.DENY;
    }
}
