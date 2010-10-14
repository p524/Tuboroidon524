package info.narazaki.android.lib.agent.http.task;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpRequestBase;
import android.util.Log;

public abstract class TextHttpTaskBase extends HttpTaskBase {
    static private final String TAG = "TextHttpTaskBase";
    
    public TextHttpTaskBase(String request_uri) {
        super(request_uri);
    }
    
    abstract protected HttpRequestBase createHttpRequest(String request_uri);
    
    protected boolean setRequestParameters(HttpRequestBase req) {
        return true;
    }
    
    abstract protected void onRequestCanceled();
    
    protected void dispatchHttpResponse(HttpResponse res, InputStream res_is) throws InterruptedException, IOException {
        BufferedReader reader;
        reader = new BufferedReader(new InputStreamReader(res_is, getTextEncode()), buf_size_);
        dispatchHttpTextResponse(res, reader);
    }
    
    abstract protected void dispatchHttpTextResponse(HttpResponse res, final BufferedReader reader)
            throws InterruptedException, IOException;
    
    @Override
    protected void onInterrupted() {
        onRequestCanceled();
    }
    
    @Override
    protected final boolean sendRequest(String request_uri) throws InterruptedException, ClientProtocolException,
            IOException {
        if (Thread.interrupted()) throw new InterruptedException();
        HttpRequestBase req = createHttpRequest(request_uri);
        if (req == null) throw new ClientProtocolException();
        if (!setRequestParameters(req)) throw new ClientProtocolException();
        
        HttpResponse res = executeRequest(req);
        
        StatusLine statusLine = res.getStatusLine();
        if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
            Log.i(TAG, "Request Failed Code:" + statusLine.getStatusCode());
            throw new IOException();
        }
        
        if (Thread.interrupted()) throw new InterruptedException();
        
        dispatchHttpResponse(res, res.getEntity().getContent());
        return true;
    }
}
