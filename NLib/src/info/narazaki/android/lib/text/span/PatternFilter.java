package info.narazaki.android.lib.text.span;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.text.Spannable;

public abstract class PatternFilter implements SpanifyFilter {
    @Override
    public SpanSpec[] gather(Spannable text, Object arg) {
        ArrayList<SpanSpec> result = new ArrayList<SpanSpec>();
        
        Pattern pattern = getPattern();
        if (pattern == null) return result.toArray(new SpanSpec[result.size()]);
        
        int pattern_cap = getPatternCaptureIndex();
        
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            
            SpanSpec spec = new SpanSpec(matcher.group(pattern_cap), start, end);
            result.add(spec);
        }
        
        return result.toArray(new SpanSpec[result.size()]);
    }
    
    abstract protected Pattern getPattern();
    
    protected int getPatternCaptureIndex() {
        return 0;
    }
}
