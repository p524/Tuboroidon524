package info.narazaki.android.tuboroid.view;

import info.narazaki.android.lib.system.MigrationSDK5;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.ZoomControls;
import android.widget.ImageView.ScaleType;
import info.narazaki.android.lib.system.MotionEventWapper;
import info.narazaki.android.tuboroid.R;


public class ScrollImageView extends ImageView implements OnTouchListener {
	private Context context_;
	

	public ScrollImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		context_ = context;
		
		setOnTouchListener(this);

		try{
			MotionEventWapper.checkAvailable();
			event_wapper = new MotionEventWapper();
		}catch(Throwable e){
			
		}
		
		if(event_wapper == null){
			//マルチタップが使えない場合はズームコントロールを表示
			ZoomControls zoom = new ZoomControls(context_);
			
		}
	
	}
	
	
	
	
	@Override
	public void setImageBitmap(Bitmap bm) {
		super.setImageBitmap(bm);
		
		image_x = bm.getWidth();
		image_y = bm.getHeight();
		
		//画像が画面に収まるようにする
		float x_scale = (float)getWidth() / bm.getWidth();
		float y_scale = (float)getHeight() / bm.getHeight();
		float scale = Math.min(x_scale, y_scale);
		if(scale >= 1.0f){
			return;
		}
		zoom(false, scale, 0, 0);
	}

	@Override
	public void scrollBy(int x, int y) {
		int a_x = getScrollX() + x;
		int a_y = getScrollY() + y;
		a_x = Math.max(a_x, (int) Math.min(getScale() * image_x - getWidth(), 0));
		a_y = Math.max(a_y, (int) Math.min(getScale() * image_y - getHeight(), 0));

		int max_x = (int) Math.max(getScale() * image_x - getWidth(), 0);
		a_x = Math.min(a_x, max_x);

		int max_y = (int) Math.max(getScale() * image_y - getHeight(), 0);
		a_y = Math.min(a_y, max_y);
		
		super.scrollTo(a_x, a_y);
	}




	final int STATE_NONE = 0;
	final int STATE_DRAGGING = 1;
	final int STATE_ZOOMING = 2;
	private int touch_state = 0;
	private PointF[] pointers = new PointF[] {new PointF(), new PointF()};
	private int[] zoom_ids = new int [] {0, 0};
	private PointF drag_pointer = new PointF();
	private Matrix before_mat = new Matrix();
	private MotionEventWapper event_wapper = null;
	private int image_x, image_y;
	
	//単なるタッチかドラッグかを見分ける用
	private boolean is_moved = false;
	
	private int getPointerIndex(MotionEvent event, int id) {
		event_wapper.set(event);
		for(int i = 0; i < event_wapper.getPointerCount(); i++) {
			if(event_wapper.getPointerId(i) == id) {
				return i;
			}
		}
		return -1;
	}
	
	private PointF getPointerPosById(MotionEvent event, int id) {
		event_wapper.set(event);
		int i = getPointerIndex(event, id);
		return new PointF(event_wapper.getX(i), event_wapper.getY(i));
	}

	private String str(MotionEvent event) {
		event_wapper.set(event);
		if(true)return String.format("%f,%f", event_wapper.getX(0), event_wapper.getY(0));
		String ret = "";
		for(int i = 0; i < event_wapper.getPointerCount(); i++) {
			ret += String.valueOf(event_wapper.getPointerId(i)) + " ";
		}
		return ret;
	}
	
	private float calcDiffLen(float x1, float y1, float x2, float y2) {
		return (float) Math.sqrt((Math.pow(x1 - x2, 2.0)
    			+ Math.pow(y1 - y2, 2.0)));
	}
	
	private void v(String s) {
		Log.v(context_.getText(R.string.app_name).toString(), s);
	}

	@Override
	public boolean onTouch(View v, MotionEvent event){

		switch (event.getAction() & MotionEvent.ACTION_MASK) {
	    case MotionEvent.ACTION_DOWN:
	    	startDrag(event, 0);
			is_moved = false;
		break;
	    case MotionEvent.ACTION_POINTER_DOWN:
	    	event_wapper.set(event);
	    	if(event_wapper.getPointerCount() == 2) {
	    		startZoom(event, new int [] {0, 1});
	    	}
		break;
	    case MotionEvent.ACTION_MOVE:
	    	is_moved = true;
	    	//if(event.getActionIndex() < 2) {
	    	if(touch_state == STATE_DRAGGING) {
	    		scrollBy((int)(drag_pointer.x - event.getX())
	    				, (int)(drag_pointer.y - event.getY()));
	    		//imageView.scrollTo((int)event.getX(0), (int)event.getY(0));

	    		drag_pointer.set(event.getX(), event.getY());
	    		
	    		v(String.format("sc:%d,%d", getScrollX(), getScrollY()));
	    	}else if(touch_state == STATE_ZOOMING) {
		    	event_wapper.set(event);

	    		PointF []new_pt = new PointF[2];
	    		float[] move = new float[2];
	    		for(int i = 0; i < 2; i++) {
	    			new_pt[i] = getPointerPosById(event, zoom_ids[i]);
	    			move[i] = calcDiffLen(new_pt[i].x, new_pt[i].y, pointers[i].x, pointers[i].y);
	    		}

	    		
	    		//あまり動かしていない方を偏倍の中心とする
	    		int center_index = move[0] > move[1] ? 1 : 0;
	    		
	    		float before_pinch = calcDiffLen(pointers[0].x, pointers[0].y, pointers[1].x, pointers[1].y);
	    		float after_pinch = calcDiffLen(new_pt[0].x, new_pt[0].y, new_pt[1].x, new_pt[1].y);
	    		
	    		float scale = after_pinch / before_pinch;
	    		
	    		zoom(false, scale, new_pt[center_index].x, new_pt[center_index].y);
	    		
	    		for(int i = 0; i < 2; i++) {
	    			pointers[i] = new_pt[i];
	    		}
	    	}

	    	//}
		break;
	    case MotionEvent.ACTION_UP:
	    	if(!is_moved){
	    		zoom(true, 1, -getScrollX(), -getScrollY());
	    	}
	    	break;
	    case MotionEvent.ACTION_POINTER_UP:
	    	event_wapper.set(event);
	    	if(event_wapper.getPointerCount() == 2) {
	    		startDrag(event, 1 - event_wapper.getActionIndex());
	    	}else if(event_wapper.getPointerCount() == 3) {
	    		int i0 = (3 - event_wapper.getActionIndex()) % 2;
	    		startZoom(event, new int [] {i0, 3 - event_wapper.getActionIndex() - i0});
	    	}
		break;
	    }
	    
	    return true;
	}
	


	public void onZoomIn(){
		zoom(false, 1.2f, getWidth() / 2, getHeight() / 2);
	}


	public void onZoomOut() {
		zoom(false, 0.8f, getWidth() / 2, getHeight() / 2);
	}
	
	void startDrag(MotionEvent event, int i) {
		touch_state = STATE_DRAGGING;
		if(i == 0){
			drag_pointer.set(event.getX(), event.getY());
		}else{
			event_wapper.set(event);
			drag_pointer.set(event_wapper.getX(i), event_wapper.getY(i));
		}

	}

	void startZoom(MotionEvent event, int []ii) {
		event_wapper.set(event);
		for(int i = 0; i < 2; i++) {
			zoom_ids[i] = event_wapper.getPointerId(ii[i]);
			pointers[i].set(event_wapper.getX(ii[i]), event_wapper.getY(ii[i]));
		}
		touch_state = STATE_ZOOMING;
		before_mat.set(getImageMatrix());
	}
	
	private void zoom(boolean is_abs, float scale, float center_x, float center_y){
		Matrix before_mat = new Matrix(getImageMatrix());
		Matrix new_mat = new Matrix(before_mat);
		
		if(is_abs){
			new_mat.setScale(scale, scale);
		}else{
			new_mat.postScale(scale, scale);
		}
		setImageMatrix(new_mat);
		
		float[] pt = new float[] {center_x + getScrollX(), center_y + getScrollY()};
		Matrix inverse = new Matrix();
		if(before_mat.invert(inverse)){
			inverse.mapPoints(pt);
		}else{
		}
		
		//postScaleに偏倍中心を設定してもよいがそれだと画像の左上がスクロール位置の原点でなくなって
		//ちょっとめんどくさくなる
		new_mat.mapPoints(pt);
		scrollBy((int)(pt[0] - (center_x + getScrollX()))
				, (int)(pt[1] - (center_y + getScrollY())));
	}
	

	
	private float getScale(){
		Matrix mat = new Matrix(getImageMatrix());
		float[] values = new float[9];
		mat.getValues(values);
		return values[0];
	}
	
}
