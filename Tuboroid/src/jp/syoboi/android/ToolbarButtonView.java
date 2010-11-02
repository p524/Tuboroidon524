package jp.syoboi.android;

import info.narazaki.android.tuboroid.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.ImageButton;


// checked = true で選択状態っぽい描画をするだけのラジオボタンっぽい動作のImageButton
// (R.styleable.ToolbarStyle の色を使って選択状態っぽい描画をする)
public class ToolbarButtonView extends ImageButton implements Checkable {
	
	private ToolbarButtonStyle style;
	private boolean isChecked;
//	private static final int[] CHECKED_STATE_SET = {
//		android.R.attr.state_checked
//	};
	
	public ToolbarButtonView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
		
        // android:checked と android:focusable の属性を反映
        // android:checked は ImageButton に無いので必要だけど
        // android:focusable は ImageButton の実装がバグってるのでここで処理...
        // obtainStyledAttribues に渡す int の配列の値はソートされている必要がある
        TypedArray a = context.obtainStyledAttributes(
                attrs, 
                new int [] { android.R.attr.focusable, android.R.attr.checked });
	
        setFocusable(a.getBoolean(0, true));
        setChecked(a.getBoolean(1, false));

        style = new ToolbarButtonStyle(context, attrs);
	}
	
	public ToolbarButtonView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		if (isChecked) {
			style.face.setBounds(0, 0, getWidth(), getHeight());
			style.face.draw(canvas);
		}
		super.onDraw(canvas);
	}

	@Override
	public boolean isChecked() {
		return isChecked;
	}

	@Override
	public void setChecked(boolean checked) {
		isChecked = checked;
		refreshDrawableState();
	}

	@Override
	public void toggle() {
		setChecked(!isChecked);
	}

	
	public static class ToolbarButtonStyle {
		public GradientDrawable face;
		
		public ToolbarButtonStyle(Context context, AttributeSet attrs) {
			TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ToolbarStyle);

			//int highlightColor = ta.getColor(R.styleable.ToolbarStyle_toolbarHighlightColor, 0xffffffff);
			int lightColor = ta.getColor(R.styleable.ToolbarStyle_toolbarDarkColor, 0xffeeeeee);
			int darkColor = ta.getColor(R.styleable.ToolbarStyle_toolbarBottomBorderColor, 0xffcccccc);
			
			face = new GradientDrawable(
					Orientation.TOP_BOTTOM, 
					new int [] { lightColor, darkColor });
			face.setGradientType(GradientDrawable.LINEAR_GRADIENT);
			face.setShape(GradientDrawable.RECTANGLE);
			face.setCornerRadius(3);
		}
	}
	
}
