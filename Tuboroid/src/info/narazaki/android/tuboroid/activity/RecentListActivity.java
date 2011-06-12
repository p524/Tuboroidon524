package info.narazaki.android.tuboroid.activity;

import info.narazaki.android.lib.adapter.SimpleListAdapterBase;
import info.narazaki.android.lib.dialog.SimpleDialog;
import info.narazaki.android.lib.system.MigrationSDK5;
import info.narazaki.android.tuboroid.R;
import info.narazaki.android.tuboroid.TuboroidApplication;
import info.narazaki.android.tuboroid.activity.base.FavoriteListBaseActivity;
import info.narazaki.android.tuboroid.adapter.RecentListAdapter;
import info.narazaki.android.tuboroid.agent.ThreadListAgent;
import info.narazaki.android.tuboroid.data.ThreadData;
import info.narazaki.android.tuboroid.dialog.ThreadInfoDialog;
import info.narazaki.android.tuboroid.service.ITuboroidService;
import info.narazaki.android.tuboroid.service.TuboroidServiceTask.ServiceSender;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;

public class RecentListActivity extends FavoriteListBaseActivity {
    public static final String TAG = "RecentListActivity";
    
    // コンテキストメニュー
    private final static int CTX_MENU_DELETE_RECENT = 1;
    private final static int CTX_MENU_THREAD_INFO = 2;
    
    // メニュー
    // 並び順
    public static final int MENU_KEY_CACHE_ORDER = 20;
    // キャッシュ削除
    public static final int MENU_KEY_CLEAR_CACHE = 30;
    
    // 手動整理
    // public static final int MENU_KEY_MANAGE = 40;
    
