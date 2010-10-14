package info.narazaki.android.tuboroid.agent;

import info.narazaki.android.lib.agent.http.HttpTaskAgentInterface;
import info.narazaki.android.lib.aplication.NSimpleApplication;
import info.narazaki.android.tuboroid.agent.task.HttpFind2chTask;
import info.narazaki.android.tuboroid.data.Find2chResultData;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.Context;

public class Find2chTask {
    private static final String TAG = "Find2chAgent";
    
    private Context context_;
    private final ExecutorService executor_;
    public static final int MAX_PAGE = 10;
    
    private HttpTaskAgentInterface multi_http_agent_;
    private boolean is_alive_;
    
    public Find2chTask(Context context, HttpTaskAgentInterface http_agent) {
        super();
        context_ = context;
        executor_ = Executors.newSingleThreadExecutor();
        multi_http_agent_ = http_agent;
        is_alive_ = true;
    }
    
    public static interface Find2chFetchedCallback {
        
        public void onFirstReceived(final int found_items);
        
        public void onReceived(final ArrayList<Find2chResultData> data_list);
        
        public void onCompleted();
        
        public void onFailed();
        
        public void onInterrupted();
        
        public void onOffline();
    }
    
    public void abort() {
        executor_.submit(new Runnable() {
            @Override
            public void run() {
                is_alive_ = false;
            }
        });
    }
    
    public void searchViaFind2ch(final String key, final int order, final Find2chFetchedCallback callback) {
        // Offlineチェック
        if (!NSimpleApplication.isOnline(context_)) {
            callback.onOffline();
            return;
        }
        
        searchViaFind2chImpl(key, 0, order, callback);
    }
    
    public void searchViaFind2chImpl(final String key, final int page, final int order,
            final Find2chFetchedCallback callback) {
        HttpFind2chTask task = new HttpFind2chTask(context_, key, page, order, new HttpFind2chTask.Callback() {
            @Override
            public void onReceived(final ArrayList<Find2chResultData> dataList, final int found_items) {
                executor_.submit(new Runnable() {
                    @Override
                    public void run() {
                        if (is_alive_) {
                            if (page == 0) callback.onFirstReceived(found_items);
                            callback.onReceived(dataList);
                            if (found_items < HttpFind2chTask.FETCH_COUNT * (page + 1) || page >= MAX_PAGE) {
                                callback.onCompleted();
                            }
                            else {
                                searchViaFind2chImpl(key, page + 1, order, callback);
                            }
                        }
                        else {
                            onInterrupted();
                        }
                    }
                });
            }
            
            @Override
            public void onConnectionFailed() {
                executor_.submit(new Runnable() {
                    @Override
                    public void run() {
                        if (is_alive_) {
                            callback.onFailed();
                        }
                        else {
                            onInterrupted();
                        }
                    }
                });
            }
            
            @Override
            public void onInterrupted() {
                callback.onInterrupted();
            }
        });
        task.sendTo(multi_http_agent_);
    }
}
