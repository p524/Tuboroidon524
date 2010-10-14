package info.narazaki.android.lib.activity.base;

import info.narazaki.android.lib.aplication.NApplication;
import android.app.ExpandableListActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;

public class NExpandableListActivity extends ExpandableListActivity implements NAbstractListScrollManager.Target {
    private static final String TAG = "NExpandableListActivity";
    
    private boolean on_first_shown_ = true;
    private NAbstractListScrollManager scroll_manager_ = null;
    
    public static class PositionData {
        public long packed_pos_;
        public int y_;
        
        public PositionData(long packed_pos, int y) {
            super();
            packed_pos_ = packed_pos;
            y_ = y;
        }
    }
    
    private class ListScrollManager extends NAbstractListScrollManager {
        
        public ListScrollManager(Target target) {
            super(target);
        }
        
        @Override
        protected int getChildTopPos() {
            ExpandableListView view = getExpandableListView();
            if (view == null) return 0;
            return view.getChildAt(0).getTop();
        }
        
        @Override
        protected int getFirstVisiblePosition() {
            ExpandableListView view = getExpandableListView();
            if (view == null) return 0;
            return view.getFirstVisiblePosition();
        }
        
        @Override
        protected int getListViewHeight() {
            ExpandableListView view = getExpandableListView();
            if (view == null) return 0;
            return view.getHeight();
        }
        
        @Override
        protected boolean postForView(Runnable action) {
            ExpandableListView view = getExpandableListView();
            if (view == null) return false;
            return view.post(action);
        }
        
        @Override
        protected void setListPosition(int position, Runnable callback) {
            NExpandableListActivity.this.setListPositionFromTopImpl(position, 0, callback);
        }
        
        @Override
        protected void setSelectionFromTop(int position, int y) {
            ExpandableListView view = getExpandableListView();
            if (view == null) return;
            view.setSelectionFromTop(position, y);
        }
        
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        on_first_shown_ = true;
        scroll_manager_ = new ListScrollManager(this);
        scroll_manager_.onCreate(savedInstanceState);
        getExpandableListView().setOnScrollListener(scroll_manager_);
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
    
    @Override
    protected void onPause() {
        scroll_manager_.onPause();
        super.onPause();
    }
    
    protected void onFirstResume() {}
    
    protected void onSecondResume() {}
    
    @Override
    protected void onDestroy() {
        scroll_manager_.onDestroy();
        getNApplication().onActivityDestroy(this);
        super.onDestroy();
    }
    
    public PositionData getCurrentPosition() {
        ExpandableListView view = getExpandableListView();
        if (view == null) return new PositionData(0, 0);
        
        long packed_pos = view.getExpandableListPosition(view.getFirstVisiblePosition());
        View top_view = view.getChildAt(0);
        int y = top_view != null ? top_view.getTop() : 0;
        return new PositionData(packed_pos, y);
    }
    
    public void setListPositionFromTop(final long packed_pos, final int y, final Runnable callback) {
        setListPositionFromTopImpl(packed_pos, y, callback);
    }
    
    public void setListPositionFromTopImpl(final long packed_pos, final int y, final Runnable callback) {
        ExpandableListView view = getExpandableListView();
        if (view != null) {
            int pos = view.getFlatListPosition(packed_pos);
            if (0 <= pos || pos < view.getCount()) {
                setListPositionFromTopImpl(pos, y, callback);
                return;
            }
        }
        if (callback != null) callback.run();
    }
    
    public void setListPositionFromTopImpl(final int position, final int y, final Runnable callback) {
        final ExpandableListView view = getExpandableListView();
        view.post(new Runnable() {
            @Override
            public void run() {
                if (view == null) return;
                view.requestFocus();
                
                if (view.getCount() > position && position >= 0) {
                    view.setSelectionFromTop(position, y);
                }
                if (callback != null) callback.run();
            }
        });
    }
    
    // ///////////////////////////////////////////////
    // scrolling
    // ///////////////////////////////////////////////
    
    @Override
    public void onScrollReachedBottom() {}
    
    @Override
    public void onScrollReachedTop() {}
    
    @Override
    public void onScrollReleasedBottom() {}
    
    @Override
    public void onScrollReleasedTop() {}
    
    @Override
    public boolean hasScrollingListData() {
        final ExpandableListAdapter adapter = getExpandableListAdapter();
        final ExpandableListView view = getExpandableListView();
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
