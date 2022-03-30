#include <jni.h>
#include <fancier.h>

#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,      "MODULE_EXAMPLE", __VA_ARGS__)

extern "C"
JNIEXPORT int JNICALL
Java_es_ull_pcg_hpc_fancierfrontend_Test_test(JNIEnv *env, jclass thiz) {
    int err;
    err = fcFancier_initJNI(env);
    FC_EXCEPTION_HANDLE_ERROR(env, err, "fcFancier_initJNI", JNI_FALSE);
    fcFancier_releaseJNI(env);
    return err;
}