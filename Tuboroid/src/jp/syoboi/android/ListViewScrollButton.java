package jp.syoboi.android;

import android.content.Context;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageButton;
import android.widget.ListView;

public class ListViewScrollButton extends ImageButton {
	public static final String TAG = ListViewScrollButton.class.getSimpleName();
	
	private ListView 	listView;
	private long		pressedTime;
	private float		pressedX;
	private float		pressedY;
	private int			scrollSpeedY;
	private Scroller	scroller;
	private float		moveY;
	
	public ListViewScrollButton(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
	}
	
	public ListViewScrollButton(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void setListView(ListView listView) {
		this.listView = listView;
	}
	
	@Override
	public boolean performClick() {
		Log.d(TAG, "click");
		return super.performClick();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			pressedTime = System.currentTimeMillis();
			pressedX = event.getX();
			pressedY = event.getY();
			break;
		case MotionEvent.ACTION_MOVE:
//			Log.d(TAG, String.format("move %.2f %.2f", 
//						event.getX() - pressedX,
//						event.getY() - pressedY));
			moveY = (event.getY() - pressedY) / 25;
			scrollSpeedY = (int)(moveY * (moveY > 0 ? moveY : -moveY));
			if (scrollSpeedY != 0) {
				if (scroller == null && listView.getChildCount() > 0) {
					scroller = new Scroller(listView, 3*60*1000, 1000/30);
					scroller.start();
				}
				scroller.scrollSpeedY = scrollSpeedY;
			}
			return true;
		case MotionEvent.ACTION_UP: 
			if (scroller != null) {
				scroller.cancel();
				scroller = null;
				setPressed(false);
				return true;
			}
			else if (System.currentTimeMillis() - pressedTime > 1000) {
				setPressed(false);
				return true;
			}
			break;
		}
		return super.onTouchEvent(event);
	}
	
	public static class Scroller extends CountDownTimer {
		private final ListView listView;
		public volatile int scrollSpeedY;
		public volatile boolean stop;
		
		private Runnable runnable;
		
		public Scroller(final ListView listView, long millisInFuture, long countDownInterval) {
			super(millisInFuture, countDownInterval);
			this.listView = listView;
			
			runnable = new Runnable() {
				
				@Override
				public void run() {
					final ListView lv = listView;
					if (scrollSpeedY != 0) {
						lv.setSelectionFromTop(
								lv.getFirstVisiblePosition(), 
								lv.getChildAt(0).getTop() - scrollSpeedY);
					}
				}
			}; 
		}
		
		@Override
		public void onTick(long millisUntilFinished) {
			listView.post(runnable);
		}

		@Override
		public void onFinish() {
		}
		
	}
}
