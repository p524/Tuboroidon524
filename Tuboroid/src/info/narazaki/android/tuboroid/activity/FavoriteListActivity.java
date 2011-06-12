package info.narazaki.android.tuboroid.activity;

import info.narazaki.android.lib.adapter.SimpleListAdapterBase;
import info.narazaki.android.lib.dialog.SimpleDialog;
import info.narazaki.android.lib.system.MigrationSDK5;
import info.narazaki.android.tuboroid.R;
import info.narazaki.android.tuboroid.TuboroidApplication;
import info.narazaki.android.tuboroid.activity.base.FavoriteListBaseActivity;
import info.narazaki.android.tuboroid.adapter.FavoriteListAdapter;
import info.narazaki.android.tuboroid.agent.FavoriteCacheListAgent;
import info.narazaki.android.tuboroid.data.BoardData;
import info.narazaki.android.tuboroid.data.FavoriteItemData;
import info.narazaki.android.tuboroid.data.Find2chKeyData;
import info.narazaki.android.tuboroid.data.ThreadData;
import info.narazaki.android.tuboroid.dialog.ThreadInfoDialog;
import info.narazaki.android.tuboroid.service.ITuboroidService;
import info.narazaki.android.tuboroid.service.TuboroidServiceTask.ServiceSender;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;

public class FavoriteListActivity extends FavoriteListBaseActivity {
    public static final String TAG = "FavoriteListActivity";
    
    // コンテキストメニュー
    private final static int CTX_MENU_DELETE_FAVORITE = 1;
    private final static int CTX_MENU_DELETE_FAVORITE_CACHE = 2;
    private final static int CTX_MENU_THREAD_INFO = 3;
    
    // メニュー
    // 自動整理
    public static final int MENU_KEY_AUTO_ORDER = 30;
    // 手動整理
    public static final int MENU_KEY_MANAGE = 40;
    
