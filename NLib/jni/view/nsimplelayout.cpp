/*
 * nsimplelayout.cpp
 *
 *  Created on: 2011/01/20
 *      Author: H.Narazaki
 */

#include "nsimplelayout.h"
#include <android/log.h>

#include <cctype>
#include <cstdlib>
#include <cstring>
#include <string>

#include <hash_map>

#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO, "native nsimplelayout.cpp", __VA_ARGS__))

static const int LINES_I_START = info_narazaki_android_lib_view_NSimpleLayout_LINES_I_START;
static const int LINES_I_FM_TOP = info_narazaki_android_lib_view_NSimpleLayout_LINES_I_FM_TOP;
static const int LINES_I_FM_DESCENT = info_narazaki_android_lib_view_NSimpleLayout_LINES_I_FM_DESCENT;
static const int LINES_I_WIDTH = info_narazaki_android_lib_view_NSimpleLayout_LINES_I_WIDTH;
static const int LINES_I_MAX = info_narazaki_android_lib_view_NSimpleLayout_LINES_I_MAX;

static const int FM_I_TOP = info_narazaki_android_lib_view_NSimpleLayout_FM_I_TOP;
static const int FM_I_BOTTOM = info_narazaki_android_lib_view_NSimpleLayout_FM_I_BOTTOM;
static const int FM_I_ASCENT = info_narazaki_android_lib_view_NSimpleLayout_FM_I_ASCENT;
static const int FM_I_DESCENT = info_narazaki_android_lib_view_NSimpleLayout_FM_I_DESCENT;
static const int FM_I_LEADING = info_narazaki_android_lib_view_NSimpleLayout_FM_I_LEADING;
static const int FM_I_MAX = info_narazaki_android_lib_view_NSimpleLayout_FM_I_MAX;

static const jchar FIRST_CJK = 0x2e80;

static void reallocLinesBuffer(jint** lines, jsize *lines_bufsize, jsize new_size) {
    jsize want = new_size * LINES_I_MAX;
    if (*lines_bufsize > want) return;
    *lines_bufsize = want * 2;
    *lines = static_cast<jint *> (std::realloc(*lines, sizeof(jint) * (*lines_bufsize)));
}

static bool isUnbreakableNotCJK(jchar c) {
    if (c >= FIRST_CJK) return false;
    if (c == L' ' || c == L'\t') return false;
    if (c == L'.' || c == L',' || c == L':' || c == L';' || c == L'/' || c == L'-') return false;
    if (c <= 0x1f) return false;
    return true;
}

