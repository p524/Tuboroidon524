package info.narazaki.android.tuboroid.activity;

import info.narazaki.android.tuboroid.FlickDetector;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.GestureDetector;
import android.view.KeyEvent;

public class ForwardableActivityUtil {
    public static final String EXTRA_FORWARD_INTENT = "forwardIntent";
    private static final int ENABLE_FLICK_ENABLE = 0;
    private static final int ENABLE_FLICK_FORWARD_ONLY = 1;
    private static final int ENABLE_FLICK_BACK_ONLY = 2;
    private static final int ENABLE_FLICK_DISABLE = 3;
    
    // 次のアクティビティを呼び出す
    public static boolean startForwardActivity(Activity activity) {
		try {
			Intent i = activity.getIntent().getParcelableExtra(EXTRA_FORWARD_INTENT);
			if (i != null) {
				activity.startActivityForResult(i, 0);
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
    }
    
    public static void onActivityResult(Activity activity, Intent data) {
    	if (data != null) {
    		Intent i = activity.getIntent();
    		i.putExtra(EXTRA_FORWARD_INTENT, data);
            activity.setResult(0, i);
    	}
    }
    
    public static void onCreate(Activity activity) {
    	Intent i = activity.getIntent();
        Intent forward = new Intent(i);
        forward.putExtra(EXTRA_FORWARD_INTENT, i.getParcelableExtra(EXTRA_FORWARD_INTENT));
        activity.setResult(Activity.RESULT_CANCELED, forward);
    }
    
    public static GestureDetector createFlickGestureDetector(final Activity activity) {
    	return new GestureDetector(new FlickDetector.OnFlickListener(activity) {
    		@Override
    		public boolean onFlickLeft() {
    			// 次のインテントを呼び出す
    			SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());
    	        int enable_flick = Integer.parseInt(pref.getString("pref_enable_flick", String.valueOf(ENABLE_FLICK_ENABLE)));
    	        if(enable_flick == ENABLE_FLICK_ENABLE || enable_flick == ENABLE_FLICK_FORWARD_ONLY){
    	        	return startForwardActivity(activity);
    	        }else{
    	        	return false;
    	        }
    		}
    		
    		@Override
    		public boolean onFlickRight() {
    			SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());
    			int enable_flick = Integer.parseInt(pref.getString("pref_enable_flick", String.valueOf(ENABLE_FLICK_ENABLE)));
    	        if(enable_flick == ENABLE_FLICK_ENABLE || enable_flick == ENABLE_FLICK_BACK_ONLY){
    	        	activity.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK));
    	        	activity.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK));
    	        	return true;
    	        }else{
    	        	return false;
    	        }
    		}
    	});    	
    }
}
