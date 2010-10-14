package info.narazaki.android.tuboroid.activity;

import info.narazaki.android.lib.adapter.SimpleListAdapterBase;
import info.narazaki.android.lib.system.MigrationSDK5;
import info.narazaki.android.lib.toast.ManagedToast;
import info.narazaki.android.tuboroid.R;
import info.narazaki.android.tuboroid.TuboroidApplication;
import info.narazaki.android.tuboroid.activity.base.TuboroidListActivity;
import info.narazaki.android.tuboroid.adapter.Find2chResultAdapter;
import info.narazaki.android.tuboroid.agent.FavoriteCacheListAgent;
import info.narazaki.android.tuboroid.agent.Find2chTask;
import info.narazaki.android.tuboroid.agent.task.HttpFind2chTask;
import info.narazaki.android.tuboroid.agent.thread.SQLiteAgent.GetFind2chKeyDataResult;
import info.narazaki.android.tuboroid.data.Find2chKeyData;
import info.narazaki.android.tuboroid.data.Find2chResultData;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

public class Find2chSearchActivity extends TuboroidListActivity {
    public static final String TAG = "Find2chSearchActivity";
    
    public static final String INTENT_KEY_SEARCH_KEYWORD = "KEY_SEARCH_KEYWORD";
    
    private Find2chTask current_find_2ch_task_;
    
    private int reload_progress_max_ = Find2chTask.MAX_PAGE * HttpFind2chTask.FETCH_COUNT;
    private int reload_progress_cur_ = 0;
    
    private Find2chKeyData key_data_;
    private String default_keyword_;
    
