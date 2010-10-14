package info.narazaki.android.tuboroid.adapter;

import info.narazaki.android.lib.adapter.SimpleListAdapterBase;
import info.narazaki.android.tuboroid.R;
import info.narazaki.android.tuboroid.TuboroidApplication;
import info.narazaki.android.tuboroid.data.Find2chResultData;

import java.util.ArrayList;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

// ////////////////////////////////////////////////////////////
// アダプタ
// ////////////////////////////////////////////////////////////
public class Find2chResultAdapter extends SimpleListAdapterBase<Find2chResultData> {
    TuboroidApplication.ViewConfig view_config_;
    protected Activity activity_;
    
    public Find2chResultAdapter(Activity activity, TuboroidApplication.ViewConfig view_config) {
        super();
        activity_ = activity;
        setDataList(new ArrayList<Find2chResultData>());
        view_config_ = new TuboroidApplication.ViewConfig(view_config);
    }
    
    public void setFontSize(TuboroidApplication.ViewConfig view_config) {
        view_config_ = new TuboroidApplication.ViewConfig(view_config);
        notifyDataSetInvalidated();
    }
    
    @Override
    protected View createView(Find2chResultData data) {
        LayoutInflater layout_inflater = LayoutInflater.from(activity_);
        View view = layout_inflater.inflate(R.layout.find2ch_list_row, null);
        Find2chResultData.initView(view, view_config_);
        return view;
    }
    
    @Override
    protected View setView(View view, Find2chResultData data, ViewGroup parent) {
        return data.setView(view, view_config_);
    }
}