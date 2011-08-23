package info.narazaki.android.lib.text.span;

import java.util.ArrayList;
import android.text.Spannable;
import android.text.Spanned;

/**
 * カスタマイズ可能なSpanify
 * 
 * @author H.Narazaki
 */
public class SpanifyAdapter {
    public static final String TAG = "SpanifyAdapter";
    
    private ArrayList<SpanifyFilter> filter_list_;
    private Spannable.Factory spannable_factory_;
    
    public SpanifyAdapter() {
        filter_list_ = new ArrayList<SpanifyFilter>();
        spannable_factory_ = Spannable.Factory.getInstance();
    }
    
    final public void addFilter(SpanifyFilter filter) {
        filter_list_.add(filter);
    }
    
    final public Spannable apply(CharSequence text) {
        return apply(text, null);
    }
    
    final public Spannable apply(CharSequence text, Object arg) {
        Spannable spannable = spannable_factory_.newSpannable(text);
        apply(spannable, arg);
        return spannable;
    }
    
    final public void apply(Spannable text) {
        apply(text, null);
    }
    
    final public void apply(Spannable text, Object arg) {
        for (SpanifyFilter filter : filter_list_) {
            SpanSpec[] spec_list = filter.gather(text, arg);
            if (spec_list != null) {
                for (SpanSpec spec : spec_list) {
                    text.setSpan(filter.getSpan(spec.text_, spec, arg), spec.start_, spec.end_,
                            Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                }
            }
        }
    }
    
}
