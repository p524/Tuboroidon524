package info.narazaki.android.lib.aplication;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NSimpleApplication extends NApplication {
    private boolean use_volume_button_scrolling = false;
    private boolean use_camera_button_scrolling = false;
    
    public NSimpleApplication(int maxActivities) {
        super(maxActivities);
        use_volume_button_scrolling = false;
        use_camera_button_scrolling = false;
    }
    
    public final boolean useVolumeButtonScrolling() {
        return use_volume_button_scrolling;
    }
    
    public final boolean useCameraButtonScrolling() {
        return use_camera_button_scrolling;
    }
    
    public final void setVolumeButtonScrolling(boolean enabled) {
        use_volume_button_scrolling = enabled;
    }
    
    public final void setCameraButtonScrolling(boolean enabled) {
        use_camera_button_scrolling = enabled;
    }
    
    public final boolean isOnline() {
        return isOnline(this);
    }
    
    public static final boolean isOnline(final Context context) {
        ConnectivityManager con_man = (ConnectivityManager) context.getApplicationContext().getSystemService(
                Context.CONNECTIVITY_SERVICE);
        if (con_man == null) return false;
        NetworkInfo net_info = con_man.getActiveNetworkInfo();
        if (net_info == null) return false;
        return net_info.isConnected();
    }
}
