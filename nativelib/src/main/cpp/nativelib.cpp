#include <jni.h>


static jmethodID getComponentTypeMethodId = nullptr;
static jmethodID getDeclaringClassMethodId = nullptr;
static jclass booleanClass = nullptr;
static jclass byteClass = nullptr;
static jclass charClass = nullptr;
static jclass floatClass = nullptr;
static jclass doubleClass = nullptr;
static jclass shortClass = nullptr;
static jclass intClass = nullptr;
static jclass longClass = nullptr;

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
        clazz = (jclass) env->CallObjectMethod(
                method,
                getDeclaringClassMethodId
        );
        auto length = env->GetArrayLength(args);
        nativeArgs = new jvalue[length];
        for (int i = 0; i < length; ++i) {
            auto subArray = env->GetObjectArrayElement(args, i);
            if (subArray != nullptr) {
                auto subArrayClass = env->GetObjectClass(subArray);
                auto valueClass = (jclass) env->CallObjectMethod(
                        subArrayClass,
                        getComponentTypeMethodId
                );
                if (env->IsSameObject(valueClass, booleanClass)) {
                    env->GetBooleanArrayRegion((jbooleanArray) subArray, 0, 1, &nativeArgs[i].z);
                } else if (env->IsSameObject(valueClass, byteClass)) {
                    env->GetByteArrayRegion((jbyteArray) subArray, 0, 1, &nativeArgs[i].b);
                } else if (env->IsSameObject(valueClass, charClass)) {
                    env->GetCharArrayRegion((jcharArray) subArray, 0, 1, &nativeArgs[i].c);
                } else if (env->IsSameObject(valueClass, floatClass)) {
                    env->GetFloatArrayRegion((jfloatArray) subArray, 0, 1, &nativeArgs[i].f);
                } else if (env->IsSameObject(valueClass, doubleClass)) {
                    env->GetDoubleArrayRegion((jdoubleArray) subArray, 0, 1, &nativeArgs[i].d);
                } else if (env->IsSameObject(valueClass, shortClass)) {
                    env->GetShortArrayRegion((jshortArray) subArray, 0, 1, &nativeArgs[i].s);
                } else if (env->IsSameObject(valueClass, intClass)) {
                    env->GetIntArrayRegion((jintArray) subArray, 0, 1, &nativeArgs[i].i);
                } else if (env->IsSameObject(valueClass, longClass)) {
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

static jclass getPrimitiveType(
        JNIEnv *env,
        const char *name
) {
    auto localClass = env->FindClass(name);
    auto fieldId = env->GetStaticFieldID(
            localClass,
            "TYPE",
            "Ljava/lang/Class;"
    );
    auto localPrimitiveType = env->GetStaticObjectField(localClass, fieldId);
    auto primitiveType = (jclass) env->NewGlobalRef(localPrimitiveType);
    env->DeleteLocalRef(localClass);
    env->DeleteLocalRef(localPrimitiveType);
    return primitiveType;
}


extern "C"
JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env = nullptr;
    vm->GetEnv((void **) (&env), JNI_VERSION_1_6);
    getDeclaringClassMethodId = env->GetMethodID(
            env->FindClass("java/lang/reflect/Method"),
            "getDeclaringClass",
            "()Ljava/lang/Class;"
    );
    getComponentTypeMethodId = env->GetMethodID(
            env->FindClass("java/lang/Class"),
            "getComponentType",
            "()Ljava/lang/Class;"
    );
    booleanClass = getPrimitiveType(env, "java/lang/Boolean");
    byteClass = getPrimitiveType(env, "java/lang/Byte");
    charClass = getPrimitiveType(env, "java/lang/Character");
    floatClass = getPrimitiveType(env, "java/lang/Float");
    doubleClass = getPrimitiveType(env, "java/lang/Double");
    shortClass = getPrimitiveType(env, "java/lang/Short");
    intClass = getPrimitiveType(env, "java/lang/Integer");
    longClass = getPrimitiveType(env, "java/lang/Long");
    return JNI_VERSION_1_6;
}

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