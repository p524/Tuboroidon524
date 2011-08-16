package info.narazaki.android.lib.text;

/**
 * HTMLに関するユーティリティ関数群
 * 
 * @author H.Narazaki
 */
public class HtmlUtils {
    public static String escapeHtml(String orig) {
        return escapeHtml(orig, false, null);
    }
    
    public static native String escapeHtml(String orig, boolean escape_single_quote, String replace_lf);
    
    public static native void initNative();
    
    public static native String stripAllHtmls(String orig, boolean conv_br);
    
    public static native String unescapeHtml(String orig);
    
    public static native String shrinkHtml(String orig, boolean shrink);
    
    static {
        System.loadLibrary("info_narazaki_android_nlib");
        initNative();
    }
    
}
