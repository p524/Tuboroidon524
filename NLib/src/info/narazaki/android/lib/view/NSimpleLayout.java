package info.narazaki.android.lib.view;

import info.narazaki.android.lib.memory.SimpleFloatArrayPool;
import info.narazaki.android.lib.memory.SimpleIntArrayPool;
import info.narazaki.android.lib.memory.SimpleObjectArrayPool;
import android.graphics.Paint.FontMetrics;
import android.text.BoringLayout;
import android.text.Layout;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.LeadingMarginSpan;
import android.text.style.LineHeightSpan;
import android.text.style.MetricAffectingSpan;
import android.text.style.ReplacementSpan;
import android.util.Log;

public class NSimpleLayout extends Layout {
    public static final String TAG = "NSimpleLayout";
    
    protected static final TextPaint DUMMY_TEXTPAING;
    protected static final BoringLayout DUMMY_BORING;
    private static final SimpleFloatArrayPool g_width_float_pool_;
    private static final SimpleIntArrayPool g_lines_int_pool_;
    private static final SimpleFloatArrayPool g_metric_float_pool_;
    private static final SimpleIntArrayPool g_metric_int_pool_;
    private static final TextPaintPool g_textpaint_pool_;
    
    static {
        DUMMY_TEXTPAING = new TextPaint();
        DUMMY_BORING = new BoringLayout("", DUMMY_TEXTPAING, 1, Alignment.ALIGN_NORMAL, 1, 0,
                new BoringLayout.Metrics(), false);
        g_width_float_pool_ = new SimpleFloatArrayPool(4);
        g_lines_int_pool_ = new SimpleIntArrayPool(4);
        g_metric_float_pool_ = new SimpleFloatArrayPool(4);
        g_metric_int_pool_ = new SimpleIntArrayPool(4);
        g_textpaint_pool_ = new TextPaintPool(2);
    }
    
    private float[] width_array_ = null;
    
    private static final int LINES_I_START = 0;
    private static final int LINES_I_FM_TOP = 1;
    private static final int LINES_I_FM_DESCENT = 2;
    private static final int LINES_I_WIDTH = 3;
    private static final int LINES_I_MAX = 4;
    
    private static final int FM_I_TOP = 0;
    private static final int FM_I_BOTTOM = 1;
    private static final int FM_I_ASCENT = 2;
    private static final int FM_I_DESCENT = 3;
    private static final int FM_I_LEADING = 4;
    private static final int FM_I_MAX = 5;
    
    private int line_count_ = 0;
    private int lines_[] = null;
    
    public NSimpleLayout(Spanned text, TextPaint paint, int width, Alignment align, float spacingMult,
            float spacingAdd, boolean includepad) {
        super(text, paint, width, align, spacingMult, spacingAdd);
        generate(text, paint, width, align);
    }
    
    @Override
    protected void finalize() throws Throwable {
        recycle();
        super.finalize();
    }
    
    public void recycle() {
        if (width_array_ != null) {
            width_array_ = g_width_float_pool_.recycle(width_array_);
        }
        if (lines_ != null) {
            lines_ = g_lines_int_pool_.recycle(lines_);
        }
    }
    
