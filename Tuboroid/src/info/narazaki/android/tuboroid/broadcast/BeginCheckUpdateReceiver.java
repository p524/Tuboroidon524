package info.narazaki.android.tuboroid.broadcast;

import info.narazaki.android.tuboroid.BackgroundCheckUpdate;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BeginCheckUpdateReceiver extends BroadcastReceiver {
    private static final String TAG = "BeginCheckUpdateReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        BackgroundCheckUpdate.onFireCheckUpdate(context.getApplicationContext());
    }
    
}
