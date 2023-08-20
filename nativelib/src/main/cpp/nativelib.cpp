#include <jni.h>
#include <alloca.h>

static jclass getPrimitiveType(
        JNIEnv *env,
        const char *name
) {
    auto clazz = env->FindClass(name);
    auto fieldId = env->GetStaticFieldID(
            clazz,
            "TYPE",
            "Ljava/lang/Class;"
    );
    auto primitiveType = (jclass) env->GetStaticObjectField(clazz, fieldId);
    env->DeleteLocalRef(clazz);
    return primitiveType;
}

static bool matchPrimitiveType(
        JNIEnv *env,
        jclass clazz,
        const char *name
) {
    auto primitiveType = getPrimitiveType(env, name);
    auto ret = env->IsSameObject(clazz, primitiveType);
    env->DeleteLocalRef(primitiveType);
    return ret;
}

struct CallContext {
    jmethodID methodId;
    jclass clazz;
    jvalue *nativeArgs;

    CallContext(
            JNIEnv *env,
            jobject method,
            jobjectArray args
    ) {
        methodId = env->FromReflectedMethod(method);
        static jmethodID getDeclaringClassMethodId = nullptr;
        if (!getDeclaringClassMethodId) {
            auto methodClazz = env->GetObjectClass(method);
            getDeclaringClassMethodId = env->GetMethodID(
                    methodClazz,
                    "getDeclaringClass",
                    "()Ljava/lang/Class;"
            );
            env->DeleteLocalRef(methodClazz);
        }
        clazz = (jclass) env->CallObjectMethod(
                method,
                getDeclaringClassMethodId
        );
        auto length = env->GetArrayLength(args);
        nativeArgs = new jvalue[length];
        static jmethodID getComponentTypeMethodId = nullptr;
        if (!getComponentTypeMethodId) {
            auto clazzClazz = env->FindClass("java/lang/Class");
            getComponentTypeMethodId = env->GetMethodID(
                    clazzClazz,
                    "getComponentType",
                    "()Ljava/lang/Class;"
            );
            env->DeleteLocalRef(clazzClazz);
        }
        for (int i = 0; i < length; ++i) {
            auto subArray = env->GetObjectArrayElement(args, i);
            if (subArray != nullptr) {
                auto subArrayType = env->GetObjectClass(subArray);
                auto valueType = (jclass) env->CallObjectMethod(
                        subArrayType,
                        getComponentTypeMethodId
                );
                if (matchPrimitiveType(env, valueType, "java/lang/Boolean")) {
                    env->GetBooleanArrayRegion((jbooleanArray) subArray, 0, 1, &nativeArgs[i].z);
                } else if (matchPrimitiveType(env, valueType, "java/lang/Byte")) {
                    env->GetByteArrayRegion((jbyteArray) subArray, 0, 1, &nativeArgs[i].b);
                } else if (matchPrimitiveType(env, valueType, "java/lang/Character")) {
                    env->GetCharArrayRegion((jcharArray) subArray, 0, 1, &nativeArgs[i].c);
                } else if (matchPrimitiveType(env, valueType, "java/lang/Float")) {
                    env->GetFloatArrayRegion((jfloatArray) subArray, 0, 1, &nativeArgs[i].f);
                } else if (matchPrimitiveType(env, valueType, "java/lang/Double")) {
                    env->GetDoubleArrayRegion((jdoubleArray) subArray, 0, 1, &nativeArgs[i].d);
                } else if (matchPrimitiveType(env, valueType, "java/lang/Short")) {
                    env->GetShortArrayRegion((jshortArray) subArray, 0, 1, &nativeArgs[i].s);
                } else if (matchPrimitiveType(env, valueType, "java/lang/Integer")) {
                    env->GetIntArrayRegion((jintArray) subArray, 0, 1, &nativeArgs[i].i);
                } else if (matchPrimitiveType(env, valueType, "java/lang/Long")) {
                    env->GetLongArrayRegion((jlongArray) subArray, 0, 1, &nativeArgs[i].j);
                } else {
                    nativeArgs[i].l = env->GetObjectArrayElement((jobjectArray) subArray, 0);
                }
            } else {
                nativeArgs[i].l = nullptr;
            }
        }
    }

    ~CallContext() {
        delete[] nativeArgs;
    }
};


