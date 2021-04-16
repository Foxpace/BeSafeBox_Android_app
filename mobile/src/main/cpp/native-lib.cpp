#include <jni.h>
#include <string>
#include "NativeModel.h"

/**
 * all methods of JNI
 */

NativeModel *nativeModel; // holds reference to whole data storage and classification

/**
 * @param limit - storage limit
 * @param activity_limit - for acceleration ofter event
 * @param nn_limit - for neural network
 */
extern "C"
JNIEXPORT void JNICALL
Java_motionapps_besafebox_models_detectors_DetectorFallNative_createObjects(JNIEnv *env,
                                                                            jobject thiz,
                                                                            jint limit,
                                                                            jfloat activity_limit,
                                                                            jfloat nn_limit) {
    nativeModel = new NativeModel(limit, activity_limit, nn_limit);

}


/**
 * @param time - time in milliseconds
 * @param x, y, z - acceleration values
 */
extern "C"
JNIEXPORT void JNICALL
Java_motionapps_besafebox_models_detectors_DetectorFallNative_passData(JNIEnv *env, jobject thiz,
                                                                       jlong time,
                                                                       jfloat x,
                                                                       jfloat y,
                                                                       jfloat z) {
    nativeModel->onValuesChanged((long) time, (float) x, (float) y, (float) z);
}

/**
 * deletes all the c++ objects
 */
extern "C"
JNIEXPORT void JNICALL
Java_motionapps_besafebox_models_detectors_DetectorFallNative_destroyObjects(JNIEnv *env, jobject thiz) {
    delete nativeModel;
}

/**
 * method to test functionality of the model
 * @param time - time in milliseconds
 * @param x, y, z - arrays with values of the whole signal
 */
extern "C"
JNIEXPORT jint JNICALL
Java_motionapps_besafebox_models_detectors_DetectorFallNative_analyseTest(JNIEnv *env, jobject thiz,
                                                                          jlongArray time_,
                                                                          jdoubleArray x_,
                                                                          jdoubleArray y_,
                                                                          jdoubleArray z_) {
    auto *s = new vector<SensorOutput>;

    jlong *time = env->GetLongArrayElements(time_,
                                            nullptr); // references to arrays in java
    jdouble *x = env->GetDoubleArrayElements(x_, nullptr);
    jdouble *y = env->GetDoubleArrayElements(y_, nullptr);
    jdouble *z = env->GetDoubleArrayElements(z_, nullptr);

    jsize len = env->GetArrayLength(x_);

    for (int i = 0; i < len; i++) {
        s->emplace_back(SensorOutput(time[i], // SensorOutput - creation of vector
                                     static_cast<float>(x[i]),
                                     static_cast<float>(y[i]),
                                     static_cast<float>(z[i])));
    }


    env->ReleaseLongArrayElements(time_, time, 0);
    env->ReleaseDoubleArrayElements(x_, x, 0);
    env->ReleaseDoubleArrayElements(y_, y, 0);
    env->ReleaseDoubleArrayElements(z_, z, 0);

    int result = nativeModel->analyseTest(s); // start of analysis
    delete s;

    return result;
}