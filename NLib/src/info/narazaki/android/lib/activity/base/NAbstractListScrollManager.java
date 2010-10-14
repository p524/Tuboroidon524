package info.narazaki.android.lib.activity.base;

import java.util.Timer;
import java.util.TimerTask;

import android.os.Bundle;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;

abstract public class NAbstractListScrollManager implements OnScrollListener {
    private static final String TAG = "NListScrollManager";
    
    public static interface Target {
        
        public void onScrollReachedTop();
        
        public void onScrollReachedBottom();
        
        public void onScrollReleasedTop();
        
        public void onScrollReleasedBottom();
        
        public boolean hasScrollingListData();
        
        public int getScrollingAmount();
    }
    
    private static final long SCROLL_INTERVAL_MS = 60;
    
    public static final int SCROLL_STATE_NONE = 0;
    public static final int SCROLL_STATE_TOP = 1;
    public static final int SCROLL_STATE_BOTTOM = 2;
    
    private Target target_ = null;
    private Timer scroll_timer_ = null;
    private int scroll_top_bottom_state_ = SCROLL_STATE_NONE;
    
    private int current_y_;
    private int remain_count_;
    
    public NAbstractListScrollManager(Target target) {
        target_ = target;
    }
    
    protected void onCreate(Bundle savedInstanceState) {
        scroll_timer_ = null;
        scroll_top_bottom_state_ = SCROLL_STATE_NONE;
    }
    
    protected void onDestroy() {}
    
    protected void onResume() {
        scroll_top_bottom_state_ = SCROLL_STATE_NONE;
    }
    
    protected void onPause() {
        stopSmoothScrolling();
    }
    
    public int getScrollTopBottomState() {
        return scroll_top_bottom_state_;
    }
    
    abstract protected int getFirstVisiblePosition();
    
    abstract protected int getChildTopPos();
    
    abstract protected boolean postForView(Runnable action);
    
    abstract protected int getListViewHeight();
    
    abstract protected void setListPosition(final int position, final Runnable callback);
    
    abstract protected void setSelectionFromTop(int position, int y);
    
    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (visibleItemCount == 0 || totalItemCount == 0) return;
        
        if (firstVisibleItem == 0) {
            if (scroll_top_bottom_state_ == SCROLL_STATE_BOTTOM) {
                target_.onScrollReleasedBottom();
            }
            if (scroll_top_bottom_state_ != SCROLL_STATE_TOP) {
                scroll_top_bottom_state_ = SCROLL_STATE_TOP;
                target_.onScrollReachedTop();
            }
        }
        else if (firstVisibleItem + visibleItemCount >= totalItemCount) {
            if (scroll_top_bottom_state_ == SCROLL_STATE_TOP) {
                target_.onScrollReleasedTop();
            }
            if (scroll_top_bottom_state_ != SCROLL_STATE_BOTTOM) {
                scroll_top_bottom_state_ = SCROLL_STATE_BOTTOM;
                target_.onScrollReachedBottom();
            }
        }
        else if (scroll_top_bottom_state_ == SCROLL_STATE_TOP) {
            scroll_top_bottom_state_ = SCROLL_STATE_NONE;
            target_.onScrollReleasedTop();
        }
        else if (scroll_top_bottom_state_ == SCROLL_STATE_BOTTOM) {
            scroll_top_bottom_state_ = SCROLL_STATE_NONE;
            target_.onScrollReleasedBottom();
        }
    }
    
    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {}
    
    private void startSmoothScrolling(final int scrolling_y, long time_span) {
        if (scroll_timer_ != null) return;
        
        if (!target_.hasScrollingListData()) return;
        
        final int pos = getFirstVisiblePosition();
        current_y_ = getChildTopPos();
        remain_count_ = (int) (time_span / SCROLL_INTERVAL_MS);
        final int delta_y = scrolling_y / remain_count_;
        final int target_y = scrolling_y + current_y_;
        
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                current_y_ += delta_y;
                remain_count_--;
                if (remain_count_ <= 0) {
                    current_y_ = target_y;
                    stopSmoothScrolling();
                }
                setSelectionFromTop(pos, current_y_);
            }
        };
        scroll_timer_ = new Timer();
        scroll_timer_.schedule(new TimerTask() {
            @Override
            public void run() {
                postForView(runnable);
            }
        }, 0, SCROLL_INTERVAL_MS);
    }
    
    public void stopSmoothScrolling() {
        if (scroll_timer_ == null) return;
        scroll_timer_.cancel();
        scroll_timer_ = null;
    }
    
    protected int getListPageUpDownTimeSpanMS() {
        return 300;
    }
    
    public void setListRollUp(final Runnable callback) {
        int pos = getFirstVisiblePosition();
        if (pos > 0) {
            setListPosition(pos - 1, callback);
        }
        else {
            setListPosition(0, callback);
        }
    }
    
    public void setListPageUp() {
        if (!target_.hasScrollingListData()) return;
        
        int amount = target_.getScrollingAmount();
        if (amount == 0) {
            setListRollUp(null);
            return;
        }
        
        startSmoothScrolling(getListViewHeight() * amount / 110, getListPageUpDownTimeSpanMS());
    }
    
    public void setListRollDown(final Runnable callback) {
        setListPosition(getFirstVisiblePosition() + 1, callback);
    }
    
    public void setListPageDown() {
        if (!target_.hasScrollingListData()) return;
        
        int amount = target_.getScrollingAmount();
        if (amount == 0) {
            setListRollDown(null);
            return;
        }
        
        startSmoothScrolling(-getListViewHeight() * amount / 110, getListPageUpDownTimeSpanMS());
    }
}
