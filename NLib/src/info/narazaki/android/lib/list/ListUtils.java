package info.narazaki.android.lib.list;

public class ListUtils {
    public static native String[] split(String with, String orig);
    
    public static native int[] split(String with, String orig, int i);
    
    public static native long[] split(String with, String orig, long l);
    
    public static native void initNative();
    
    static {
        System.loadLibrary("info_narazaki_android_nlib");
        initNative();
    }
    
}
