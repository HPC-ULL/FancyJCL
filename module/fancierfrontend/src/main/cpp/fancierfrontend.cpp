#include <jni.h>
#include <fancier.h>
#include <string.h>
#include <vector>

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

int set_ocl_parameter(JNIEnv *env, jobject jobj, jstring jtype, cl_uint index, cl_kernel kernel) {
    const char *type = env->GetStringUTFChars(jtype, nullptr);
    int err;
    if (strcmp(type, "ByteArray") == 0) {
        fcByteArray *data = fcByteArray_getJava(env, jobj);
        err = clSetKernelArg(kernel, index, sizeof(cl_mem), &data->ocl);
    }
    if (strcmp(type, "ShortArray") == 0) {
        fcShortArray *data = fcShortArray_getJava(env, jobj);
        err = clSetKernelArg(kernel, index, sizeof(cl_mem), &data->ocl);
    }
    if (strcmp(type, "IntArray") == 0) {
        fcIntArray *data = fcIntArray_getJava(env, jobj);
        err = clSetKernelArg(kernel, index, sizeof(cl_mem), &data->ocl);
    }
    if (strcmp(type, "FloatArray") == 0) {
        fcFloatArray *data = fcFloatArray_getJava(env, jobj);
        err = clSetKernelArg(kernel, index, sizeof(cl_mem), &data->ocl);
    }
    if (strcmp(type, "DoubleArray") == 0) {
        fcDoubleArray *data = fcDoubleArray_getJava(env, jobj);
        err = clSetKernelArg(kernel, index, sizeof(cl_mem), &data->ocl);
    }
    FC_EXCEPTION_HANDLE_ERROR(env, err, "clSetKernelArg", JNI_FALSE);
    env->ReleaseStringUTFChars(jtype, type);
    return 0;
}

extern "C"
JNIEXPORT  jlong JNICALL
Java_es_ull_pcg_hpc_fancierfrontend_Stage_prepare(JNIEnv *env, jobject thiz, jstring kernel_source,
                                                  jstring kernel_name, jobjectArray inputs,
                                                  jobjectArray outputs,
                                                  jobjectArray input_types,
                                                  jobjectArray output_types) {
    // Get the kernel and name into a C char *
    const char *kernel_source_c = env->GetStringUTFChars(kernel_source, nullptr);
    const char *kernel_name_c = env->GetStringUTFChars(kernel_name, nullptr);

    int err;
    // Kernel compilation
    cl_program program = fcOpenCL_compileKernel(1, &kernel_source_c, &err);
    FC_EXCEPTION_HANDLE_BUILD(env, err, "fcOpenCL_compileKernel", program, JNI_FALSE);
    cl_kernel kernel = clCreateKernel(program, kernel_name_c, &err);
    FC_EXCEPTION_HANDLE_ERROR(env, err, "clCreateKernel", JNI_FALSE);

    // Parameter setting
    for (cl_uint i = 0; i < env->GetArrayLength(inputs); i++) {
        auto jtype = (jstring) env->GetObjectArrayElement(input_types, i);
        auto jobj = (jobject) env->GetObjectArrayElement(inputs, i);
        set_ocl_parameter(env, jobj, jtype, i, kernel);
    }

    for (cl_uint i = 0; i < env->GetArrayLength(outputs); i++) {
        int idx = i + env->GetArrayLength(inputs);
        auto jtype = (jstring) env->GetObjectArrayElement(output_types, i);
        auto jobj = (jobject) env->GetObjectArrayElement(outputs, i);
        set_ocl_parameter(env, jobj, jtype, idx, kernel);
    }

    // Release mem
    env->ReleaseStringUTFChars(kernel_source, kernel_source_c);
    env->ReleaseStringUTFChars(kernel_name, kernel_name_c);

    // Return program and kernel native pointers
    jclass clazz = env->GetObjectClass(thiz);
    jfieldID returnParam1Field = env->GetFieldID(clazz, "cl_program_ptr", "J");
    jfieldID returnParam2Field = env->GetFieldID(clazz, "cl_kernel_ptr", "J");
    env->SetLongField(thiz, returnParam1Field, (jlong) program);
    env->SetLongField(thiz, returnParam2Field, (jlong) kernel);
    return 0;
}
extern "C"
JNIEXPORT jlong JNICALL
Java_es_ull_pcg_hpc_fancierfrontend_Stage_run(JNIEnv *env, jobject thiz, jlong cl_kernel_ptr,
                                              jintArray dimensions) {
    int ndims = env->GetArrayLength(dimensions);
    auto dims_c = env->GetIntArrayElements(dimensions, nullptr);
    size_t * dims_ocl = new size_t[ndims];
    for (int i = 0; i < ndims; i ++) {
        dims_ocl[i] = dims_c[i];
    }
    int err;
    cl_kernel kernel_pointer = (cl_kernel) cl_kernel_ptr;
    err = clEnqueueNDRangeKernel(fcOpenCL_rt.queue,
                                 kernel_pointer,
                                 ndims,
                                 nullptr,
                                 dims_ocl, // global work size
                                 nullptr, // local work size
                                 0,
                                 nullptr,
                                 nullptr);
    FC_EXCEPTION_HANDLE_ERROR(env, err, "clEnqueueNDRangeKernel", JNI_FALSE);
    return 0;
}
extern "C"
JNIEXPORT jlong JNICALL
Java_es_ull_pcg_hpc_fancierfrontend_Stage_waitForQueueToFinish(JNIEnv *env, jobject thiz) {
    int err = clFinish(fcOpenCL_rt.queue);
    FC_EXCEPTION_HANDLE_ERROR(env, err, "clEnqueueNDRangeKernel", JNI_FALSE);
    return 0;
}