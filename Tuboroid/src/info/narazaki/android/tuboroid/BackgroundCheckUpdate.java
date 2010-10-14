package info.narazaki.android.tuboroid;

import info.narazaki.android.tuboroid.service.ITuboroidService;
import info.narazaki.android.tuboroid.service.TuboroidServiceTask;
import info.narazaki.android.tuboroid.service.TuboroidServiceTask.ServiceSender;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.RemoteException;
import android.preference.PreferenceManager;

public class BackgroundCheckUpdate {
    private static final String TAG = "BackgroundCheckUpdate";
    
    public static final String ACTION = "info.narazaki.android.tuboroid.service.TuboroidService.BEGIN_CHECK_UPDATE";
    
    public static synchronized void onFireCheckUpdate(Context context) {
        final TuboroidServiceTask service_task = new TuboroidServiceTask(context);
        service_task.bind();
        
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        boolean download = pref.getBoolean("favorites_update_service_fetch_entries", false);
        if (download) {
            service_task.send(new ServiceSender() {
                @Override
                public void send(ITuboroidService service) throws RemoteException {
                    service.checkDownloadFavorites(true);
                }
            });
        }
        else {
            service_task.send(new ServiceSender() {
                @Override
                public void send(ITuboroidService service) throws RemoteException {
                    service.checkUpdateFavorites(true);
                }
            });
        }
    }
    
}
