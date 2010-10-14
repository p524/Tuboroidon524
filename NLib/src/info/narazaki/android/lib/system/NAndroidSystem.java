package info.narazaki.android.lib.system;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NAndroidSystem {
    
    public static boolean isOnline(Context context) {
        try {
            ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = manager.getActiveNetworkInfo();
            if (info != null) {
                return manager.getActiveNetworkInfo().isConnected();
            }
            return false;
        }
        catch (Exception e) {
        }
        return true;
    }
    
}
