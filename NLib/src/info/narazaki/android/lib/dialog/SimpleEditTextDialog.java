package info.narazaki.android.lib.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.EditText;

public class SimpleEditTextDialog {
    
    public static interface OnSubmitListener {
        public void onSubmitted(String data);
        
        public void onCanceled();
    }
    
    public static void show(Activity activity, String default_data, int title_id, int submit_id,
            final OnSubmitListener listener) {
        // ビュー作成
        final EditText edit_text = new EditText(activity);
        edit_text.setText(default_data);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(title_id);
        builder.setView(edit_text);
        builder.setPositiveButton(submit_id, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                listener.onSubmitted(edit_text.getText().toString());
            }
        });
        builder.setCancelable(true);
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                listener.onCanceled();
            }
        });
        builder.create().show();
    }
}
