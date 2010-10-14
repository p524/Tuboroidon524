package info.narazaki.android.lib.aplication;

import java.util.LinkedList;

import android.app.Activity;

public class NActivityStackManager {
    private LinkedList<Activity> activity_stack_;
    private int max_activities_ = 10;
    
    public NActivityStackManager(int max_activities) {
        activity_stack_ = new LinkedList<Activity>();
        max_activities_ = max_activities;
    }
    
    public void onResume(Activity activity) {
        activity_stack_.remove(activity);
        activity_stack_.addLast(activity);
        if (activity_stack_.size() > max_activities_) {
            popActivity();
        }
    }
    
    private void popActivity() {
        Activity old_activity = activity_stack_.getFirst();
        activity_stack_.removeFirst();
        old_activity.finish();
    }
    
    public void onDestroy(Activity activity) {
        activity_stack_.remove(activity);
    }
}