    private void generate(Spanned text, TextPaint paint, int width, Alignment align) {
        int text_len = text.length();
        width_array_ = g_width_float_pool_.obtain(text_len + 1);
        float[] work_width_array = g_width_float_pool_.obtain(text_len + 1);
        
        // デフォルトのMetricで計算
        paint.getTextWidths(text.toString(), width_array_);
        FontMetrics default_fm = paint.getFontMetrics();
        FontMetrics fm = new FontMetrics();
        
        // MetricAffectingSpanの部分を再計算
        MetricAffectingSpan[] metric_affecting_spans = text.getSpans(0, text_len, MetricAffectingSpan.class);
        int[] metric_change_point = g_metric_int_pool_.obtain((metric_affecting_spans.length + 1) * 2);
        float[] metric_list = g_metric_float_pool_.obtain((metric_affecting_spans.length + 1) * FM_I_MAX);
        metric_change_point[0] = 0;
        metric_change_point[1] = 0;
        metric_list[FM_I_TOP] = default_fm.top;
        metric_list[FM_I_BOTTOM] = default_fm.bottom;
        metric_list[FM_I_ASCENT] = default_fm.ascent;
        metric_list[FM_I_DESCENT] = default_fm.descent;
        metric_list[FM_I_LEADING] = default_fm.leading;
        int metric_changes = 1;
        int spans_len = metric_affecting_spans.length;
        if (spans_len > 0) {
            TextPaint work_paint = g_textpaint_pool_.obtain();
            for (int i = 0; i < spans_len; i++) {
                MetricAffectingSpan span = metric_affecting_spans[i];
                int start = text.getSpanStart(span);
                int end = text.getSpanEnd(span);
                int len = end - start;
                if (len > 0) {
                    work_paint.set(paint);
                    span.updateMeasureState(work_paint);
                    len = work_paint.getTextWidths(text, start, end, work_width_array);
                    System.arraycopy(work_width_array, 0, width_array_, start, len);
                    work_paint.getFontMetrics(fm);
                    if (default_fm.top != fm.top || default_fm.bottom != fm.bottom || default_fm.ascent != fm.ascent
                            || default_fm.descent != fm.descent || default_fm.leading != fm.leading) {
                        metric_change_point[metric_changes * 2] = start;
                        metric_change_point[metric_changes * 2 + 1] = end;
                        metric_list[metric_changes * FM_I_MAX + FM_I_TOP] = fm.top;
                        metric_list[metric_changes * FM_I_MAX + FM_I_BOTTOM] = fm.bottom;
                        metric_list[metric_changes * FM_I_MAX + FM_I_ASCENT] = fm.ascent;
                        metric_list[metric_changes * FM_I_MAX + FM_I_DESCENT] = fm.descent;
                        metric_list[metric_changes * FM_I_MAX + FM_I_LEADING] = fm.leading;
                        metric_changes++;
                    }
                }
            }
            g_textpaint_pool_.recycle(work_paint);
        }
        work_width_array = g_width_float_pool_.recycle(work_width_array);
        
        // 禁則処理付き折り返し
        int[] lines_tmp = g_lines_int_pool_.obtain((text.length() / 20 + 2) * LINES_I_MAX);
        lines_ = generateNative(text.toString(), width, lines_tmp, width_array_, metric_change_point, metric_list,
                metric_changes);
        if (lines_ != lines_tmp) {
            lines_tmp = g_lines_int_pool_.recycle(lines_tmp);
        }
        line_count_ = lines_[lines_.length - 1];
        
        g_metric_int_pool_.recycle(metric_change_point);
        g_metric_float_pool_.recycle(metric_list);
    }
    
    private native int[] generateNative(String text, int max_width, int[] lines_tmp, float[] width_array,
            int[] metric_change_point, float[] metric_list, int metric_changes);
    
    private void reallocLines(int size) {
        int want = size * LINES_I_MAX;
        if (lines_ != null && lines_.length > want) {
            return;
        }
        int[] new_lines = g_lines_int_pool_.obtain(want * 2 + 1);
        if (lines_ != null) {
            System.arraycopy(lines_, 0, new_lines, 0, lines_.length);
            g_lines_int_pool_.recycle(lines_);
        }
        lines_ = new_lines;
    }
    
    // check unsupported span
    public static boolean checkSupported(Spanned text, TextPaint paint, int width, Alignment align, float spacingMult,
            float spacingAdd, boolean includepad) {
        if (spacingMult != 1 || spacingAdd != 0) return false;
        if (align != Alignment.ALIGN_NORMAL) return false;
        if (!includepad) return false;
        int len = text.length();
        int found = text.nextSpanTransition(0, len, LeadingMarginSpan.class);
        if (found != len) return false;
        
        found = text.nextSpanTransition(0, len, LineHeightSpan.class);
        if (found != len) return false;
        
        found = text.nextSpanTransition(0, len, ReplacementSpan.class);
        if (found != len) return false;
        
        return checkSupportedNative(text.toString());
    }
    
