package info.narazaki.android.lib.text.span;

import android.text.Spannable;

public interface SpanifyFilter {
    public SpanSpec[] gather(Spannable text, Object arg);
    
    public Object getSpan(String text, SpanSpec spec, Object arg);
}
