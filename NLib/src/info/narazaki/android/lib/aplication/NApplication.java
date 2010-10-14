package info.narazaki.android.lib.aplication;

import android.app.Activity;
import android.app.Application;

public class NApplication extends Application {
    private NActivityStackManager activity_stack_;
    
    public NApplication(int max_activities) {
        activity_stack_ = new NActivityStackManager(max_activities);
    }
    
    public void onActivityResume(Activity activity) {
        activity_stack_.onResume(activity);
    }
    
    public void onActivityDestroy(Activity activity) {
        activity_stack_.onDestroy(activity);
    }
    
    public int getScrollingAmount() {
        return 100;
    }
    
}
