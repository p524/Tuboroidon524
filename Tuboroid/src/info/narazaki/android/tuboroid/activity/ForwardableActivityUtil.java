package info.narazaki.android.tuboroid.activity;

import info.narazaki.android.tuboroid.FlickDetector;
import android.app.Activity;
import android.content.Intent;
import android.view.GestureDetector;
import android.view.KeyEvent;

public class ForwardableActivityUtil {
    public static final String EXTRA_FORWARD_INTENT = "forwardIntent";
    
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
    			return startForwardActivity(activity);
    		}
    		
    		@Override
    		public boolean onFlickRight() {
    			activity.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK));
    			activity.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK));
    			return true;
    		}
    	});    	
    }
}
