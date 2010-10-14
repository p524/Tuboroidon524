package info.narazaki.android.lib.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

public class SimpleDialog {
    
    public static void showYesNo(Activity activity, int title_id, int summary_id,
            final DialogInterface.OnClickListener ok_listener, final DialogInterface.OnCancelListener cancel_listener) {
        showYesNo(activity, title_id, activity.getString(summary_id), ok_listener, cancel_listener);
    }
    
    public static void showYesNo(Activity activity, int title_id, String summary,
            final DialogInterface.OnClickListener ok_listener, final DialogInterface.OnCancelListener cancel_listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(title_id);
        builder.setMessage(summary);
        builder.setPositiveButton(android.R.string.yes, ok_listener);
        builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (cancel_listener != null) cancel_listener.onCancel(dialog);
            }
        });
        builder.setCancelable(true);
        builder.setOnCancelListener(cancel_listener);
        builder.create().show();
    }
    
    public static void showYesEtcNo(Activity activity, int title_id, int summary_id, int yes_id, int etc_id,
            final DialogInterface.OnClickListener ok_listener, final DialogInterface.OnClickListener etc_listener,
            final DialogInterface.OnCancelListener cancel_listener) {
        showYesEtcNo(activity, title_id, activity.getString(summary_id), yes_id, etc_id, ok_listener, etc_listener,
                cancel_listener);
    }
    
    public static void showYesEtcNo(Activity activity, int title_id, String summary, int yes_id, int etc_id,
            final DialogInterface.OnClickListener ok_listener, final DialogInterface.OnClickListener etc_listener,
            final DialogInterface.OnCancelListener cancel_listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(title_id);
        builder.setMessage(summary);
        builder.setPositiveButton(yes_id, ok_listener);
        builder.setNeutralButton(etc_id, etc_listener);
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (cancel_listener != null) cancel_listener.onCancel(dialog);
            }
        });
        builder.setCancelable(true);
        builder.setOnCancelListener(cancel_listener);
        builder.create().show();
    }
    
    public static void showNotice(Activity activity, int title_id, int summary_id, final Runnable callback) {
        showNotice(activity, title_id, activity.getString(summary_id), callback);
    }
    
    public static void showNotice(Activity activity, int title_id, String message, final Runnable callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(title_id);
        builder.setMessage(message);
        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (callback != null) callback.run();
            }
        });
        builder.setCancelable(true);
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (callback != null) callback.run();
            }
        });
        builder.create().show();
    }
}
