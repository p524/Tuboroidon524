package info.narazaki.android.lib.agent.http.task;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpRequestBase;

import android.util.Log;

public class HttpGetFileTask extends HttpTaskBase {
    private static final String TAG = "HttpGetFileTask";
    
    static public interface Callback {
        void onCompleted();
        
        void onFailed();
    }
    
    private Callback callback_;
    private File save_file_;
    private File temp_file_;
    
    public HttpGetFileTask(String request_uri, File save_file, Callback callback) {
        super(request_uri);
        save_file_ = save_file;
        callback_ = callback;
        
        try {
            temp_file_ = File.createTempFile(save_file_.getName(), ".tmp", save_file_.getParentFile());
        }
        catch (IOException e) {
            e.printStackTrace();
            temp_file_ = save_file_;
        }
    }
    
    @Override
    protected void onConnectionError(boolean connectionFailed) {
        try {
            temp_file_.delete();
        }
        catch (SecurityException e) {
        }
        if (callback_ != null) callback_.onFailed();
        callback_ = null;
    }
    
    @Override
    protected void onInterrupted() {
        onConnectionError(false);
    }
    
    @Override
    protected boolean sendRequest(String request_uri) throws InterruptedException, ClientProtocolException, IOException {
        if (Thread.interrupted()) throw new InterruptedException();
        
        FileOutputStream fos = new FileOutputStream(temp_file_);
        
        HttpRequestBase req = factoryGetRequest(request_uri);
        HttpResponse res = executeRequest(req);
        
        StatusLine statusLine = res.getStatusLine();
        switch (statusLine.getStatusCode()) {
        case HttpStatus.SC_OK:
        case HttpStatus.SC_CREATED:
        case HttpStatus.SC_NON_AUTHORITATIVE_INFORMATION:
            break;
        default:
            Log.i(TAG, "Request Failed Code:" + statusLine.getStatusCode());
            throw new IOException();
        }
        
        if (Thread.interrupted()) throw new InterruptedException();
        
        int bufsize = 1024 * 16;
        InputStream is = new BufferedInputStream(res.getEntity().getContent(), bufsize);
        
        byte[] buf = new byte[bufsize];
        int size = 0;
        while (true) {
            size = is.read(buf, 0, buf.length);
            if (size < 0) break;
            fos.write(buf, 0, size);
        }
        
        fos.flush();
        fos.close();
        is.close();
        
        try {
            temp_file_.renameTo(save_file_);
        }
        catch (SecurityException e) {
            throw new IOException();
        }
        
        callback_.onCompleted();
        callback_ = null;
        return true;
    }
    
}
