package info.narazaki.android.tuboroid.agent.task;

import info.narazaki.android.lib.agent.http.HttpTaskAgentInterface;
import info.narazaki.android.lib.agent.http.task.HttpTaskBase;
import info.narazaki.android.lib.list.ListUtils;
import info.narazaki.android.lib.text.CharsetInfo;
import info.narazaki.android.lib.text.HtmlUtils;
import info.narazaki.android.tuboroid.data.ThreadData;
import info.narazaki.android.tuboroid.data.ThreadEntryData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.CircularRedirectException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;

import android.util.Log;

public class HttpGetThreadEntryListTask2ch extends HttpTaskBase implements HttpGetThreadEntryListTask {
    static private final String TAG = "HttpGetThreadEntryListTask2ch";
    private static final int RECV_PROGRESS_INTERVAL = 1000;
    
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
    String session_key_;
    
    Callback callback_;
    
    @Override
    public void sendTo(HttpTaskAgentInterface httpAgent) {
        httpAgent.send(this);
    }
    
    public HttpGetThreadEntryListTask2ch(ThreadData thread_data, String session_key, Callback callback) {
        super(session_key == null ? thread_data.getDatFileURI() : thread_data.getSpecialDatFileURI(session_key));
        session_key_ = session_key;
        thread_data_ = thread_data;
        callback_ = callback;
    }
    
    @Override
    protected String getTextEncode() {
        return CharsetInfo.getEmojiShiftJis();
    }
    
    @Override
    protected void finish() {
        super.finish();
        thread_data_ = null;
        callback_ = null;
    }
    
