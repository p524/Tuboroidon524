package jp.syoboi.android;

import info.narazaki.android.tuboroid.activity.ForwardableActivityUtil;
import android.R;
import android.app.Activity;
import android.content.Context;
import android.os.CountDownTimer;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;

public class ListViewScrollButton extends ImageButton {
	public static final String TAG = ListViewScrollButton.class.getSimpleName();
	
	private ListView 	listView;
	private Activity activity_;
	private long		pressedTime;
	private float		pressedX;
	private float		pressedY;
	private int			scrollSpeedY;
	private Scroller	scroller;
	private float		moveY;
	private boolean 	reverse;
	private boolean   active;
	
	public ListViewScrollButton(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
	}
	
	public ListViewScrollButton(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void setListView(Activity activity, ListView lv) {
		activity_ = activity;
		this.listView = lv;
	}
	
	public void setReverse(boolean b) {
		this.reverse = b;
	}
	
	@Override
	public boolean performClick() {
		Log.d(TAG, "click");
		return super.performClick();
	}
	
	//ImageButtonではそのままではR.attr.state_activeがきちんと扱えないので
	//DrawableStateを作りなおしたときに面倒を見てやる必要がある
    @Override
	public int[] onCreateDrawableState(int extraSpace) {
    	int [] new_list = super.onCreateDrawableState(extraSpace);
    	if(active){
    		int [] new_list_active = new int [new_list.length + 1];
    		System.arraycopy(new_list, 0, new_list_active, 0, new_list.length);
    		for(int i = 0; i < new_list_active.length; i++){
    			if(new_list_active[i] == 0){
    				new_list_active[i] = R.attr.state_active;
    				break;
    			}
    		}
    		return new_list_active;
    	}else{
    		return new_list;
    	}
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
				if(scroller != null){
					scroller.scrollSpeedY = (reverse ? -scrollSpeedY : scrollSpeedY);
				}
			}

			if(event.getX() - pressedX > getDragThreshold()){
				setActive(true);
			}else if(event.getX() - pressedX < -getDragThreshold()){
	        	setActive(true);
			}else{
				setActive(false);
			}
			
			return true;
		case MotionEvent.ACTION_UP:
			setActive(false);

			boolean scrolled = false;
			if (scroller != null) {
				scroller.cancel();
				scroller = null;
				scrolled = true;
			}
			if(event.getX() - pressedX > getDragThreshold()){
				ForwardableActivityUtil.startForwardActivity(activity_);
				setPressed(false);
				return true;
			}else if(event.getX() - pressedX < -getDragThreshold()){
	        	activity_.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK));
	        	activity_.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK));
	        	setPressed(false);
				return true;
			}else if (System.currentTimeMillis() - pressedTime > 1000) {
				setPressed(false);
				return true;
			}
			if(scrolled){
				setPressed(false);
				return true;
			}
			break;
		}
		return super.onTouchEvent(event);
	}
	
	public int getDragThreshold(){
		return listView.getWidth() / 4;
	}
	
	public void setActive(boolean active){
		this.active = active;
		refreshDrawableState();
	}
	
	// ListViewをピクセル数を指定してスクロールする
	public static void scrollBy(ListView lv, int distance) {
		if (distance == 0 || lv.getChildCount() == 0) return;
		
		int top = 0;
		int bottom = lv.getMeasuredHeight();
		int childCount = lv.getChildCount();
		if (distance > bottom) {
			distance = bottom - 1;
		} else if (distance < -bottom) {
			distance = -(bottom - 1);
		}
		
		// スクラップが発生するかどうか確認
		boolean scrap = false;
		
		View vTop = lv.getChildAt(0);
		View vBottom = lv.getChildAt(childCount-1);
		if ((vTop.getTop() - distance) > top || (vTop.getBottom() - distance) < top) {
			scrap = true;
		}
		if ((vBottom.getTop() - distance) > bottom || (vBottom.getBottom() - distance) < bottom) {
			scrap = true;
		}

		if (scrap) {
			// スクラップが発生する場合は、諦めてsetSelectionFromTop
			View v;
			int pos;
			if (distance > 0) {
				v = lv.getChildAt(childCount - 1);
				pos = lv.getLastVisiblePosition();
			} else {
				v = lv.getChildAt(0);
				pos = lv.getFirstVisiblePosition();
			}
			lv.setSelectionFromTop(pos, v.getTop() - distance);
		} else {
			// スクラップが発生しない場合は、アイテムの位置を移動
			for (int j=0; j<childCount; j++) {
				View v = lv.getChildAt(j);
				v.offsetTopAndBottom(-distance);
			}
			lv.invalidate();
		}
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
						scrollBy(lv, scrollSpeedY);
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
