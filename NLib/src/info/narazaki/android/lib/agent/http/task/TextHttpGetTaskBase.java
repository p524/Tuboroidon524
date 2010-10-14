package info.narazaki.android.lib.agent.http.task;

import org.apache.http.client.methods.HttpRequestBase;

public abstract class TextHttpGetTaskBase extends TextHttpTaskBase {
    
    public TextHttpGetTaskBase(String request_uri) {
        super(request_uri);
    }
    
    @Override
    protected HttpRequestBase createHttpRequest(String request_uri) {
        return factoryGetRequest(request_uri);
    }
}
