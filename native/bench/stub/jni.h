/*
 * bench/stub/jni.h — minimal JNI surface for LOCAL SYNTAX VALIDATION
 * of jni_bridge.cpp only (this sandbox has a JRE, not a JDK, so the real
 * jni.h is unavailable). The real build uses the JDK headers via CMake
 * find_package(JNI) / CI — see .github/workflows/native-build.yml.
 *
 * Mirrors the structure of the real C++ jni.h: a JNINativeInterface_
 * function table plus a _JNIEnv wrapper whose inline members forward
 * to the table with `this` as the first argument — so `env->Fn(args)`
 * compiles exactly like against the real header.
 */
#ifndef STUB_JNI_H
#define STUB_JNI_H

#include <stdint.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef uint8_t  jboolean;
typedef int8_t   jbyte;
typedef int32_t  jint;
typedef int64_t  jlong;
typedef double   jdouble;
typedef float    jfloat;

struct _jobject;
typedef struct _jobject *jobject;
typedef jobject jstring;
typedef jobject jclass;
typedef jobject jmethodID;
typedef jobject jarray;
typedef jarray jbyteArray;
typedef jarray jintArray;
typedef jarray jfloatArray;
typedef jarray jdoubleArray;
typedef jarray jobjectArray;
typedef jint jsize;

struct _JNIEnv;
struct _JavaVM;

struct JNINativeInterface_ {
    jint        (*GetVersion)(struct _JNIEnv*);
    jclass      (*FindClass)(struct _JNIEnv*, const char*);
    jmethodID   (*GetMethodID)(struct _JNIEnv*, jclass, const char*, const char*);
    jmethodID   (*GetStaticMethodID)(struct _JNIEnv*, jclass, const char*, const char*);
    jobject     (*NewObject)(struct _JNIEnv*, jclass, jmethodID, ...);
    jclass      (*GetObjectClass)(struct _JNIEnv*, jobject);
    jobject     (*NewGlobalRef)(struct _JNIEnv*, jobject);
    void        (*DeleteGlobalRef)(struct _JNIEnv*, jobject);
    void        (*DeleteLocalRef)(struct _JNIEnv*, jobject);
    jstring     (*NewStringUTF)(struct _JNIEnv*, const char*);
    const char* (*GetStringUTFChars)(struct _JNIEnv*, jstring, jboolean*);
    void        (*ReleaseStringUTFChars)(struct _JNIEnv*, jstring, const char*);
    jboolean    (*ExceptionCheck)(struct _JNIEnv*);
    void        (*ExceptionDescribe)(struct _JNIEnv*);
    void        (*ExceptionClear)(struct _JNIEnv*);
    void*       (*GetDirectBufferAddress)(struct _JNIEnv*, jobject);
    jlong       (*GetDirectBufferCapacity)(struct _JNIEnv*, jobject);
    jobject     (*NewDirectByteBuffer)(struct _JNIEnv*, void*, jlong);
    /* array + call surface (compile-only: signatures trimmed to what
       jni_bridge.cpp actually invokes) */
    jarray      (*NewByteArray)(struct _JNIEnv*, jsize);
    jarray      (*NewIntArray)(struct _JNIEnv*, jsize);
    jarray      (*NewFloatArray)(struct _JNIEnv*, jsize);
    void        (*SetByteArrayRegion)(struct _JNIEnv*, jarray, jsize, jsize, const jbyte*);
    void        (*SetIntArrayRegion)(struct _JNIEnv*, jarray, jsize, jsize, const jint*);
    void        (*SetFloatArrayRegion)(struct _JNIEnv*, jarray, jsize, jsize, const jfloat*);
    void        (*GetByteArrayRegion)(struct _JNIEnv*, jarray, jsize, jsize, jbyte*);
    void        (*GetIntArrayRegion)(struct _JNIEnv*, jarray, jsize, jsize, jint*);
    void        (*GetDoubleArrayRegion)(struct _JNIEnv*, jarray, jsize, jsize, jdouble*);
    jsize       (*GetArrayLength)(struct _JNIEnv*, jarray);
    jint        (*PushLocalFrame)(struct _JNIEnv*, jint);
    jobject     (*PopLocalFrame)(struct _JNIEnv*, jobject);
    jint        (*CallIntMethod)(struct _JNIEnv*, jobject, jmethodID, ...);
    jobject     (*CallObjectMethod)(struct _JNIEnv*, jobject, jmethodID, ...);
    void        (*CallVoidMethod)(struct _JNIEnv*, jobject, jmethodID, ...);
    jobject     (*CallStaticObjectMethod)(struct _JNIEnv*, jclass, jmethodID, ...);
    jint        (*GetEnv)(struct _JavaVM*, void**, jint);
};