static bool isUnbreakableCJK(jchar c) {
    if (c < FIRST_CJK) return false;

    if (c >= 0x2E80 && c <= 0x2FFF) {
        return false; // CJK, KANGXI RADICALS, DESCRIPTION SYMBOLS
    }
    if (c == 0x3000) {
        return false; // IDEOGRAPHIC SPACE
    }
    if (c >= 0x3040 && c <= 0x309F) {
        switch (c) {
        case 0x3041: //  # HIRAGANA LETTER SMALL A
        case 0x3043: //  # HIRAGANA LETTER SMALL I
        case 0x3045: //  # HIRAGANA LETTER SMALL U
        case 0x3047: //  # HIRAGANA LETTER SMALL E
        case 0x3049: //  # HIRAGANA LETTER SMALL O
        case 0x3063: //  # HIRAGANA LETTER SMALL TU
        case 0x3083: //  # HIRAGANA LETTER SMALL YA
        case 0x3085: //  # HIRAGANA LETTER SMALL YU
        case 0x3087: //  # HIRAGANA LETTER SMALL YO
        case 0x308E: //  # HIRAGANA LETTER SMALL WA
        case 0x3095: //  # HIRAGANA LETTER SMALL KA
        case 0x3096: //  # HIRAGANA LETTER SMALL KE
        case 0x309B: //  # KATAKANA-HIRAGANA VOICED SOUND MARK
        case 0x309C: //  # KATAKANA-HIRAGANA SEMI-VOICED SOUND MARK
        case 0x309D: //  # HIRAGANA ITERATION MARK
        case 0x309E: //  # HIRAGANA VOICED ITERATION MARK
            return true;
        }
        return false; // Hiragana (except small characters)
    }
    if (c >= 0x30A0 && c <= 0x30FF) {
        switch (c) {
        case 0x30A0: //  # KATAKANA-HIRAGANA DOUBLE HYPHEN
        case 0x30A1: //  # KATAKANA LETTER SMALL A
        case 0x30A3: //  # KATAKANA LETTER SMALL I
        case 0x30A5: //  # KATAKANA LETTER SMALL U
        case 0x30A7: //  # KATAKANA LETTER SMALL E
        case 0x30A9: //  # KATAKANA LETTER SMALL O
        case 0x30C3: //  # KATAKANA LETTER SMALL TU
        case 0x30E3: //  # KATAKANA LETTER SMALL YA
        case 0x30E5: //  # KATAKANA LETTER SMALL YU
        case 0x30E7: //  # KATAKANA LETTER SMALL YO
        case 0x30EE: //  # KATAKANA LETTER SMALL WA
        case 0x30F5: //  # KATAKANA LETTER SMALL KA
        case 0x30F6: //  # KATAKANA LETTER SMALL KE
        case 0x30FB: //  # KATAKANA MIDDLE DOT
        case 0x30FC: //  # KATAKANA-HIRAGANA PROLONGED SOUND MARK
        case 0x30FD: //  # KATAKANA ITERATION MARK
        case 0x30FE: //  # KATAKANA VOICED ITERATION MARK
            return true;
        }
        return false; // Katakana (except small characters)
    }
    if (c >= 0x3400 && c <= 0x4DB5) {
        return false; // CJK UNIFIED IDEOGRAPHS EXTENSION A
    }
    if (c >= 0x4E00 && c <= 0x9FBB) {
        return false; // CJK UNIFIED IDEOGRAPHS
    }
    if (c >= 0xF900 && c <= 0xFAD9) {
        return false; // CJK COMPATIBILITY IDEOGRAPHS
    }
    if (c >= 0xA000 && c <= 0xA48F) {
        return false; // YI SYLLABLES
    }
    if (c >= 0xA490 && c <= 0xA4CF) {
        return false; // YI RADICALS
    }
    if (c >= 0xFE62 && c <= 0xFE66) {
        return false; // SMALL PLUS SIGN to SMALL EQUALS SIGN
    }
    if (c >= 0xFF10 && c <= 0xFF19) {
        return false; // WIDE DIGITS
    }

    return true;
}

