package info.narazaki.android.tuboroid.adapter;

import info.narazaki.android.lib.adapter.SimpleListAdapterBase;
import info.narazaki.android.tuboroid.R;
import info.narazaki.android.tuboroid.TuboroidApplication;
import info.narazaki.android.tuboroid.data.FavoriteItemData;
import info.narazaki.android.tuboroid.data.FavoriteManageItemData;

import java.util.ArrayList;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

// ////////////////////////////////////////////////////////////
// アダプタ
// ////////////////////////////////////////////////////////////
public class FavoriteListManageAdapter extends SimpleListAdapterBase<FavoriteManageItemData> {
    Activity activity_;
    TuboroidApplication.ViewConfig view_config_;
    
    public FavoriteListManageAdapter(Activity activity, TuboroidApplication.ViewConfig view_config) {
        super();
        activity_ = activity;
        setDataList(new ArrayList<FavoriteManageItemData>());
        view_config_ = new TuboroidApplication.ViewConfig(view_config);
    }
    
    public void setFontSize(TuboroidApplication.ViewConfig view_config) {
        view_config_ = new TuboroidApplication.ViewConfig(view_config);
        notifyDataSetInvalidated();
    }
    
    @Override
    protected View createView(FavoriteManageItemData data) {
        LayoutInflater layout_inflater = LayoutInflater.from(activity_);
        View layout_view = layout_inflater.inflate(R.layout.favorite_manage_list_row, null);
        
        layout_view = FavoriteManageItemData.initView(layout_view, view_config_);
        return layout_view;
    }
    
    @Override
    protected View setView(View view, FavoriteManageItemData data, ViewGroup parent) {
        return data.setView(view, view_config_);
    }
    
    public FavoriteManageItemData pickUp(int position) {
        FavoriteManageItemData target = data_list_.get(position);
        data_list_.remove(position);
        return target;
    }
    
    public void pushDown(int position, FavoriteManageItemData target) {
        data_list_.add(position, target);
    }
    
    public void pushDown(FavoriteManageItemData target) {
        data_list_.add(target);
    }
    
    public ArrayList<FavoriteItemData> commitDelete() {
        ArrayList<FavoriteItemData> delete_list = new ArrayList<FavoriteItemData>();
        ArrayList<FavoriteManageItemData> new_list = new ArrayList<FavoriteManageItemData>();
        for (FavoriteManageItemData data : data_list_) {
            if (data.checked_) {
                delete_list.add(data.item_);
            }
            else {
                new_list.add(data);
            }
        }
        setDataList(new_list);
        
        return delete_list;
    }
    
    public ArrayList<FavoriteItemData> getNewOrderedList() {
        ArrayList<FavoriteItemData> new_list = new ArrayList<FavoriteItemData>();
        for (FavoriteManageItemData data : data_list_) {
            new_list.add(data.item_);
        }
        
        return new_list;
    }
}