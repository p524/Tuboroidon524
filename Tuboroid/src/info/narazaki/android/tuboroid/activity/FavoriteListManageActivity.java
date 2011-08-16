package info.narazaki.android.tuboroid.activity;

import info.narazaki.android.lib.adapter.SimpleListAdapterBase;
import info.narazaki.android.lib.dialog.SimpleDialog;
import info.narazaki.android.lib.view.NLabelView;
import info.narazaki.android.tuboroid.R;
import info.narazaki.android.tuboroid.TuboroidApplication;
import info.narazaki.android.tuboroid.activity.base.TuboroidListActivity;
import info.narazaki.android.tuboroid.adapter.FavoriteListManageAdapter;
import info.narazaki.android.tuboroid.agent.FavoriteCacheListAgent;
import info.narazaki.android.tuboroid.data.FavoriteItemData;
import info.narazaki.android.tuboroid.data.FavoriteManageItemData;

import java.util.ArrayList;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class FavoriteListManageActivity extends TuboroidListActivity {
    public static final String TAG = "FavoriteListManagerActivity";
    
    // メニュー
    public static final int MENU_KEY_APPLY_DELETE = 40;
    
    FavoriteManageItemData picked_data_ = null;
    View append_last_footer_view_ = null;
    
    // ////////////////////////////////////////////////////////////
    // ステート管理系
    // ////////////////////////////////////////////////////////////
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.favorite_manage_list);
        
        setTitle(getString(R.string.title_favorite_manage, getString(R.string.app_name)));
        
        picked_data_ = null;
        
        LayoutInflater layout_inflater = LayoutInflater.from(this);
        append_last_footer_view_ = layout_inflater.inflate(R.layout.favorite_manage_list_footer_row, null);
        ((NLabelView) append_last_footer_view_.findViewById(R.id.favorite_footer_label))
                .setTextSize(getTuboroidApplication().view_config_.board_list_);
        getListView().addFooterView(append_last_footer_view_);
        append_last_footer_view_.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pushDown();
            }
        });
        getListView().postInvalidate();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        ((FavoriteListManageAdapter) getListAdapter()).setFontSize(getTuboroidApplication().view_config_);
    }
    
    @Override
    protected void onPause() {
        commitChange(false);
        super.onPause();
    }
    
    @Override
    protected SimpleListAdapterBase<?> createListAdapter() {
        return new FavoriteListManageAdapter(this, getListFontPref());
    }
    
    @Override
    protected void onFirstDataRequired() {
        reloadList();
    }
    
    @Override
    protected void onResumeDataRequired() {
        reloadList();
    }
    
    // ////////////////////////////////////////////////////////////
    // アイテムタップ
    // ////////////////////////////////////////////////////////////
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        if (picked_data_ == null) {
            pickUp(position);
        }
        else {
            pushDown(position);
        }
        
    }
    
    void pickUp(int position) {
        FavoriteManageItemData target = ((FavoriteListManageAdapter) getListAdapter()).pickUp(position);
        LinearLayout stack_box = (LinearLayout) findViewById(R.id.favorite_stack_box);
        stack_box.setVisibility(View.VISIBLE);
        
        NLabelView stack_name = (NLabelView) stack_box.findViewById(R.id.favorite_stack_name);
        target.setStackView(stack_name, getTuboroidApplication().view_config_);
        
        picked_data_ = target;
        
    }
    
    void pushDown(int position) {
        ((FavoriteListManageAdapter) getListAdapter()).pushDown(position, picked_data_);
        pushDownCleanUp();
    }
    
    void pushDown() {
        if (picked_data_ == null) return;
        ((FavoriteListManageAdapter) getListAdapter()).pushDown(picked_data_);
        pushDownCleanUp();
    }
    
    void pushDownCleanUp() {
        LinearLayout stack_box = (LinearLayout) findViewById(R.id.favorite_stack_box);
        stack_box.setVisibility(View.GONE);
        
        picked_data_ = null;
    }
    
    void commitChange(final boolean reload_on_save) {
        pushDown();
        final ArrayList<FavoriteItemData> new_list = ((FavoriteListManageAdapter) getListAdapter()).getNewOrderedList();
        final Runnable reload_callback = new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        reloadList();
                    }
                });
            }
        };
        getAgent().setNewFavoriteListOrder(new_list, reload_on_save ? reload_callback : null);
    }
    
    void commitDelete() {
        pushDown();
        final ArrayList<FavoriteItemData> delete_list = ((FavoriteListManageAdapter) getListAdapter()).commitDelete();
        
        getAgent().deleteFavoriteList(delete_list, null);
    }
    
    // ////////////////////////////////////////////////////////////
    // オプションメニュー
    // ////////////////////////////////////////////////////////////
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        
        // 削除
        MenuItem delete_item = menu.add(0, MENU_KEY_APPLY_DELETE, MENU_KEY_APPLY_DELETE,
                getString(R.string.label_menu_delete_checked_favorite));
        delete_item.setIcon(android.R.drawable.ic_menu_delete);
        delete_item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                SimpleDialog.showYesNo(FavoriteListManageActivity.this, R.string.label_menu_delete_checked_favorite,
                        R.string.dialog_text_delete_checked_threads, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                commitDelete();
                            }
                        }, null);
                return false;
            }
        });
        
        return true;
    }
    
    // ////////////////////////////////////////////////////////////
    // リロード
    // ////////////////////////////////////////////////////////////
    protected void reloadList() {
        if (!is_active_) return;
        
        getAgent().fetchFavoriteList(new FavoriteCacheListAgent.FavoriteListFetchedCallback() {
            
            @Override
            public void onFavoriteListFetched(final ArrayList<FavoriteItemData> dataList) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!is_active_) return;
                        ArrayList<FavoriteManageItemData> new_list = new ArrayList<FavoriteManageItemData>();
                        for (FavoriteItemData data : dataList) {
                            new_list.add(new FavoriteManageItemData(data));
                        }
                        ((FavoriteListManageAdapter) list_adapter_).setDataList(new_list);
                    }
                });
            }
        });
    }
    
    protected TuboroidApplication.ViewConfig getListFontPref() {
        return getTuboroidApplication().view_config_;
    }
    
}
