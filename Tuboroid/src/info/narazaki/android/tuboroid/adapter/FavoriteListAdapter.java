package info.narazaki.android.tuboroid.adapter;

import info.narazaki.android.tuboroid.R;
import info.narazaki.android.tuboroid.TuboroidApplication;
import info.narazaki.android.tuboroid.data.BoardIdentifier;
import info.narazaki.android.tuboroid.data.FavoriteItemData;
import info.narazaki.android.tuboroid.data.Find2chKeyData;
import info.narazaki.android.tuboroid.data.ThreadData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

// ////////////////////////////////////////////////////////////
// アダプタ
// ////////////////////////////////////////////////////////////
public class FavoriteListAdapter extends FavoriteListAdapterBase<FavoriteItemData> {
    public FavoriteListAdapter(Activity activity, TuboroidApplication.ViewConfig view_config) {
        super(activity, view_config);
    }
    
    @Override
    public void setShowUpdatedOnly(boolean updated_only) {
        if (updated_only) {
            setFilter(new Filter<FavoriteItemData>() {
                @Override
                public boolean filter(FavoriteItemData data) {
                    return data.isThread() && data.getThreadData().online_count_ > data.getThreadData().read_count_;
                }
            }, null);
        }
        else {
            setFilter(null, null);
        }
    }
    
    @Override
    public int getItemViewType(int position) {
        FavoriteItemData data = getData(position);
        if (data.isBoard()) return 1;
        if (data.isSearchKey()) return 2;
        return 0;
    }
    
    @Override
    public int getViewTypeCount() {
        return 3;
    }
    
    @Override
    protected View createView(FavoriteItemData data) {
        LayoutInflater layout_inflater = LayoutInflater.from(activity_);
        if (data.isBoard()) {
            View board_row = layout_inflater.inflate(R.layout.favorite_list_board_row, null);
            return board_row;
        }
        else if (data.isSearchKey()) {
            View search_key_row = layout_inflater.inflate(R.layout.find2ch_search_key_row, null);
            search_key_row = Find2chKeyData.initView(search_key_row, view_config_);
            return search_key_row;
        }
        View thread_list_row = layout_inflater.inflate(R.layout.thread_list_row, null);
        thread_list_row = ThreadData.initView(thread_list_row, view_config_);
        return thread_list_row;
    }
    
    @Override
    protected View setView(View view, FavoriteItemData data, ViewGroup parent) {
        return data.setView(view, view_config_);
    }
    
    public interface DeleteFilledCallback {
        public void onDeleted(ArrayList<FavoriteItemData> delete_list);
    }
    
    public void deleteFilled(final DeleteFilledCallback callback) {
        postAdapterThread(new Runnable() {
            @Override
            public void run() {
                final ArrayList<FavoriteItemData> delete_list = new ArrayList<FavoriteItemData>();
                final ArrayList<FavoriteItemData> new_list = new ArrayList<FavoriteItemData>();
                
                for (FavoriteItemData data : inner_data_list_) {
                    if (data.isThread() && data.getThreadData().isFilled()) {
                        delete_list.add(data);
                    }
                    else {
                        new_list.add(data);
                    }
                }
                setDataList(new_list, new Runnable() {
                    @Override
                    public void run() {
                        callback.onDeleted(delete_list);
                    }
                });
            }
        });
    }
    
    public interface OnSortedCallback {
        public void onSorted(ArrayList<FavoriteItemData> data_list);
    }
    
    public void sortAutomatically(final OnSortedCallback callback) {
        postAdapterThread(new Runnable() {
            @Override
            public void run() {
                final ArrayList<FavoriteItemData> work_list = new ArrayList<FavoriteItemData>();
                work_list.addAll(inner_data_list_);
                
                final HashMap<BoardIdentifier, Integer> server_tag_order_map = new HashMap<BoardIdentifier, Integer>();
                int i = 0;
                for (FavoriteItemData data : work_list) {
                    if (!server_tag_order_map.containsKey(data.getServerDef()) || data.isBoard()) {
                        server_tag_order_map.put(data.getServerDef(), i);
                        i++;
                    }
                }
                
                Collections.sort(work_list, new Comparator<FavoriteItemData>() {
                    @Override
                    public int compare(FavoriteItemData object1, FavoriteItemData object2) {
                        if (object1.isSearchKey()) return 1;
                        if (object2.isSearchKey()) return -1;
                        
                        int result = server_tag_order_map.get(object1.getServerDef())
                                - server_tag_order_map.get(object2.getServerDef());
                        if (result != 0) return result;
                        
                        if (object1.isBoard()) return -1;
                        if (object2.isBoard()) return 1;
                        
                        return 0;
                    }
                });
                
                activity_.runOnUiThread(new Runnable() {
                    
                    @Override
                    public void run() {
                        setDataList(work_list);
                        callback.onSorted(work_list);
                    }
                });
            }
        });
        
    }
    
}