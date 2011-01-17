package info.narazaki.android.tuboroid.agent;

import info.narazaki.android.tuboroid.TuboroidApplication.AccountPref;
import info.narazaki.android.tuboroid.data.BoardData;
import info.narazaki.android.tuboroid.data.NewThreadData;
import java.util.concurrent.Future;

abstract public class CreateNewThreadTask {
    private static final String TAG = "NewThreadTask";
    
    protected final TuboroidAgentManager agent_manager_;
    protected Future<?> http_pending_;
    
    protected CreateNewThreadTask(TuboroidAgentManager agent_manager) {
        super();
        agent_manager_ = agent_manager;
        http_pending_ = null;
    }
    
    static public interface OnCreateNewThreadCallback {
        void onCreated();
        
        void onCreateFailed(final String message);
        
        void onRetryNotice(final NewThreadData retry_new_thread_data, final String message);
        
        void onConnectionError(final boolean connection_failed);
    }
    
    public class FutureCreateNewThread {
        public void abort() {
            if (http_pending_ != null) {
                http_pending_.cancel(true);
            }
        }
    }
    
    abstract public FutureCreateNewThread createNewThread(final BoardData board_data,
            final NewThreadData new_thread_data, final AccountPref account_pref, final String user_agent,
            final OnCreateNewThreadCallback callback);
}
