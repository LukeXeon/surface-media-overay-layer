package com.example.nativelib;


import androidx.annotation.NonNull;
import androidx.collection.SimpleArrayMap;
import androidx.collection.SparseArrayCompat;
import androidx.core.util.Pools;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Objects;

public class UnsafeReflection {
    private UnsafeReflection() {
        throw new AssertionError();
    }

    static {
        System.loadLibrary("unsafe-reflection");
    }

    private static class NativeArgsCache {
        private final SparseArrayCompat<Object[]> mArgumentsArrayCache = new SparseArrayCompat<>();
        private final SimpleArrayMap<Class<?>, Pools.SimplePool<Object>> mSubArrayCache = new SimpleArrayMap<>();

        Object[] obtain(Method method, Object[] arguments) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            Object[] nativeArguments = mArgumentsArrayCache.get(parameterTypes.length);
            if (nativeArguments == null) {
                nativeArguments = new Object[parameterTypes.length];
                mArgumentsArrayCache.put(parameterTypes.length, nativeArguments);
            }
            for (int i = 0; i < parameterTypes.length; i++) {
                if (arguments[i] != null) {
                    Class<?> parameterType = parameterTypes[i];
                    Class<?> subArrayComponentType = parameterType.isPrimitive() ? parameterType : Object.class;
                    Pools.SimplePool<Object> subArrayPool = mSubArrayCache.get(subArrayComponentType);
                    if (subArrayPool == null) {
                        subArrayPool = new Pools.SimplePool<>(16);
                        mSubArrayCache.put(subArrayComponentType, subArrayPool);
                    }
                    Object subArray = subArrayPool.acquire();
                    if (subArray == null) {
                        subArray = Array.newInstance(subArrayComponentType, 1);
                    }
                    Array.set(subArray, 0, arguments[i]);
                    nativeArguments[i] = subArray;
                }
            }
            return nativeArguments;
        }

        void recycle(Object[] arguments) {
            for (int i = 0; i < arguments.length; i++) {
                Object jValue = arguments[i];
                if (jValue instanceof Object[]) {
                    Array.set(jValue, 0, null);
                }
                arguments[i] = null;
            }
        }
    }

    private static final ThreadLocal<NativeArgsCache> sNativeArgsCache = new ThreadLocal<NativeArgsCache>() {
        @NonNull
        @Override
        protected NativeArgsCache initialValue() {
            return new NativeArgsCache();
        }
    };

    private static native boolean callNonvirtualBooleanMethodA(Method method, Object obj, Object[] args);

    private static native int callNonvirtualIntMethodA(Method method, Object obj, Object[] args);

    private static native long callNonvirtualLongMethodA(Method method, Object obj, Object[] args);

    private static native short callNonvirtualShortMethodA(Method method, Object obj, Object[] args);

    private static native double callNonvirtualDoubleMethodA(Method method, Object obj, Object[] args);

    private static native float callNonvirtualFloatMethodA(Method method, Object obj, Object[] args);

    private static native byte callNonvirtualByteMethodA(Method method, Object obj, Object[] args);

    private static native void callNonvirtualVoidMethodA(Method method, Object obj, Object[] args);

    private static native Object callNonvirtualObjectMethodA(Method method, Object obj, Object[] args);

    public static Object callNonvirtualMethod(Method method, Object obj, Object... args) {
        NativeArgsCache nativeArgsCache = Objects.requireNonNull(sNativeArgsCache.get());
        Object[] nativeArgs = nativeArgsCache.obtain(method, args);
        try {
            Class<?> returnType = method.getReturnType();
            if (returnType == boolean.class) {
                return callNonvirtualBooleanMethodA(method, obj, nativeArgs);
            } else if (returnType == int.class) {
                return callNonvirtualIntMethodA(method, obj, nativeArgs);
            } else if (returnType == long.class) {
                return callNonvirtualLongMethodA(method, obj, nativeArgs);
            } else if (returnType == short.class) {
                return callNonvirtualShortMethodA(method, obj, nativeArgs);
            } else if (returnType == double.class) {
                return callNonvirtualDoubleMethodA(method, obj, nativeArgs);
            } else if (returnType == float.class) {
                return callNonvirtualFloatMethodA(method, obj, nativeArgs);
            } else if (returnType == byte.class) {
                return callNonvirtualByteMethodA(method, obj, nativeArgs);
            } else if (returnType == void.class) {
                callNonvirtualVoidMethodA(method, obj, nativeArgs);
                return null;
            } else {
                return callNonvirtualObjectMethodA(method, obj, nativeArgs);
            }
        } finally {
            nativeArgsCache.recycle(nativeArgs);
        }
    }
}
