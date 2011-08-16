/*
 * htmlutils.cpp
 *
 *  Created on: 2011/01/12
 *      Author: H.Narazaki
 */

#include "htmlutils.h"
#include <android/log.h>

#include <cctype>
#include <cstdlib>
#include <cstring>
#include <string>

#include <hash_map>

#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO, "native htmlutils.cpp", __VA_ARGS__))

static bool stripHtmlTags(const jchar* orig_str, const jsize orig_len, jchar** p_result_str, jsize* p_result_len,
        jboolean conv_br);
static bool unescapeHtml(const jchar* orig_str, const jsize orig_len, jchar** p_result_str, jsize* p_result_len);

typedef std::hash_map<std::string, jchar> EntitiesMapType;
static EntitiesMapType* g_html_entities;
static const int MAX_ENTITY_NAME_LEN = 10;

static jchar* appendJcharBuffer(const jchar *src, jsize len, jchar *dist, jsize dist_len, jsize* p_buf_len) {
    if (len + dist_len > *p_buf_len) {
        *p_buf_len = (len + dist_len) * 2;
        dist = static_cast<jchar *> (std::realloc(dist, sizeof(jchar) * (*p_buf_len)));
    }

    std::memcpy(dist + dist_len, src, sizeof(jchar) * len);
    return dist;
}

JNIEXPORT jstring JNICALL Java_info_narazaki_android_lib_text_HtmlUtils_escapeHtml(JNIEnv *env, jclass cls,
        jstring orig, jboolean escape_single_quote, jstring replace_lf) {
    if (orig == NULL) {
        return env->NewString((jchar*) "", 0);
    }
    const jchar* orig_str = env->GetStringChars(orig, NULL);
    const jsize orig_len = env->GetStringLength(orig);

    const jchar* lf_str = replace_lf != NULL ? env->GetStringChars(replace_lf, NULL) : NULL;
    const jsize lf_len = replace_lf != NULL ? env->GetStringLength(replace_lf) : 0;

    const jchar* orig_str_end = orig_str + orig_len;

    jsize buf_len = orig_len * 2;
    jchar *result_str = static_cast<jchar*> (std::malloc(sizeof(jchar) * buf_len));
    jsize result_len = 0;
    bool changed = false;

    const jchar* p = orig_str;
    const jchar* q = p;

    jchar token_buf[6];
    const jchar* token;
    jsize token_len = 0;

    for (; q != orig_str_end; q++) {
        switch (*q) {
        case L'\'':
            if (!escape_single_quote) continue;
            token_buf[0] = L'&';
            token_buf[1] = L'a';
            token_buf[2] = L'p';
            token_buf[3] = L'o';
            token_buf[4] = L's';
            token_buf[5] = L';';
            token = token_buf;
            token_len = 6;
            break;
        case L'&':
            token_buf[0] = L'&';
            token_buf[1] = L'a';
            token_buf[2] = L'm';
            token_buf[3] = L'p';
            token_buf[4] = L';';
            token = token_buf;
            token_len = 5;
            break;
        case L'"':
            token_buf[0] = L'&';
            token_buf[1] = L'q';
            token_buf[2] = L'u';
            token_buf[3] = L'o';
            token_buf[4] = L't';
            token_buf[5] = L';';
            token = token_buf;
            token_len = 6;
            break;
        case L'<':
            token_buf[0] = L'&';
            token_buf[1] = L'l';
            token_buf[2] = L't';
            token_buf[3] = L';';
            token = token_buf;
            token_len = 4;
            break;
        case L'>':
            token_buf[0] = L'&';
            token_buf[1] = L'g';
            token_buf[2] = L't';
            token_buf[3] = L';';
            token = token_buf;
            token_len = 4;
            break;
        case L'\n':
            if (lf_len == 0) continue;
            token = lf_str;
            token_len = lf_len;
            break;
        default:
            continue;
        }

        if (p < q) {
            jsize len = q - p;
            result_str = appendJcharBuffer(p, len, result_str, result_len, &buf_len);
            result_len += len;
        }
        result_str = appendJcharBuffer(token, token_len, result_str, result_len, &buf_len);
        result_len += token_len;

        p = q + 1;
        changed = true;
    }

    if (p < q) {
        jsize len = q - p;
        result_str = appendJcharBuffer(p, len, result_str, result_len, &buf_len);
        result_len += len;
    }

    env->ReleaseStringChars(orig, orig_str);
    if (lf_str != NULL) env->ReleaseStringChars(replace_lf, lf_str);
    if (!changed) {
        std::free(result_str);
        return orig;
    }

    jstring result = env->NewString(result_str, result_len);
    std::free(result_str);
    return result;
}

