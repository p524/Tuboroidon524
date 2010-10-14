package info.narazaki.android.lib.activity.base;

import java.util.ArrayList;

import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.ExpandableListView;
import info.narazaki.android.lib.adapter.SimpleExpandableListAdapterBase;
import info.narazaki.android.lib.aplication.NSimpleApplication;

abstract public class NSimpleExpandableListActivity extends NExpandableListActivity {
    private static final String TAG = "NSimpleExpandableListActivity";
    
    private static final String INTENT_KEY_RESUME_ITEM_PACKED_POS = "__TuboroidListActivity_resume_item_packed_pos";
    private static final String INTENT_KEY_RESUME_ITEM_TOP = "__TuboroidListActivity_resume_item_top";
    private static final String INTENT_KEY_RESUME_ITEM_EXPAND_STAT_LIST = "__TuboroidListActivity_resume_item_expand_stat_list";
    
    public static class StatData {
        public PositionData pos_data_;
        public ArrayList<Boolean> expand_stat_list_;
        
        public StatData() {
            super();
            pos_data_ = new PositionData(0, 0);
            expand_stat_list_ = new ArrayList<Boolean>();
        }
        
        public StatData(StatData stat) {
            super();
            pos_data_ = new PositionData(stat.pos_data_.packed_pos_, stat.pos_data_.y_);
            expand_stat_list_ = new ArrayList<Boolean>(stat.expand_stat_list_);
        }
        
        public StatData(PositionData pos_data, ArrayList<Boolean> expand_stat_list) {
            super();
            pos_data_ = pos_data;
            expand_stat_list_ = expand_stat_list;
        }
    }
    
    protected boolean is_active_;
    protected SimpleExpandableListAdapterBase<?> list_adapter_ = null;
    
    private boolean reload_in_progress_ = false;
    
    private StatData resume_data_ = null;
    
    private boolean has_initial_data_ = false;
    
    abstract protected SimpleExpandableListAdapterBase<?> createListAdapter();
    
    abstract protected void onFirstDataRequired();
    
    abstract protected void onResumeDataRequired();
    
    protected final NSimpleApplication getNSimpleApplication() {
        return (NSimpleApplication) getApplication();
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        is_active_ = true;
        
        has_initial_data_ = false;
        resume_data_ = null;
        reload_in_progress_ = false;
        
        String class_name = this.getClass().getName();
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(class_name + INTENT_KEY_RESUME_ITEM_PACKED_POS)) {
                resume_data_ = new StatData();
                resume_data_.pos_data_.packed_pos_ = savedInstanceState.getLong(class_name
                        + INTENT_KEY_RESUME_ITEM_PACKED_POS);
                resume_data_.pos_data_.y_ = savedInstanceState.getInt(class_name + INTENT_KEY_RESUME_ITEM_TOP);
                boolean[] list = savedInstanceState.getBooleanArray(class_name
                        + INTENT_KEY_RESUME_ITEM_EXPAND_STAT_LIST);
                for (boolean data : list) {
                    resume_data_.expand_stat_list_.add(data);
                }
            }
        }
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        String class_name = this.getClass().getName();
        if (has_initial_data_) {
            saveResumePosition();
            if (resume_data_ != null) {
                onSaveResumePositon(resume_data_);
                outState.putLong(class_name + INTENT_KEY_RESUME_ITEM_PACKED_POS, resume_data_.pos_data_.packed_pos_);
                outState.putInt(class_name + INTENT_KEY_RESUME_ITEM_TOP, resume_data_.pos_data_.y_);
                boolean[] list = new boolean[resume_data_.expand_stat_list_.size()];
                for (int i = 0; i < resume_data_.expand_stat_list_.size(); i++) {
                    list[i] = resume_data_.expand_stat_list_.get(i);
                }
                outState.putBooleanArray(class_name + INTENT_KEY_RESUME_ITEM_EXPAND_STAT_LIST, list);
            }
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
        is_active_ = false;
        saveResumePosition();
        list_adapter_.clear();
    }
    
    protected boolean hasResumeItemPos() {
        return resume_data_ != null;
    }
    
    private void saveResumePosition() {
        if (list_adapter_ == null) return;
        if (resume_data_ == null && list_adapter_.getGroupCount() > 0) {
            ExpandableListView view = getExpandableListView();
            
            resume_data_ = new StatData();
            PositionData pos_data = getCurrentPosition();
            resume_data_.pos_data_ = pos_data;
            resume_data_.expand_stat_list_.clear();
            int group_count = list_adapter_.getGroupCount();
            for (int i = 0; i < group_count; i++) {
                resume_data_.expand_stat_list_.add(view.isGroupExpanded(i));
            }
        }
    }
    
    public void setResumePosition(StatData stat) {
        resume_data_ = new StatData(stat);
    }
    
    protected void onSaveResumePositon(StatData stat) {}
    
    protected final boolean onBeginReload() {
        if (reload_in_progress_) return false;
        reload_in_progress_ = true;
        return true;
    }
    
    protected final boolean isReloadInProgress() {
        return reload_in_progress_;
    }
    
    protected void onEndReload() {
        resumeItemPos(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onEndReloadJumped();
                    }
                });
            }
        });
        resume_data_ = null;
        reload_in_progress_ = false;
    }
    
    protected void onEndReloadJumped() {
        has_initial_data_ = true;
    }
    
    protected void resumeItemPos(Runnable callback) {
        if (resume_data_ != null) {
            ExpandableListView view = getExpandableListView();
            if (list_adapter_.getGroupCount() > 0) {
                int group_count = list_adapter_.getGroupCount();
                if (group_count > resume_data_.expand_stat_list_.size()) {
                    group_count = resume_data_.expand_stat_list_.size();
                }
                for (int i = 0; i < group_count; i++) {
                    if (resume_data_.expand_stat_list_.get(i)) {
                        view.expandGroup(i);
                    }
                    else {
                        view.collapseGroup(i);
                    }
                }
                resume_data_.expand_stat_list_.clear();
            }
            setListPositionFromTop(resume_data_.pos_data_.packed_pos_, resume_data_.pos_data_.y_, callback);
            return;
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
}
