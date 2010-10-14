package info.narazaki.android.lib.list;

import java.lang.ref.SoftReference;
import java.util.LinkedHashMap;

public class SimpleCacheDataList<T_TAG, T_DATA> {
    private static final String TAG = "SimpleCacheDataList";
    
    private final int max_cache_size_;
    private final int min_cache_size_;
    
    private final LinkedHashMap<T_TAG, T_DATA> hard_cache_;
    private final LinkedHashMap<T_TAG, SoftReference<T_DATA>> soft_cache_;
    
    public SimpleCacheDataList(int min_cache_size, int max_cache_size) {
        min_cache_size_ = min_cache_size;
        max_cache_size_ = max_cache_size;
        
        if (min_cache_size_ > 0) {
            hard_cache_ = new LinkedHashMap<T_TAG, T_DATA>(min_cache_size_ / 2, 0.75f, true) {
                private static final long serialVersionUID = 1L;
                
                @Override
                protected boolean removeEldestEntry(LinkedHashMap.Entry<T_TAG, T_DATA> eldest) {
                    if (size() <= min_cache_size_) return false;
                    return true;
                }
            };
        }
        else {
            hard_cache_ = null;
        }
        soft_cache_ = new LinkedHashMap<T_TAG, SoftReference<T_DATA>>(max_cache_size_ / 2, 0.75f, true) {
            private static final long serialVersionUID = 1L;
            
            @Override
            protected boolean removeEldestEntry(LinkedHashMap.Entry<T_TAG, SoftReference<T_DATA>> eldest) {
                if (size() <= max_cache_size_) return false;
                return true;
            }
        };
    }
    
    public synchronized T_DATA getData(T_TAG tag, boolean update_order) {
        SoftReference<T_DATA> ref = soft_cache_.get(tag);
        if (ref == null) return null;
        T_DATA data = ref.get();
        if (data == null) return null;
        
        if (hard_cache_ != null) hard_cache_.put(tag, data);
        
        return data;
    }
    
    public synchronized void setData(T_TAG tag, T_DATA data) {
        if (hard_cache_ != null) hard_cache_.put(tag, data);
        soft_cache_.put(tag, new SoftReference<T_DATA>(data));
    }
    
    public synchronized void removeData(T_TAG tag) {
        if (hard_cache_ != null) hard_cache_.remove(tag);
        soft_cache_.remove(tag);
    }
    
}
