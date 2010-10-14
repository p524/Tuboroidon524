package info.narazaki.android.tuboroid.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

public class EntryDataTextView extends TextView {
    private int current_background_color_ = -1;
    private CharSequence current_char_sequence_ = null;
    private BufferType current_buffer_type_ = null;
    
    public EntryDataTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    public EntryDataTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public EntryDataTextView(Context context) {
        super(context);
    }
    
    @Override
    public void setBackgroundColor(int color) {
        if (current_background_color_ != color) {
            super.setBackgroundColor(color);
        }
        current_background_color_ = color;
    }
    
    @Override
    public void setLongClickable(boolean longClickable) {
        if (isLongClickable() == longClickable) return;
        super.setLongClickable(longClickable);
    }
    
    @Override
    public void setText(CharSequence text, BufferType type) {
        if (current_char_sequence_ == text && current_buffer_type_ == type) return;
        current_char_sequence_ = text;
        current_buffer_type_ = type;
        super.setText(text, type);
    }
    
}
