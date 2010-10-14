package info.narazaki.android.lib.toast;

import android.content.Context;
import android.widget.Toast;

/**
 * 複数Toastの衝突を自動的に調停するToast
 * 
 * @author H.Narazaki
 */
public class ManagedToast extends Toast {
    private static Toast toast_ = null;
    
    public ManagedToast(Context context) {
        super(context);
    }
    
    @Override
    public void show() {
        updateToast(this);
        super.show();
    }
    
    private static synchronized void updateToast(Toast new_toast) {
        if (toast_ != null) toast_.cancel();
        toast_ = new_toast;
    }
    
    /**
     * 簡単なToastを出力する
     */
    public static void raiseToast(final Context context, final String str) {
        raiseToast(context, str, Toast.LENGTH_LONG);
    }
    
    /**
     * 簡単なToastを出力する
     * 
     * @param context
     * @param duration
     *            ManagedToast.LENGTH_LONG または ManagedToast.LENGTH_SHORT
     */
    public static void raiseToast(final Context context, final String str, final int duration) {
        if (context != null) {
            Toast.makeText(context, str, duration).show();
        }
    }
    
    /**
     * 簡単なToastを出力する
     * 
     * @param context
     * @param resid
     *            文字列のリソースID
     */
    public static void raiseToast(final Context context, final int resid) {
        raiseToast(context, resid, Toast.LENGTH_LONG);
    }
    
    /**
     * 簡単なToastを出力する
     * 
     * @param context
     * @param resid
     *            文字列のリソースID
     * @param duration
     *            ManagedToast.LENGTH_LONG または ManagedToast.LENGTH_SHORT
     */
    public static void raiseToast(final Context context, final int resid, final int duration) {
        if (context != null) {
            Toast.makeText(context, resid, duration).show();
        }
    }
}