    @Override
    protected boolean sendRequest(String request_uri) throws InterruptedException, ClientProtocolException, IOException {
        // Log.d(TAG, "sendRequest=" + request_uri + " cache=" +
        // thread_data_.working_cache_count_);
        if (Thread.interrupted()) throw new InterruptedException();
        callback_.onFetchStarted();
        try {
            HttpGet req = factoryGetRequest(request_uri);
            
            boolean full_content = true;
            if (thread_data_.working_cache_size_ > 1) {
                if (thread_data_.working_cache_timestamp_.length() > 0 && thread_data_.working_cache_etag_.length() > 0) {
                    req.addHeader("If-Modified-Since", thread_data_.working_cache_timestamp_);
                    req.addHeader("If-None-Match", thread_data_.working_cache_etag_);
                }
                
                req.addHeader("Range", "bytes= " + (thread_data_.working_cache_size_ - 1) + "-");
                full_content = false;
            }
            
            HttpResponse res = executeRequest(req);
            StatusLine statusLine = res.getStatusLine();
            
            // あぼーんチェックフラグ
            boolean check_partial_abone = false;
            
            int status_code = statusLine.getStatusCode();
            
            switch (status_code) {
            case HttpStatus.SC_OK:
                thread_data_.working_cache_count_ = 0;
                thread_data_.working_cache_size_ = 0;
                full_content = true;
                callback_.onReceivedNew();
                break;
            case HttpStatus.SC_PARTIAL_CONTENT:
                check_partial_abone = true;
                break;
            case HttpStatus.SC_NOT_MODIFIED:
                throw new UpToDateException();
            case HttpStatus.SC_REQUESTED_RANGE_NOT_SATISFIABLE:
                // あぼーん発見
                throw new AboneFoundException();
            case HttpStatus.SC_NON_AUTHORITATIVE_INFORMATION:
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
            // 部分取得の時は最初の1バイトであぼーんチェック
            // (1バイト手前から読んでいるので直前の終端=LF=0x0Aで始まっていること)
            if (check_partial_abone) {
                if (is.read() != 0x0A) {
                    throw new AboneFoundException();
                }
            }
            
            if (res.containsHeader("Last-Modified")) {
                thread_data_.working_cache_timestamp_ = res.getFirstHeader("Last-Modified").getValue();
            }
            String thread_new_etag = "";
            if (res.containsHeader("ETag")) {
                thread_new_etag = res.getFirstHeader("ETag").getValue();
                thread_data_.working_cache_etag_ = thread_new_etag.replace("-gzip", "");
            }
            
            dispatchHttpResponse(is, full_content, check_partial_abone);
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
    
    protected void dispatchHttpResponse(final InputStream is, final boolean full_content, boolean check_partial_abone)
            throws InterruptedException, IOException, BrokenDataException {
        List<ThreadEntryData> data_list = new LinkedList<ThreadEntryData>();
        DelegateInputStream dis = new DelegateInputStream(is);
        BufferedReader reader = new BufferedReader(new InputStreamReader(dis, getTextEncode()), buf_size_);
        
        long thread_cur_count = thread_data_.working_cache_count_;
        long progress = System.currentTimeMillis();
        // ●使用の時、1行目はスキップ
        boolean skip_head = session_key_ != null;
        try {
            while (true) {
                String line = reader.readLine();
                if (line == null) break;
                
                String[] tokens = ListUtils.split("<>", line);
                
                if (skip_head && thread_cur_count == 0) {
                    skip_head = false;
                    continue;
                }
                
                ThreadEntryData data;
                thread_cur_count++;
                
                if (tokens.length < 2) {
                    // <>が2個未満になるほど破損しているものは論外。多分datファイルですらない
                    Log.i(TAG, "BROKEN DATA :" + line);
                    throw new BrokenDataException();
                }
                else if (tokens.length < 4) {
                    // >>1が破損しているのは論外。多分datファイルですらない
                    if (thread_cur_count == 1) {
                        Log.i(TAG, "BROKEN >>1");
                        throw new BrokenDataException();
                    }
                    Log.w(TAG, "Invalid Token : " + line);
                    data = new ThreadEntryData(false, thread_cur_count, "", "", "", "", "", "", "", "");
                }
                else {
                    String author_name = tokens[0];
                    String author_mail = tokens[1];
                    String time_and_id = tokens[2];
                    String entry_body = tokens[3];
                    
                    if (thread_cur_count == 1 && tokens.length >= 5 && tokens[4].length() > 0) {
                        thread_data_.thread_name_ = HtmlUtils.stripAllHtmls(tokens[4], false);
                    }
                    
                    String author_id = "";
                    String author_be = "";
                    String entry_time;
                    int index_author_id = time_and_id.indexOf(" ID:");
                    if (index_author_id <= 1) {
                        entry_time = time_and_id;
                    }
                    else {
                        entry_time = time_and_id.substring(0, index_author_id);
                        String author_id_str = time_and_id.substring(index_author_id + 4);
                        int index_author_be = author_id_str.indexOf(' ');
                        if (index_author_be <= 1) {
                            author_id = author_id_str;
                        }
                        else {
                            author_id = author_id_str.substring(0, index_author_be);
                            author_be = author_id_str.substring(index_author_be + 1);
                        }
                    }
                    
                    data = new ThreadEntryData(false, thread_cur_count, author_name, author_mail, entry_body,
                            author_id, author_be, entry_time, "", "");
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
        thread_data_.working_cache_size_ += dis.getReadSize();
        
        callback_.onCompleted();
    }
    
    static private class DelegateInputStream extends InputStream {
        private final InputStream input_stream_;
        private int read_size_;
        
        public DelegateInputStream(InputStream input_stream) {
            super();
            input_stream_ = input_stream;
            read_size_ = 0;
        }
        
        public int getReadSize() {
            return read_size_;
        }
        
        @Override
        public int read() throws IOException {
            int c = input_stream_.read();
            if (c != -1) read_size_++;
            return c;
        }
        
        @Override
        public int read(byte[] b, int offset, int length) throws IOException {
            int size = input_stream_.read(b, offset, length);
            if (size > 0) read_size_ += size;
            return size;
        }
        
        @Override
        public int read(byte[] b) throws IOException {
            int size = input_stream_.read(b);
            if (size > 0) read_size_ += size;
            return size;
        }
        
        @Override
        public synchronized void reset() throws IOException {
            input_stream_.reset();
        }
        
        @Override
        public long skip(long n) throws IOException {
            return input_stream_.skip(n);
        }
        
        @Override
        public int available() throws IOException {
            return input_stream_.available();
        }
        
        @Override
        public void close() throws IOException {
            input_stream_.close();
            super.close();
        }
        
        @Override
        public void mark(int readlimit) {
            input_stream_.mark(readlimit);
        }
        
        @Override
        public boolean markSupported() {
            return input_stream_.markSupported();
        }
        
    }
}
