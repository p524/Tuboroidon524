package jp.syoboi.android;

import android.os.CountDownTimer;
import android.view.View;
import android.widget.ListView;

public class ListViewScroller {
	
	public static final String TAG = ListViewScroller.class.getSimpleName();
	
	public static final int ANCHOR_CENTER = 0; 
	public static final int ANCHOR_TOP = 1; 
	public static final int ANCHOR_BOTTOM = 2; 
	
	private MyScroller scroller;
	
	public void cancel() {
		if (scroller != null) {
			scroller.cancel();
			scroller = null;
		}
	}
	
	public void scroll(ListView list, float page, boolean animation) {
		int position = list.getFirstVisiblePosition();
		int from = list.getChildAt(0).getTop();
		cancel();
		
		int to = (int)(from - list.getMeasuredHeight() * page);
		//Log.d(TAG, "from:"+from + " to:"+to);
		
		if (animation) {
			scroller = new MyScroller(list, position, from, to, 200, 1000/60);
			scroller.start();
		} else {
			list.setSelectionFromTop(position, to);
		}
	}
	
	public static void scrollCenterNow(ListView list, View v, int position, int anchor) {
		list.setSelectionFromTop(position, calcScrollPosition(list, v, anchor)); 
	}
	
	private static int calcScrollPosition(ListView list, View v, int anchor) {
		final int listHeight = list.getMeasuredHeight();
		final int selectedViewHeight = v.getMeasuredHeight();
		
		int to = listHeight / 2;
		switch (anchor) {
		case ANCHOR_CENTER:
			to -= selectedViewHeight / 2;
			break;
		case ANCHOR_TOP:
			break;
		case ANCHOR_BOTTOM:
			to -= selectedViewHeight;
			break;
		}
		return to;
	}
	
	public void scrollCenter(ListView list, View v, int position, int msec, int fps, int anchor) {
		cancel();

		final int from = v.getTop();
		final int to = calcScrollPosition(list, v, anchor);

		scroller = new MyScroller(list, position, from, to, msec, fps);
		scroller.start();
	}
	
	public void scrollMargin(ListView list, View v, int position, int msec, int fps, int margin) {
		cancel();

		final int from = v.getTop();
		final int listHeight = list.getMeasuredHeight();
		int to;
		
		if (v.getTop() < margin) {
			to = margin;
		}
		else if (v.getBottom() > listHeight - margin) {
			to = listHeight - margin - v.getHeight();
		}
		else {
			return;
		}
		
		scroller = new MyScroller(list, position, from, to, msec, fps);
		scroller.start();
	}
	
	public static class MyScroller extends CountDownTimer {
		private final int from;
		private final int to;
		private final int position;
		private final long msec;
		private ListView list;
		private long remain; 
		
		public MyScroller(ListView list, int position, int from, int to, long msec, int fps) {
			super(msec, fps);
			this.msec = msec;
			this.list = list;
			this.position = position;
			this.from = from;
			this.to = to;
			this.remain = msec;
		}

		@Override
		public void onFinish() {
			this.remain = 0;
			list.setSelectionFromTop(position, to);
			list = null;
		}

		@Override
		public void onTick(long millisUntilFinished) {
			remain = millisUntilFinished;
			double pos = (msec - millisUntilFinished) * Math.PI / msec;
			double pos2 = (from - to) * (Math.cos(pos) + 1) / 2; 
			//Log.d("MyScroller", "pos:" + pos + " pos2:" + pos2);
			int y = to + (int)pos2;
			list.setSelectionFromTop(position, y);
		}
		
		public float getRemain() {
			return (float)remain / this.msec; 
		}
	}
}
