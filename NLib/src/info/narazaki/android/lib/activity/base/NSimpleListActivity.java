package info.narazaki.android.lib.activity.base;

import info.narazaki.android.lib.adapter.SimpleListAdapterBase;
import info.narazaki.android.lib.aplication.NSimpleApplication;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;

abstract public class NSimpleListActivity extends NListActivity {
    private static final String TAG = "NSimpleListActivity";
    
    private static final String INTENT_KEY_RESUME_ITEM_POS = "__TuboroidListActivity_resume_item_pos";
    private static final String INTENT_KEY_RESUME_ITEM_TOP = "__TuboroidListActivity_resume_item_top";
    
    protected boolean is_active_;
    protected SimpleListAdapterBase<?> list_adapter_ = null;
    
    private boolean reload_in_progress_ = false;
    
    private PositionData resume_data_ = null;
    
    private boolean has_initial_data_ = false;
    
    abstract protected SimpleListAdapterBase<?> createListAdapter();
    
    abstract protected void onFirstDataRequired();
    
    abstract protected void onResumeDataRequired();
    
    static protected class ReloadTerminator {
        public boolean is_terminated_ = false;
    }
    
    private ReloadTerminator current_reload_terminator_;
    
    protected final NSimpleApplication getNSimpleApplication() {
        return (NSimpleApplication) getApplication();
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        is_active_ = true;
        has_initial_data_ = false;
        reload_in_progress_ = false;
        
        resume_data_ = null;
        current_reload_terminator_ = new ReloadTerminator();
        
        String class_name = this.getClass().getName();
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(class_name + INTENT_KEY_RESUME_ITEM_POS)) {
                resume_data_ = new PositionData(0, 0);
                resume_data_.position_ = savedInstanceState.getInt(class_name + INTENT_KEY_RESUME_ITEM_POS);
                resume_data_.y_ = savedInstanceState.getInt(class_name + INTENT_KEY_RESUME_ITEM_TOP);
            }
        }
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        saveResumePosition();
        if (resume_data_ != null && has_initial_data_) {
            String class_name = this.getClass().getName();
            onSaveResumePositon(resume_data_);
            outState.putInt(class_name + INTENT_KEY_RESUME_ITEM_POS, resume_data_.position_);
            outState.putInt(class_name + INTENT_KEY_RESUME_ITEM_TOP, resume_data_.y_);
        }
        super.onSaveInstanceState(outState);
    }
    
    @Override
    protected void onDestroy() {
        list_adapter_.clear();
        super.onDestroy();
    }
    
    @Override
    protected void onStart() {
        is_active_ = true;
        super.onStart();
        if (list_adapter_ == null) {
            list_adapter_ = createListAdapter();
            setListAdapter(list_adapter_);
        }
    }
    
    @Override
    protected void onResume() {
        is_active_ = true;
        super.onResume();
    }
    
    @Override
    protected void onFirstResume() {
        super.onFirstResume();
        onFirstDataRequired();
    }
    
    protected boolean hasInitialData() {
        return has_initial_data_;
    }
    
    protected void hasInitialData(boolean has_initial_data) {
        has_initial_data_ = has_initial_data;
    }
    
    protected boolean hasValidData() {
        return has_initial_data_ && !isReloadInProgress();
    }
    
    @Override
    protected void onSecondResume() {
        super.onSecondResume();
        onResumeDataRequired();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        saveResumePosition();
        is_active_ = false;
        discardDataOnPause();
        has_initial_data_ = false;
        reload_in_progress_ = false;
        current_reload_terminator_.is_terminated_ = true;
    }
    
    protected void discardDataOnPause() {
        list_adapter_.clear();
    }
    
    protected boolean hasResumeItemPos() {
        return resume_data_ != null;
    }
    
    protected ReloadTerminator getNewReloadTerminator() {
        current_reload_terminator_.is_terminated_ = true;
        current_reload_terminator_ = new ReloadTerminator();
        return current_reload_terminator_;
    }
    
    protected void saveResumePosition() {
        if (list_adapter_ == null) return;
        if (resume_data_ == null && list_adapter_.getCount() > 0 && has_initial_data_) {
            PositionData pos_data = getCurrentItemPos();
            if (pos_data != null) resume_data_ = pos_data;
        }
    }
    
    public PositionData getCurrentItemPos() {
        if (list_adapter_ == null || list_adapter_.getCount() == 0) return null;
        View target = getListView().getChildAt(0);
        if (target == null) return null;
        return new PositionData(getListView().getFirstVisiblePosition(), target.getTop());
    }
    
    protected void setResumeItemPos(int resume_item_pos, int resume_item_top_y) {
        resume_data_ = new PositionData(resume_item_pos, resume_item_top_y);
    }
    
    protected PositionData getResumeItemPos() {
        return resume_data_;
    }
    
    protected void clearResumeItemPos() {
        resume_data_ = null;
    }
    
    protected void onSaveResumePositon(PositionData stat) {}
    
    protected final boolean onBeginReload() {
        if (reload_in_progress_) return false;
        reload_in_progress_ = true;
        return true;
    }
    
    protected final boolean isReloadInProgress() {
        return reload_in_progress_;
    }
    
    protected final void setReloadInProgress(boolean reload_in_progress) {
        reload_in_progress_ = reload_in_progress;
    }
    
    protected void onEndReload() {
        resumeItemPos(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hasInitialData(true);
                        setReloadInProgress(false);
                        clearResumeItemPos();
                        onEndReloadJumped();
                    }
                });
            }
        });
    }
    
    protected void onEndReloadJumped() {}
    
    protected void resumeItemPos(Runnable callback) {
        if (resume_data_ != null && list_adapter_ != null) {
            //setListPositionFromTop(resume_data_.position_, resume_data_.y_, callback);
        	getListView().setSelectionFromTop(resume_data_.position_, resume_data_.y_);
        }
        if (callback != null) callback.run();
    }
    
    protected boolean useVolumeButtonScrolling() {
        return getNSimpleApplication().useVolumeButtonScrolling();
    }
    
    protected boolean useCameraButtonScrolling() {
        return getNSimpleApplication().useCameraButtonScrolling();
    }
    
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (useVolumeButtonScrolling()) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (hasInitialData()) setListPageUp();
                }
                return true;
            }
            if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (hasInitialData()) setListPageDown();
                }
                return true;
            }
        }
        if (useCameraButtonScrolling()) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_CAMERA) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (hasInitialData()) setListPageDown();
                }
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }
    
    public void postListViewAndUiThread(final Runnable runnable) {
        final SimpleListAdapterBase<?> adapter = list_adapter_;
        if (adapter != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adapter.postAdapterThread(new Runnable() {
                        @Override
                        public void run() {
                            runOnUiThread(runnable);
                        }
                    });
                }
            });
        }
        else {
            runOnUiThread(runnable);
        }
    }
    
}
