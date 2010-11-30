package info.narazaki.android.tuboroid;

import android.app.Activity;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;

public abstract class FlickDetector {
	
	// フリックで戻る
	// # もっとマシなやりかたがありそう
	public static GestureDetector createFlickDetector(final Activity activity) {
		return new GestureDetector(new GestureDetector.OnGestureListener() {
			private boolean handled;
			
			@Override
			public boolean onSingleTapUp(MotionEvent e) {
				return handled;
			}
			
			@Override
			public void onShowPress(MotionEvent e) {}
			
			@Override
			public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
					float distanceY) {
				return false;
			}
			
			@Override
			public void onLongPress(MotionEvent e) {}
			
			@Override
			public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
					float velocityY) {
				float x = Math.abs(velocityX); 
				if (x > Math.abs(velocityY) && x > activity.getResources().getDisplayMetrics().widthPixels) {
					if (Math.abs(e1.getX()-e2.getX()) > Math.abs(e1.getY()-e2.getY())) {
						handled = true;
						activity.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK));
						activity.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK));
						return true;
					}
				}
				return false;
			}
			
			@Override
			public boolean onDown(MotionEvent e) {
				handled = false;
				return false;
			}
		});
	}
}
