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
	private ScrollTask	scrollTask;
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
					scroller = new Scroller(3*60*1000, 1000/30);
					scroller.start();
				}
			}
			return true;
		case MotionEvent.ACTION_UP: 
			if (scroller != null) {
				scrollSpeedY = 0;
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
	
	public class Scroller extends CountDownTimer {

		public Scroller(long millisInFuture, long countDownInterval) {
			super(millisInFuture, countDownInterval);
		}
		
		@Override
		public void onTick(long millisUntilFinished) {
			ListView lv = listView;
			lv.setSelectionFromTop(
					lv.getFirstVisiblePosition(), 
					lv.getChildAt(0).getTop() - scrollSpeedY);
		}

		@Override
		public void onFinish() {
		}
		
	}
	
	private static class ScrollTask extends AsyncTask<Void,Void,Void> {
		private final ListView listView;
		public int scrollSpeedY;

		private Runnable scroll = new Runnable() {
			
			@Override
			public void run() {
				int position = listView.getFirstVisiblePosition();
				int y = listView.getChildAt(0).getTop();
				listView.setSelectionFromTop(position, y - scrollSpeedY);
			}
		};

		public ScrollTask(ListView listView) {
			this.listView = listView;
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			while (!isCancelled()) {
				listView.post(scroll);
				try {
					Thread.sleep(1000/60);
				} catch (InterruptedException e) {
					e.printStackTrace();
					break;
				}
			}
			return null;
		}
		
	}
}
