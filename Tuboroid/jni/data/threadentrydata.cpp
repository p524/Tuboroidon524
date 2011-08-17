/*
 * threadentrydata.cpp
 *
 *  Created on: 2011/01/26
 *      Author: H.Narazaki
 */

#include <jni.h>
#include "threadentrydata.h"
#include <android/log.h>

#include <cstdlib>
#include <cstring>

static const int HAN_SPACE = (0x0020);
static const int ZEN_SPACE = (0x3000);
static const int MIN_AA_CHR_SEQ_COUNT = 4;
static const int MIN_AA_LINE = 4;
static const int MIN_CHRS_FOR_AA_LINE = 8;

static bool isAAChar(jchar chr);

#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO, "native threadentrydata.cpp", __VA_ARGS__))

JNIEXPORT jboolean JNICALL Java_info_narazaki_android_tuboroid_data_ThreadEntryData_is2chAsciiArt(JNIEnv *env,
        jclass cls, jstring orig) {
    if (orig == NULL) {
        return false;
    }
    const jchar* str = env->GetStringChars(orig, NULL);
    const jsize len = env->GetStringLength(orig);
    const jchar* str_end = str + len;

    int aa_chr_seq_count = 0;
    int aa_chr_seq_count_tmp = 0;
    int line_chr_count = 0;
    int line_aa_chr_count = 0;
    int line_not_aa_chr_count = 0;
    int line_count = 0;
    int aa_line_count = 0;

    for (const jchar* p = str; p <= str_end; p++) {
        if (p == str_end || *p == L'\r' || *p == L'\n') {
            if (aa_chr_seq_count >= MIN_AA_CHR_SEQ_COUNT) {
                aa_line_count++;
            }
            else if (line_chr_count >= MIN_CHRS_FOR_AA_LINE && line_aa_chr_count >= line_not_aa_chr_count) {
                aa_line_count++;
            }
            line_count++;

            aa_chr_seq_count_tmp = 0;
            aa_chr_seq_count = 0;
            line_chr_count = 0;
            line_aa_chr_count = 0;
            line_not_aa_chr_count = 0;
            if (p == str_end) break;
        }
        else {
            if (isAAChar(*p)) {
                aa_chr_seq_count_tmp++;
                line_aa_chr_count++;
                if (aa_chr_seq_count < aa_chr_seq_count_tmp) aa_chr_seq_count = aa_chr_seq_count_tmp;
            }
            else if (*p == HAN_SPACE || *p == ZEN_SPACE) {
            }
            else {
                aa_chr_seq_count_tmp = 0;
                line_not_aa_chr_count++;
            }
            line_chr_count++;
        }
    }
    env->ReleaseStringChars(orig, str);

    if (aa_line_count >= MIN_AA_LINE && aa_line_count * 4 > line_count) return true;
    if (line_count > 4 && aa_line_count * 2 > line_count) return true;

    return false;
}

bool isAAChar(const jchar chr) {
    // 0x0027 - 0x0029 : '()
    // 0x002f          : /
    // 0x003c - 0x003d : <=
    // 0x005e - 0x0060 : ^_`
    // 0x007b - 0x007e : {|}~
    // 0x0080 - 0x3040 : ラテン語文字～各種記号～他言語文字～部首、など。ひらがなの手前まで(全角スペース0x3000を除く)
    // 0x3099 - 0x30a0 : ゙ ゚゛゜ゝゞゟ゠
    // 0x30fb - 0x30ff : ・ーヽヾヿ
    // 0x3100 - 0x33ff : 記号類。CJK統合漢字拡張Aの直前まで
    //  -- 0x3400 - 0x4d8f : CJK統合漢字拡張A
    // 0x4dc0 - 0x4dff : 易経記号
    //  -- 0x4e00 - 0x9fff : CJK統合漢字
    // 0xa000 - 0xfeff : 他言語文字
    // - 全角
    // 0xff07 - 0xff0f : ＇（）＊＋，－．／
    // 0xff1a - 0xff1e : ：；＜＝＞
    // 0xff3b - 0xff40 : ［＼］＾＿｀
    // 0xff5b - 0xff60 : ｛｜｝～｟｠
    // - 半角
    // 0xff61 - 0xff65 : ｡ ｢ ｣ ､ ･
    // 0xff9e - 0xff9f : ﾞ ﾟ

    // 0xff9e - 0x1ffff : ﾞﾟおよび記号類。他言語文字、かな文字補助(非JIS)、記号など
    //  -- 0x20000-0x2a6df : CJK統合漢字拡張B
    // 0x2a6e0 - 0x2F7ff : CJK統合漢字拡張C/D
    //  -- 0x2f800 - 0x2fa1f : CJK互換漢字補助(一応……)
    // これ以降は、タグ、字形選択子補助、私用領域
    if (0x0027 <= chr && chr <= 0x0029) return true;
    if (0x002f == chr) return true;
    if (0x003a <= chr && chr <= 0x003d) return true;
    if (0x005e <= chr && chr <= 0x0060) return true;
    if (0x007b <= chr && chr <= 0x3040) {
        if (chr == 0x3000) {
            // 全角スペース
            return false;
        }
        else if (0x25a0 <= chr && chr <= 0x25ff) {
            // ●とか▲とか
            return false;
        }
        else if (0x3007 <= chr && chr <= 0x301c) {
            // 「」とか【】とか～とか
            return false;
        }
        else {
            return true;
        }
    }
    if (0x3099 <= chr && chr <= 0x30a0) return true;
    if (0x30fb <= chr && chr <= 0x30ff) return true;
    if (0x3100 <= chr && chr <= 0x33ff) return true;
    if (0x4dc0 <= chr && chr <= 0x4dff) return true;
    // ここから0x9fffまで統合漢字
    if (chr <= 0x9fff) return false;

    // 易経記号
    if (0xa000 <= chr && chr <= 0xfeff) return true;

    // 全角記号
    if (0xff07 <= chr && chr <= 0xff0f) return true;
    if (0xff1a <= chr && chr <= 0xff1e) return true;
    if (0xff3b <= chr && chr <= 0xff40) return true;
    if (0xff5b <= chr && chr <= 0xff60) return true;
    if (0xff9e <= chr && chr <= 0x1ffff) return true;

    // 半角記号
    if (0xff61 <= chr && chr <= 0xff65) return true;
    if (0xff9e <= chr && chr <= 0xff9f) return true;

    // ここから0x2a6dfまでCJK統合漢字拡張B(一応JISらしい)
    if (chr <= 0x2a6df) return false;

    // この先はCJK互換漢字補助(一応JISらしい?)以外は全部記号扱いする
    if (0x2f800 <= chr && chr <= 0x2fa1f) return false;

    return true;
}
JNIEXPORT void JNICALL Java_info_narazaki_android_tuboroid_data_ThreadEntryData_initNative(JNIEnv *env, jclass cls) {
    
}
