package info.narazaki.android.tuboroid.agent.task;

import info.narazaki.android.lib.agent.http.HttpTaskAgentInterface;
import info.narazaki.android.lib.agent.http.task.HttpTaskBase;
import info.narazaki.android.lib.list.ListUtils;
import info.narazaki.android.lib.text.HtmlUtils;
import info.narazaki.android.tuboroid.data.ThreadData;
import info.narazaki.android.tuboroid.data.ThreadEntryData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.CircularRedirectException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;

import android.util.Log;

public class HttpGetThreadEntryListTaskMachi extends HttpTaskBase implements HttpGetThreadEntryListTask {
    static private final String TAG = "HttpGetThreadEntryListTaskMachi";
    private static final int RECV_PROGRESS_INTERVAL = 1000;
    private static final int MAX_ABONE_DIFF = 1000;
    
    public class AboneFoundException extends Exception {
        private static final long serialVersionUID = 1L;
    }
    
    public class DatDroppedException extends Exception {
        private static final long serialVersionUID = 1L;
    }
    
    public class BrokenDataException extends Exception {
        private static final long serialVersionUID = 1L;
    }
    
    public class UpToDateException extends Exception {
        private static final long serialVersionUID = 1L;
    }
    
    ThreadData thread_data_;
    
    Callback callback_;
    
    @Override
    public void sendTo(HttpTaskAgentInterface httpAgent) {
        httpAgent.send(this);
    }
    
    public HttpGetThreadEntryListTaskMachi(ThreadData thread_data, String session_key, Callback callback) {
        super(thread_data.getDatFileURI());
        thread_data_ = thread_data;
        callback_ = callback;
    }
    
    @Override
    protected String getTextEncode() {
        return "MS932";
    }
    
    @Override
    protected void finish() {
        super.finish();
        thread_data_ = null;
        callback_ = null;
    }
    
    @Override
    protected boolean sendRequest(String request_uri) throws InterruptedException, ClientProtocolException, IOException {
        if (Thread.interrupted()) throw new InterruptedException();
        callback_.onFetchStarted();
        try {
            
            boolean full_content = true;
            if (thread_data_.working_cache_count_ > 0) {
                request_uri += (thread_data_.working_cache_count_ + 1) + "-";
                full_content = false;
            }
            else {
                thread_data_.working_cache_count_ = 0;
            }
            
            HttpGet req = factoryGetRequest(request_uri);
            
            HttpResponse res = executeRequest(req);
            StatusLine statusLine = res.getStatusLine();
            
            int status_code = statusLine.getStatusCode();
            
            switch (status_code) {
            case HttpStatus.SC_OK:
                break;
            case HttpStatus.SC_NOT_MODIFIED:
                throw new UpToDateException();
            case HttpStatus.SC_NOT_FOUND:
            case HttpStatus.SC_MOVED_TEMPORARILY:
            case HttpStatus.SC_FORBIDDEN:
                throw new DatDroppedException();
            default:
                Log.i(TAG, "Request Failed Code:" + statusLine.getStatusCode());
                throw new IOException();
            }
            
            if (Thread.interrupted()) throw new InterruptedException();
            
            InputStream is = res.getEntity().getContent();
            
            if (res.containsHeader("Last-Modified")) {
                thread_data_.working_cache_timestamp_ = res.getFirstHeader("Last-Modified").getValue();
            }
            String thread_new_etag = "";
            if (res.containsHeader("ETag")) {
                thread_new_etag = res.getFirstHeader("ETag").getValue();
                thread_data_.working_cache_etag_ = thread_new_etag.replace("-gzip", "");
            }
            
            dispatchHttpResponse(is, full_content);
        }
        catch (AboneFoundException e) {
            Log.i(TAG, "abone found");
            callback_.onAboneFound();
        }
        catch (DatDroppedException e) {
            callback_.onDatDropped();
        }
        catch (UpToDateException e) {
            callback_.onNoUpdated();
        }
        catch (BrokenDataException e) {
            // dat file broken Error
            callback_.onDatBroken();
        }
        catch (ClientProtocolException e) {
            // Connection Error
            // redirect loopとか発生することがあるので……
            Throwable cause = e.getCause();
            if (cause instanceof CircularRedirectException) {
                callback_.onDatDropped();
            }
            else {
                throw e;
            }
        }
        return true;
    }
    
