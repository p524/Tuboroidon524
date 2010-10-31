package jp.syoboi.android;

import info.narazaki.android.tuboroid.R;
import jp.syoboi.android.ToolbarView.ToolbarStyle;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.ImageButton;

public class ToolbarButtonView extends ImageButton implements Checkable {
	
	private ToolbarButtonStyle style;
	private boolean isChecked;
	private static final int[] CHECKED_STATE_SET = {
		android.R.attr.state_checked
	};
	
	public ToolbarButtonView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initButton(context, attrs);
	}
	
	public ToolbarButtonView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initButton(context, attrs);
	}
	
	public void initButton(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(
                    attrs, new int [] {0});
		
		style = new ToolbarButtonStyle(context, attrs);
		setChecked(attrs.getAttributeBooleanValue(0, false));
		//setChecked(true);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if (isChecked) {
			style.face.setBounds(0, 0, getWidth(), getHeight());
			style.face.draw(canvas);
		}
		super.onDraw(canvas);
	}
//	@Override
//	protected int[] onCreateDrawableState(int extraSpace) {
//		final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
//		if (isChecked()) {
//			mergeDrawableStates(drawableState, CHECKED_STATE_SET);
//		}
//		return drawableState;
//	}

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
