#include <jni.h>
#include <fancier.h>
#include <string.h>
#include <vector>

#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,      "MODULE_EXAMPLE", __VA_ARGS__)

int set_ocl_parameter(JNIEnv *env, jobject jobj, jstring jtype, cl_uint index, cl_kernel kernel) {
    const char *type = env->GetStringUTFChars(jtype, nullptr);
    int err = 0;
    if (strcmp(type, "ByteArray") == 0) {
        fcByteArray *data = fcByteArray_getJava(env, jobj);
        err = clSetKernelArg(kernel, index, sizeof(cl_mem), &data->ocl);
    } else if (strcmp(type, "ShortArray") == 0) {
        fcShortArray *data = fcShortArray_getJava(env, jobj);
        err = clSetKernelArg(kernel, index, sizeof(cl_mem), &data->ocl);
    } else if (strcmp(type, "IntArray") == 0) {
        fcIntArray *data = fcIntArray_getJava(env, jobj);
        err = clSetKernelArg(kernel, index, sizeof(cl_mem), &data->ocl);
    } else if (strcmp(type, "FloatArray") == 0) {
        fcFloatArray *data = fcFloatArray_getJava(env, jobj);
        err = clSetKernelArg(kernel, index, sizeof(cl_mem), &data->ocl);
    } else if (strcmp(type, "DoubleArray") == 0) {
        fcDoubleArray *data = fcDoubleArray_getJava(env, jobj);
        err = clSetKernelArg(kernel, index, sizeof(cl_mem), &data->ocl);
    } else if (strcmp(type, "char") == 0) {
        jclass cls = env->FindClass("java/lang/Byte");
        jmethodID getVal = env->GetMethodID(cls, "byteValue", "()B");
        char val = env->CallByteMethod(jobj, getVal);
        err = clSetKernelArg(kernel, index, sizeof(char), &val);
    } else if (strcmp(type, "short") == 0) {
        jclass cls = env->FindClass("java/lang/Short");
        jmethodID getVal = env->GetMethodID(cls, "shortValue", "()S");
        short val = env->CallShortMethod(jobj, getVal);
        err = clSetKernelArg(kernel, index, sizeof(short), &val);
    } else if (strcmp(type, "int") == 0) {
        jclass cls = env->FindClass("java/lang/Integer");
        jmethodID getVal = env->GetMethodID(cls, "intValue", "()I");
        int val = env->CallIntMethod(jobj, getVal);
        err = clSetKernelArg(kernel, index, sizeof(int), &val);
    } else if (strcmp(type, "float") == 0) {
        jclass cls = env->FindClass("java/lang/Float");
        jmethodID getVal = env->GetMethodID(cls, "floatValue", "()F");
        float val = env->CallFloatMethod(jobj, getVal);
        err = clSetKernelArg(kernel, index, sizeof(float), &val);
    } else if (strcmp(type, "double") == 0) {
        jclass cls = env->FindClass("java/lang/Double");
        jmethodID getVal = env->GetMethodID(cls, "doubleValue", "()D");
        double val = env->CallDoubleMethod(jobj, getVal);
        err = clSetKernelArg(kernel, index, sizeof(double), &val);
    } else if (strcmp(type, "RGBAImage") == 0) {
        fcRGBAImage *data = fcRGBAImage_getJava(env, jobj);
        err = clSetKernelArg(kernel, index, sizeof(cl_mem), &data->pixels->ocl);
    } else {
        err = FC_EXCEPTION_BAD_PARAMETER;
    }
    FC_EXCEPTION_HANDLE_ERROR(env, err, "clSetKernelArg", JNI_FALSE);
    env->ReleaseStringUTFChars(jtype, type);
    return 0;
}

extern "C"
JNIEXPORT  jlong JNICALL
Java_es_ull_pcg_hpc_fancyjcl_Stage_prepare(JNIEnv *env, jobject thiz, jstring kernel_source,
                                                  jstring kernel_name, jobjectArray parameter_names,
                                                  jobjectArray parameters,
                                                  jobjectArray parameter_types) {
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
    for (cl_uint i = 0; i < env->GetArrayLength(parameter_names); i++) {
        auto jtype = (jstring) env->GetObjectArrayElement(parameter_types, i);
        auto jobj = (jobject) env->GetObjectArrayElement(parameters, i);
        set_ocl_parameter(env, jobj, jtype, i, kernel);
    }

    // Release mem
    env->ReleaseStringUTFChars(kernel_source, kernel_source_c);
    env->ReleaseStringUTFChars(kernel_name, kernel_name_c);

    // Return kernel native pointer
    jclass clazz = env->GetObjectClass(thiz);
    jfieldID returnParam2Field = env->GetFieldID(clazz, "cl_kernel_ptr", "J");
    env->SetLongField(thiz, returnParam2Field, (jlong) kernel);
    return 0;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_es_ull_pcg_hpc_fancyjcl_Stage_run(JNIEnv *env, jobject thiz, jlong cl_kernel_ptr,
                                              jlongArray dimensions, jlongArray parallelization) {
    int ndims = env->GetArrayLength(dimensions);
    // Get dimensions to an array
    auto dims_c = env->GetLongArrayElements(dimensions, nullptr);
    size_t *dims_ocl = new size_t[ndims];
    for (int i = 0; i < ndims; i++) {
        dims_ocl[i] = dims_c[i];
    }
    // Get parallelization to an array
    auto parallelization_c = env->GetLongArrayElements(parallelization, nullptr);
    size_t *parallelization_ocl = new size_t[ndims];
    for (int i = 0; i < ndims; i++) {
        parallelization_ocl[i] = parallelization_c[i];
    }
    // Enqueue the kernel for execution
    int err;
    cl_kernel kernel_pointer = (cl_kernel) cl_kernel_ptr;
    err = clEnqueueNDRangeKernel(fcOpenCL_rt.queue,
                                 kernel_pointer,
                                 ndims,
                                 nullptr,
                                 dims_ocl, // global work size
                                 parallelization_ocl, // local work size
                                 0,
                                 nullptr,
                                 nullptr);
    FC_EXCEPTION_HANDLE_ERROR(env, err, "clEnqueueNDRangeKernel", JNI_FALSE);
    return 0;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_es_ull_pcg_hpc_fancyjcl_Stage_waitForQueueToFinish(JNIEnv *env, jobject thiz) {
    int err = clFinish(fcOpenCL_rt.queue);
    FC_EXCEPTION_HANDLE_ERROR(env, err, "clEnqueueNDRangeKernel", JNI_FALSE);
    return 0;
}