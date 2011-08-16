package info.narazaki.android.lib.view;

import info.narazaki.android.lib.R;
import info.narazaki.android.lib.system.MigrationSDK5;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;

public class NLabelView extends View {
    public static final String TAG = "NLabelView";
    
    private static final Spanned EMPTY_SPANNED = new SpannedString("");
    private static final int SANS = 1;
    private static final int SERIF = 2;
    private static final int MONOSPACE = 3;
    
    private Spannable text_;
    private TextPaint paint_;
    private Layout layout_;
    
    private static final int LAYOUT_NONE = 0;
    private static final int LAYOUT_STATIC = 1;
    private static final int LAYOUT_SIMPLE = 2;
    private int layout_type_;
    
    private ColorStateList color_;
    private ColorStateList link_color_;
    private int current_color_;
    
    private int min_height_ = 0;
    private boolean min_height_is_pixels_ = true;
    private float line_spacing_mul_ = 1;
    private float line_spacing_add_ = 0;
    
    // touch!
    private BackgroundColorSpan on_clicked_bg_color_span_ = null;
    private boolean wide_touch_margin_ = false;
    private boolean link_pressed_ = false;
    
    public NLabelView(Context context) {
        this(context, null);
    }
    
    public NLabelView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
    }
    
    public NLabelView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        
        Spanned text = EMPTY_SPANNED;
        layout_type_ = LAYOUT_NONE;
        
        paint_ = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        MigrationSDK5.TextPaint_SetDensity(paint_, getResources().getDisplayMetrics().density);
        // paint_.setCompatibilityScaling(getResources().getCompatibilityInfo().applicationScale);
        
        ColorStateList color = null;
        ColorStateList link_color = null;
        int text_size = 14;
        int typeface_index = -1;
        int style_index = -1;
        
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.NLabelView, defStyle, 0);
        
        TypedArray appearance = null;
        int ap = a.getResourceId(R.styleable.NLabelView_android_textAppearance, -1);
        if (ap != -1) {
            appearance = context.obtainStyledAttributes(ap, R.styleable.android_textAppearance);
        }
        
        if (appearance != null) {
            int n = appearance.getIndexCount();
            for (int i = 0; i < n; i++) {
                int attr = appearance.getIndex(i);
                
                switch (attr) {
                case R.styleable.android_textAppearance_android_textColor:
                    color = appearance.getColorStateList(attr);
                    break;
                case R.styleable.android_textAppearance_android_textColorLink:
                    link_color = appearance.getColorStateList(attr);
                    break;
                case R.styleable.android_textAppearance_android_textSize:
                    text_size = appearance.getDimensionPixelSize(attr, text_size);
                    break;
                case R.styleable.android_textAppearance_android_typeface:
                    typeface_index = appearance.getInt(attr, -1);
                    break;
                case R.styleable.android_textAppearance_android_textStyle:
                    style_index = appearance.getInt(attr, -1);
                    break;
                }
            }
            
            appearance.recycle();
        }
        
        int n = a.getIndexCount();
        for (int i = 0; i < n; i++) {
            int attr = a.getIndex(i);
            
            switch (attr) {
            case R.styleable.NLabelView_android_minLines:
                setMinLines(a.getInt(attr, -1));
                break;
            case R.styleable.NLabelView_android_minHeight:
                setMinHeight(a.getDimensionPixelSize(attr, -1));
                break;
            case R.styleable.NLabelView_android_text:
                text = new SpannedString(a.getText(attr));
                break;
            case R.styleable.NLabelView_android_textColor:
                color = a.getColorStateList(attr);
                break;
            case R.styleable.NLabelView_android_textColorLink:
                link_color = a.getColorStateList(attr);
                break;
            case R.styleable.NLabelView_android_textSize:
                text_size = a.getDimensionPixelSize(attr, text_size);
                break;
            case R.styleable.NLabelView_android_typeface:
                typeface_index = a.getInt(attr, typeface_index);
                break;
            case R.styleable.NLabelView_android_textStyle:
                style_index = a.getInt(attr, style_index);
                break;
            }
        }
        a.recycle();
        
        setTypefaceByIndex(typeface_index, style_index);
        setTextColor(color != null ? color : ColorStateList.valueOf(0xFF000000));
        setLinkTextColor(link_color);
        setRawTextSize(text_size);
        setText(text);
    }
    
    // ////////////////////////////////////////////////////////////
    // Get Internal...
    // ////////////////////////////////////////////////////////////
    
    public final Layout getLayout() {
        return layout_;
    }
    
    private void invalidateLayout() {
        if (recycleLayout()) {
            recycleLayout();
            requestLayout();
            invalidate();
            layout_type_ = LAYOUT_NONE;
        }
    }
    
    private boolean recycleLayout() {
        if (layout_ == null) return false;
        if (layout_ instanceof NSimpleLayout) {
            ((NSimpleLayout) layout_).recycle();
        }
        layout_ = null;
        return true;
    }
    
    // ////////////////////////////////////////////////////////////
    // Paint
    // ////////////////////////////////////////////////////////////
    
    public TextPaint getPaint() {
        return paint_;
    }
    
    public void setPaintFlags(int flags) {
        if (paint_.getFlags() != flags) {
            paint_.setFlags(flags);
            
            invalidateLayout();
        }
    }
    
    // ////////////////////////////////////////////////////////////
    // Min/Max Height
    // ////////////////////////////////////////////////////////////
    
    public void setMinLines(int minlines) {
        min_height_ = minlines;
        min_height_is_pixels_ = false;
        
        requestLayout();
        invalidate();
    }
    
    public void setMinHeight(int minHeight) {
        min_height_ = minHeight;
        min_height_is_pixels_ = true;
        
        requestLayout();
        invalidate();
    }
    
    public void setLineSpacing(float add, float mult) {
        line_spacing_mul_ = mult;
        line_spacing_add_ = add;
        
        invalidateLayout();
    }
    
    public int getLineHeight() {
        return Math.round(paint_.getFontMetricsInt(null) * line_spacing_mul_ + line_spacing_add_);
    }
    
    // ////////////////////////////////////////////////////////////
    // TypeFace
    // ////////////////////////////////////////////////////////////
    private void setTypefaceByIndex(int typefaceIndex, int styleIndex) {
        Typeface tf = null;
        switch (typefaceIndex) {
        case SANS:
            tf = Typeface.SANS_SERIF;
            break;
        
        case SERIF:
            tf = Typeface.SERIF;
            break;
        
        case MONOSPACE:
            tf = Typeface.MONOSPACE;
            break;
        }
        
        setTypeface(tf, styleIndex);
    }
    
    public void setTypeface(Typeface tf, int style) {
        if (style > 0) {
            if (tf == null) {
                tf = Typeface.defaultFromStyle(style);
            }
            else {
                tf = Typeface.create(tf, style);
            }
            
            setTypeface(tf);
            int typefaceStyle = tf != null ? tf.getStyle() : 0;
            int need = style & ~typefaceStyle;
            paint_.setFakeBoldText((need & Typeface.BOLD) != 0);
            paint_.setTextSkewX((need & Typeface.ITALIC) != 0 ? -0.25f : 0);
        }
        else {
            paint_.setFakeBoldText(false);
            paint_.setTextSkewX(0);
            setTypeface(tf);
        }
    }
    
    public void setTypeface(Typeface tf) {
        if (paint_.getTypeface() != tf) {
            paint_.setTypeface(tf);
            
            invalidateLayout();
        }
    }
    
    // ////////////////////////////////////////////////////////////
    // TextSize
    // ////////////////////////////////////////////////////////////
    public void setTextSize(float size) {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
    }
    
    public void setTextSize(int unit, float size) {
        Context c = getContext();
        Resources r;
        
        if (c == null) r = Resources.getSystem();
        else r = c.getResources();
        
        setRawTextSize(TypedValue.applyDimension(unit, size, r.getDisplayMetrics()));
    }
    
    private void setRawTextSize(float size) {
        if (size != paint_.getTextSize()) {
            paint_.setTextSize(size);
            
            invalidateLayout();
        }
    }
    
    // ////////////////////////////////////////////////////////////
    // TextColor
    // ////////////////////////////////////////////////////////////
    public void setTextColor(int color) {
        color_ = ColorStateList.valueOf(color);
        updateTextColors();
    }
    
    public void setTextColor(ColorStateList colors) {
        if (colors == null) throw new NullPointerException();
        
        color_ = colors;
        updateTextColors();
    }
    
    public final void setLinkTextColor(int color) {
        link_color_ = ColorStateList.valueOf(color);
        updateTextColors();
    }
    
    public final void setLinkTextColor(ColorStateList colors) {
        link_color_ = colors;
        updateTextColors();
    }
    
    private void updateTextColors() {
        boolean updated = false;
        int color = color_.getColorForState(getDrawableState(), 0);
        if (color != current_color_) {
            current_color_ = color;
            updated = true;
        }
        if (link_color_ != null) {
            color = link_color_.getColorForState(getDrawableState(), 0);
            if (color != paint_.linkColor) {
                paint_.linkColor = color;
                updated = true;
            }
        }
        if (updated) invalidate();
    }
    
    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (color_ != null && color_.isStateful()) {
            updateTextColors();
        }
    }
    
    // ////////////////////////////////////////////////////////////
    // Text Body
    // ////////////////////////////////////////////////////////////
    public final void setText(CharSequence text) {
        if (text instanceof Spannable) {
            text_ = (Spannable) text;
        }
        else {
            text_ = new SpannableString(text);
        }
        invalidateLayout();
    }
    
    public final void setText(int resid) {
        setText(getContext().getResources().getText(resid));
    }
    
    // ////////////////////////////////////////////////////////////
    // Touch!
    // ////////////////////////////////////////////////////////////
    
    public final void setTouchMargin(boolean wide_touch_margin) {
        wide_touch_margin_ = wide_touch_margin;
    }
    
    public final void setTouchBackgroundColorSpan(BackgroundColorSpan on_clicked_bg_color_span) {
        on_clicked_bg_color_span_ = on_clicked_bg_color_span;
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (on_clicked_bg_color_span_ == null || layout_ == null) {
            return super.onTouchEvent(event);
        }
        
        int action = event.getAction();
        
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            int x = (int) event.getX();
            int y = (int) event.getY();
            
            x -= getPaddingLeft();
            y -= getPaddingTop();
            
            x += getScrollX();
            y += getScrollY();
            
            ClickableSpan target_link = null;
            
            int line = layout_.getLineForVertical(y);
            int line_width = (int) layout_.getLineWidth(line);
            if (x <= line_width) {
                int off = layout_.getOffsetForHorizontal(line, x);
                ClickableSpan[] links = text_.getSpans(off, off, ClickableSpan.class);
                
                if (links.length != 0) target_link = links[0];
                
                if (target_link == null && wide_touch_margin_) {
                    int line2 = -1;
                    if (line2 == 0 && layout_.getLineCount() > 0) {
                        line2 = line + 1;
                    }
                    else {
                        line2 = line - 1;
                    }
                    if (line2 >= 0) {
                        off = layout_.getOffsetForHorizontal(line2, x);
                        links = text_.getSpans(off, off, ClickableSpan.class);
                        if (links.length != 0) target_link = links[0];
                    }
                }
                
                if (target_link != null) {
                    if (action == MotionEvent.ACTION_UP && link_pressed_) {
                        link_pressed_ = false;
                        text_.removeSpan(on_clicked_bg_color_span_);
                        target_link.onClick(this);
                        return true;
                    }
                    else if (action == MotionEvent.ACTION_DOWN) {
                        link_pressed_ = true;
                        text_.setSpan(on_clicked_bg_color_span_, text_.getSpanStart(target_link),
                                text_.getSpanEnd(target_link), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                        invalidate();
                        setLongClickDelegate();
                        
                        return true;
                    }
                }
            }
            
            if (link_pressed_) {
                link_pressed_ = false;
                text_.removeSpan(on_clicked_bg_color_span_);
            }
        }
        
        return super.onTouchEvent(event);
    }
    
    private void setLongClickDelegate() {
        final Runnable callback = new Runnable() {
            @Override
            public void run() {
                if (isShown() && link_pressed_) {
                    link_pressed_ = false;
                    text_.removeSpan(on_clicked_bg_color_span_);
                    View target_view = NLabelView.this;
                    
                    while (true) {
                        if (target_view.isLongClickable()) break;
                        ViewParent parent = target_view.getParent();
                        if (parent == null || !(parent instanceof View)) return;
                        target_view = (View) parent;
                    }
                    target_view.performLongClick();
                }
            }
        };
        postDelayed(callback, ViewConfiguration.getLongPressTimeout());
    }
    
    // ////////////////////////////////////////////////////////////
    // Draw!
    // ////////////////////////////////////////////////////////////
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (layout_ == null) {
            int width = getRight() - getLeft();
            if (width < 0) width = 0;
            createNewLayout(width);
        }
        
        paint_.setColor(current_color_);
        paint_.drawableState = getDrawableState();
        
        layout_.draw(canvas, null, null, 0);
    }
    
    protected void createNewLayout(int width) {
        if (width < 0) width = 0;
        recycleLayout();
        if (layout_type_ == LAYOUT_NONE) {
            if (NSimpleLayout.checkSupported(text_, paint_, width, Layout.Alignment.ALIGN_NORMAL, line_spacing_mul_,
                    line_spacing_add_, true)) {
                layout_type_ = LAYOUT_SIMPLE;
            }
            else {
                layout_type_ = LAYOUT_STATIC;
            }
            
        }
        if (layout_type_ == LAYOUT_SIMPLE) {
            layout_ = new NSimpleLayout(text_, paint_, width, Layout.Alignment.ALIGN_NORMAL, line_spacing_mul_,
                    line_spacing_add_, true);
        }
        else {
            layout_ = new StaticLayout(text_, paint_, width, Layout.Alignment.ALIGN_NORMAL, line_spacing_mul_,
                    line_spacing_add_, true);
        }
    }
    
    public int getDesiredWidth() {
        int n = layout_.getLineCount();
        CharSequence text = layout_.getText();
        float max = 0;
        
        for (int i = 0; i < n - 1; i++) {
            if (text.charAt(layout_.getLineEnd(i) - 1) != '\n') return -1;
        }
        
        for (int i = 0; i < n; i++) {
            max = Math.max(max, layout_.getLineWidth(i));
        }
        
        return (int) FloatMath.ceil(max);
    }
    
    private int getDesiredHeight() {
        return getDesiredHeight(layout_, true);
    }
    
    private int getDesiredHeight(Layout layout, boolean cap) {
        if (layout == null) return 0;
        
        int line_count = layout.getLineCount();
        int desired_height = layout.getLineTop(line_count);
        
        if (min_height_is_pixels_) {
            desired_height = Math.max(desired_height, min_height_);
        }
        else {
            if (line_count < min_height_) {
                desired_height += getLineHeight() * (min_height_ - line_count);
            }
        }
        desired_height = Math.max(desired_height, getSuggestedMinimumHeight());
        return desired_height;
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width_size = MeasureSpec.getSize(widthMeasureSpec);
        int width_mode = MeasureSpec.getMode(widthMeasureSpec);
        int height_size = MeasureSpec.getSize(heightMeasureSpec);
        int height_mode = MeasureSpec.getMode(heightMeasureSpec);
        
        int width;
        int height;
        
        int desired_width = -1;
        
        if (width_mode == MeasureSpec.EXACTLY) {
            width = width_size;
        }
        else {
            if (layout_ == null) {
                desired_width = (int) FloatMath.ceil(Layout.getDesiredWidth(text_, paint_));
            }
            else {
                // Lauoutが利用可能なら既に改行区切りされているのでそっちを使おう
                desired_width = getDesiredWidth();
            }
            width = desired_width;
            width = Math.max(width, getSuggestedMinimumWidth());
            
            if (width_mode == MeasureSpec.AT_MOST) {
                width = Math.min(width_size, width);
            }
        }
        
        if (layout_ == null) {
            createNewLayout(width);
        }
        else if (layout_.getWidth() < width) {
            layout_.increaseWidthTo(width);
        }
        else if (layout_.getWidth() > width) {
            createNewLayout(width);
        }
        
        if (height_mode == MeasureSpec.EXACTLY) {
            height = height_size;
        }
        else {
            height = getDesiredHeight();
            if (height_mode == MeasureSpec.AT_MOST) {
                height = Math.min(height, height_size);
            }
        }
        
        setMeasuredDimension(width, height);
    }
}