    // ////////////////////////////////////////////////////////////
    // ステート管理系
    // ////////////////////////////////////////////////////////////
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recent_list);
        registerForContextMenu(getListView());
        
        setTitle(getString(R.string.title_recents, getString(R.string.app_name)));
        
        getTuboroidApplication().setHomeTabActivity(TuboroidApplication.KEY_HOME_ACTIVITY_RECENTS);
        
        onPostCreated();
        createNoticeFooter();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        getTuboroidApplication().setHomeTabActivity(TuboroidApplication.KEY_HOME_ACTIVITY_RECENTS);
        ((RecentListAdapter) getListAdapter()).setFontSize(getTuboroidApplication().view_config_);
        ((RecentListAdapter) getListAdapter()).setShowUpdatedOnly(isShowUpdatedOnlyMode());
    }
    
    @Override
    protected void onPause() {
        super.onPause();
    }
    
    @Override
    protected SimpleListAdapterBase<?> createListAdapter() {
        return new RecentListAdapter(this, getListFontPref());
    }
    
    // ////////////////////////////////////////////////////////////
    // アイテムタップ
    // ////////////////////////////////////////////////////////////
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        ThreadData thread_data = ((RecentListAdapter) getListAdapter()).getData(position);
        if (thread_data == null) {
            super.onListItemClick(l, v, position, id);
            return;
        }
        
        Intent intent = new Intent(this, ThreadEntryListActivity.class);
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
        menu.add(0, CTX_MENU_DELETE_RECENT, CTX_MENU_DELETE_RECENT, R.string.ctx_menu_delete_thread);
        menu.add(0, CTX_MENU_THREAD_INFO, CTX_MENU_THREAD_INFO, R.string.ctx_menu_thread_info);
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        ThreadData thread_data = ((RecentListAdapter) getListAdapter()).getData(info.position);
        if (thread_data == null) return true;
        
        switch (item.getItemId()) {
        case CTX_MENU_DELETE_RECENT:
            getAgent().deleteThreadEntryListCache(thread_data, new Runnable() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            reloadList();
                        }
                    });
                }
            });
            break;
        case CTX_MENU_THREAD_INFO:
        	ThreadInfoDialog dialog = new ThreadInfoDialog(this, thread_data);
        	dialog.show();
        	break;
        default:
            break;
        }
        return true;
    }
    
    // ////////////////////////////////////////////////////////////
    // オプションメニュー
    // ////////////////////////////////////////////////////////////
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        
        // キャッシュ並び替え
        
        MenuItem order_item = menu.add(0, MENU_KEY_CACHE_ORDER, MENU_KEY_CACHE_ORDER,
                getString(R.string.label_menu_sort_threads));
        order_item.setIcon(android.R.drawable.ic_menu_sort_by_size);
        order_item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                showDialogCacheOrder();
                return false;
            }
        });
        
        // キャッシュ削除ボタン
        MenuItem clear_item = menu.add(0, MENU_KEY_CLEAR_CACHE, MENU_KEY_CLEAR_CACHE,
                getString(R.string.label_menu_clear_cache));
        clear_item.setIcon(android.R.drawable.ic_menu_delete);
        clear_item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                showDialogClearCache();
                return false;
            }
        });
        return true;
    }
    
    private void showDialogClearCache() {
        String[] menu_strings = new String[] { getString(R.string.label_submenu_delete_filled),
                getString(R.string.label_submenu_delete_except_favorite) };
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.label_menu_clear_cache);
        builder.setItems(menu_strings, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                case 0:
                    SimpleDialog.showYesNo(RecentListActivity.this, R.string.label_submenu_delete_filled,
                            R.string.dialog_text_delete_filled_threads, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    deleteFilled();
                                }
                            }, null);
                    break;
                case 1:
                    SimpleDialog.showYesNo(RecentListActivity.this, R.string.label_submenu_delete_except_favorite,
                            R.string.dialog_text_delete_threads_except_favorite, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    deleteExceptFavorite();
                                }
                            }, null);
                    break;
                
                }
            }
        });
        builder.create().show();
    }
    
    private void showDialogCacheOrder() {
        String[] menu_strings = new String[] { getString(R.string.label_submenu_recent_read),
                getString(R.string.label_submenu_recent_write) };
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.label_menu_sort_threads);
        builder.setItems(menu_strings, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor editor = pref.edit();
                switch (which) {
                case 0:
                    editor.putInt("recents_order", ThreadData.KEY.RECENT_ORDER_READ);
                    break;
                case 1:
                    editor.putInt("recents_order", ThreadData.KEY.RECENT_ORDER_WRITE);
                    break;
                }
                editor.commit();
                reloadList();
            }
        });
        builder.create().show();
    }
    
    public void deleteFilled() {
        ((RecentListAdapter) getListAdapter()).deleteFilled(new RecentListAdapter.DeleteFilledCallback() {
            @Override
            public void onDeleted(ArrayList<ThreadData> deleteList) {
                getAgent().deleteRecentList(deleteList, new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                reloadList();
                            }
                        });
                    }
                });
            }
        });
    }
    
    public void deleteExceptFavorite() {
        ((RecentListAdapter) getListAdapter()).deleteExceptFavorite(new RecentListAdapter.DeleteFilledCallback() {
            @Override
            public void onDeleted(ArrayList<ThreadData> deleteList) {
                getAgent().deleteRecentList(deleteList, new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                reloadList();
                            }
                        });
                    }
                });
            }
        });
    }
    
    @Override
    protected void setShowUpdatedOnlyButton(boolean updated_only) {
        super.setShowUpdatedOnlyButton(updated_only);
        ((RecentListAdapter) getListAdapter()).setShowUpdatedOnly(updated_only);
    }
    
    // ////////////////////////////////////////////////////////////
    // リロード
    // ////////////////////////////////////////////////////////////
    
    @Override
    protected void onEndReload() {
        super.onEndReload();
    }
    
    @Override
    protected void reloadList() {
        if (!is_active_) return;
        
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final int recent_order = pref.getInt("recents_order", 0);
        
        final ReloadTerminator reload_terminator = getNewReloadTerminator();
        getAgent().fetchRecentList(recent_order, new ThreadListAgent.RecentListFetchedCallback() {
            @Override
            public void onRecentListFetched(final ArrayList<ThreadData> data_list) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (reload_terminator.is_terminated_) return;
                        ((RecentListAdapter) list_adapter_).setDataList(data_list, new Runnable() {
                            @Override
                            public void run() {
                                onEndReload();
                            }
                        });
                    }
                });
            }
        });
    }
    
    @Override
    protected void checkUpdate(boolean download) {
        if (service_task_ == null) return;
        if (download) {
            service_task_.send(new ServiceSender() {
                @Override
                public void send(ITuboroidService service) throws RemoteException {
                    service.checkDownloadRecents();
                }
            });
        }
        else {
            service_task_.send(new ServiceSender() {
                @Override
                public void send(ITuboroidService service) throws RemoteException {
                    service.checkUpdateRecents();
                }
            });
        }
    }
}
