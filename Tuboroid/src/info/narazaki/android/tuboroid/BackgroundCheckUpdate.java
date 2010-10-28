package info.narazaki.android.tuboroid;

import info.narazaki.android.tuboroid.service.ITuboroidService;
import info.narazaki.android.tuboroid.service.TuboroidServiceTask;
import info.narazaki.android.tuboroid.service.TuboroidServiceTask.ServiceSender;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;

public class BackgroundCheckUpdate {
    private static final String TAG = "BackgroundCheckUpdate";
    
    public static final String ACTION = "info.narazaki.android.tuboroid.service.TuboroidService.BEGIN_CHECK_UPDATE";
    
    public static synchronized void onFireCheckUpdate(final Context context) {
        PowerManager pm = (PowerManager) context.getApplicationContext().getSystemService(Context.POWER_SERVICE);
        final WakeLock wake_lock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Tuboroid Update Receiver");
        
        wake_lock.acquire();
        
        final TuboroidServiceTask service_task = new TuboroidServiceTask(context);
        service_task.bind();
        
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        boolean download = pref.getBoolean("favorites_update_service_fetch_entries", false);
        if (download) {
            service_task.send(new ServiceSender() {
                @Override
                public void send(ITuboroidService service) throws RemoteException {
                    service.checkDownloadFavorites(true);
                    wake_lock.release();
                }
            });
        }
        else {
            service_task.send(new ServiceSender() {
                @Override
                public void send(ITuboroidService service) throws RemoteException {
                    service.checkUpdateFavorites(true);
                    wake_lock.release();
                }
            });
        }
    }
    
}
