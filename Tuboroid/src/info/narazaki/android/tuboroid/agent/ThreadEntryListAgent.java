package info.narazaki.android.tuboroid.agent;

import info.narazaki.android.lib.agent.db.SQLiteAgentBase;
import info.narazaki.android.lib.list.SimpleCacheDataList;
import info.narazaki.android.tuboroid.TuboroidApplication;
import info.narazaki.android.tuboroid.TuboroidApplication.AccountPref;
import info.narazaki.android.tuboroid.agent.ThreadEntryListTask.ThreadEntryListFetchedCallback;
import info.narazaki.android.tuboroid.agent.thread.DataFileAgent;
import info.narazaki.android.tuboroid.data.ThreadData;
import info.narazaki.android.tuboroid.data.ThreadEntryData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadEntryListAgent {
    private static final String TAG = "ThreadEntryListAgent";
    
    private TuboroidAgentManager agent_manager_;
    
    private final ExecutorService executor_;
    
    private SimpleCacheDataList<ThreadData, List<ThreadEntryData>> cache_data_;
    
    public ThreadEntryListAgent(TuboroidAgentManager agent_manager) {
        super();
        agent_manager_ = agent_manager;
        executor_ = Executors.newSingleThreadExecutor();
        cache_data_ = new SimpleCacheDataList<ThreadData, List<ThreadEntryData>>(0, 1);
    }
    
    private synchronized void pushTask(final Runnable runnable) {
        executor_.submit(runnable);
    }
    
    private synchronized void popTask() {}
    
    public static interface ThreadEntryListAgentCallback {
        public void onThreadEntryListFetchedByCache(final List<ThreadEntryData> data_list);
        
        public void onThreadEntryListClear();
        
        public void onThreadEntryListFetchStarted(final ThreadData thread_data);
        
        public void onThreadEntryListFetched(final List<ThreadEntryData> data_list);
        
        public void onThreadEntryListFetchedCompleted(final ThreadData thread_data, final boolean is_analyzed);
        
        public void onInterrupted();
        
        public void onDatDropped(final boolean is_permanently);
        
        public void onConnectionFailed(final boolean connectionFailed);
        
        public void onConnectionOffline(final ThreadData thread_data);
    }
    
    private class DelegateThreadEntryListFetchedCallback implements ThreadEntryListFetchedCallback {
        
        private final ThreadEntryListAgentCallback callback_;
        private boolean is_active_;
        
        public DelegateThreadEntryListFetchedCallback(ThreadEntryListAgentCallback callback) {
            super();
            callback_ = callback;
            is_active_ = true;
        }
        
        @Override
        public void onThreadEntryListFetchedCompleted(final ThreadData threadData) {
            threadData.is_dropped_ = false;
            agent_manager_.getDBAgent().updateThreadCacheTagData(threadData, null);
            onThreadEntryListFetchedCompleted(threadData, false);
        }
        
        public void onThreadEntryListFetchedCompleted(ThreadData threadData, final boolean is_analyzed) {
            if (callback_ != null) callback_.onThreadEntryListFetchedCompleted(threadData, is_analyzed);
            if (is_active_) {
                is_active_ = false;
                popTask();
            }
        }
        
        @Override
        public void onThreadEntryListFetchedByCache(List<ThreadEntryData> dataList) {
            if (callback_ != null) callback_.onThreadEntryListFetchedByCache(dataList);
        }
        
        @Override
        public void onThreadEntryListFetched(List<ThreadEntryData> dataList) {
            if (callback_ != null) callback_.onThreadEntryListFetched(dataList);
        }
        
        @Override
        public void onThreadEntryListFetchStarted(ThreadData threadData) {
            if (callback_ != null) callback_.onThreadEntryListFetchStarted(threadData);
        }
        
        @Override
        public void onThreadEntryListClear() {
            if (callback_ != null) callback_.onThreadEntryListClear();
        }
        
        @Override
        public void onInterrupted() {
            if (callback_ != null) callback_.onInterrupted();
            if (is_active_) {
                is_active_ = false;
                popTask();
            }
        }
        
        @Override
        public void onDatDropped(boolean isPermanently) {
            if (callback_ != null) callback_.onDatDropped(isPermanently);
            if (is_active_) {
                is_active_ = false;
                popTask();
            }
        }
        
        @Override
        public void onConnectionFailed(boolean connectionFailed) {
            if (callback_ != null) callback_.onConnectionFailed(connectionFailed);
            if (is_active_) {
                is_active_ = false;
                popTask();
            }
        }
        
        @Override
        public void onConnectionOffline(final ThreadData thread_data) {
            if (callback_ != null) callback_.onConnectionOffline(thread_data);
            if (is_active_) {
                is_active_ = false;
                popTask();
            }
        }
    };
    
    public void storeThreadEntryListAnalyzedCache(final ThreadData thread_data, List<ThreadEntryData> new_cache_list) {
        cache_data_.setData(thread_data, new_cache_list);
    }
    
    public void reloadThreadEntryList(final ThreadData thread_data, final boolean reload,
            final ThreadEntryListAgentCallback callback) {
        final boolean use_external_storage = TuboroidApplication
                .getExternalStoragePathName(agent_manager_.getContext()) != null ? true : false;
        final ThreadEntryListTask task = thread_data.factoryThreadEntryListTask(agent_manager_);
        
        pushTask(new Runnable() {
            @Override
            public void run() {
                List<ThreadEntryData> cached_list = cache_data_.getData(thread_data, true);
                DelegateThreadEntryListFetchedCallback delegate_callback = new DelegateThreadEntryListFetchedCallback(
                        callback);
                if (reload || cached_list == null) {
                    cache_data_.removeData(thread_data);
                    task.reloadThreadEntryList(thread_data, use_external_storage, reload, delegate_callback);
                    return;
                }
                delegate_callback.onThreadEntryListFetchStarted(thread_data);
                delegate_callback.onThreadEntryListFetchedByCache(cached_list);
                delegate_callback.onThreadEntryListFetchedCompleted(thread_data);
            }
        });
    }
    
    public void reloadSpecialThreadEntryList(final ThreadData thread_data, final AccountPref account_pref,
            final ThreadEntryListAgentCallback callback) {
        final ThreadEntryListTask task = thread_data.factoryThreadEntryListTask(agent_manager_);
        
        pushTask(new Runnable() {
            @Override
            public void run() {
                task.reloadSpecialThreadEntryList(thread_data, account_pref, agent_manager_.getHttpUserAgentName(),
                        new DelegateThreadEntryListFetchedCallback(callback));
            }
        });
    }
    
    public void deleteRecentList(final ArrayList<ThreadData> delete_list, final Runnable callback) {
        pushTask(new Runnable() {
            @Override
            public void run() {
                execDeleteRecentList(delete_list, callback);
            }
        });
    }
    
    private void execDeleteRecentList(final ArrayList<ThreadData> delete_list, final Runnable callback) {
        if (delete_list.size() == 0) {
            if (callback != null) callback.run();
            popTask();
            return;
        }
        ThreadData data = delete_list.get(0);
        delete_list.remove(0);
        execDeleteThreadEntryListCache(data, new Runnable() {
            @Override
            public void run() {
                execDeleteRecentList(delete_list, callback);
            }
        });
    }
    
    public void deleteThreadEntryListCache(final ThreadData thread_data, final Runnable callback) {
        pushTask(new Runnable() {
            @Override
            public void run() {
                execDeleteThreadEntryListCache(thread_data, new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) callback.run();
                        popTask();
                    }
                });
            }
        });
    }
    
    private void execDeleteThreadEntryListCache(final ThreadData thread_data, final Runnable callback) {
        String[] files = new String[] { thread_data.getLocalDatFile(agent_manager_.getContext()).getAbsolutePath(),
                thread_data.getLocalAttachFileDir(agent_manager_.getContext()).getAbsolutePath() };
        agent_manager_.getFileAgent().deleteFiles(files, new DataFileAgent.FileWroteCallback() {
            @Override
            public void onFileWrote(boolean succeeded) {
                agent_manager_.getDBAgent().deleteThreadData(thread_data,
                        new SQLiteAgentBase.DbTransactionDelegate(callback));
            }
        });
    }
    
}