struct _JNIEnv {
    const struct JNINativeInterface_ *functions;
#ifdef __cplusplus
    jint GetVersion() { return functions->GetVersion(this); }
    jclass FindClass(const char* name) { return functions->FindClass(this, name); }
    jclass GetObjectClass(jobject obj) { return functions->GetObjectClass(this, obj); }
    jmethodID GetMethodID(jclass c, const char* n, const char* s) {
        return functions->GetMethodID(this, c, n, s);
    }
    jmethodID GetStaticMethodID(jclass c, const char* n, const char* s) {
        return functions->GetStaticMethodID(this, c, n, s);
    }
    jobject NewObject(jclass c, jmethodID m, ...) {
        return functions->NewObject(this, c, m);
    }
    jobject NewGlobalRef(jobject o) { return functions->NewGlobalRef(this, o); }
    void DeleteGlobalRef(jobject o) { functions->DeleteGlobalRef(this, o); }
    void DeleteLocalRef(jobject o) { functions->DeleteLocalRef(this, o); }
    jstring NewStringUTF(const char* s) { return functions->NewStringUTF(this, s); }
    const char* GetStringUTFChars(jstring s, jboolean* b) {
        return functions->GetStringUTFChars(this, s, b);
    }
    void ReleaseStringUTFChars(jstring s, const char* c) {
        return functions->ReleaseStringUTFChars(this, s, c);
    }
    void* GetDirectBufferAddress(jobject b) {
        return functions->GetDirectBufferAddress(this, b);
    }
    jlong GetDirectBufferCapacity(jobject b) {
        return functions->GetDirectBufferCapacity(this, b);
    }
    jobject NewDirectByteBuffer(void* addr, jlong cap) {
        return functions->NewDirectByteBuffer(this, addr, cap);
    }
    jboolean ExceptionCheck() { return functions->ExceptionCheck(this); }
    void ExceptionDescribe() { functions->ExceptionDescribe(this); }
    void ExceptionClear() { functions->ExceptionClear(this); }
    jbyteArray NewByteArray(jsize n) {
        return static_cast<jbyteArray>(functions->NewByteArray(this, n));
    }
    jintArray NewIntArray(jsize n) {
        return static_cast<jintArray>(functions->NewIntArray(this, n));
    }
    jfloatArray NewFloatArray(jsize n) {
        return static_cast<jfloatArray>(functions->NewFloatArray(this, n));
    }
    void SetByteArrayRegion(jbyteArray a, jsize s, jsize l, const jbyte* b) {
        functions->SetByteArrayRegion(this, a, s, l, b);
    }
    void SetIntArrayRegion(jintArray a, jsize s, jsize l, const jint* b) {
        functions->SetIntArrayRegion(this, a, s, l, b);
    }
    void SetFloatArrayRegion(jfloatArray a, jsize s, jsize l, const jfloat* b) {
        functions->SetFloatArrayRegion(this, a, s, l, b);
    }
    void GetByteArrayRegion(jbyteArray a, jsize s, jsize l, jbyte* b) {
        functions->GetByteArrayRegion(this, a, s, l, b);
    }
    void GetIntArrayRegion(jintArray a, jsize s, jsize l, jint* b) {
        functions->GetIntArrayRegion(this, a, s, l, b);
    }
    void GetDoubleArrayRegion(jdoubleArray a, jsize s, jsize l, jdouble* b) {
        functions->GetDoubleArrayRegion(this, a, s, l, b);
    }
    jsize GetArrayLength(jarray a) { return functions->GetArrayLength(this, a); }
    jint PushLocalFrame(jint capacity) { return functions->PushLocalFrame(this, capacity); }
    jobject PopLocalFrame(jobject result) { return functions->PopLocalFrame(this, result); }
    jint CallIntMethod(jobject o, jmethodID m, ...) {
        return functions->CallIntMethod(this, o, m);
    }
    jobject CallObjectMethod(jobject o, jmethodID m, ...) {
        return functions->CallObjectMethod(this, o, m);
    }
    void CallVoidMethod(jobject o, jmethodID m, ...) {
        functions->CallVoidMethod(this, o, m);
    }
    jobject CallStaticObjectMethod(jclass c, jmethodID m, ...) {
        return functions->CallStaticObjectMethod(this, c, m);
    }
#endif
};

struct _JavaVM {
    void* reserved;
#ifdef __cplusplus
    jint GetEnv(void** env, jint version) {
        (void)reserved;
        (void)version;
        *env = nullptr;
        return 0;
    }
    jint AttachCurrentThread(void** env, void* args) {
        (void)env; (void)args;
        return 0;
    }
    jint DetachCurrentThread() { return 0; }
#endif
};

typedef struct _JNIEnv JNIEnv;
typedef struct _JavaVM JavaVM;

#define JNIEXPORT
#define JNICALL
#define JNI_OK 0
#define JNI_ERR (-1)
#define JNI_EDETACHED (-2)
#define JNI_VERSION_1_8 0x00010008

#ifdef __cplusplus
}
#endif

#endif /* STUB_JNI_H */
