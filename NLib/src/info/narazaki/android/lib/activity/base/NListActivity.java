package info.narazaki.android.lib.activity.base;

import info.narazaki.android.lib.aplication.NApplication;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

public class NListActivity extends ListActivity implements NAbstractListScrollManager.Target {
    private static final String TAG = "NListActivity";
    
    private boolean on_first_shown_ = true;
    private NAbstractListScrollManager scroll_manager_ = null;
    
    public static class PositionData {
        public int position_;
        public int y_;
        
        public PositionData(int position, int y) {
            super();
            position_ = position;
            y_ = y;
        }
        
        public PositionData(PositionData orig) {
            super();
            position_ = orig.position_;
            y_ = orig.y_;
        }
        
        @Override
        public boolean equals(Object o) {
            if (o instanceof PositionData) {
                return (position_ == ((PositionData) o).position_) && (y_ == ((PositionData) o).y_);
            }
            return super.equals(o);
        }
    }
    
    private class ListScrollManager extends NAbstractListScrollManager {
        
        public ListScrollManager(Target target) {
            super(target);
        }
        
        @Override
        protected int getChildTopPos() {
            ListView view = getListView();
            if (view == null) return 0;
            return view.getChildAt(0).getTop();
        }
        
        @Override
        protected int getFirstVisiblePosition() {
            ListView view = getListView();
            if (view == null) return 0;
            return view.getFirstVisiblePosition();
        }
        
        @Override
        protected int getListViewHeight() {
            ListView view = getListView();
            if (view == null) return 0;
            return view.getHeight();
        }
        
        @Override
        protected boolean postForView(Runnable action) {
            ListView view = getListView();
            if (view == null) return false;
            return view.post(action);
        }
        
        @Override
        protected void setListPosition(int position, Runnable callback) {
            NListActivity.this.setListPosition(position, callback);
        }
        
        @Override
        protected void setSelectionFromTop(int position, int y) {
            ListView view = getListView();
            if (view == null) return;
            view.setSelectionFromTop(position, y);
        }
        
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        on_first_shown_ = true;
        scroll_manager_ = new ListScrollManager(this);
        scroll_manager_.onCreate(savedInstanceState);
        getListView().setOnScrollListener(scroll_manager_);
        super.onCreate(savedInstanceState);
    }
    
    protected NApplication getNApplication() {
        return (NApplication) getApplication();
    }
    
    @Override
    public int getScrollingAmount() {
        return getNApplication().getScrollingAmount();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        scroll_manager_.onResume();
        getNApplication().onActivityResume(this);
        if (on_first_shown_) {
            onFirstResume();
            on_first_shown_ = false;
        }
        else {
            onSecondResume();
        }
    }
    
    protected void onFirstResume() {}
    
    protected void onSecondResume() {}
    
    @Override
    protected void onPause() {
        scroll_manager_.onPause();
        super.onPause();
    }
    
    @Override
    protected void onDestroy() {
        scroll_manager_.onDestroy();
        getNApplication().onActivityDestroy(this);
        super.onDestroy();
    }
    
    public void redrawListView() {
        getListView().post(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ListAdapter adapter = getListAdapter();
                        if (adapter == null) return;
                        
                        if (adapter instanceof BaseAdapter) {
                            BaseAdapter base_adapter = (BaseAdapter) adapter;
                            base_adapter.notifyDataSetChanged();
                        }
                    }
                });
            }
        });
    }
    
    public PositionData getCurrentPosition() {
        ListView view = getListView();
        if (view == null) return new PositionData(0, 0);
        
        int pos = view.getFirstVisiblePosition();
        View top_view = getListView().getChildAt(0);
        int y = top_view != null ? top_view.getTop() : 0;
        return new PositionData(pos, y);
    }
    
    public int getCurrentBottomPosition() {
        ListView view = getListView();
        if (view == null) return 0;
        
        int pos = view.getLastVisiblePosition();
        if (pos != -1) return pos;
        
        ListAdapter adapter = getListAdapter();
        if (adapter == null) return 0;
        return adapter.getCount() - 1;
    }
    
    public void setListPosition(final int position, final Runnable callback) {
        setListPositionFromTop(position, 0, callback);
    }
    
    public void setListPositionFromTop(final int position, final int y, final Runnable callback) {
        setListPositionFromTopImpl(position, y, callback);
    }
    
    public void setListPositionFromTopImpl(final int position, final int y, final Runnable callback) {
        final ListAdapter adapter = getListAdapter();
        final ListView view = getListView();
        view.post(new Runnable() {
            @Override
            public void run() {
                if (adapter != null && view != null) {
                    view.requestFocus();
                    
                    if (adapter.getCount() > position && position >= 0) {
                        view.setSelectionFromTop(position, y);
                    }
                }
                if (callback != null) callback.run();
            }
        });
    }
    
    public void setListPositionTop(final Runnable callback) {
        setListPositionFromTopImpl(0, 0, callback);
    }
    
    public void setListPositionBottom(final Runnable callback) {
        ListAdapter adapter = getListAdapter();
        if (adapter != null) {
            setListPositionFromTopImpl(adapter.getCount() - 1, 0, callback);
        }
    }
    
    // ///////////////////////////////////////////////
    // scrolling
    // ///////////////////////////////////////////////
    
    @Override
    public void onScrollReachedTop() {}
    
    @Override
    public void onScrollReachedBottom() {}
    
    @Override
    public void onScrollReleasedTop() {}
    
    @Override
    public void onScrollReleasedBottom() {}
    
    @Override
    public boolean hasScrollingListData() {
        final ListAdapter adapter = getListAdapter();
        final ListView view = getListView();
        if (adapter == null || view == null || view.getCount() <= 0) return false;
        return true;
    }
    
    public void setListRollUp(final Runnable callback) {
        scroll_manager_.setListRollUp(callback);
    }
    
    public void setListPageUp() {
        scroll_manager_.setListPageUp();
    }
    
    public void setListRollDown(final Runnable callback) {
        scroll_manager_.setListRollDown(callback);
    }
    
    public void setListPageDown() {
        scroll_manager_.setListPageDown();
    }
}
