package info.narazaki.android.tuboroid.activity.base;

import info.narazaki.android.lib.system.MigrationSDK5;
import info.narazaki.android.tuboroid.R;
import info.narazaki.android.tuboroid.TuboroidApplication;
import info.narazaki.android.tuboroid.activity.Find2chSearchActivity;
import info.narazaki.android.tuboroid.agent.TuboroidAgent;
import info.narazaki.android.tuboroid.service.TuboroidService;
import info.narazaki.android.tuboroid.service.TuboroidServiceTask;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;

abstract public class FavoriteListBaseActivity extends TuboroidListActivity {
    protected View notice_row_;
    
    // サービスクライアント
    private BroadcastReceiver service_intent_receiver_;
    protected TuboroidServiceTask service_task_ = null;
    
    private BroadcastReceiver reload_intent_receiver_;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        notice_row_ = null;
        super.onCreate(savedInstanceState);
        
    }
    
    protected void onPostCreated() {
        getTuboroidApplication().createMainTabButtons(this, new Runnable() {
            @Override
            public void run() {
                toggleSearchBar();
            }
        }, new Runnable() {
            @Override
            public void run() {
                checkUpdate(false);
            }
        }, new Runnable() {
            @Override
            public void run() {
                checkUpdate(true);
            }
        });
        
        service_intent_receiver_ = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, final Intent intent) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onReceiveCheckedUpdateIntent(intent);
                    }
                });
            }
        };
        reload_intent_receiver_ = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        reloadList();
                    }
                });
            }
        };
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        setShowUpdatedOnlyButton(isShowUpdatedOnlyMode());
        
        registerReceiver(service_intent_receiver_, new IntentFilter(TuboroidService.CHECK_UPDATE.ACTION_NEW));
        registerReceiver(reload_intent_receiver_, new IntentFilter(TuboroidAgent.THREAD_DATA_UPDATED_ACTION));
        
        service_task_ = new TuboroidServiceTask(getApplicationContext());
        service_task_.bind();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(service_intent_receiver_);
        unregisterReceiver(reload_intent_receiver_);
        showProgressBar(false);
        service_task_.unbind();
        service_task_ = null;
    }
    
    @Override
    protected void onFirstDataRequired() {
        reloadList();
    }
    
    @Override
    protected void onResumeDataRequired() {
        reloadList();
    }
    
    @Override
    public boolean onSearchRequested() {
        toggleSearchBar();
        return true;
    }
    
    public void toggleSearchBar() {
        Intent intent = new Intent(this, Find2chSearchActivity.class);
        MigrationSDK5.Intent_addFlagNoAnimation(intent);
        startActivity(intent);
    }
    
    private void onReceiveCheckedUpdateIntent(Intent intent) {
        int max_items = intent.getIntExtra(TuboroidService.CHECK_UPDATE.MAX, 0);
        int progress1 = intent.getIntExtra(TuboroidService.CHECK_UPDATE.PROGRESS1, 0);
        int progress2 = intent.getIntExtra(TuboroidService.CHECK_UPDATE.PROGRESS2, 0);
        showProgressBar(true);
        
        setProgressBar(progress1, max_items);
        setSecondaryProgressBar(progress2, max_items);
        if (max_items == progress1) {
            showProgressBar(false);
        }
        reloadList();
    }
    
    abstract protected void reloadList();
    
    abstract protected void checkUpdate(boolean download);
    
    protected TuboroidApplication.ViewConfig getListFontPref() {
        return getTuboroidApplication().view_config_;
    }
    
    // ////////////////////////////////////////////////////////////
    // アイテムタップ
    // ////////////////////////////////////////////////////////////
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        toggleShowUpdatedOnly();
    }
    
    protected void createNoticeFooter() {
        View notice_layout = LayoutInflater.from(this).inflate(R.layout.favorite_list_notice_row, null);
        notice_row_ = notice_layout.findViewById(R.id.notice_row);
        notice_row_.setVisibility(isShowUpdatedOnlyMode() ? View.VISIBLE : View.GONE);
        getListView().addFooterView(notice_layout);
    }
    
    @Override
    protected void createToolbarButtons() {
        super.createToolbarButtons();
        updateToolbarButtonUpdateOnly();
    }
    
    protected void updateToolbarButtonUpdateOnly() {
        ImageButton button_updated_only = (ImageButton) findViewById(R.id.button_tab_updated_only);
        if (button_updated_only != null) {
            button_updated_only.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleShowUpdatedOnly();
                }
            });
            setShowUpdatedOnlyButton(isShowUpdatedOnlyMode());
        }
    }
    
    protected boolean isShowUpdatedOnlyMode() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        return pref.getBoolean("favorite_recents_show_updated_only", false);
    }
    
    protected void toggleShowUpdatedOnly() {
        if (hasInitialData()) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            boolean updated_only = pref.getBoolean("favorite_recents_show_updated_only", false);
            updated_only = !updated_only;
            SharedPreferences.Editor editor = pref.edit();
            editor.putBoolean("favorite_recents_show_updated_only", updated_only);
            editor.commit();
            setShowUpdatedOnlyButton(updated_only);
        }
    }
    
    protected void setShowUpdatedOnlyButton(boolean updated_only) {
        ImageButton button_updated_only = (ImageButton) findViewById(R.id.button_tab_updated_only);
        if (button_updated_only != null) {
            if (updated_only) {
                button_updated_only.setImageResource(R.drawable.toolbar_btn_updated_on);
            }
            else {
                button_updated_only.setImageResource(R.drawable.toolbar_btn_updated_off);
            }
        }
        if (notice_row_ != null) {
            notice_row_.setVisibility(updated_only ? View.VISIBLE : View.GONE);
        }
    }
}
