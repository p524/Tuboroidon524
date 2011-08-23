package info.narazaki.android.tuboroid.text.span;

import android.text.Spannable;
import android.text.style.UnderlineSpan;
import info.narazaki.android.lib.text.span.SpanSpec;
import info.narazaki.android.lib.text.span.SpanifyFilter;

public class EntryAnchorFilter implements SpanifyFilter {
    
    @Override
    public SpanSpec[] gather(Spannable text, Object arg) {
        return gatherNative(text.toString());
    }
    
    public native EntryAnchorSpanSpec[] gatherNative(String text);
    
    public static native void initNative();
    
    static {
        System.loadLibrary("info_narazaki_android_tuboroid");
        initNative();
    }
    
    @Override
    public Object getSpan(String text, SpanSpec spec, Object arg) {
        return new UnderlineSpan();
    }
}
