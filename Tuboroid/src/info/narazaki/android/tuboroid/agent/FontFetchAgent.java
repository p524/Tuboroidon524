package info.narazaki.android.tuboroid.agent;

import info.narazaki.android.lib.agent.http.HttpTaskAgentInterface;
import info.narazaki.android.lib.agent.http.task.HttpGetFileTask;

import java.io.File;
import java.util.concurrent.Future;

public class FontFetchAgent {
    private static final String TAG = "FontFetchAgent";
    
    private HttpTaskAgentInterface http_agent_;
    private FontFetchedCallback callback_;
    
    public interface FontFetchedCallback {
        public void onFailed();
        
        public void onCompleted();
    }
    
    public FontFetchAgent(HttpTaskAgentInterface http_agent, FontFetchedCallback callback) {
        super();
        http_agent_ = http_agent;
        callback_ = callback;
    }
    
    public Future<?> fetchFile(final File local_file, final String uri) {
        if (local_file == null) return null;
        
        HttpGetFileTask task = new HttpGetFileTask(uri, local_file, new HttpGetFileTask.Callback() {
            @Override
            public void onFailed() {
                callback_.onFailed();
            }
            
            @Override
            public void onCompleted() {
                callback_.onCompleted();
            }
            
            @Override
            public void onStart() {
            }

            @Override
            public void onProgress(int current_length, int content_length) {
                // TODO 自動生成されたメソッド・スタブ
                
            }
        });
        
        return http_agent_.send(task);
    }
    
}
