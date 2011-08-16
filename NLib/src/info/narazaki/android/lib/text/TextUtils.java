package info.narazaki.android.lib.text;

public class TextUtils {
    public static native void initNative();
    
    public static native boolean is2chAsciiArt(String entry);
    
    public static int parseInt(String text) {
        return parseInt(text, 0, text.length());
    }
    
    public static native int parseInt(String text, int start, int len);
    
    public static long parseLong(String text) {
        return parseLong(text, 0, text.length());
    }
    
    public static native long parseLong(String text, int start, int len);
    
    static {
        System.loadLibrary("info_narazaki_android_nlib");
        initNative();
    }
}
