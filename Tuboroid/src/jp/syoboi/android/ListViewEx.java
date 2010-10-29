package jp.syoboi.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.CountDownTimer;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ListView;

public class ListViewEx extends ListView {
	public static final String TAG = ListViewEx.class.getSimpleName();

	private int			hlPosition;
	private TransitionDrawable 	hlDrawable;
	private Rect 		hlRect = new Rect();
	private ListViewScroller scroller;
	private HighlightTimer	hlTimer;
	
	public ListViewEx(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	public ListViewEx(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	public ListViewEx(Context context) {
		super(context);
	}
	
	public void setHighlight(int position, int millis) {
		hlPosition = position;
		
		TransitionDrawable d = hlDrawable;
		if (d == null) {
			d = new TransitionDrawable(
				new Drawable [] {
						new ColorDrawable(0xffFCD87A),
						new ColorDrawable(0x00000000)
				}
			);
			d.setCrossFadeEnabled(true);
			hlDrawable = d;
		}
		updateHighlightRect();
		
		d.startTransition(millis);
		
		if (hlTimer != null) hlTimer.cancel();
		hlTimer = new HighlightTimer(millis+1, 1000/15);
		hlTimer.start();
	}
	
	private ListViewScroller getScroller() {
		if (scroller != null) {
			scroller.cancel();
		}
		scroller = new ListViewScroller();
		return scroller;
	}

	// positionまでスクロールする(指定したpositionが画面の端からmargin分余裕を持つ位置になる)
	public void scrollToPosition(final int position, final int margin, 
			final int millis, final int fps) {
		int first = getFirstVisiblePosition();
		int last = getLastVisiblePosition();
		final ListViewScroller scroller = getScroller();

		if (position < first) {
			setSelectionFromTop(position + 1, getDividerHeight()+1);
		}
		else if (position > last) {
			setSelectionFromTop(position, getMeasuredHeight() - 1 - getDividerHeight());
		}

		post(new Runnable() {
			@Override
			public void run() {
				int first = getFirstVisiblePosition();
				int last = getLastVisiblePosition();
				if (first <= position && position <= last) {
					// 画面内に対象のアイテムが存在する
					View v = getChildAt(position - first);
					scroller.scrollMargin(ListViewEx.this, v, position, millis, fps, margin);
				}
			}
		});
	}
	
	public void updateHighlightRect() {
		final int pos = hlPosition;
		final int first = getFirstVisiblePosition();
		
		if (first <= pos) {
			final int last = getLastVisiblePosition();
			
			if (pos <= last) {
				View v = getChildAt(pos - first);
				hlRect.set(v.getLeft(), v.getTop(),
						v.getRight(), v.getBottom());
				return;
			}
		}
		hlRect.setEmpty();
	}
	
	@Override
	protected void dispatchDraw(Canvas canvas) {
		Drawable d = hlDrawable;
		if (hlPosition != -1 && d != null) {
			updateHighlightRect();
			if (!hlRect.isEmpty()) {
				canvas.save();
				canvas.clipRect(hlRect);
				d.draw(canvas);
				canvas.restore();
			}
		}
		super.dispatchDraw(canvas);
	}
	
	// positionのViewが画面内に見える状態で存在するかどうかを返す
	public boolean isVisiblePosition(int position) {
		int first = getFirstVisiblePosition();
		int last = getLastVisiblePosition();
		if (getCount() > 0) {
			if (getChildAt(0).getTop() < 0) first++;
			
			return (first <= position && position < last );
		}
		return false;
	}
	
	private class HighlightTimer extends CountDownTimer {
		public HighlightTimer(long millisInFuture, long countDownInterval) {
			super(millisInFuture, countDownInterval);
		}
		@Override
		public void onFinish() {
			invalidate(hlRect);
			hlPosition = -1;
		}
		@Override
		public void onTick(long millisUntilFinished) { invalidate(hlRect); }
	}
}