JNIEXPORT jintArray JNICALL Java_info_narazaki_android_lib_view_NSimpleLayout_generateNative(JNIEnv * env,
        jobject self, jstring text, jint max_width, jintArray lines_tmp, jfloatArray width_array,
        jintArray metric_change_point, jfloatArray metric_list, jint metric_changes) {
    // 初期化
    const jchar* text_str = env->GetStringChars(text, NULL);
    const jsize text_len = env->GetStringLength(text);
    jfloat* p_width_array = env->GetFloatArrayElements(width_array, NULL);
    const jsize width_array_len = env->GetArrayLength(width_array);
    jint* p_metric_change_point = env->GetIntArrayElements(metric_change_point, NULL);
    jfloat* p_metric_list = env->GetFloatArrayElements(metric_list, NULL);
    jint* p_lines_tmp = env->GetIntArrayElements(lines_tmp, NULL);
    const jsize lines_tmp_len = env->GetArrayLength(lines_tmp);

    // 禁則に基づいて横幅を擬似的に調整する
    jfloat* tweaked_width = static_cast<jfloat*> (std::malloc(sizeof(jfloat) * width_array_len));
    std::memcpy(tweaked_width, p_width_array, sizeof(jfloat) * width_array_len);

    jint max_token_width = max_width / 4;
    for (int i = 0; i < text_len; i++) {
        jchar chr = text_str[i];

        // 数値モード
        // 数字、および.,:;/-では改行しない
        if (L'0' <= chr && chr <= L'9') {
            jfloat width = p_width_array[i];
            int j = i + 1;
            int mode = 0;
            for (; j < text_len; j++) {
                jchar c = text_str[j];
                if (L'0' <= c && c <= L'9') {
                    width += p_width_array[j];
                }
                else if ((mode == 0 || mode == 1) && (c == L'.' || c == L',' || c == L':' || c == L';' || c == L'/'
                        || c == L'-')) {
                    mode = 1;
                    width += p_width_array[j];
                }
                else if ((mode == 0 || mode == 2) && isUnbreakableNotCJK(c)) {
                    mode = 2;
                    width += p_width_array[j];
                }
                else {
                    break;
                }
            }
            if (width <= max_token_width) {
                // i～j-1まで改行しない
                tweaked_width[i] = width;
                for (int k = i + 1; k < j; k++) {
                    tweaked_width[k] = 0;
                }
                i = j - 1;
                continue;
            }
            // 長い数値はあきらめて放置……と言いたいところだけど英文モードならいけるかも?
        }
        if (isUnbreakableNotCJK(chr)) {
            // 英文モード
            jfloat width = p_width_array[i];
            int j = i + 1;
            for (; j < text_len; j++) {
                jchar c = text_str[j];
                if (isUnbreakableNotCJK(c)) {
                    width += p_width_array[j];
                }
                else {
                    break;
                }
            }
            if (width > max_token_width) {
                // 長い英単語はあきらめて放置
            }
            else {
                // i～j-1まで改行しない
                tweaked_width[i] = width;
                for (int k = i + 1; k < j; k++) {
                    tweaked_width[k] = 0;
                }
            }
            i = j - 1;
        }
        else if (chr >= FIRST_CJK) {
            // 日本語禁則モード
            if (i + 1 >= text_len || !isUnbreakableCJK(text_str[i + 1])) {
                continue;
            }
            jfloat width = p_width_array[i];
            int j = i + 1;
            for (; j < text_len; j++) {
                jchar c = text_str[j];
                if (isUnbreakableCJK(c)) {
                    width += p_width_array[j];
                }
                else {
                    break;
                }
            }
            if (width > max_token_width) {
                // 長い禁則文字列はあきらめて放置
            }
            else {
                // i～j-1まで改行しない
                tweaked_width[i] = width;
                for (int k = i + 1; k < j; k++) {
                    tweaked_width[k] = 0;
                }
            }
            i = j - 1;
        }
        else {
            // その他(スペースなど)は単純にコピー
        }
    }

    //
    jsize lines_bufsize = lines_tmp_len;
    jint* lines = static_cast<jint*> (std::malloc(sizeof(jint) * lines_bufsize));

    jint current_height = 0;
    jint current_line = 0;

    jfloat top = p_metric_list[FM_I_TOP];
    jfloat descent = p_metric_list[FM_I_DESCENT];
    jfloat max_top = top;
    jfloat max_descent = descent;

    jfloat current_width = 0;
    jfloat broken_width = 0;

    int metric_index = 1;
    int next_metric_pos = -1;

    if (metric_changes > 1) next_metric_pos = p_metric_change_point[1];

    lines[current_line * LINES_I_MAX + LINES_I_START] = 0;
    lines[current_line * LINES_I_MAX + LINES_I_FM_TOP] = 0;
    for (int i = 0; i <= text_len; i++) {
        // check metrics change
        if (next_metric_pos >= 0 && i >= next_metric_pos) {
            top = p_metric_list[metric_index * FM_I_MAX + FM_I_TOP];
            descent = p_metric_list[metric_index * FM_I_MAX + FM_I_DESCENT];
            if (top > max_top) max_top = top;
            if (descent > max_descent) max_descent = descent;
            metric_index++;
            if (metric_index < metric_changes) {
                next_metric_pos = p_metric_change_point[metric_index];
            }
            else {
                next_metric_pos = -1;
            }
        }
        bool is_break = false;
        bool has_nr = false;
        if (i == text_len) {
            is_break = true;
            broken_width = current_width;
            current_width = 0;
        }
        else if (text_str[i] == L'\n') {
            is_break = true;
            has_nr = true;
            broken_width = current_width;
            current_width = 0;
        }
        else {
            float char_width = tweaked_width[i];
            float next_width = current_width + char_width;
            if (next_width > max_width) {
                is_break = true;
                broken_width = current_width;
                current_width = char_width;
            }
            else {
                current_width = next_width;
            }
        }

        if (is_break) {
            current_height += (int) (max_descent - max_top + 0.5);
            lines[current_line * LINES_I_MAX + LINES_I_FM_DESCENT] = (int) (max_descent + 0.5);
            lines[current_line * LINES_I_MAX + LINES_I_WIDTH] = broken_width;
            current_line++;
            reallocLinesBuffer(&lines, &lines_bufsize, current_line + 2);
            lines[current_line * LINES_I_MAX + LINES_I_START] = i + (has_nr ? 1 : 0);
            lines[current_line * LINES_I_MAX + LINES_I_FM_TOP] = current_height;
            max_top = top;
            max_descent = descent;
        }
    }
    lines[current_line * LINES_I_MAX + LINES_I_FM_DESCENT] = 0;
    lines[current_line * LINES_I_MAX + LINES_I_WIDTH] = 0;

    std::free(tweaked_width);

    // 戻り値を作成する
    jintArray result = lines_tmp;
    bool use_orig_linebuf = false;
    if (lines_bufsize == lines_tmp_len) {
        // p_lines_tmpにそのまま書き戻す
        std::memcpy(p_lines_tmp, lines, sizeof(jint) * lines_tmp_len);
        p_lines_tmp[lines_tmp_len - 1] = current_line;
        use_orig_linebuf = true;
    }
    else {
        // 新しい配列を作る
        result = env->NewIntArray((current_line + 2) * LINES_I_MAX);
        lines[(current_line + 2) * LINES_I_MAX - 1] = current_line;
        env->SetIntArrayRegion(result, 0, (current_line + 2) * LINES_I_MAX, lines);
    }
    std::free(lines);

    // 後始末
    env->ReleaseIntArrayElements(lines_tmp, p_lines_tmp, use_orig_linebuf ? 0 : JNI_ABORT);
    env->ReleaseFloatArrayElements(metric_list, p_metric_list, JNI_ABORT);
    env->ReleaseIntArrayElements(metric_change_point, p_metric_change_point, JNI_ABORT);
    env->ReleaseFloatArrayElements(width_array, p_width_array, JNI_ABORT);
    env->ReleaseStringChars(text, text_str);
    return result;
}

