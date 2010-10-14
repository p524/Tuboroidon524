package info.narazaki.android.tuboroid.adapter;

import info.narazaki.android.lib.adapter.FilterableListAdapterBase;
import info.narazaki.android.tuboroid.R;
import info.narazaki.android.tuboroid.TuboroidApplication;
import info.narazaki.android.tuboroid.agent.TuboroidAgent;
import info.narazaki.android.tuboroid.data.ThreadData;
import info.narazaki.android.tuboroid.data.ThreadEntryData;
import info.narazaki.android.tuboroid.data.ThreadEntryData.ImageViewerLauncher;

import java.util.ArrayList;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ThreadEntryListAdapter extends FilterableListAdapterBase<ThreadEntryData> {
    private static final String TAG = "ThreadEntryListAdapter";
    
    TuboroidApplication.ViewConfig view_config_;
    ThreadEntryData.ViewStyle view_style_;
    TuboroidAgent agent_;
    ThreadData thread_data_;
    
    boolean is_quick_show_;
    
    int read_count_;
    
    public ThreadEntryListAdapter(Activity activity, TuboroidAgent agent, TuboroidApplication.ViewConfig view_config,
            ImageViewerLauncher image_viewer_launcher, ThreadEntryData.OnAnchorClickedCallback anchor_callback) {
        super(activity);
        setDataList(new ArrayList<ThreadEntryData>());
        agent_ = agent;
        thread_data_ = null;
        view_config_ = view_config;
        read_count_ = 0;
        
        view_style_ = new ThreadEntryData.ViewStyle(activity, image_viewer_launcher, anchor_callback);
    }
    
    public void setFontSize(TuboroidApplication.ViewConfig view_config) {
        view_config_ = view_config;
    }
    
    public void setThreadData(ThreadData thread_data) {
        thread_data_ = thread_data;
    }
    
    public void setReadCount(int read_count) {
        read_count_ = read_count;
    }
    
    public void setQuickShow(boolean is_quick_show) {
        is_quick_show_ = is_quick_show;
    }
    
    public void analyzeThreadEntryList(final Runnable callback,
            final ThreadEntryData.AnalyzeThreadEntryListProgressCallback progress) {
        postAdapterThread(new Runnable() {
            @Override
            public void run() {
                ThreadEntryData.analyzeThreadEntryList(agent_, thread_data_, view_config_, view_style_,
                        inner_data_list_, progress);
                activity_.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        notifyDataSetChanged();
                        if (callback != null) callback.run();
                    }
                });
            }
        });
    }
    
    // ////////////////////////////////////////////////////////////
    @Override
    public void clearData() {
        super.clearData();
    }
    
    @Override
    public int getCount() {
        return super.getCount();
    }
    
    @Override
    public boolean hasStableIds() {
        return true;
    }
    
    @Override
    public int getItemViewType(int position) {
        if (getData(position).entry_is_aa_) return 1;
        return 0;
    }
    
    @Override
    public int getViewTypeCount() {
        return 2;
    }
    
    @Override
    protected View createView(ThreadEntryData data) {
        LayoutInflater layout_inflater = LayoutInflater.from(activity_);
        if (data == null || !data.entry_is_aa_) {
            View view = layout_inflater.inflate(R.layout.entry_list_row, null);
            ThreadEntryData.initView(view, view_config_, view_style_, false);
            return view;
        }
        View view = layout_inflater.inflate(R.layout.entry_list_row_aa, null);
        ThreadEntryData.initView(view, view_config_, view_style_, true);
        return view;
    }
    
    @Override
    protected View setView(View view, ThreadEntryData data, ViewGroup parent) {
        if (data == null) return view;
        return data.setView(agent_, thread_data_, view, parent, read_count_, view_config_, view_style_, is_quick_show_);
    }
}