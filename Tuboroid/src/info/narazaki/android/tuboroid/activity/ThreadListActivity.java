package info.narazaki.android.tuboroid.activity;

import info.narazaki.android.lib.adapter.SimpleListAdapterBase;
import info.narazaki.android.lib.dialog.SimpleDialog;
import info.narazaki.android.lib.system.MigrationSDK5;
import info.narazaki.android.lib.toast.ManagedToast;
import info.narazaki.android.tuboroid.R;
import info.narazaki.android.tuboroid.TuboroidApplication;
import info.narazaki.android.tuboroid.activity.base.SearchableListActivity;
import info.narazaki.android.tuboroid.adapter.ThreadListAdapter;
import info.narazaki.android.tuboroid.agent.BoardListAgent;
import info.narazaki.android.tuboroid.agent.FavoriteCacheListAgent;
import info.narazaki.android.tuboroid.agent.ThreadListAgent;
import info.narazaki.android.tuboroid.agent.TuboroidAgent;
import info.narazaki.android.tuboroid.data.BoardData;
import info.narazaki.android.tuboroid.data.ThreadData;

import java.util.List;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;

public class ThreadListActivity extends SearchableListActivity {
    public static final String TAG = "ThreadListActivity";
    
    public static final String PREF_KEY_THREAD_SORT_ORDER = "PREF_KEY_THREAD_SORT_ORDER";
    
    // コンテキストメニュー
    private final static int CTX_MENU_DELETE_THREAD = 1;
    private final static int CTX_MENU_COPY_TO_CLIPBOARD = 2;
    
    // メニュー
    // ツールバーの出し入れ
    public static final int MENU_KEY_TOOLBAR_1 = 10;
    public static final int MENU_KEY_TOOLBAR_2 = 11;
    
    // サーチバーの出し入れ
    public static final int MENU_KEY_SEARCH_BAR_1 = 15;
    public static final int MENU_KEY_SEARCH_BAR_2 = 16;
    
    // ソート方式の変更
    public static final int MENU_KEY_SORT = 30;
    
    // プログレスバー
    private final static int DEFAULT_MAX_PROGRESS = 300;
    private final static int DEFAULT_FAKE_PROGRESS = 60;
    private final static int DEFAULT_DB_PROGRESS = 50;
    
    private int reload_progress_max_ = DEFAULT_MAX_PROGRESS;
    private int reload_progress_cur_ = 0;
    
    private BoardData board_data_;
    private String filter_ = null;
    
    private BroadcastReceiver reload_intent_receiver_;
    
