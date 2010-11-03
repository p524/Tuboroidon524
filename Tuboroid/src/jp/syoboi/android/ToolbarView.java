package jp.syoboi.android;

import info.narazaki.android.tuboroid.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.util.AttributeSet;
import android.widget.LinearLayout;

// ツールバーのクラス(ただ背景を塗るだけのLinearLayout)
public class ToolbarView extends LinearLayout {
	public static final String TAG = ToolbarView.class.getSimpleName();
	
	public ToolbarStyle style;
	
	public ToolbarView(Context context, AttributeSet attrs) {
		super(context, attrs);

		style = new ToolbarStyle(context, attrs, getOrientation());
	} 

	@Override
	protected void dispatchDraw(Canvas canvas) {

		if (!canvas.getClipBounds().isEmpty()) {
			int width = getWidth();
			int height = getHeight();
	 
			if (getOrientation() == LinearLayout.HORIZONTAL) {
				int y = 0;
				Paint paint = new Paint();
				paint.setColor(style.topBorderColor);
				canvas.drawLine(0, y, width, y, paint);
				y++;
				
				paint.setColor(style.highlightColor);
				canvas.drawLine(0, y, width, y, paint);
				y++;
				
				style.face.setBounds(0, y, width, height - 1);
				style.face.draw(canvas);
				
				paint.setColor(style.bottomBorderColor);
				canvas.drawLine(0, height - 1, width, height - 1, paint);
			}
			else {
				int x = 0;
				Paint paint = new Paint();
				paint.setColor(style.highlightColor);
				canvas.drawLine(x, 0, x, height, paint);
				x++;
				
				style.face.setBounds(x, 0, width - 1, height);
				style.face.draw(canvas);
				
				x = width - 2;
				paint.setColor(style.bottomBorderColor);
				canvas.drawLine(x, 0, x, height, paint);
				x++;
				
				paint.setColor(style.topBorderColor);
				canvas.drawLine(x, 0, x, height, paint);
			}
		}
		super.dispatchDraw(canvas);
	}

	public static class ToolbarStyle {
		public int topBorderColor;
		public int bottomBorderColor;
		public int highlightColor;
		public int lightColor;
		public int darkColor;
		public GradientDrawable face;
		
		public ToolbarStyle(Context context, AttributeSet attrs, int orientation) {
			TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ToolbarStyle);
			
			topBorderColor = ta.getColor(R.styleable.ToolbarStyle_toolbarTopBorderColor, 0xff000000);
			bottomBorderColor = ta.getColor(R.styleable.ToolbarStyle_toolbarBottomBorderColor, 0xff000000);
			highlightColor = ta.getColor(R.styleable.ToolbarStyle_toolbarHighlightColor, 0xffffffff);
			lightColor = ta.getColor(R.styleable.ToolbarStyle_toolbarLightColor, 0xffeeeeee);
			darkColor = ta.getColor(R.styleable.ToolbarStyle_toolbarDarkColor, 0xffcccccc);
			
			face = new GradientDrawable(
					Orientation.TOP_BOTTOM, 
					//orientation == LinearLayout.HORIZONTAL ? Orientation.TOP_BOTTOM : Orientation.RIGHT_LEFT, 
					new int [] { lightColor, darkColor });
			face.setGradientType(GradientDrawable.LINEAR_GRADIENT);
			face.setShape(GradientDrawable.RECTANGLE);
		
			//Log.d(TAG, String.format("color: %x", topBorderColor));
		}
	}
}