    @Override
    protected void onConnectionError(boolean connectionFailed) {
        callback_.onConnectionFailed(connectionFailed);
    }
    
    @Override
    protected void onInterrupted() {
        callback_.onInterrupted();
    }
    
    protected void dispatchHttpResponse(final InputStream is, final boolean full_content) throws InterruptedException,
            IOException, BrokenDataException, AboneFoundException {
        List<ThreadEntryData> data_list = new LinkedList<ThreadEntryData>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, getTextEncode()), buf_size_);
        
        long thread_cur_count = thread_data_.working_cache_count_;
        long progress = System.currentTimeMillis();
        try {
            while (true) {
                String line = reader.readLine();
                if (line == null) break;
                
                ArrayList<String> tokens = ListUtils.split("<>", line);
                
                ThreadEntryData data;
                thread_cur_count++;
                if (tokens.size() < 2) {
                    // <>が2個未満になるほど破損しているものは論外。多分datファイルですらない
                    Log.i(TAG, "BROKEN DATA :" + line);
                    throw new BrokenDataException();
                }
                
                int entry_id = 0;
                try {
                    entry_id = Integer.parseInt(tokens.get(0));
                }
                catch (NumberFormatException e) {
                    continue;
                }
                
                if (entry_id != thread_cur_count) {
                    // あぼーんによるずれ発見。補完する
                    if (entry_id < thread_cur_count || entry_id - thread_cur_count > MAX_ABONE_DIFF) {
                        // あまりに極端にあぼーんされている時は破損とみなす
                        throw new AboneFoundException();
                    }
                    while (entry_id > thread_cur_count) {
                        data_list.add(new ThreadEntryData(false, thread_cur_count, "", "", "", "", "", "", "", ""));
                        thread_cur_count++;
                    }
                }
                
                if (tokens.size() < 6) {
                    // >>1が破損しているのは論外。多分datファイルですらない
                    if (thread_cur_count == 1) {
                        Log.i(TAG, "BROKEN >>1");
                        throw new BrokenDataException();
                    }
                    Log.w(TAG, "Invalid Token : " + line);
                    data = new ThreadEntryData(false, entry_id, "", "", "", "", "", "", "", "");
                }
                else {
                    String author_name = tokens.get(1);
                    String author_mail = tokens.get(2);
                    String time_and_id = tokens.get(3);
                    String entry_body = tokens.get(4);
                    String thread_name = tokens.get(5);
                    
                    if (entry_id == 1 && thread_name.length() > 0) {
                        thread_data_.thread_name_ = HtmlUtils.stripAllHtmls(thread_name, false);
                    }
                    
                    String author_id = "";
                    String entry_time;
                    int index_author_id = time_and_id.indexOf(" ID:");
                    if (index_author_id <= 1) {
                        entry_time = time_and_id;
                    }
                    else {
                        entry_time = time_and_id.substring(0, index_author_id);
                        author_id = time_and_id.substring(index_author_id + 4);
                    }
                    
                    data = new ThreadEntryData(false, thread_cur_count, author_name, author_mail, entry_body,
                            author_id, "", entry_time, "", "");
                    
                }
                data_list.add(data);
                if (System.currentTimeMillis() - progress > RECV_PROGRESS_INTERVAL) {
                    progress = System.currentTimeMillis();
                    callback_.onReceived(data_list);
                    data_list = new LinkedList<ThreadEntryData>();
                }
            }
            if (data_list.size() > 0) {
                callback_.onReceived(data_list);
            }
        }
        finally {
            reader.close();
        }
        if (Thread.interrupted()) throw new InterruptedException();
        
        thread_data_.working_cache_count_ = (int) thread_cur_count;
        
        callback_.onCompleted();
    }
}