JNIEXPORT jstring JNICALL Java_info_narazaki_android_lib_text_HtmlUtils_stripAllHtmls(JNIEnv *env, jclass cls,
        jstring orig, jboolean conv_br) {
    if (orig == NULL) {
        return env->NewString((jchar*) "", 0);
    }
    const jchar* orig_str = env->GetStringChars(orig, NULL);
    const jsize orig_len = env->GetStringLength(orig);
    jchar* strip_result_str = NULL;
    jsize strip_result_len = 0;
    jchar* result_str = NULL;
    jsize result_len = 0;

    bool changed_strip = stripHtmlTags(orig_str, orig_len, &strip_result_str, &strip_result_len, conv_br);
    env->ReleaseStringChars(orig, orig_str);

    bool changed_unescape = unescapeHtml(strip_result_str, strip_result_len, &result_str, &result_len);

    std::free(strip_result_str);

    if (!changed_strip && !changed_unescape) {
        std::free(result_str);
        return orig;
    }

    jstring result = env->NewString(result_str, result_len);
    std::free(result_str);
    return result;
}

JNIEXPORT jstring JNICALL Java_info_narazaki_android_lib_text_HtmlUtils_unescapeHtml(JNIEnv *env, jclass cls,
        jstring orig) {
    if (orig == NULL) {
        return env->NewString((jchar*) "", 0);
    }
    const jchar* orig_str = env->GetStringChars(orig, NULL);
    const jsize orig_len = env->GetStringLength(orig);
    jchar* result_str = NULL;
    jsize result_len = 0;

    bool changed_unescape = unescapeHtml(orig_str, orig_len, &result_str, &result_len);
    env->ReleaseStringChars(orig, orig_str);

    if (!changed_unescape) {
        std::free(result_str);
        return orig;
    }

    jstring result = env->NewString(result_str, result_len);
    std::free(result_str);
    return result;
}

bool stripHtmlTags(const jchar* orig_str, const jsize orig_len, jchar** p_result_str, jsize* p_result_len,
        jboolean conv_br) {
    const jchar* orig_str_end = orig_str + orig_len;

    jchar *result_str = static_cast<jchar*> (std::malloc(sizeof(jchar) * orig_len));
    jsize result_len = 0;

    const jchar* p = orig_str;
    const jchar* q = p;

    for (; q != orig_str_end; q++) {
        if ((conv_br && (*q == L'\r' || *q == L'\n')) || *q == L'<') {
            if (p < q) {
                jsize len = q - p;
                std::memcpy(result_str + result_len, p, sizeof(jchar) * len);
                result_len += len;
            }
            p = q + 1;
        }

        if (*q == L'<') {
            const jchar* r = q;
            for (; r != orig_str_end; r++) {
                if (*r == L'>') {
                    if (conv_br) {
                        const jchar* tag_begin = q + 1;
                        const jchar* tag_end = r;
                        const jsize tag_len = tag_end - tag_begin;
                        // CHECK TAG
                        // BR
                        if (tag_len >= 2 && (tag_begin[0] == L'b' || tag_begin[0] == L'B') && (tag_begin[1] == L'r'
                                || tag_begin[1] == L'R')) {
                            result_str[result_len] = L'\n';
                            result_len++;
                        }
                    }
                    break;
                }
            }
            q = r;
            p = q + 1;
            if (r == orig_str_end) break;
        }
    }

    if (p < q) {
        jsize len = q - p;
        std::memcpy(result_str + result_len, p, sizeof(jchar) * len);
        result_len += len;
    }

    *p_result_str = result_str;
    *p_result_len = result_len;

    return orig_len != result_len;
}

