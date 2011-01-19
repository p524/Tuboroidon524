package info.narazaki.android.tuboroid.activity.base;

import info.narazaki.android.lib.activity.base.NSimpleExpandableListActivity;
import info.narazaki.android.lib.system.MigrationSDK5;
import info.narazaki.android.tuboroid.TuboroidApplication;
import info.narazaki.android.tuboroid.TuboroidApplication.SettingInvalidateChecker;
import info.narazaki.android.tuboroid.activity.ForwardableActivityUtil;
import info.narazaki.android.tuboroid.agent.TuboroidAgent;
import jp.syoboi.android.ListViewScrollButton;
import android.content.Intent;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;

abstract public class TuboroidExpandableListActivityBase extends NSimpleExpandableListActivity {
    private static final String TAG = "TuboroidExpandableListActivityBase";
    
    private SettingInvalidateChecker setting_invalidate_checker_;
    protected ListViewScrollButton btnListScroll; 
    
    protected TuboroidApplication getTuboroidApplication() {
        return ((TuboroidApplication) getApplication());
    }
    
    public TuboroidAgent getAgent() {
        return getTuboroidApplication().getAgent();
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (getTuboroidApplication().isFullScreenMode()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        }
        
        setRequestedOrientation(getTuboroidApplication().getCurrentScreenOrientation());
        
        getTuboroidApplication().applyTheme(this);
        setting_invalidate_checker_ = getTuboroidApplication().getSettingInvalidateChecker();
        
        super.onCreate(savedInstanceState);
        ForwardableActivityUtil.onCreate(this);
    }

    @Override
    public void startActivity(Intent intent) {
    	startActivityForResult(intent, 0);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	ForwardableActivityUtil.onActivityResult(this, data);
    	super.onActivityResult(requestCode, resultCode, data);
    }
    
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
    	super.onPostCreate(savedInstanceState);
    	
        final GestureDetector gd = ForwardableActivityUtil.createFlickGestureDetector(this);
    	
    	getExpandableListView().setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return gd.onTouchEvent(event);
			}
		});
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
    public void onContentChanged() {
    	super.onContentChanged();
    	
    	btnListScroll = TuboroidListActivity.createScrollButton(this, getExpandableListView());
    	if (btnListScroll != null) {
    		btnListScroll.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					setListPageDown();
				}
			});
            TuboroidListActivity.setScrollButtonPosition(btnListScroll, 
            		getTuboroidApplication().view_config_.scroll_button_position);
    	}
    }
   
}
