#include <jni.h>
#include <string>
#include <opencv2/core/version.hpp>
#include <android/log.h>

#define TAG "OpenCVCheck"
#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__))
#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__))

extern "C" {

JNIEXPORT jstring JNICALL
Java_com_purnajear_edgedetectorapp_MainActivity_getOpenCVVersion(JNIEnv* env, jobject thiz) {
    #ifdef CV_VERSION
        LOGI("OpenCV Version: %s", CV_VERSION);
        return env->NewStringUTF(CV_VERSION);
    #else
        LOGE("OpenCV not found");
        return env->NewStringUTF("OpenCV Not Found");
    #endif
}

// Add the function for DebugActivity
JNIEXPORT jstring JNICALL
Java_com_purnajear_edgedetectorapp_DebugActivity_getOpenCVVersion(JNIEnv* env, jobject thiz) {
    #ifdef CV_VERSION
        LOGI("OpenCV Version (Debug): %s", CV_VERSION);
        return env->NewStringUTF(CV_VERSION);
    #else
        LOGE("OpenCV not found (Debug)");
        return env->NewStringUTF("OpenCV Not Found");
    #endif
}

// Add the function for DebugActivity.initOpenCV()
JNIEXPORT void JNICALL
Java_com_purnajear_edgedetectorapp_DebugActivity_initOpenCV(JNIEnv* env, jobject thiz) {
    try {
        LOGI("Initializing OpenCV from DebugActivity...");
        // Just to validate OpenCV is working
        #ifdef CV_VERSION
            LOGI("OpenCV is available: %s", CV_VERSION);
        #else
            LOGE("OpenCV version not defined");
        #endif
    } catch (...) {
        LOGE("Unknown error in DebugActivity.initOpenCV");
    }
}

} // extern "C" 