static bool unescapeHtml(const jchar* orig_str, const jsize orig_len, jchar** p_result_str, jsize* p_result_len) {
    const jchar* orig_str_end = orig_str + orig_len;

    jchar *result_str = static_cast<jchar*> (std::malloc(sizeof(jchar) * orig_len));
    jsize result_len = 0;
    bool changed = false;

    const jchar* p = orig_str;
    const jchar* q = p;

    for (; q != orig_str_end; q++) {
        if (*q == L'&') {
            if (p < q) {
                jsize len = q - p;
                std::memcpy(result_str + result_len, p, sizeof(jchar) * len);
                result_len += len;
            }
            p = q + 1;
            const jchar* r = q + 1;
            bool is_entity = false;
            for (; r < q + MAX_ENTITY_NAME_LEN; r++) {
                if (r == orig_str_end || *r > 0xff || (!std::isalnum((char) *r & 0xff) && *r != L'#')) {
                    const jchar* name_begin = q + 1;
                    const jchar* name_end = r;
                    std::string::value_type p_name[MAX_ENTITY_NAME_LEN + 1];
                    std::string::size_type name_len = name_end - name_begin;
                    if (name_len == 0) break;
                    for (int i = 0; i < name_len; i++) {
                        p_name[i] = (char) name_begin[i];
                    }
                    p_name[name_len] = '\0';
                    std::string name(p_name, name_len);
                    EntitiesMapType::iterator it = g_html_entities->find(name);
                    if (it != g_html_entities->end()) {
                        result_str[result_len] = it->second;
                        result_len++;
                        is_entity = true;
                        break;
                    }
                    else if (name_len > 3 && p_name[0] == '#' && p_name[1] == 'x') {
                        // 16進
                        int code = std::strtol(p_name + 2, NULL, 16);
                        result_str[result_len] = code;
                        result_len++;
                        is_entity = true;
                        break;
                    }
                    else if (name_len > 2 && p_name[0] == '#' && p_name[1] != 'x') {
                        // 10進
                        int code = std::atoi(p_name + 1);
                        result_str[result_len] = code;
                        result_len++;
                        is_entity = true;
                        break;
                    }
                    break;
                }
            }
            if (is_entity) {
                changed = true;
                if (*r != L';') r--;
                q = r;
                p = q + 1;
                if (p == orig_str_end) break;
            }
            else {
                result_str[result_len] = L'&';
                result_len++;
            }
        }
    }
    if (p < q) {
        jsize len = q - p;
        std::memcpy(result_str + result_len, p, sizeof(jchar) * len);
        result_len += len;
    }

    *p_result_str = result_str;
    *p_result_len = result_len;

    return changed;
}

