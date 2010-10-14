package info.narazaki.android.lib.adapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import android.app.Activity;

public abstract class FilterableListAdapterBase<T_DATA extends NListAdapterDataInterface> extends
        SimpleListAdapterBase<T_DATA> {
    static private final String TAG = "FilterableListAdapterBase";
    
    static public interface Filter<T_DATA> {
        public boolean filter(T_DATA data);
    }
    
    protected Activity activity_;
    protected ArrayList<T_DATA> inner_data_list_;
    private ArrayList<Integer> mapped_pos_list_;
    private Filter<T_DATA> filter_;
    private Comparator<T_DATA> comparator_;
    
    public FilterableListAdapterBase(Activity activity) {
        super();
        activity_ = activity;
        inner_data_list_ = new ArrayList<T_DATA>();
        mapped_pos_list_ = null;
        filter_ = null;
        comparator_ = null;
    }
    
    public void setFilter(Filter<T_DATA> filter, final Runnable callback) {
        filter_ = filter;
        applyFilter(callback);
    }
    
    static public interface PrepareFilter<T_DATA> {
        public void prepare(final ArrayList<T_DATA> inner_data_list);
    }
    
    public void setFilter(final PrepareFilter<T_DATA> prepare, Filter<T_DATA> filter, final Runnable callback) {
        filter_ = filter;
        postAdapterThread(new Runnable() {
            @Override
            public void run() {
                prepare.prepare(inner_data_list_);
                applyFilter(callback);
            }
        });
    }
    
    public void setComparer(Comparator<T_DATA> comparator, final Runnable callback) {
        comparator_ = comparator;
        applyFilter(callback);
    }
    
    public int getMappedPosition(int global_pos) {
        final ArrayList<Integer> mapped_pos_list = mapped_pos_list_;
        if (mapped_pos_list == null) return global_pos;
        if (mapped_pos_list.size() <= global_pos || global_pos < 0) return -1;
        return mapped_pos_list.get(global_pos);
    }
    
    @Override
    public void addData(final T_DATA data) {
        postAdapterThread(new Runnable() {
            @Override
            public void run() {
                addDataOrderd(inner_data_list_, data);
                activity_.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (filter_ == null || filter_.filter(data)) {
                            addDataOrderd(data_list_, data);
                            if (filter_ != null) mapped_pos_list_.add(mapped_pos_list_.size());
                            
                        }
                        else {
                            if (filter_ != null) mapped_pos_list_.add(-1);
                        }
                    }
                });
            }
        });
    }
    
    private void addDataOrderd(final ArrayList<T_DATA> list, final T_DATA data) {
        if (comparator_ == null) {
            list.add(data);
        }
        else {
            int index = Collections.binarySearch(list, data, comparator_);
            if (index >= 0) {
                list.add(index + 1, data);
            }
            else {
                list.add(-index - 1, data);
            }
        }
    }
    
    public void addDataList(final List<T_DATA> data_list, final Runnable callback) {
        addAndApplyFilter(data_list, false, callback);
    }
    
    public void applyFilter(final Runnable callback) {
        addAndApplyFilter(null, false, callback);
    }
    
    private void addAndApplyFilter(final List<T_DATA> append_data_list, final boolean clear_data,
            final Runnable callback) {
        postAdapterThread(new Runnable() {
            @Override
            public void run() {
                if (append_data_list != null) {
                    if (clear_data) {
                        inner_data_list_.clear();
                    }
                    inner_data_list_.addAll(append_data_list);
                }
                
                final ArrayList<T_DATA> new_data_list = new ArrayList<T_DATA>();
                final ArrayList<Integer> new_mapped_pos_list = new ArrayList<Integer>();
                if (filter_ == null) {
                    new_data_list.addAll(inner_data_list_);
                }
                else {
                    int i = 0;
                    for (T_DATA data : inner_data_list_) {
                        if (filter_.filter(data)) {
                            new_data_list.add(data);
                            new_mapped_pos_list.add(i);
                            i++;
                        }
                        else {
                            new_mapped_pos_list.add(-1);
                        }
                    }
                }
                if (comparator_ != null) {
                    Collections.sort(new_data_list, comparator_);
                }
                
                activity_.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        data_list_ = new_data_list;
                        mapped_pos_list_ = (filter_ == null) ? null : new_mapped_pos_list;
                        notifyDataSetChanged();
                        if (callback != null) {
                            callback.run();
                        }
                    }
                });
            }
        });
    }
    
    @Override
    public void clearData() {
        postAdapterThread(new Runnable() {
            @Override
            public void run() {
                inner_data_list_.clear();
            }
        });
        super.clearData();
    }
    
    public static interface GetInnerDataListCallback<T_DATA2 extends NListAdapterDataInterface> {
        public void onFetched(ArrayList<T_DATA2> data_list);
    }
    
    public void getInnerDataList(final GetInnerDataListCallback<T_DATA> callback) {
        postAdapterThread(new Runnable() {
            @Override
            public void run() {
                final ArrayList<T_DATA> data_list = new ArrayList<T_DATA>(inner_data_list_);
                callback.onFetched(data_list);
            }
        });
    }
    
    public void setDataList(List<T_DATA> data_list, final Runnable callback) {
        addAndApplyFilter(data_list, true, callback);
    }
    
    @Override
    public void setDataList(ArrayList<T_DATA> data_list) {
        setDataList(data_list, null);
    }
    
}
