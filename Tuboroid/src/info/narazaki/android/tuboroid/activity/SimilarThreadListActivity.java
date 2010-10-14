package info.narazaki.android.tuboroid.activity;

import info.narazaki.android.lib.adapter.SimpleListAdapterBase;
import info.narazaki.android.lib.dialog.SimpleProgressDialog;
import info.narazaki.android.lib.system.MigrationSDK5;
import info.narazaki.android.lib.toast.ManagedToast;
import info.narazaki.android.tuboroid.R;
import info.narazaki.android.tuboroid.TuboroidApplication;
import info.narazaki.android.tuboroid.activity.base.TuboroidListActivity;
import info.narazaki.android.tuboroid.adapter.ThreadListAdapter;
import info.narazaki.android.tuboroid.agent.BoardListAgent;
import info.narazaki.android.tuboroid.agent.ThreadListAgent.ThreadListFetchedCallback;
import info.narazaki.android.tuboroid.data.BoardData;
import info.narazaki.android.tuboroid.data.ThreadData;

import java.util.List;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ListView;

public class SimilarThreadListActivity extends TuboroidListActivity {
    public static final String TAG = "SimilarThreadsListActivity";
    
    public static final String KEY_SEARCH_KEY_NAME = "KEY_SEARCH_KEY_NAME";
    public static final String KEY_SEARCH_THREAD_ID = "KEY_SEARCH_THREAD_ID";
    
    private SimpleProgressDialog progress_dialog_;
    
    private BoardData board_data_;
    private long target_thread_id_ = 0;
    private String search_key_ = null;
    
    // ////////////////////////////////////////////////////////////
    // ステート管理系
    // ////////////////////////////////////////////////////////////
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.similar_thread_list);
        
        Uri uri = getIntent().getData();
        board_data_ = getAgent().getBoardData(uri, false, new BoardListAgent.BoardFetchedCallback() {
            @Override
            public void onBoardFetched(final BoardData newBoardData) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        board_data_ = newBoardData;
                    }
                });
            }
        });
        
        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_SEARCH_KEY_NAME)) {
            search_key_ = savedInstanceState.getString(KEY_SEARCH_KEY_NAME);
        }
        if (getIntent().hasExtra(KEY_SEARCH_KEY_NAME)) {
            search_key_ = getIntent().getStringExtra(KEY_SEARCH_KEY_NAME);
        }
        if (search_key_ == null || search_key_.length() == 0) search_key_ = "";
        setTitle(getString(R.string.title_similar_thread) + " : " + search_key_);
        
        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_SEARCH_THREAD_ID)) {
            target_thread_id_ = savedInstanceState.getLong(KEY_SEARCH_THREAD_ID);
        }
        if (getIntent().hasExtra(KEY_SEARCH_KEY_NAME)) {
            target_thread_id_ = getIntent().getLongExtra(KEY_SEARCH_THREAD_ID, 0);
        }
        
        progress_dialog_ = new SimpleProgressDialog();
        createToolbarButtons();
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(KEY_SEARCH_KEY_NAME, search_key_);
        outState.putLong(KEY_SEARCH_THREAD_ID, target_thread_id_);
        super.onSaveInstanceState(outState);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        showToolBar(getTuboroidApplication().tool_bar_visible_);
        ((ThreadListAdapter) getListAdapter()).setFontSize(getTuboroidApplication().view_config_);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        progress_dialog_.hide();
    }
    
    @Override
    public boolean onSearchRequested() {
        reloadList(true);
        return true;
    }
    
    @Override
    protected SimpleListAdapterBase<?> createListAdapter() {
        return new ThreadListAdapter(this, getListFontPref());
    }
    
    @Override
    protected void onFirstDataRequired() {
        if (search_key_.length() == 0) {
            finish();
            return;
        }
        reloadList(false);
    }
    
    @Override
    protected void onResumeDataRequired() {
        reloadList(false);
    }
    
    @Override
    protected void createToolbarButtons() {
        super.createToolbarButtons();
        // ツールバーボタンの初期化
        ImageButton button_reload = (ImageButton) findViewById(R.id.button_toolbar_reload);
        button_reload.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                reloadList(true);
            }
        });
    }
    
    // ////////////////////////////////////////////////////////////
    // アイテムタップ
    // ////////////////////////////////////////////////////////////
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Intent intent = new Intent(this, ThreadEntryListActivity.class);
        ThreadData thread_data = ((ThreadListAdapter) getListAdapter()).getData(position);
        if (thread_data == null) return;
        
        intent.setData(Uri.parse(thread_data.getThreadURI()));
        intent.putExtra(ThreadEntryListActivity.INTENT_KEY_MAYBE_THREAD_NAME, thread_data.thread_name_);
        intent.putExtra(ThreadEntryListActivity.INTENT_KEY_MAYBE_ONLINE_COUNT, thread_data.online_count_);
        MigrationSDK5.Intent_addFlagNoAnimation(intent);
        startActivity(intent);
    }
    
    // ////////////////////////////////////////////////////////////
    // リロード
    // ////////////////////////////////////////////////////////////
    @Override
    protected void reloadList(boolean force_reload) {
        if (!is_active_) return;
        
        if (!onBeginReload()) return;
        
        progress_dialog_.show(this, R.string.dialog_loading_progress, new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (is_active_) finish();
            }
        });
        
        final ReloadTerminator reload_terminator = getNewReloadTerminator();
        getAgent().fetchSimilarThreadList(board_data_, target_thread_id_, search_key_, force_reload,
                new ThreadListFetchedCallback() {
                    
                    @Override
                    public void onThreadListFetchCompleted() {
                        postListViewAndUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (reload_terminator.is_terminated_) return;
                                onSearchCompleted();
                            }
                        });
                    }
                    
                    @Override
                    public void onThreadListFetchedCache(final List<ThreadData> dataList) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (reload_terminator.is_terminated_) return;
                                ((ThreadListAdapter) list_adapter_).setDataList(dataList, null);
                            }
                        });
                    }
                    
                    @Override
                    public void onThreadListFetched(final List<ThreadData> dataList) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (reload_terminator.is_terminated_) return;
                                ((ThreadListAdapter) list_adapter_).addDataList(dataList, null);
                            }
                        });
                    }
                    
                    @Override
                    public void onInterrupted() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (reload_terminator.is_terminated_) return;
                                onEndReload();
                            }
                        });
                    }
                    
                    @Override
                    public void onThreadListFetchFailed(final boolean maybe_moved) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (reload_terminator.is_terminated_) return;
                                onSearchFailed();
                            }
                        });
                    }
                    
                    @Override
                    public void onConnectionOffline() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (reload_terminator.is_terminated_) return;
                                onSearchOffline();
                            }
                        });
                        if (reload_terminator.is_terminated_) return;
                    }
                });
        
    }
    
    private void onSearchCompleted() {
        if (!is_active_) return;
        progress_dialog_.hide();
        ((ThreadListAdapter) list_adapter_).notifyDataSetChanged();
        onEndReload();
    }
    
    private void onSearchFailed() {
        if (!is_active_) return;
        progress_dialog_.hide();
        ManagedToast.raiseToast(getApplicationContext(), R.string.toast_reload_entry_list_failed);
        onEndReload();
    }
    
    private void onSearchOffline() {
        if (!is_active_) return;
        progress_dialog_.hide();
        ManagedToast.raiseToast(getApplicationContext(), R.string.toast_network_is_offline);
        onEndReload();
    }
    
    protected TuboroidApplication.ViewConfig getListFontPref() {
        return getTuboroidApplication().view_config_;
    }
    
}