static bool isWhiteSpace(jchar c) {
    return (c == L' ' || c == L'\t');
}
static bool shrinkWhiteSpace(const jchar* orig_str, const jsize orig_len, jchar** p_result_str, jsize* p_result_len,
        jboolean f_trim) {
    const jchar* orig_str_end = orig_str + orig_len;

    jchar *result_str = static_cast<jchar*> (std::malloc(sizeof(jchar) * orig_len));
    bool changed = false;
    bool found = false;

    const jchar* p = orig_str;
    jchar* q = result_str;
    if (f_trim && isWhiteSpace(*p)) {
        changed = true;
        p++;
    }

    for (; p != orig_str_end; p++) {
        if (isWhiteSpace(*p)) {
            if (found) {
                changed = true;
                continue;
            }
            found = true;
            *q = ' ';
        }
        else if (f_trim && *p == L'\n') {
            if (q > result_str && isWhiteSpace(q[-1])) {
                changed = true;
                q--;
            }
            *q = '\n';
            if (p < orig_str_end - 1 && isWhiteSpace(p[1])) {
                changed = true;
                p++;
            }
        }
        else {
            found = false;
            *q = *p;
        }
        q++;
    }

    if (f_trim && q > result_str && isWhiteSpace(q[-1])) {
        changed = true;
        q--;
    }

    *p_result_str = result_str;
    *p_result_len = q - result_str;

    return changed;
}
JNIEXPORT jstring JNICALL Java_info_narazaki_android_lib_text_HtmlUtils_shrinkHtml(JNIEnv *env, jclass cls,
        jstring orig, jboolean f_trim) {
    if (orig == NULL) {
        return env->NewString((jchar*) "", 0);
    }
    const jchar* orig_str = env->GetStringChars(orig, NULL);
    const jsize orig_len = env->GetStringLength(orig);
    jchar* strip_result_str = NULL;
    jsize strip_result_len = 0;
    jchar* unescape_result_str = NULL;
    jsize unescape_result_len = 0;
    jchar* shrink_result_str = NULL;
    jsize shrink_result_len = 0;

    bool changed_strip = stripHtmlTags(orig_str, orig_len, &strip_result_str, &strip_result_len, true);
    env->ReleaseStringChars(orig, orig_str);

    bool changed_unescape =
            unescapeHtml(strip_result_str, strip_result_len, &unescape_result_str, &unescape_result_len);
    std::free(strip_result_str);

    bool changed_shrink = shrinkWhiteSpace(unescape_result_str, unescape_result_len, &shrink_result_str,
            &shrink_result_len, f_trim);
    std::free(unescape_result_str);

    if (!changed_strip && !changed_unescape && !changed_shrink) {
        std::free(shrink_result_str);
        return orig;
    }

    jstring result = env->NewString(shrink_result_str, shrink_result_len);
    std::free(shrink_result_str);
    return result;
}