    // ////////////////////////////////////////////////////////////
    // ステート管理系
    // ////////////////////////////////////////////////////////////
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.favorite_list);
        registerForContextMenu(getListView());
        
        setTitle(getString(R.string.title_favorite, getString(R.string.app_name)));
        
        getTuboroidApplication().setHomeTabActivity(TuboroidApplication.KEY_HOME_ACTIVITY_FAVORITES);
        
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
        getTuboroidApplication().setHomeTabActivity(TuboroidApplication.KEY_HOME_ACTIVITY_FAVORITES);
        ((FavoriteListAdapter) getListAdapter()).setFontSize(getTuboroidApplication().view_config_);
        ((FavoriteListAdapter) getListAdapter()).setShowUpdatedOnly(isShowUpdatedOnlyMode());
    }
    
    @Override
    protected void onPause() {
        super.onPause();
    }
    
    @Override
    protected SimpleListAdapterBase<?> createListAdapter() {
        return new FavoriteListAdapter(this, getListFontPref());
    }
    
    // ////////////////////////////////////////////////////////////
    // アイテムタップ
    // ////////////////////////////////////////////////////////////
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        FavoriteItemData data = ((FavoriteListAdapter) getListAdapter()).getData(position);
        if (data == null) {
            super.onListItemClick(l, v, position, id);
            return;
        }
        if (data.isBoard()) {
            Intent intent = new Intent(this, ThreadListActivity.class);
            intent.setData(Uri.parse(data.getBoardData().getSubjectsURI()));
            MigrationSDK5.Intent_addFlagNoAnimation(intent);
            startActivity(intent);
        }
        else if (data.isThread()) {
            Intent intent = new Intent(this, ThreadEntryListActivity.class);
            ThreadData thread_data = data.getThreadData();
            String uri = thread_data.getThreadURI();
            intent.setData(Uri.parse(uri));
            intent.putExtra(ThreadEntryListActivity.INTENT_KEY_MAYBE_THREAD_NAME, thread_data.thread_name_);
            intent.putExtra(ThreadEntryListActivity.INTENT_KEY_MAYBE_ONLINE_COUNT, thread_data.online_count_);
            MigrationSDK5.Intent_addFlagNoAnimation(intent);
            startActivity(intent);
        }
        else if (data.isSearchKey()) {
            Find2chKeyData search_key_data = data.getSearchKey();
            Intent intent = new Intent(this, Find2chSearchActivity.class);
            intent.putExtra(Find2chSearchActivity.INTENT_KEY_SEARCH_KEYWORD, search_key_data.keyword_);
            MigrationSDK5.Intent_addFlagNoAnimation(intent);
            startActivity(intent);
        }
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menu_info) {
        menu.clear();
        menu.setHeaderTitle(R.string.ctx_menu_title_favorite);
        menu.add(0, CTX_MENU_DELETE_FAVORITE, CTX_MENU_DELETE_FAVORITE, R.string.ctx_menu_delete_favorite);
        
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menu_info;
        FavoriteItemData data = ((FavoriteListAdapter) getListAdapter()).getData(info.position);
        if (data != null && data.isThread()) {
            menu.add(0, CTX_MENU_DELETE_FAVORITE_CACHE, CTX_MENU_DELETE_FAVORITE_CACHE,
                    R.string.ctx_menu_delete_favorite_cache);
            menu.add(0, CTX_MENU_THREAD_INFO, CTX_MENU_THREAD_INFO,
            		R.string.ctx_menu_thread_info);
        }
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        FavoriteItemData data = ((FavoriteListAdapter) getListAdapter()).getData(info.position);
        if (data == null) return true;
        
        if (data.isBoard()) {
            onContextBoardItemSelected(item, data.getBoardData());
        }
        else if (data.isThread()) {
            onContextThreadItemSelected(item, data.getThreadData());
        }
        else if (data.isSearchKey()) {
            onContextThreadItemSelected(item, data.getSearchKey());
        }
        
        return true;
    }
    
    private void onContextBoardItemSelected(MenuItem item, BoardData board_data) {
        switch (item.getItemId()) {
        case CTX_MENU_DELETE_FAVORITE:
            board_data.is_favorite_ = false;
            getAgent().delFavorite(board_data, new Runnable() {
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
        default:
            break;
        }
    }
    
    private void onContextThreadItemSelected(MenuItem item, ThreadData thread_data) {
        switch (item.getItemId()) {
        case CTX_MENU_DELETE_FAVORITE:
            thread_data.is_favorite_ = false;
            getAgent().delFavorite(thread_data, new Runnable() {
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
        case CTX_MENU_DELETE_FAVORITE_CACHE:
            thread_data.is_favorite_ = false;
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
    }
    
    private void onContextThreadItemSelected(MenuItem item, Find2chKeyData search_key) {
        switch (item.getItemId()) {
        case CTX_MENU_DELETE_FAVORITE:
            search_key.is_favorite_ = false;
            getAgent().delFavorite(search_key, new Runnable() {
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
        default:
            break;
        }
    }
    
    // ////////////////////////////////////////////////////////////
    // オプションメニュー
    // ////////////////////////////////////////////////////////////
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        
        // 並び替えボタン
        MenuItem manage_item = menu.add(0, MENU_KEY_MANAGE, MENU_KEY_MANAGE,
                getString(R.string.label_menu_manual_order));
        manage_item.setIcon(android.R.drawable.ic_menu_manage);
        manage_item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent intent = new Intent(FavoriteListActivity.this, FavoriteListManageActivity.class);
                MigrationSDK5.Intent_addFlagNoAnimation(intent);
                startActivity(intent);
                return false;
            }
        });
        
        // 自動整理ボタン
        MenuItem sort_item = menu.add(0, MENU_KEY_AUTO_ORDER, MENU_KEY_AUTO_ORDER,
                getString(R.string.label_menu_auto_order));
        sort_item.setIcon(android.R.drawable.ic_menu_sort_by_size);
        sort_item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                showDialogAutoOrder();
                return false;
            }
        });
        
        return true;
    }
    
    private void showDialogAutoOrder() {
        String[] menu_strings = new String[] { getString(R.string.label_submenu_delete_filled),
                getString(R.string.label_submenu_auto_sort) };
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.label_menu_auto_order);
        builder.setItems(menu_strings, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                case 0:
                    SimpleDialog.showYesNo(FavoriteListActivity.this, R.string.label_submenu_delete_filled,
                            R.string.dialog_text_delete_filled_threads, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    deleteFilled();
                                }
                            }, null);
                    break;
                case 1:
                    SimpleDialog.showYesNo(FavoriteListActivity.this, R.string.label_submenu_auto_sort,
                            R.string.dialog_text_auto_sort, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    sortAutomatically();
                                }
                            }, null);
                    break;
                }
            }
        });
        builder.create().show();
    }
    
    public void deleteFilled() {
        ((FavoriteListAdapter) getListAdapter()).deleteFilled(new FavoriteListAdapter.DeleteFilledCallback() {
            @Override
            public void onDeleted(ArrayList<FavoriteItemData> deleteList) {
                getAgent().deleteFavoriteList(deleteList, new Runnable() {
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
    
    public void sortAutomatically() {
        ((FavoriteListAdapter) getListAdapter()).sortAutomatically(new FavoriteListAdapter.OnSortedCallback() {
            @Override
            public void onSorted(ArrayList<FavoriteItemData> dataList) {
                getAgent().setNewFavoriteListOrder(dataList, new Runnable() {
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
        ((FavoriteListAdapter) getListAdapter()).setShowUpdatedOnly(updated_only);
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
        final ReloadTerminator reload_terminator = getNewReloadTerminator();
        getAgent().fetchFavoriteList(new FavoriteCacheListAgent.FavoriteListFetchedCallback() {
            
            @Override
            public void onFavoriteListFetched(final ArrayList<FavoriteItemData> data_list) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (reload_terminator.is_terminated_) return;
                        ((FavoriteListAdapter) list_adapter_).setDataList(data_list, new Runnable() {
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
                    service.checkDownloadFavorites(false);
                }
            });
        }
        else {
            service_task_.send(new ServiceSender() {
                @Override
                public void send(ITuboroidService service) throws RemoteException {
                    service.checkUpdateFavorites(false);
                }
            });
        }
    }
    
}