    // ////////////////////////////////////////////////////////////
    // ステート管理系
    // ////////////////////////////////////////////////////////////
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.thread_list);
        registerForContextMenu(getListView());
        
        Uri uri = getIntent().getData();
        if (uri == null) return;
        
        board_data_ = getAgent().getBoardData(uri, false, new BoardListAgent.BoardFetchedCallback() {
            @Override
            public void onBoardFetched(final BoardData new_board_data) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        board_data_ = new_board_data;
                        setTitle(board_data_.board_category_ + " : " + board_data_.board_name_);
                        onFavoriteUpdated();
                    }
                });
            }
        });
        
        reload_intent_receiver_ = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        reloadList(false);
                    }
                });
            }
        };
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(reload_intent_receiver_, new IntentFilter(TuboroidAgent.THREAD_DATA_UPDATED_ACTION));
        ((ThreadListAdapter) getListAdapter()).setFontSize(getTuboroidApplication().view_config_);
        if (board_data_ == null) {
            finish();
            return;
        }
    }
    
    @Override
    protected void onPause() {
        if (board_data_ != null) {
            unregisterReceiver(reload_intent_receiver_);
            reload_progress_max_ = DEFAULT_MAX_PROGRESS;
            reload_progress_cur_ = 0;
            showProgressBar(false);
        }
        super.onPause();
    }
    
    @Override
    protected SimpleListAdapterBase<?> createListAdapter() {
        return new ThreadListAdapter(this, getListFontPref());
    }
    
    @Override
    protected void onFirstDataRequired() {
        if (board_data_ == null) return;
        initSortOrder();
        reloadList(false);
    }
    
    @Override
    protected void onResumeDataRequired() {
        if (board_data_ == null) return;
        reloadList(false);
    }
    
    // ////////////////////////////////////////////////////////////
    // キー管理系
    // ////////////////////////////////////////////////////////////
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && filter_ != null) {
            cancelSearchBar();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    
    // ////////////////////////////////////////////////////////////
    // アイテムタップ
    // ////////////////////////////////////////////////////////////
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Intent intent = new Intent(this, ThreadEntryListActivity.class);
        ThreadData thread_data = ((ThreadListAdapter) getListAdapter()).getData(position);
        if (thread_data == null) return;
        
        String uri = thread_data.getThreadURI();
        intent.setData(Uri.parse(uri));
        intent.putExtra(ThreadEntryListActivity.INTENT_KEY_MAYBE_THREAD_NAME, thread_data.thread_name_);
        intent.putExtra(ThreadEntryListActivity.INTENT_KEY_MAYBE_ONLINE_COUNT, thread_data.online_count_);
        MigrationSDK5.Intent_addFlagNoAnimation(intent);
        startActivity(intent);
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menu_info) {
        menu.clear();
        menu.setHeaderTitle(R.string.ctx_menu_title_thread);
        menu.add(0, CTX_MENU_DELETE_THREAD, CTX_MENU_DELETE_THREAD, R.string.ctx_menu_delete_thread);
        menu.add(0, CTX_MENU_COPY_TO_CLIPBOARD, CTX_MENU_COPY_TO_CLIPBOARD, R.string.ctx_menu_copy_to_clipboard);
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        
        ThreadData thread_data = ((ThreadListAdapter) getListAdapter()).getData(info.position);
        if (thread_data == null) return true;
        
        switch (item.getItemId()) {
        case CTX_MENU_DELETE_THREAD:
            getAgent().deleteThreadEntryListCache(thread_data, new Runnable() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            reloadList(false);
                        }
                    });
                }
            });
            break;
        case CTX_MENU_COPY_TO_CLIPBOARD:
            copyToClipboard(thread_data);
            break;
        default:
            break;
        }
        return true;
    }
    
    private void copyToClipboard(final ThreadData thread_data) {
        String[] menu_strings = new String[] { getString(R.string.label_submenu_copy_thread_info_title),
                getString(R.string.label_submenu_copy_thread_info_url),
                getString(R.string.label_submenu_copy_thread_info_title_url) };
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.label_menu_copy_thread_info);
        builder.setItems(menu_strings, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                switch (which) {
                case 0:
                    cm.setText(thread_data.thread_name_);
                    break;
                case 1:
                    cm.setText(thread_data.getThreadURI());
                    break;
                case 2:
                    cm.setText(thread_data.thread_name_ + "\n" + thread_data.getThreadURI());
                    break;
                default:
                    return;
                }
                ManagedToast.raiseToast(getApplicationContext(), R.string.toast_copied);
            }
        });
        builder.create().show();
    }
    
    // ////////////////////////////////////////////////////////////
    // オプションメニュー
    // ////////////////////////////////////////////////////////////
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        
        // ツールバー
        createToolBarOptionMenu(menu, MENU_KEY_TOOLBAR_1, MENU_KEY_TOOLBAR_2, MENU_KEY_SEARCH_BAR_1,
                MENU_KEY_SEARCH_BAR_2);
        
        // ソート
        MenuItem sort_item = menu.add(0, MENU_KEY_SORT, MENU_KEY_SORT, getString(R.string.label_menu_sort_threads));
        sort_item.setIcon(android.R.drawable.ic_menu_sort_by_size);
        sort_item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                showSortOrderDialog();
                return false;
            }
        });
        
        return true;
    }
    
    private void showSortOrderDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.label_menu_sort_threads);
        builder.setItems(R.array.thread_sort_orders, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int sort_order = which;
                SharedPreferences.Editor editor = PreferenceManager
                        .getDefaultSharedPreferences(getApplicationContext()).edit();
                editor.putInt(PREF_KEY_THREAD_SORT_ORDER, sort_order);
                editor.commit();
                updateSortOrder(sort_order);
            }
        });
        builder.create().show();
    }
    
    // ////////////////////////////////////////////////////////////
    // その他
    // ////////////////////////////////////////////////////////////
    @Override
    protected boolean isFavorite() {
        if (board_data_ == null) return false;
        return board_data_.is_favorite_;
    }
    
    @Override
    protected void addFavorite() {
        if (board_data_ == null) return;
        getAgent().addFavorite(board_data_, FavoriteCacheListAgent.ADD_BOARD_RULE_TAIL, new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        board_data_.is_favorite_ = true;
                        onFavoriteUpdated();
                    }
                });
            }
        });
    }
    
    @Override
    protected void deleteFavorite() {
        if (board_data_ == null) return;
        getAgent().delFavorite(board_data_, new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        board_data_.is_favorite_ = false;
                        onFavoriteUpdated();
                    }
                });
            }
        });
    }
    
    // ////////////////////////////////////////////////////////////
    // リロード
    // ////////////////////////////////////////////////////////////
    @Override
    protected void reloadList(boolean force_reload) {
        if (!is_active_) return;
        
        if (!onBeginReload()) return;
        
        // 嘘プログレスバー
        reload_progress_max_ = DEFAULT_MAX_PROGRESS;
        reload_progress_cur_ = 0;
        setProgress(0);
        setSecondaryProgress(0);
        showProgressBar(true);
        final ReloadTerminator reload_terminator = getNewReloadTerminator();
        getAgent().fetchThreadList(board_data_, force_reload, new ThreadListAgent.ThreadListFetchedCallback() {
            @Override
            public void onThreadListFetchCompleted() {
                // 読み込みとの待ち合わせ
                postListViewAndUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (reload_terminator.is_terminated_) return;
                        setProgress(10000);
                        showProgressBar(false);
                        ((ThreadListAdapter) list_adapter_).applyFilter(new Runnable() {
                            @Override
                            public void run() {
                                onEndReload();
                            }
                        });
                    }
                });
            }
            
            @Override
            public void onThreadListFetchFailed(final boolean maybe_moved) {
                onThreadListFetchCompleted();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (reload_terminator.is_terminated_) return;
                        if (maybe_moved) {
                            SimpleDialog.showNotice(ThreadListActivity.this, R.string.dialog_thread_list_moved_title,
                                    R.string.dialog_thread_list_moved_summary, null);
                        }
                        else {
                            ManagedToast.raiseToast(ThreadListActivity.this, R.string.toast_reload_thread_list_failed);
                        }
                    }
                });
            }
            
            @Override
            public void onThreadListFetchedCache(final List<ThreadData> data_list) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (reload_terminator.is_terminated_) return;
                        ((ThreadListAdapter) list_adapter_).setDataList(data_list, null);
                        if (reload_progress_cur_ * 100 / reload_progress_max_ > DEFAULT_FAKE_PROGRESS) {
                            reload_progress_cur_ += (reload_progress_max_ - reload_progress_cur_) / 5;
                        }
                        else {
                            reload_progress_cur_ += data_list.size();
                        }
                        setProgressBar(reload_progress_cur_, reload_progress_max_);
                    }
                });
            }
            
            @Override
            public void onThreadListFetched(final List<ThreadData> data_list) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (reload_terminator.is_terminated_) return;
                        ((ThreadListAdapter) list_adapter_).addDataList(data_list, null);
                        reload_progress_cur_ += DEFAULT_DB_PROGRESS;
                        setProgressBar(reload_progress_cur_, reload_progress_max_);
                    }
                });
            }
            
            @Override
            public void onInterrupted() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (reload_terminator.is_terminated_) return;
                        setProgress(10000);
                        showProgressBar(false);
                        onEndReload();
                    }
                });
                
            }
            
            @Override
            public void onConnectionOffline() {
                onThreadListFetchCompleted();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (reload_terminator.is_terminated_) return;
                        ManagedToast.raiseToast(ThreadListActivity.this, R.string.toast_network_is_offline);
                    }
                });
            }
        });
    }
    
    protected TuboroidApplication.ViewConfig getListFontPref() {
        return getTuboroidApplication().view_config_;
    }
    
    @Override
    protected void updateFilter(final String filter) {
        if (!is_active_) return;
        
        filter_ = filter;
        if (filter_ == null || filter_.length() == 0) {
            ((ThreadListAdapter) list_adapter_).setFilter(null, null);
        }
        else {
            final String filter_lc = filter_.toLowerCase();
            ((ThreadListAdapter) list_adapter_).setFilter(new ThreadListAdapter.Filter<ThreadData>() {
                @Override
                public boolean filter(ThreadData data) {
                    if (data.thread_name_.toLowerCase().indexOf(filter_lc) == -1) return false;
                    return true;
                }
            }, null);
        }
    }
    
    private void initSortOrder() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int sort_order = pref.getInt(PREF_KEY_THREAD_SORT_ORDER, ThreadData.Order.ORDER_DEFAULT);
        updateSortOrder(sort_order);
    }
    
    private void updateSortOrder(int sort_order) {
        ((ThreadListAdapter) list_adapter_).setComparer(ThreadData.Order.getComparator(sort_order), null);
    }
    
}
