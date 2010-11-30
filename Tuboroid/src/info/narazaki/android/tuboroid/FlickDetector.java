package info.narazaki.android.tuboroid;

import android.app.Activity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.GestureDetector.OnGestureListener;

public abstract class FlickDetector {
	// フリックで戻る
	// # もっとマシなやりかたがありそう
	public abstract static class OnFlickListener implements OnGestureListener {
		private boolean handled;
		private Activity activity;
		
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
			float x = Math.abs(velocityX);
			if (x > Math.abs(velocityY) && x > activity.getResources().getDisplayMetrics().widthPixels) {
				if (Math.abs(e1.getX()-e2.getX()) > Math.abs(e1.getY()-e2.getY())) {
					handled = (velocityX > 0 ? onFlickRight() : onFlickLeft());
					if (handled) {
						e2.setAction(MotionEvent.ACTION_CANCEL);
					}
					return handled;
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