    // ////////////////////////////////////////////////////////////
    // ステート管理系
    // ////////////////////////////////////////////////////////////
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.find2ch_list);
        setTitle(R.string.title_find2ch);
        
        current_find_2ch_task_ = null;
        key_data_ = null;
        
        default_keyword_ = null;
        
        if (savedInstanceState != null && savedInstanceState.containsKey(INTENT_KEY_SEARCH_KEYWORD)) {
            default_keyword_ = savedInstanceState.getString(INTENT_KEY_SEARCH_KEYWORD);
        }
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            if (extras.containsKey(INTENT_KEY_SEARCH_KEYWORD)) {
                default_keyword_ = extras.getString(INTENT_KEY_SEARCH_KEYWORD);
            }
        }
        
        // ツールバーボタンの初期化
        createToolbarButtons();
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        EditText edit_text = (EditText) findViewById(R.id.edit_search_find2ch);
        outState.putString(INTENT_KEY_SEARCH_KEYWORD, edit_text.getText().toString());
        super.onSaveInstanceState(outState);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        showToolBar(getTuboroidApplication().tool_bar_visible_);
        ((Find2chResultAdapter) getListAdapter()).setFontSize(getTuboroidApplication().view_config_);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        reload_progress_max_ = Find2chTask.MAX_PAGE * HttpFind2chTask.FETCH_COUNT;
        reload_progress_cur_ = 0;
        abortSearch();
        showProgressBar(false);
    }
    
    @Override
    protected SimpleListAdapterBase<?> createListAdapter() {
        return new Find2chResultAdapter(this, getListFontPref());
    }
    
    @Override
    protected void onFirstDataRequired() {
        EditText edit_text = (EditText) findViewById(R.id.edit_search_find2ch);
        if (default_keyword_ != null) {
            edit_text.setText(default_keyword_);
            if (default_keyword_.length() > 0) {
                updateSearch(default_keyword_, false);
            }
            default_keyword_ = null;
        }
    }
    
    @Override
    protected void onResumeDataRequired() {
        EditText edit_text = (EditText) findViewById(R.id.edit_search_find2ch);
        String keyword = edit_text.getText().toString();
        if (keyword.length() > 0) {
            updateSearch(keyword, false);
        }
    }
    
    // ////////////////////////////////////////////////////////////
    // アイテムタップ
    // ////////////////////////////////////////////////////////////
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Intent intent = new Intent(this, ThreadEntryListActivity.class);
        Find2chResultData result_data = ((Find2chResultAdapter) getListAdapter()).getData(position);
        String uri = result_data.uri_;
        intent.setData(Uri.parse(uri));
        intent.putExtra(ThreadEntryListActivity.INTENT_KEY_MAYBE_THREAD_NAME, result_data.thread_name_);
        intent.putExtra(ThreadEntryListActivity.INTENT_KEY_MAYBE_ONLINE_COUNT, result_data.online_count_);
        MigrationSDK5.Intent_addFlagNoAnimation(intent);
        startActivity(intent);
    }
    
    @Override
    protected void createToolbarButtons() {
        super.createToolbarButtons();
        final EditText edit_text = (EditText) findViewById(R.id.edit_search_find2ch);
        edit_text.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int action_id, KeyEvent event) {
                if ((action_id | EditorInfo.IME_ACTION_DONE) != 0) {
                    onSubmitSearchBar();
                    return true;
                }
                return false;
            }
        });
        
        final ImageButton search_button = (ImageButton) findViewById(R.id.button_search_find2ch);
        search_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSubmitSearchBar();
            }
        });
    }
    
    private void onSubmitSearchBar() {
        EditText edit_text = (EditText) findViewById(R.id.edit_search_find2ch);
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(edit_text.getWindowToken(), 0);
        updateSearch(edit_text.getText().toString(), true);
    }
    
    // ////////////////////////////////////////////////////////////
    @Override
    protected boolean isFavorite() {
        if (key_data_ == null) {
            return false;
        }
        return key_data_.is_favorite_;
    }
    
    @Override
    protected void addFavorite() {
        if (key_data_ == null) return;
        getAgent().addFavorite(key_data_, FavoriteCacheListAgent.ADD_BOARD_RULE_TAIL, new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        key_data_.is_favorite_ = true;
                        onFavoriteUpdated();
                    }
                });
            }
        });
    }
    
    @Override
    protected void deleteFavorite() {
        if (key_data_ == null) return;
        getAgent().delFavorite(key_data_, new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        key_data_.is_favorite_ = false;
                        onFavoriteUpdated();
                    }
                });
            }
        });
    }
    
    private void updateSeachKeyData(final String key, final int hit_count) {
        getAgent().getFind2chKeyData(key, new GetFind2chKeyDataResult() {
            @Override
            public void onQuery(final Find2chKeyData searchKeyData) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        long epoc_time = System.currentTimeMillis() / 1000;
                        if (searchKeyData != null) {
                            key_data_ = searchKeyData;
                            key_data_.hit_count_ = hit_count;
                            key_data_.prev_time_ = epoc_time;
                            if (searchKeyData.is_favorite_) {
                                getAgent().setFind2chKeyData(searchKeyData);
                            }
                        }
                        else {
                            key_data_ = new Find2chKeyData(0, key, hit_count, false, epoc_time);
                        }
                        onFavoriteUpdated();
                    }
                });
            }
        });
    }
    
    // ////////////////////////////////////////////////////////////
    private void abortSearch() {
        if (current_find_2ch_task_ != null) {
            current_find_2ch_task_.abort();
            current_find_2ch_task_ = null;
        }
        setReloadInProgress(false);
    }
    
    private void updateSearch(final String key_orig, final boolean force_reload) {
        if (!onBeginReload()) return;
        
        final String key = key_orig.trim();
        if (key.length() == 0) return;
        
        // TODO
        final int order = 0;
        
        setTitle(getString(R.string.title_find2ch) + " : " + key);
        
        updateSeachKeyData(key, 0);
        
        reload_progress_max_ = Find2chTask.MAX_PAGE * HttpFind2chTask.FETCH_COUNT;
        reload_progress_cur_ = 0;
        setProgress(0);
        setSecondaryProgress(0);
        showProgressBar(true);
        
        final ReloadTerminator reload_terminator = getNewReloadTerminator();
        current_find_2ch_task_ = getAgent().searchViaFind2ch(key, order, force_reload,
                new Find2chTask.Find2chFetchedCallback() {
                    
                    @Override
                    public void onFirstReceived(final int found_items) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (reload_terminator.is_terminated_) return;
                                reload_progress_max_ = found_items;
                                setProgressBar(reload_progress_cur_, reload_progress_max_);
                                ((Find2chResultAdapter) list_adapter_).clear();
                            }
                        });
                    }
                    
                    @Override
                    public void onCompleted() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (reload_terminator.is_terminated_) return;
                                onSearchCompleted(key);
                            }
                        });
                    }
                    
                    @Override
                    public void onReceived(final ArrayList<Find2chResultData> data_list) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (reload_terminator.is_terminated_) return;
                                onSearchProgress(data_list);
                            }
                        });
                    }
                    
                    @Override
                    public void onFailed() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (reload_terminator.is_terminated_) return;
                                onSearchFailed();
                            }
                        });
                    }
                    
                    @Override
                    public void onInterrupted() {
                        if (reload_terminator.is_terminated_) return;
                    }
                    
                    @Override
                    public void onOffline() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (reload_terminator.is_terminated_) return;
                                onSearchOffline();
                            }
                        });
                    }
                });
    }
    
    private void onSearchProgress(ArrayList<Find2chResultData> data_list) {
        if (!is_active_) return;
        if (data_list == null) {
            onSearchFailed();
            return;
        }
        else {
            hasInitialData(true);
            ((Find2chResultAdapter) list_adapter_).addDataList(data_list);
        }
        setProgressBar(((Find2chResultAdapter) list_adapter_).getCount(), reload_progress_max_);
    }
    
    private void onSearchCompleted(final String key) {
        current_find_2ch_task_ = null;
        if (!is_active_) return;
        setProgressBar(reload_progress_max_, reload_progress_max_);
        showProgressBar(false);
        updateSeachKeyData(key, ((Find2chResultAdapter) list_adapter_).getCount());
        onEndReload();
    }
    
    private void onSearchFailed() {
        current_find_2ch_task_ = null;
        if (!is_active_) return;
        ManagedToast.raiseToast(getApplicationContext(), R.string.toast_search_find2ch_failed);
        showProgressBar(false);
        onEndReload();
    }
    
    private void onSearchOffline() {
        current_find_2ch_task_ = null;
        if (!is_active_) return;
        ManagedToast.raiseToast(getApplicationContext(), R.string.toast_network_is_offline);
        showProgressBar(false);
        onEndReload();
    }
    
    protected TuboroidApplication.ViewConfig getListFontPref() {
        return getTuboroidApplication().view_config_;
    }
    
}
