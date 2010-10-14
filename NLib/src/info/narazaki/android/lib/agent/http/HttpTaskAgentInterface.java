package info.narazaki.android.lib.agent.http;

import info.narazaki.android.lib.agent.http.task.HttpTaskBase;

import java.util.concurrent.Future;

public interface HttpTaskAgentInterface {
    public Future<?> send(HttpTaskBase task);
    
    public void setCookieStoreData(String cookie_bare_data);
    
    public void clearCookieStore();
    
    public String getCookieStoreData();
    
    public void onHttpTaskFinished(HttpTaskBase task);
    
    public boolean isOnline();
}
