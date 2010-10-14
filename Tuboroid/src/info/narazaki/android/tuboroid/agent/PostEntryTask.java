package info.narazaki.android.tuboroid.agent;

import info.narazaki.android.tuboroid.TuboroidApplication.AccountPref;
import info.narazaki.android.tuboroid.data.PostEntryData;
import info.narazaki.android.tuboroid.data.ThreadData;

import java.util.concurrent.Future;

abstract public class PostEntryTask {
    private static final String TAG = "PostEntryTask";
    
    protected final TuboroidAgentManager agent_manager_;
    protected Future<?> http_pending_;
    
    protected PostEntryTask(TuboroidAgentManager agent_manager) {
        super();
        agent_manager_ = agent_manager;
        http_pending_ = null;
    }
    
    static public interface OnPostEntryCallback {
        void onPosted();
        
        void onPostFailed(final String message);
        
        void onConnectionError(final boolean connection_failed);
        
        void onPostRetryNotice(final PostEntryData retry_post_entry_data, final String message);
    }
    
    public class FuturePostEntry {
        public void abort() {
            if (http_pending_ != null) {
                http_pending_.cancel(true);
            }
        }
    }
    
    abstract public FuturePostEntry post(final ThreadData thread_data, final PostEntryData post_entry_data,
            final AccountPref account_pref, final String user_agent, final OnPostEntryCallback callback);
}
