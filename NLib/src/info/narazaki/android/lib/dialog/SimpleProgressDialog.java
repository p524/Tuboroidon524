package info.narazaki.android.lib.dialog;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;

public class SimpleProgressDialog {
    ProgressDialog dialog_;
    
    public void show(Activity activity, int label_id, DialogInterface.OnCancelListener on_cancel_listener) {
        if (dialog_ != null) return;
        dialog_ = new ProgressDialog(activity);
        dialog_.setMessage(activity.getString(label_id));
        dialog_.setIndeterminate(true);
        dialog_.setCancelable(true);
        dialog_.setOnCancelListener(on_cancel_listener);
        dialog_.show();
    }
    
    public void showModal(Activity activity, int label_id) {
        if (dialog_ != null) hide();
        dialog_ = new ProgressDialog(activity);
        dialog_.setMessage(activity.getString(label_id));
        dialog_.setIndeterminate(true);
        dialog_.setCancelable(false);
        dialog_.show();
    }
    
    public void hide() {
        if (dialog_ != null) {
            dialog_.setOnCancelListener(null);
            dialog_.cancel();
            dialog_ = null;
        }
    }
}
