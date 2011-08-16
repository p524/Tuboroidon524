/*
 * span_weburlfilter
 *
 *  Created on: 2011/01/22
 *      Author: H.Narazaki
 */

#include "span_weburlfilter.h"
#include <android/log.h>

#include <cctype>
#include <cstdlib>
#include <cstring>
#include <vector>

#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO, "native span_weburlfilter.cpp", __VA_ARGS__))
typedef std::vector<jobject> JObjectList;

struct Scheme {
    jchar* str_;
    jsize len_;
    Scheme(const jchar* str, const jsize& len) :
        len_(len) {
        str_ = new jchar[len_];
        std::memcpy(str_, str, sizeof(jchar) * len_);
    }
    ~Scheme() {
        delete[] str_;
    }
};

struct WebUrlFilter {
    Scheme** schemes_;
    jsize schemes_reg_count_;
    jsize schemes_len_;
    int max_scheme_len_;
    WebUrlFilter(JNIEnv *env, const jsize& schemes_len) :
        schemes_len_(schemes_len) {
        schemes_ = static_cast<Scheme**> (std::malloc(sizeof(Scheme*) * schemes_len));
        for (int i = 0; i < schemes_len; i++) {
            schemes_[i] = NULL;
        }
        schemes_reg_count_ = 0;
        max_scheme_len_ = 0;
    }
    void add(const jchar* str, const jsize& len) {
        schemes_[schemes_reg_count_] = new Scheme(str, len);
        schemes_reg_count_++;
        if (max_scheme_len_ < len) max_scheme_len_ = len;
    }
    void release(JNIEnv *env) {
    }
    ~WebUrlFilter() {
        for (int i = 0; i < schemes_len_; i++) {
            if (schemes_ != NULL) delete schemes_[i];
        }
        std::free(schemes_);
    }
};

JNIEXPORT jlong JNICALL Java_info_narazaki_android_lib_text_span_WebURLFilter_constructNative(JNIEnv *env,
        jobject j_self, jobjectArray j_schemes_array) {
    jsize schemes_len = env->GetArrayLength(j_schemes_array);
    WebUrlFilter* p_filter = new WebUrlFilter(env, schemes_len);
    for (int i = 0; i < schemes_len; i++) {
        jstring j_scheme = static_cast<jstring> (env->GetObjectArrayElement(j_schemes_array, i));
        const jchar* str = env->GetStringChars(j_scheme, NULL);
        p_filter->add(str, env->GetStringLength(j_scheme));
        env->ReleaseStringChars(j_scheme, str);
    }
    return (jlong) p_filter;
}

JNIEXPORT void JNICALL Java_info_narazaki_android_lib_text_span_WebURLFilter_destructNative(JNIEnv *env,
        jobject j_self, jlong ptr) {
    WebUrlFilter* p_filter = (WebUrlFilter*) ptr;
    p_filter->release(env);
    delete p_filter;
}

/*
 * Class:     info_narazaki_android_lib_text_span_WebURLFilter
 * Method:    gatherNative
 * Signature: (Ljava/lang/String;J)[Linfo/narazaki/android/lib/text/span/SpanSpec;
 */
JNIEXPORT jobjectArray JNICALL Java_info_narazaki_android_lib_text_span_WebURLFilter_gatherNative(JNIEnv *env,
        jobject j_self, jstring j_text, jlong ptr) {
    const jchar* text_str = env->GetStringChars(j_text, NULL);
    const jsize text_len = env->GetStringLength(j_text);

    jclass spanspec_class = env->FindClass("info/narazaki/android/lib/text/span/SpanSpec");
    jmethodID spanspec_init = env->GetMethodID(spanspec_class, "<init>", "(Ljava/lang/String;II)V");

    WebUrlFilter* p_filter = (WebUrlFilter*) ptr;
    Scheme* scheme;
    jsize i, j;
    JObjectList result_vector;

    for (i = 0; i < text_len - 3; i++) {
        const jchar chr = text_str[i];
        if (i == 0 || chr < L'a' || L'z' < chr) {
            // URL開始の直前はa～z以外の文字であるか、もしくはtextの開始時
            // その次の文字は当然a～zのみ
            const jchar next_chr = text_str[i + 1];
            if (next_chr < L'a' || L'z' < next_chr) continue;

            jsize start = i;
            if (i != 0) start++;
            int found = -1;

            for (int s = 0; s < p_filter->schemes_len_; s++) {
                scheme = p_filter->schemes_[s];
                if (start + scheme->len_ + 3 < text_len) {
                    if (std::memcmp(text_str + start, scheme->str_, sizeof(jchar) * (scheme->len_)) == 0) {
                        found = s;
                        break;
                    }
                }
            }
            if (found == -1) continue;
            scheme = p_filter->schemes_[found];
            jsize start2 = start + scheme->len_;
            // "://"
            if (text_len - start2 < 4 || //
                    text_str[start2] != L':' || text_str[start2 + 1] != L'/' || text_str[start2 + 2] != L'/') {
                continue;
            }

            // TODO 簡易判定です
            // [-_.!~*'()a-zA-Z0-9;/?:@&=+$,%#]+
            start2 += 3;
            for (j = start2; j < text_len; j++) {
                const jchar c = text_str[j];
                if ( //
                (c == 0x21) || // !
                        (0x23 <= c && c <= 0x3b) || // #$%&'()*+,-./ [0-9] :;
                        (c == 0x3d) || // =
                        (0x3f <= c && c <= 0x5a) || // ?@ [A-Z]
                        (c == 0x5f) || // _
                        (0x61 <= c && c <= 0x7a) || // [a-z]
                        (c == 0x7e) // ~
                ) {
                    continue;
                }
                else {
                    break;
                }
            }
            if (j - start2 <= 0) continue;
            jsize end = j;
            jstring url = env->NewString(text_str + start, end - start);
            jobject span_spec = env->NewObject(spanspec_class, spanspec_init, url, start, end);
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

/*
 * Class:     info_narazaki_android_lib_text_span_WebURLFilter
 * Method:    initNative
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_info_narazaki_android_lib_text_span_WebURLFilter_initNative(JNIEnv *env, jclass cls) {

}

