package info.narazaki.android.tuboroid.broadcast;

import info.narazaki.android.tuboroid.BackgroundTimerUpdater;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BeginCheckTimerUpdateReceiver extends BroadcastReceiver {
    private static final String TAG = "BeginCheckUpdateTimerReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        BackgroundTimerUpdater.updateTimer(context.getApplicationContext());
    }
    
}
