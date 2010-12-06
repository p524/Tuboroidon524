package info.narazaki.android.tuboroid;

import android.app.Activity;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.GestureDetector.OnGestureListener;

public abstract class FlickDetector {
	// フリックで戻る
	// # もっとマシなやりかたがありそう
	public abstract static class OnFlickListener implements OnGestureListener {
		private boolean handled;
		private Activity activity;
		private final static int touchSlop = ViewConfiguration.getTouchSlop();
		
		public OnFlickListener(Activity a) {
			activity = a;
		}

		public abstract boolean onFlickLeft();
		
		public abstract boolean onFlickRight();
		
		@Override
		public boolean onDown(MotionEvent e) {
			handled = false;
			return false;
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			if (e1 == null || e2 == null) return false;
			if (Math.abs(e2.getX() - e1.getX()) > touchSlop) {
				float x = Math.abs(velocityX);
				float y = Math.abs(velocityY);
				if (x > y*3 && x > activity.getResources().getDisplayMetrics().widthPixels) {
					if (Math.abs(e1.getX()-e2.getX()) > Math.abs(e1.getY()-e2.getY())) {
						handled = (velocityX > 0 ? onFlickRight() : onFlickLeft());
						if (handled) {
							e2.setAction(MotionEvent.ACTION_CANCEL);
						}
						return handled;
					}
				}
			}
			return false;
		}

		@Override
		public void onLongPress(MotionEvent e) {
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {
			return false;
		}

		@Override
		public void onShowPress(MotionEvent e) {
		}

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			return handled;
		}
		
	}
}
