/*
 * span_entryanchorfilter
 *
 *  Created on: 2011/01/23
 *      Author: H.Narazaki
 */

#include <jni.h>
#include "span_entryanchorfilter.h"
#include <android/log.h>

#include <cctype>
#include <cstdlib>
#include <cstring>
#include <vector>

typedef std::vector<jobject> JObjectList;

#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO, "native span_entryanchorfilter.cpp", __VA_ARGS__))
typedef std::vector<jobject> JObjectList;

JNIEXPORT jobjectArray JNICALL Java_info_narazaki_android_tuboroid_text_span_EntryAnchorFilter_gatherNative(
        JNIEnv *env, jobject j_self, jstring j_text) {
    const jchar* text_str = env->GetStringChars(j_text, NULL);
    const jsize text_len = env->GetStringLength(j_text);
    JObjectList result_vector;

    jclass spanspec_class = env->FindClass("info/narazaki/android/tuboroid/text/span/EntryAnchorSpanSpec");
    jmethodID spanspec_init = env->GetMethodID(spanspec_class, "<init>", "(Ljava/lang/String;IIJ)V");

    int i, j;
    for (i = 0; i < text_len - 1; i++) {
        const jchar chr = text_str[i];
        if (chr == L'>') {
            int start = i;
            const jchar chr2 = text_str[i + 1];
            if (chr2 == L'>' && i < text_len - 2) i++;
            jlong entry_id = 0;
            for (j = i + 1; j < text_len; j++) {
                const jchar chr3 = text_str[j];
                if (chr3 < L'0' || L'9' < chr3) break;
                entry_id = entry_id * 10 + (chr3 - L'0');
            }
            if (j == i + 1) continue;

            jsize end = j;
            jstring str = env->NewString(text_str + start, end - start);
            jobject span_spec = env->NewObject(spanspec_class, spanspec_init, str, start, end, entry_id);
            result_vector.push_back(span_spec);

            i = j - 1;
        }
    }

    env->ReleaseStringChars(j_text, text_str);
    jobjectArray result_array = env->NewObjectArray(result_vector.size(), spanspec_class, NULL);

    i = 0;
    for (JObjectList::iterator it = result_vector.begin(); it != result_vector.end(); it++) {
        env->SetObjectArrayElement(result_array, i, *it);
        i++;
    }

    return result_array;

}

JNIEXPORT void JNICALL Java_info_narazaki_android_tuboroid_text_span_EntryAnchorFilter_initNative(JNIEnv *env,
        jclass cls) {
}
