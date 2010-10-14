package info.narazaki.android.tuboroid.agent;

import info.narazaki.android.tuboroid.data.Find2chResultData;

import java.lang.ref.SoftReference;
import java.util.ArrayList;

public class Find2chAgent {
    private static final String TAG = "Find2chAgent";
    
    private TuboroidAgentManager agent_manager_;
    
    static class Find2chCacheData {
        public final String cached_key_;
        public final ArrayList<Find2chResultData> list_cache_;
        
        public Find2chCacheData(String cachedKey, ArrayList<Find2chResultData> listCache) {
            super();
            cached_key_ = cachedKey;
            list_cache_ = listCache;
        }
    }
    
    public SoftReference<Find2chCacheData> cache_ref_;
    
    public Find2chAgent(TuboroidAgentManager agent_manager) {
        super();
        agent_manager_ = agent_manager;
        
        cache_ref_ = null;
    }
    
    public Find2chTask searchViaFind2ch(final String key, final int order, final boolean force_reload,
            final Find2chTask.Find2chFetchedCallback callback) {
        if (!force_reload && cache_ref_ != null) {
            final Find2chCacheData cache = cache_ref_.get();
            if (cache != null && cache.cached_key_.equals(key)) {
                callback.onFirstReceived(cache.list_cache_.size());
                callback.onReceived(cache.list_cache_);
                callback.onCompleted();
                return null;
            }
        }
        
        final ArrayList<Find2chResultData> new_list_cache = new ArrayList<Find2chResultData>();
        Find2chTask find_2ch_task = new Find2chTask(agent_manager_.getContext(), agent_manager_.getMultiHttpAgent());
        find_2ch_task.searchViaFind2ch(key, order, new Find2chTask.Find2chFetchedCallback() {
            
            @Override
            public void onReceived(ArrayList<Find2chResultData> dataList) {
                new_list_cache.addAll(dataList);
                callback.onReceived(dataList);
            }
            
            @Override
            public void onInterrupted() {
                callback.onInterrupted();
            }
            
            @Override
            public void onFirstReceived(int foundItems) {
                callback.onFirstReceived(foundItems);
            }
            
            @Override
            public void onFailed() {
                callback.onFailed();
            }
            
            @Override
            public void onCompleted() {
                cache_ref_ = new SoftReference<Find2chCacheData>(new Find2chCacheData(key, new_list_cache));
                callback.onCompleted();
            }
            
            @Override
            public void onOffline() {
                callback.onOffline();
            }
        });
        return find_2ch_task;
    }
}