JNIEXPORT void JNICALL Java_info_narazaki_android_lib_text_HtmlUtils_initNative(JNIEnv *env, jclass cls) {
    g_html_entities = new EntitiesMapType();
    (*g_html_entities)["quot"] = 34;
    (*g_html_entities)["amp"] = 38;
    (*g_html_entities)["lt"] = 60;
    (*g_html_entities)["gt"] = 62;
    (*g_html_entities)["nbsp"] = 160;
    (*g_html_entities)["iexcl"] = 161;
    (*g_html_entities)["cent"] = 162;
    (*g_html_entities)["pound"] = 163;
    (*g_html_entities)["curren"] = 164;
    (*g_html_entities)["yen"] = 165;
    (*g_html_entities)["brvbar"] = 166;
    (*g_html_entities)["sect"] = 167;
    (*g_html_entities)["uml"] = 168;
    (*g_html_entities)["copy"] = 169;
    (*g_html_entities)["ordf"] = 170;
    (*g_html_entities)["laquo"] = 171;
    (*g_html_entities)["not"] = 172;
    (*g_html_entities)["shy"] = 173;
    (*g_html_entities)["reg"] = 174;
    (*g_html_entities)["macr"] = 175;
    (*g_html_entities)["deg"] = 176;
    (*g_html_entities)["plusmn"] = 177;
    (*g_html_entities)["sup2"] = 178;
    (*g_html_entities)["sup3"] = 179;
    (*g_html_entities)["acute"] = 180;
    (*g_html_entities)["micro"] = 181;
    (*g_html_entities)["para"] = 182;
    (*g_html_entities)["middot"] = 183;
    (*g_html_entities)["cedil"] = 184;
    (*g_html_entities)["sup1"] = 185;
    (*g_html_entities)["ordm"] = 186;
    (*g_html_entities)["raquo"] = 187;
    (*g_html_entities)["frac14"] = 188;
    (*g_html_entities)["frac12"] = 189;
    (*g_html_entities)["frac34"] = 190;
    (*g_html_entities)["iquest"] = 191;
    (*g_html_entities)["Agrave"] = 192;
    (*g_html_entities)["Aacute"] = 193;
    (*g_html_entities)["Acirc"] = 194;
    (*g_html_entities)["Atilde"] = 195;
    (*g_html_entities)["Auml"] = 196;
    (*g_html_entities)["Aring"] = 197;
    (*g_html_entities)["AElig"] = 198;
    (*g_html_entities)["Ccedil"] = 199;
    (*g_html_entities)["Egrave"] = 200;
    (*g_html_entities)["Eacute"] = 201;
    (*g_html_entities)["Ecirc"] = 202;
    (*g_html_entities)["Euml"] = 203;
    (*g_html_entities)["Igrave"] = 204;
    (*g_html_entities)["Iacute"] = 205;
    (*g_html_entities)["Icirc"] = 206;
    (*g_html_entities)["Iuml"] = 207;
    (*g_html_entities)["ETH"] = 208;
    (*g_html_entities)["Ntilde"] = 209;
    (*g_html_entities)["Ograve"] = 210;
    (*g_html_entities)["Oacute"] = 211;
    (*g_html_entities)["Ocirc"] = 212;
    (*g_html_entities)["Otilde"] = 213;
    (*g_html_entities)["Ouml"] = 214;
    (*g_html_entities)["times"] = 215;
    (*g_html_entities)["Oslash"] = 216;
    (*g_html_entities)["Ugrave"] = 217;
    (*g_html_entities)["Uacute"] = 218;
    (*g_html_entities)["Ucirc"] = 219;
    (*g_html_entities)["Uuml"] = 220;
    (*g_html_entities)["Yacute"] = 221;
    (*g_html_entities)["THORN"] = 222;
    (*g_html_entities)["szlig"] = 223;
    (*g_html_entities)["agrave"] = 224;
    (*g_html_entities)["aacute"] = 225;
    (*g_html_entities)["acirc"] = 226;
    (*g_html_entities)["atilde"] = 227;
    (*g_html_entities)["auml"] = 228;
    (*g_html_entities)["aring"] = 229;
    (*g_html_entities)["aelig"] = 230;
    (*g_html_entities)["ccedil"] = 231;
    (*g_html_entities)["egrave"] = 232;
    (*g_html_entities)["eacute"] = 233;
    (*g_html_entities)["ecirc"] = 234;
    (*g_html_entities)["euml"] = 235;
    (*g_html_entities)["igrave"] = 236;
    (*g_html_entities)["iacute"] = 237;
    (*g_html_entities)["icirc"] = 238;
    (*g_html_entities)["iuml"] = 239;
    (*g_html_entities)["eth"] = 240;
    (*g_html_entities)["ntilde"] = 241;
    (*g_html_entities)["ograve"] = 242;
    (*g_html_entities)["oacute"] = 243;
    (*g_html_entities)["ocirc"] = 244;
    (*g_html_entities)["otilde"] = 245;
    (*g_html_entities)["ouml"] = 246;
    (*g_html_entities)["divide"] = 247;
    (*g_html_entities)["oslash"] = 248;
    (*g_html_entities)["ugrave"] = 249;
    (*g_html_entities)["uacute"] = 250;
    (*g_html_entities)["ucirc"] = 251;
    (*g_html_entities)["uuml"] = 252;
    (*g_html_entities)["yacute"] = 253;
    (*g_html_entities)["thorn"] = 254;
    (*g_html_entities)["yuml"] = 255;
    (*g_html_entities)["fnof"] = 402;
    (*g_html_entities)["Alpha"] = 913;
    (*g_html_entities)["Beta"] = 914;
    (*g_html_entities)["Gamma"] = 915;
    (*g_html_entities)["Delta"] = 916;
    (*g_html_entities)["Epsilon"] = 917;
    (*g_html_entities)["Zeta"] = 918;
    (*g_html_entities)["Eta"] = 919;
    (*g_html_entities)["Theta"] = 920;
    (*g_html_entities)["Iota"] = 921;
    (*g_html_entities)["Kappa"] = 922;
    (*g_html_entities)["Lambda"] = 923;
    (*g_html_entities)["Mu"] = 924;
    (*g_html_entities)["Nu"] = 925;
    (*g_html_entities)["Xi"] = 926;
    (*g_html_entities)["Omicron"] = 927;
    (*g_html_entities)["Pi"] = 928;
    (*g_html_entities)["Rho"] = 929;
    (*g_html_entities)["Sigma"] = 931;
    (*g_html_entities)["Tau"] = 932;
    (*g_html_entities)["Upsilon"] = 933;
    (*g_html_entities)["Phi"] = 934;
    (*g_html_entities)["Chi"] = 935;
    (*g_html_entities)["Psi"] = 936;
    (*g_html_entities)["Omega"] = 937;
    (*g_html_entities)["alpha"] = 945;
    (*g_html_entities)["beta"] = 946;
    (*g_html_entities)["gamma"] = 947;
    (*g_html_entities)["delta"] = 948;
    (*g_html_entities)["epsilon"] = 949;
    (*g_html_entities)["zeta"] = 950;
    (*g_html_entities)["eta"] = 951;
    (*g_html_entities)["theta"] = 952;
    (*g_html_entities)["iota"] = 953;
    (*g_html_entities)["kappa"] = 954;
    (*g_html_entities)["lambda"] = 955;
    (*g_html_entities)["mu"] = 956;
    (*g_html_entities)["nu"] = 957;
    (*g_html_entities)["xi"] = 958;
    (*g_html_entities)["omicron"] = 959;
    (*g_html_entities)["pi"] = 960;
    (*g_html_entities)["rho"] = 961;
    (*g_html_entities)["sigmaf"] = 962;
    (*g_html_entities)["sigma"] = 963;
    (*g_html_entities)["tau"] = 964;
    (*g_html_entities)["upsilon"] = 965;
    (*g_html_entities)["phi"] = 966;
    (*g_html_entities)["chi"] = 967;
    (*g_html_entities)["psi"] = 968;
    (*g_html_entities)["omega"] = 969;
    (*g_html_entities)["thetasym"] = 977;
    (*g_html_entities)["upsih"] = 978;
    (*g_html_entities)["piv"] = 982;
    (*g_html_entities)["bull"] = 8226;
    (*g_html_entities)["hellip"] = 8230;
    (*g_html_entities)["prime"] = 8242;
    (*g_html_entities)["Prime"] = 8243;
    (*g_html_entities)["oline"] = 8254;
    (*g_html_entities)["frasl"] = 8260;
    (*g_html_entities)["weierp"] = 8472;
    (*g_html_entities)["image"] = 8465;
    (*g_html_entities)["real"] = 8476;
    (*g_html_entities)["trade"] = 8482;
    (*g_html_entities)["alefsym"] = 8501;
    (*g_html_entities)["larr"] = 8592;
    (*g_html_entities)["uarr"] = 8593;
    (*g_html_entities)["rarr"] = 8594;
    (*g_html_entities)["darr"] = 8595;
    (*g_html_entities)["harr"] = 8596;
    (*g_html_entities)["crarr"] = 8629;
    (*g_html_entities)["lArr"] = 8656;
    (*g_html_entities)["uArr"] = 8657;
    (*g_html_entities)["rArr"] = 8658;
    (*g_html_entities)["dArr"] = 8659;
    (*g_html_entities)["hArr"] = 8660;
    (*g_html_entities)["forall"] = 8704;
    (*g_html_entities)["part"] = 8706;
    (*g_html_entities)["exist"] = 8707;
    (*g_html_entities)["empty"] = 8709;
    (*g_html_entities)["nabla"] = 8711;
    (*g_html_entities)["isin"] = 8712;
    (*g_html_entities)["notin"] = 8713;
    (*g_html_entities)["ni"] = 8715;
    (*g_html_entities)["prod"] = 8719;
    (*g_html_entities)["sum"] = 8721;
    (*g_html_entities)["minus"] = 8722;
    (*g_html_entities)["lowast"] = 8727;
    (*g_html_entities)["radic"] = 8730;
    (*g_html_entities)["prop"] = 8733;
    (*g_html_entities)["infin"] = 8734;
    (*g_html_entities)["ang"] = 8736;
    (*g_html_entities)["and"] = 8743;
    (*g_html_entities)["or"] = 8744;
    (*g_html_entities)["cap"] = 8745;
    (*g_html_entities)["cup"] = 8746;
    (*g_html_entities)["int"] = 8747;
    (*g_html_entities)["there4"] = 8756;
    (*g_html_entities)["sim"] = 8764;
    (*g_html_entities)["cong"] = 8773;
    (*g_html_entities)["asymp"] = 8776;
    (*g_html_entities)["ne"] = 8800;
    (*g_html_entities)["equiv"] = 8801;
    (*g_html_entities)["le"] = 8804;
    (*g_html_entities)["ge"] = 8805;
    (*g_html_entities)["sub"] = 8834;
    (*g_html_entities)["sup"] = 8835;
    (*g_html_entities)["nsub"] = 8836;
    (*g_html_entities)["sube"] = 8838;
    (*g_html_entities)["supe"] = 8839;
    (*g_html_entities)["oplus"] = 8853;
    (*g_html_entities)["otimes"] = 8855;
    (*g_html_entities)["perp"] = 8869;
    (*g_html_entities)["sdot"] = 8901;
    (*g_html_entities)["lceil"] = 8968;
    (*g_html_entities)["rceil"] = 8969;
    (*g_html_entities)["lfloor"] = 8970;
    (*g_html_entities)["rfloor"] = 8971;
    (*g_html_entities)["lang"] = 9001;
    (*g_html_entities)["rang"] = 9002;
    (*g_html_entities)["loz"] = 9674;
    (*g_html_entities)["spades"] = 9824;
    (*g_html_entities)["clubs"] = 9827;
    (*g_html_entities)["hearts"] = 9829;
    (*g_html_entities)["diams"] = 9830;
    (*g_html_entities)["OElig"] = 338;
    (*g_html_entities)["oelig"] = 339;
    (*g_html_entities)["Scaron"] = 352;
    (*g_html_entities)["scaron"] = 353;
    (*g_html_entities)["Yuml"] = 376;
    (*g_html_entities)["circ"] = 710;
    (*g_html_entities)["tilde"] = 732;
    (*g_html_entities)["ensp"] = 8194;
    (*g_html_entities)["emsp"] = 8195;
    (*g_html_entities)["thinsp"] = 8201;
    (*g_html_entities)["zwnj"] = 8204;
    (*g_html_entities)["zwj"] = 8205;
    (*g_html_entities)["lrm"] = 8206;
    (*g_html_entities)["rlm"] = 8207;
    (*g_html_entities)["ndash"] = 8211;
    (*g_html_entities)["mdash"] = 8212;
    (*g_html_entities)["lsquo"] = 8216;
    (*g_html_entities)["rsquo"] = 8217;
    (*g_html_entities)["sbquo"] = 8218;
    (*g_html_entities)["ldquo"] = 8220;
    (*g_html_entities)["rdquo"] = 8221;
    (*g_html_entities)["bdquo"] = 8222;
    (*g_html_entities)["dagger"] = 8224;
    (*g_html_entities)["Dagger"] = 8225;
    (*g_html_entities)["permil"] = 8240;
    (*g_html_entities)["lsaquo"] = 8249;
    (*g_html_entities)["rsaquo"] = 8250;
    (*g_html_entities)["euro"] = 8364;
}

