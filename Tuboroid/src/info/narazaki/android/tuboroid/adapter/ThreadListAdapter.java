package info.narazaki.android.tuboroid.adapter;

import info.narazaki.android.lib.adapter.FilterableListAdapterBase;
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
public class ThreadListAdapter extends FilterableListAdapterBase<ThreadData> {
    TuboroidApplication.ViewConfig view_config_;
    final ThreadData.ViewStyle view_style_;
    
    public ThreadListAdapter(Activity activity, TuboroidApplication.ViewConfig view_config) {
        super(activity);
        setDataList(new ArrayList<ThreadData>());
        view_config_ = new TuboroidApplication.ViewConfig(view_config);
        view_style_ = new ThreadData.ViewStyle(activity);
    }
    
    public void setFontSize(TuboroidApplication.ViewConfig view_config) {
        view_config_ = new TuboroidApplication.ViewConfig(view_config);
        notifyDataSetInvalidated();
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
        return data.setView(view, view_config_, view_style_);
    }
}