JNIEXPORT jboolean JNICALL Java_info_narazaki_android_lib_view_NSimpleLayout_checkSupportedNative(JNIEnv *env,
        jclass cls, jstring text) {
    const jchar* text_str = env->GetStringChars(text, NULL);
    const jsize text_len = env->GetStringLength(text);
    jboolean result = true;

    for (jsize i = 0; i < text_len; i++) {
        jchar chr = text_str[i];
        // タブ(0x09)不可
        if (chr == L'\t') {
            result = false;
            break;
        }

        // BIDI http://www.unicode.org/reports/tr9/

        // Implicit Bidi Controls
        // U+200E..U+200F LEFT-TO-RIGHT MARK..RIGHT-TO-LEFT MARK
        if (chr < 0x200e) continue;
        if (chr == 0x200e || chr == 0x200f) {
            result = false;
            break;
        }

        // Explicit Bidi Controls
        // U+202A..U+202E LEFT-TO-RIGHT EMBEDDING..RIGHT-TO-LEFT OVERRIDE
        if (0x202a <= chr && chr <= 0x202e) {
            result = false;
            break;
        }

        if (chr < 0x0590) continue;
        // http://www.unicode.org/Public/UNIDATA/extracted/DerivedBidiClass.txt
        // The unassigned code points that default to AL are in the ranges:
        //     [\u0600-\u07BF \uFB50-\uFDFF \uFE70-\uFEFF]
        //
        //     Arabic:            U+0600  -  U+06FF
        //     Syriac:            U+0700  -  U+074F
        //     Arabic_Supplement: U+0750  -  U+077F
        //     Thaana:            U+0780  -  U+07BF
        //     Arabic_Presentation_Forms_A:
        //                        U+FB50  -  U+FDFF
        //     Arabic_Presentation_Forms_B:
        //                        U+FE70  -  U+FEFF
        //           minus noncharacter code points.
        if ((0x0600 <= chr && chr <= 0x07bf) || (0xfb50 <= chr && chr <= 0xfdff) || (0xfe70 <= chr && chr <= 0xfeff)) {
            result = false;
            break;
        }

        // The unassigned code points that default to R are in the ranges:
        //     [\u0590-\u05FF \u07C0-\u08FF \uFB1D-\uFB4F \U00010800-\U00010FFF \U0001E800-\U0001EFFF]
        //
        //     Hebrew:            U+0590  -  U+05FF
        //     NKo:               U+07C0  -  U+07FF
        //     Cypriot_Syllabary: U+10800 - U+1083F
        //     Phoenician:        U+10900 - U+1091F
        //     Lydian:            U+10920 - U+1093F
        //     Kharoshthi:        U+10A00 - U+10A5F
        //     and any others in the ranges:
        //                        U+0800  -  U+08FF,
        //                        U+FB1D  -  U+FB4F,
        //                        U+10840 - U+10FFF,
        //                        U+1E800 - U+1EFFF
        if ((0x0590 <= chr && chr <= 0x05ff) || (0x07c0 <= chr && chr <= 0x08ff) || (0xfb1d <= chr && chr <= 0xfb4f)
                || (0x00010800 <= chr && chr <= 0x00010fff) || (0x0001e800 <= chr && chr <= 0x0001efff)) {
            result = false;
            break;
        }
    }

    env->ReleaseStringChars(text, text_str);
    return result;
}