    private static native boolean checkSupportedNative(String text);
    
    /**
     * Gets the horizontal extent of the specified line, including trailing
     * whitespace.
     */
    @Override
    public float getLineWidth(int line) {
        if (line > line_count_) return 0;
        return lines_[line * LINES_I_MAX + LINES_I_WIDTH];
    }
    
    /**
     * Return the number of lines of text in this layout.
     */
    @Override
    public int getLineCount() {
        return line_count_;
    }
    
    /**
     * Return the vertical position of the top of the specified line
     * (0&hellip;getLineCount()). If the specified line is equal to the line
     * count, returns the bottom of the last line.
     */
    @Override
    public int getLineTop(int line) {
        if (line > line_count_) return line = line_count_;
        return lines_[line * LINES_I_MAX + LINES_I_FM_TOP];
    }
    
    /**
     * Return the descent of the specified line(0&hellip;getLineCount() - 1).
     */
    @Override
    public int getLineDescent(int line) {
        if (line > line_count_) return line = line_count_;
        return lines_[line * LINES_I_MAX + LINES_I_FM_DESCENT];
    }
    
    /**
     * Return the text offset of the beginning of the specified line (
     * 0&hellip;getLineCount()). If the specified line is equal to the line
     * count, returns the length of the text.
     */
    @Override
    public int getLineStart(int line) {
        if (line > line_count_) return line = line_count_;
        return lines_[line * LINES_I_MAX + LINES_I_START];
    }
    
    /**
     * Returns the primary directionality of the paragraph containing the
     * specified line, either 1 for left-to-right lines, or -1 for right-to-left
     * lines (see {@link #DIR_LEFT_TO_RIGHT}, {@link #DIR_RIGHT_TO_LEFT}).
     */
    @Override
    public int getParagraphDirection(int line) {
        return DIR_LEFT_TO_RIGHT;
    }
    
    /**
     * Returns whether the specified line contains one or more characters that
     * need to be handled specially, like tabs or emoji.
     */
    @Override
    public boolean getLineContainsTab(int line) {
        return false;
    }
    
    /**
     * Returns the directional run information for the specified line. The array
     * alternates counts of characters in left-to-right and right-to-left
     * segments of the line.
     * 
     * <p>
     * NOTE: this is inadequate to support bidirectional text, and will change.
     */
    @Override
    public Directions getLineDirections(int line) {
        // Directions is damn private...
        return DUMMY_BORING.getLineDirections(0);
    }
    
    /**
     * Returns the (negative) number of extra pixels of ascent padding in the
     * top line of the Layout.
     */
    @Override
    public int getTopPadding() {
        return 0;
    }
    
    /**
     * Returns the number of extra pixels of descent padding in the bottom line
     * of the Layout.
     */
    @Override
    public int getBottomPadding() {
        return 0;
    }
    
    /**
     * Return the offset of the first character to be ellipsized away, relative
     * to the start of the line. (So 0 if the beginning of the line is
     * ellipsized, not getLineStart().)
     */
    @Override
    public int getEllipsisStart(int line) {
        return getLineStart(line);
    }
    
    /**
     * Returns the number of characters to be ellipsized away, or 0 if no
     * ellipsis is to take place.
     */
    @Override
    public int getEllipsisCount(int line) {
        return 0;
    }
    
    @Override
    public int getOffsetForHorizontal(int line, float horiz) {
        int start = getLineStart(line);
        int end = getLineEnd(line);
        if (end - 1 <= start) return start;
        float width = 0;
        for (int i = start + 1; i < end; i++) {
            width += width_array_[i];
            if (width > horiz) return i - 1;
        }
        return end - 1;
    }
    
    static private class TextPaintPool extends SimpleObjectArrayPool<TextPaint> {
        public TextPaintPool(int max_pool) {
            super(max_pool);
        }
        
        @Override
        protected TextPaint construct() {
            return new TextPaint();
        }
    }
    
    static {
        System.loadLibrary("info_narazaki_android_nlib");
        // initNative();
    }
    
}
