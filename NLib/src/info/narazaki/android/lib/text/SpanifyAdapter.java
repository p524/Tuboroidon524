package info.narazaki.android.lib.text;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.text.Spannable;
import android.text.Spanned;
import android.text.style.URLSpan;

/**
 * カスタマイズ可能なSpanify
 * 
 * @author H.Narazaki
 */
public class SpanifyAdapter {
    public static final String TAG = "SpanifyAdapter";
    
    public static class SpanSpec {
        public String text_;
        public int start_;
        public int end_;
        
        public SpanSpec(String text, int start, int end) {
            text_ = text;
            start_ = start;
            end_ = end;
        }
    }
    
    public static interface SpanifyFilter {
        public List<SpanSpec> gather(Spannable text, Object arg);
        
        public Object getSpan(String text, Object arg);
    }
    
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
        class SpecList {
            public SpanifyFilter filter_;
            public List<SpanSpec> spec_list_;
            
            public SpecList(SpanifyFilter filter, List<SpanSpec> spec_list) {
                filter_ = filter;
                spec_list_ = spec_list;
            }
        }
        
        LinkedList<SpecList> spec_list_list = new LinkedList<SpecList>();
        for (SpanifyFilter filter : filter_list_) {
            spec_list_list.add(new SpecList(filter, filter.gather(text, arg)));
        }
        
        for (SpecList spec_list : spec_list_list) {
            for (SpanSpec spec : spec_list.spec_list_) {
                text.setSpan(spec_list.filter_.getSpan(spec.text_, arg), spec.start_, spec.end_,
                        Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            }
        }
    }
    
    public static abstract class PatternFilter implements SpanifyFilter {
        
        @Override
        public List<SpanSpec> gather(Spannable text, Object arg) {
            List<SpanSpec> result = new LinkedList<SpanSpec>();
            
            Pattern pattern = getPattern();
            if (pattern == null) return result;
            
            int pattern_cap = getPatternCaptureIndex();
            
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                int start = matcher.start();
                int end = matcher.end();
                
                SpanSpec spec = new SpanSpec(matcher.group(pattern_cap), start, end);
                result.add(spec);
            }
            
            return result;
        }
        
        abstract protected Pattern getPattern();
        
        protected int getPatternCaptureIndex() {
            return 0;
        }
    }
    
    public static class WebURLFilter extends PatternFilter {
        private static final Pattern URL_PATTERN = Pattern.compile("s?https?://[-_.!~*'()a-zA-Z0-9;/?:@&=+$,%#]+");
        
        @Override
        protected Pattern getPattern() {
            return URL_PATTERN;
        }
        
        @Override
        public Object getSpan(String text, Object arg) {
            URLSpan span = new URLSpan(text);
            return span;
        }
    }
}
