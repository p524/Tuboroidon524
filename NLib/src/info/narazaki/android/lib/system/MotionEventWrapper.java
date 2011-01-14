package info.narazaki.android.lib.system;

import android.view.MotionEvent;

public class MotionEventWrapper{
	private MotionEvent inst;

	/* class initialization fails when this throws an exception */
	static {
		try {
			Class.forName("android.view.MotionEvent");
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public MotionEventWrapper(){

	}
	
	public static void checkAvailable(){
	}

	public void set(MotionEvent inst){
		this.inst = inst;
	}

	public int getPointerCount(){
		return inst.getPointerCount();
	}

	public int getPointerId(int pointerIndex){
		return inst.getPointerId(pointerIndex);
	}
	
	public int getActionIndex(){
		return (inst.getAction() & MotionEvent.ACTION_POINTER_ID_MASK) >> MotionEvent.ACTION_POINTER_ID_SHIFT;
	}
	
	public float getX(int i){
		return inst.getX(i);
	}
	
	public float getY(int i){
		return inst.getY(i);
	}
}
