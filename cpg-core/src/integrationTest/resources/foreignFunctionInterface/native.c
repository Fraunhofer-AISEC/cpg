#include <jni.h>

JNIEXPORT jint JNICALL Java_MyClass_nativeAdd(JNIEnv *env, jclass cls, jint left, jint right) {
    (void)env;
    (void)cls;
    return left + right;
}

