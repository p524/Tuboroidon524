package info.narazaki.android.tuboroid.adapter;

import info.narazaki.android.tuboroid.R;
import info.narazaki.android.tuboroid.TuboroidApplication;
import info.narazaki.android.tuboroid.data.ThreadData;

import java.util.ArrayList;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

// ////////////////////////////////////////////////////////////
// アダプタ
// ////////////////////////////////////////////////////////////
public class RecentListAdapter extends FavoriteListAdapterBase<ThreadData> {
    public RecentListAdapter(Activity activity, TuboroidApplication.ViewConfig view_config) {
        super(activity, view_config);
    }
    
    @Override
    public void setShowUpdatedOnly(boolean updated_only) {
        if (updated_only) {
            setFilter(new Filter<ThreadData>() {
                @Override
                public boolean filter(ThreadData data) {
                    return data.online_count_ > data.read_count_;
                }
            }, null);
        }
        else {
            setFilter(null, null);
        }
    }
    
    @Override
    protected View createView(ThreadData data) {
        LayoutInflater layout_inflater = LayoutInflater.from(activity_);
        View view = layout_inflater.inflate(R.layout.thread_list_row, null);
        ThreadData.initView(view, view_config_);
        
        return view;
    }
    
    @Override
    protected View setView(View view, ThreadData data, ViewGroup parent) {
        return data.setView(view, view_config_);
    }
    
    public interface DeleteFilledCallback {
        public void onDeleted(ArrayList<ThreadData> delete_list);
    }
    
    public void deleteFilled(final DeleteFilledCallback callback) {
        postAdapterThread(new Runnable() {
            @Override
            public void run() {
                final ArrayList<ThreadData> delete_list = new ArrayList<ThreadData>();
                final ArrayList<ThreadData> new_list = new ArrayList<ThreadData>();
                
                for (ThreadData data : inner_data_list_) {
                    if (data.isFilled()) {
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
    
    public void deleteExceptFavorite(final DeleteFilledCallback callback) {
        postAdapterThread(new Runnable() {
            @Override
            public void run() {
                final ArrayList<ThreadData> delete_list = new ArrayList<ThreadData>();
                final ArrayList<ThreadData> new_list = new ArrayList<ThreadData>();
                
                for (ThreadData data : inner_data_list_) {
                    if (!data.is_favorite_) {
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
}