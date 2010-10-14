package info.narazaki.android.tuboroid.activity.base;

import info.narazaki.android.lib.activity.base.NActivity;
import info.narazaki.android.lib.system.MigrationSDK5;
import info.narazaki.android.tuboroid.TuboroidApplication;
import info.narazaki.android.tuboroid.TuboroidApplication.SettingInvalidateChecker;
import info.narazaki.android.tuboroid.agent.TuboroidAgent;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

abstract public class TuboroidActivity extends NActivity {
    private static final String TAG = "TuboroidActivity";
    
    private SettingInvalidateChecker setting_invalidate_checker_;
    protected boolean is_active_;
    
    private boolean indeterminate_progress_bar_visible_ = false;
    private boolean progress_bar_visible_ = false;
    
    protected TuboroidApplication getTuboroidApplication() {
        return ((TuboroidApplication) getApplication());
    }
    
    public TuboroidAgent getAgent() {
        return getTuboroidApplication().getAgent();
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        requestWindowFeature(Window.FEATURE_PROGRESS);
        
        if (getTuboroidApplication().isFullScreenMode()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        }
        else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        
        setRequestedOrientation(getTuboroidApplication().getCurrentScreenOrientation());
        
        getTuboroidApplication().applyTheme(this);
        setting_invalidate_checker_ = getTuboroidApplication().getSettingInvalidateChecker();
        
        super.onCreate(savedInstanceState);
        
        is_active_ = true;
        indeterminate_progress_bar_visible_ = false;
        progress_bar_visible_ = false;
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    
    @Override
    protected void onRestart() {
        super.onRestart();
        if (setting_invalidate_checker_.isInvalidated()) {
            Intent intent = new Intent(this, this.getClass());
            MigrationSDK5.Intent_addFlagNoAnimation(intent);
            startActivity(intent);
            finish();
            return;
        }
    }
    
    @Override
    protected void onResume() {
        is_active_ = true;
        super.onResume();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        is_active_ = false;
    }
    
    protected void showIndeterminateProgressBar(boolean visible) {
        if (indeterminate_progress_bar_visible_ == visible) return;
        indeterminate_progress_bar_visible_ = visible;
        setProgressBarIndeterminateVisibility(visible);
    }
    
    protected void showProgressBar(boolean visible) {
        if (progress_bar_visible_ == visible) return;
        progress_bar_visible_ = visible;
        if (visible) {
            setProgress(0);
            setSecondaryProgress(0);
        }
        setProgressBarVisibility(visible);
    }
    
    protected void setProgressBar(int value, int max) {
        if (!progress_bar_visible_) return;
        if (value >= max || max == 0) {
            setProgress(10000);
        }
        else {
            setProgress(value * 10000 / max);
        }
    }
    
    protected void setSecondaryProgressBar(int value, int max) {
        if (!progress_bar_visible_) return;
        if (value >= max || max == 0) {
            setSecondaryProgress(10000);
        }
        else {
            setSecondaryProgress(value * 10000 / max);
        }
    }
}
