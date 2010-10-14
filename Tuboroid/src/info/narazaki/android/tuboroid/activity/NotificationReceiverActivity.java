package info.narazaki.android.tuboroid.activity;

import info.narazaki.android.lib.system.MigrationSDK5;
import info.narazaki.android.tuboroid.TuboroidApplication;
import info.narazaki.android.tuboroid.activity.base.TuboroidActivity;
import android.app.NotificationManager;
import android.content.Intent;

public class NotificationReceiverActivity extends TuboroidActivity {
    @Override
    protected void onResume() {
        super.onResume();
        
        NotificationManager notif_manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notif_manager.cancel(TuboroidApplication.NOTIF_ID_BACKGROUND_UPDATED);
        
        Intent intent = new Intent(this, FavoriteListActivity.class);
        MigrationSDK5.Intent_addFlagNoAnimation(intent);
        startActivity(intent);
        
        finish();
    }
    
}
