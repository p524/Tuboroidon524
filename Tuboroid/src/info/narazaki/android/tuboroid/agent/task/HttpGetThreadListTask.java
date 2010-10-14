package info.narazaki.android.tuboroid.agent.task;

import info.narazaki.android.lib.agent.http.HttpTaskAgentInterface;
import info.narazaki.android.tuboroid.data.ThreadData;

import java.util.List;

public interface HttpGetThreadListTask {
    static public interface Callback {
        void onReceived(final List<ThreadData> data_list);
        
        void onCompleted();
        
        void onConnectionFailed();
        
        void onInterrupted();
    }
    
    public void sendTo(HttpTaskAgentInterface http_agent);
    
}
