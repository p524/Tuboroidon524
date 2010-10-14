package info.narazaki.android.tuboroid.agent.task;

import info.narazaki.android.lib.agent.http.HttpTaskAgentInterface;
import info.narazaki.android.tuboroid.data.ThreadEntryData;

import java.util.List;

public interface HttpGetThreadEntryListTask {
    static public interface Callback {
        
        void onReceivedNew();
        
        void onFetchStarted();
        
        void onReceived(final List<ThreadEntryData> data_list);
        
        void onCompleted();
        
        void onAboneFound();
        
        void onDatDropped();
        
        void onDatBroken();
        
        void onNoUpdated();
        
        void onConnectionFailed(final boolean connectionFailed);
        
        void onInterrupted();
    }
    
    public void sendTo(HttpTaskAgentInterface http_agent);
    
}
