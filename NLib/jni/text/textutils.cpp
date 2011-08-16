/*
 * textutils.cpp
 *
 *  Created on: 2011/01/17
 *      Author: H.Narazaki
 */

#include "textutils.h"
#include <android/log.h>

#include <cstdlib>
#include <cstring>

#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO, "native textutils.cpp", __VA_ARGS__))

static const int MAX_NUM_LEN = 100;

JNIEXPORT jint JNICALL Java_info_narazaki_android_lib_text_TextUtils_parseInt(JNIEnv *env, jclass cls, jstring text,
        jint start, jint len) {
    return Java_info_narazaki_android_lib_text_TextUtils_parseLong(env, cls, text, start, len);
}

JNIEXPORT jlong JNICALL Java_info_narazaki_android_lib_text_TextUtils_parseLong(JNIEnv *env, jclass cls, jstring text,
        jint start, jint len) {
    const jchar* str = env->GetStringChars(text, NULL);
    const jsize text_len = env->GetStringLength(text);
    if (len > text_len - start) len = text_len - start;
    if (len <= 0) return 0;
    if (len > MAX_NUM_LEN) len = MAX_NUM_LEN;

    bool plus = true;
    jlong result = 0;
    int i = start;
    if (str[i] == L'-') {
        plus = false;
        i++;
    }
    else if (str[i] == L'+') {
        i++;
    }
    int end = start + len;
    for (; i < end; i++) {
        jchar chr = str[i];
        if (L'0' <= chr && chr <= L'9') {
            result *= 10;
            result += chr - L'0';
        }
        else {
            break;
        }
    }

    if (!plus) result *= -1;

    env->ReleaseStringChars(text, str);
    return result;
}

JNIEXPORT void JNICALL Java_info_narazaki_android_lib_text_TextUtils_initNative(JNIEnv *env, jclass cls) {
}

