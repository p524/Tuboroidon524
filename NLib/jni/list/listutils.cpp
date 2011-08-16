/*
 * listutils.cpp
 *
 *  Created on: 2011/01/22
 *      Author: H.Narazaki
 */

#include "listutils.h"
#include <android/log.h>

#include <cctype>
#include <cstdlib>
#include <cstring>
#include <vector>

#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO, "native listutils.cpp", __VA_ARGS__))

JNIEXPORT jobjectArray
JNICALL Java_info_narazaki_android_lib_list_ListUtils_split__Ljava_lang_String_2Ljava_lang_String_2(JNIEnv * env,
        jclass cls, jstring with, jstring orig) {
    const jchar* with_str = env->GetStringChars(with, NULL);
    const jsize with_len = env->GetStringLength(with);
    if (with_len == 0) return NULL;
    const jchar* orig_str = env->GetStringChars(orig, NULL);
    const jsize orig_len = env->GetStringLength(orig);
    jclass string_clas = env->FindClass("java/lang/String");
    //
    std::vector<jstring> result_vector;
    jchar with_str_start = with_str[0];
    jsize i;
    jsize prev_end = 0;
    jsize end_pos = orig_len - with_len;
    for (i = 0; i <= end_pos; i++) {
        if (orig_str[i] == with_str_start) {
            if (std::memcmp(orig_str + i, with_str, sizeof(jchar) * with_len) == 0) {
                result_vector.push_back(env->NewString(orig_str + prev_end, i - prev_end));
                i += with_len;
                prev_end = i;
                i--;
            }
        }
    }
    result_vector.push_back(env->NewString(orig_str + prev_end, orig_len - prev_end));

    jobjectArray result_array = env->NewObjectArray(result_vector.size(), string_clas, NULL);
    i = 0;
    for (std::vector<jstring>::iterator it = result_vector.begin(); it != result_vector.end(); it++) {
        env->SetObjectArrayElement(result_array, i, *it);
        i++;
    }

    env->ReleaseStringChars(orig, orig_str);
    env->ReleaseStringChars(with, with_str);

    return result_array;
}

static long wideToLong(const jchar* str, jsize len) {
    char buf[31];
    if (len > 30) len = 30;
    for (int i = 0; i < len; i++) {
        buf[i] = (char) str[i];
    }
    buf[len] = 0;

    return std::atol(buf);
}

static void splitToLongVector(std::vector<long>& result_vector, const jchar* with_str, const jsize with_len,
        const jchar* orig_str, const jsize orig_len) {
    jchar with_str_start = with_str[0];
    jsize i;
    jsize prev_end = 0;
    jsize end_pos = orig_len - with_len;
    for (i = 0; i <= end_pos; i++) {
        if (orig_str[i] == with_str_start) {
            if (std::memcmp(orig_str + i, with_str, sizeof(jchar) * with_len) == 0) {
                result_vector.push_back(wideToLong(orig_str + prev_end, i - prev_end));
                i += with_len;
                prev_end = i;
                i--;
            }
        }
    }
    result_vector.push_back(wideToLong(orig_str + prev_end, orig_len - prev_end));
}

JNIEXPORT jintArray
JNICALL Java_info_narazaki_android_lib_list_ListUtils_split__Ljava_lang_String_2Ljava_lang_String_2I(JNIEnv *env,
        jclass cls, jstring with, jstring orig, jint dummy_int) {
    const jchar* with_str = env->GetStringChars(with, NULL);
    const jsize with_len = env->GetStringLength(with);
    if (with_len == 0) return NULL;
    const jchar* orig_str = env->GetStringChars(orig, NULL);
    const jsize orig_len = env->GetStringLength(orig);
    //
    std::vector<long> result_vector;
    splitToLongVector(result_vector, with_str, with_len, orig_str, orig_len);

    jsize result_size = result_vector.size();
    jintArray result_array = env->NewIntArray(result_size);
    jint* int_array = static_cast<jint*> (std::malloc(sizeof(jint) * result_size));
    jsize i = 0;
    for (std::vector<long>::iterator it = result_vector.begin(); it != result_vector.end(); it++) {
        int_array[i] = *it;
        i++;
    }

    env->SetIntArrayRegion(result_array, 0, result_size, int_array);
    std::free(int_array);

    env->ReleaseStringChars(orig, orig_str);
    env->ReleaseStringChars(with, with_str);

    return result_array;
}

JNIEXPORT jlongArray
JNICALL Java_info_narazaki_android_lib_list_ListUtils_split__Ljava_lang_String_2Ljava_lang_String_2J(JNIEnv *env,
        jclass cls, jstring with, jstring orig, jlong dummy_long) {
    const jchar* with_str = env->GetStringChars(with, NULL);
    const jsize with_len = env->GetStringLength(with);
    if (with_len == 0) return NULL;
    const jchar* orig_str = env->GetStringChars(orig, NULL);
    const jsize orig_len = env->GetStringLength(orig);
    //
    std::vector<long> result_vector;
    splitToLongVector(result_vector, with_str, with_len, orig_str, orig_len);

    jsize result_size = result_vector.size();
    jlongArray result_array = env->NewLongArray(result_size);
    jlong* long_array = static_cast<jlong*> (std::malloc(sizeof(jlong) * result_size));
    jsize i = 0;
    for (std::vector<long>::iterator it = result_vector.begin(); it != result_vector.end(); it++) {
        long_array[i] = *it;
        i++;
    }

    env->SetLongArrayRegion(result_array, 0, result_size, long_array);
    std::free(long_array);

    env->ReleaseStringChars(orig, orig_str);
    env->ReleaseStringChars(with, with_str);

    return result_array;
}

JNIEXPORT void JNICALL Java_info_narazaki_android_lib_list_ListUtils_initNative(JNIEnv * env, jclass) {
}

