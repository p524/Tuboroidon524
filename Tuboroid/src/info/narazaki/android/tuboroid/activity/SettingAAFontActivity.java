package info.narazaki.android.tuboroid.activity;

import info.narazaki.android.lib.dialog.SimpleProgressDialog;
import info.narazaki.android.lib.toast.ManagedToast;
import info.narazaki.android.tuboroid.R;
import info.narazaki.android.tuboroid.activity.base.TuboroidActivity;
import info.narazaki.android.tuboroid.agent.FontFetchAgent.FontFetchedCallback;

import java.io.File;
import java.util.concurrent.Future;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;

public class SettingAAFontActivity extends TuboroidActivity {
    private static final String TAG = "SettingAAFontActivity";
    
    private SimpleProgressDialog progress_dialog_;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting_font_view);
        setTitle(R.string.title_install_aa_font);
        progress_dialog_ = new SimpleProgressDialog();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        WebView web_view = (WebView) findViewById(R.id.aa_font_license);
        web_view.loadUrl(getString(R.string.const_font_license_url));
        
        TextView already_installed_view = (TextView) findViewById(R.id.aa_font_already_installed);
        Button install_button = (Button) findViewById(R.id.aa_font_install);
        if (getTuboroidApplication().view_config_.use_ext_aa_font_) {
            already_installed_view.setVisibility(View.VISIBLE);
            install_button.setEnabled(false);
        }
        else {
            already_installed_view.setVisibility(View.GONE);
            install_button.setEnabled(true);
            install_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    beginDownloadFont();
                }
            });
        }
    }
    
    @Override
    protected void onPause() {
        progress_dialog_.hide();
        super.onPause();
        getTuboroidApplication().reloadPreferences(true);
    }
    
    private void beginDownloadFont() {
        File ext_font_file = getTuboroidApplication().getExternalFontFile();
        if (ext_font_file == null) {
            onDownloadFontFailed();
            return;
        }
        final Future<?> future = getAgent().downloadExternalFont(ext_font_file, new FontFetchedCallback() {
            @Override
            public void onFailed() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onDownloadFontFailed();
                    }
                });
            }
            
            @Override
            public void onCompleted() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!is_active_) return;
                        ManagedToast.raiseToast(SettingAAFontActivity.this, R.string.toast_ext_font_succeeded);
                        finish();
                    }
                });
            }
        });
        progress_dialog_.show(this, R.string.dialog_loading_progress, new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                future.cancel(true);
                if (!is_active_) return;
                finish();
            }
        });
        
    }
    
    private void onDownloadFontFailed() {
        if (!is_active_) return;
        ManagedToast.raiseToast(SettingAAFontActivity.this, R.string.toast_ext_font_failed);
        finish();
    }
    
}