extern "C"
JNIEXPORT jboolean JNICALL
Java_com_example_nativelib_UnsafeReflection_callNonvirtualBooleanMethodA(JNIEnv *env, jclass clazz,
                                                                         jobject method,
                                                                         jobject obj,
                                                                         jobjectArray args) {
    auto context = new CallContext(env, method, args);
    auto ret = env->CallNonvirtualBooleanMethodA(
            obj,
            context->clazz,
            context->methodId,
            context->nativeArgs
    );
    delete context;
    return ret;
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_example_nativelib_UnsafeReflection_callNonvirtualIntMethodA(JNIEnv *env, jclass clazz,
                                                                     jobject method, jobject obj,
                                                                     jobjectArray args) {
    auto context = new CallContext(env, method, args);
    auto ret = env->CallNonvirtualIntMethodA(
            obj,
            context->clazz,
            context->methodId,
            context->nativeArgs
    );
    delete context;
    return ret;
}
extern "C"
JNIEXPORT jlong JNICALL
Java_com_example_nativelib_UnsafeReflection_callNonvirtualLongMethodA(JNIEnv *env, jclass clazz,
                                                                      jobject method, jobject obj,
                                                                      jobjectArray args) {
    auto context = new CallContext(env, method, args);
    auto ret = env->CallNonvirtualLongMethodA(
            obj,
            context->clazz,
            context->methodId,
            context->nativeArgs
    );
    delete context;
    return ret;
}
extern "C"
JNIEXPORT jshort JNICALL
Java_com_example_nativelib_UnsafeReflection_callNonvirtualShortMethodA(JNIEnv *env, jclass clazz,
                                                                       jobject method, jobject obj,
                                                                       jobjectArray args) {
    auto context = new CallContext(env, method, args);
    auto ret = env->CallNonvirtualShortMethodA(
            obj,
            context->clazz,
            context->methodId,
            context->nativeArgs
    );
    delete context;
    return ret;
}
extern "C"
JNIEXPORT jdouble JNICALL
Java_com_example_nativelib_UnsafeReflection_callNonvirtualDoubleMethodA(JNIEnv *env, jclass clazz,
                                                                        jobject method, jobject obj,
                                                                        jobjectArray args) {
    auto context = new CallContext(env, method, args);
    auto ret = env->CallNonvirtualDoubleMethodA(
            obj,
            context->clazz,
            context->methodId,
            context->nativeArgs
    );
    delete context;
    return ret;
}
extern "C"
JNIEXPORT jfloat JNICALL
Java_com_example_nativelib_UnsafeReflection_callNonvirtualFloatMethodA(JNIEnv *env, jclass clazz,
                                                                       jobject method, jobject obj,
                                                                       jobjectArray args) {
    auto context = new CallContext(env, method, args);
    auto ret = env->CallNonvirtualFloatMethodA(
            obj,
            context->clazz,
            context->methodId,
            context->nativeArgs
    );
    delete context;
    return ret;
}
extern "C"
JNIEXPORT jbyte JNICALL
Java_com_example_nativelib_UnsafeReflection_callNonvirtualByteMethodA(JNIEnv *env, jclass clazz,
                                                                      jobject method, jobject obj,
                                                                      jobjectArray args) {
    auto context = new CallContext(env, method, args);
    auto ret = env->CallNonvirtualByteMethodA(
            obj,
            context->clazz,
            context->methodId,
            context->nativeArgs
    );
    delete context;
    return ret;
}
extern "C"
JNIEXPORT void JNICALL
Java_com_example_nativelib_UnsafeReflection_callNonvirtualVoidMethodA(JNIEnv *env, jclass clazz,
                                                                      jobject method, jobject obj,
                                                                      jobjectArray args) {
    auto context = new CallContext(env, method, args);
    env->CallNonvirtualVoidMethodA(
            obj,
            context->clazz,
            context->methodId,
            context->nativeArgs
    );
    delete context;
}
extern "C"
JNIEXPORT jobject JNICALL
Java_com_example_nativelib_UnsafeReflection_callNonvirtualObjectMethodA(JNIEnv *env, jclass clazz,
                                                                        jobject method, jobject obj,
                                                                        jobjectArray args) {
    auto context = new CallContext(env, method, args);
    auto ret = env->CallNonvirtualObjectMethodA(
            obj,
            context->clazz,
            context->methodId,
            context->nativeArgs
    );
    delete context;
    return ret;
}