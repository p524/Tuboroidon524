package info.narazaki.android.tuboroid.agent.task;

import info.narazaki.android.lib.agent.http.task.TextHttpPostTaskBase;
import info.narazaki.android.lib.text.HtmlUtils;
import info.narazaki.android.tuboroid.data.PostEntryData;
import info.narazaki.android.tuboroid.data.ThreadData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;

public class HttpPostEntryTaskShitaraba extends TextHttpPostTaskBase {
    private static final String TAG = "HttpPostEntryTaskShitaraba";
    
    static public interface Callback {
        void onPosted();
        
        void onPostFailed(final String message);
        
        void onConnectionError(final boolean connection_failed);
    }
    
    private ThreadData thread_data_;
    private PostEntryData post_entry_data_;
    private Callback callback_;
    
    static Pattern pattern_title_;
    static Pattern pattern_tag_2ch_x_;
    static Pattern pattern_body_;
    static {
        pattern_title_ = Pattern.compile("<title.*?\\>(.+?)</title\\>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
                | Pattern.DOTALL);
        pattern_tag_2ch_x_ = Pattern.compile("<!-- 2ch_X:(.+?) --\\>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
                | Pattern.DOTALL);
        pattern_body_ = Pattern.compile("<body.*?\\>(.+?)</body\\>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
                | Pattern.DOTALL);
    }
    
    @Override
    protected String getTextEncode() {
        return "EUC_JP";
    }
    
    public HttpPostEntryTaskShitaraba(ThreadData thread_data, PostEntryData post_entry_data, Callback callback) {
        super(thread_data.getPostEntryURI());
        thread_data_ = thread_data;
        post_entry_data_ = post_entry_data;
        callback_ = callback;
    }
    
    @Override
    protected boolean setRequestParameters(HttpRequestBase req) {
        try {
            long epoc_time = System.currentTimeMillis() / 1000;
            epoc_time -= 60 * 15;
            
            req.setHeader("Content-type", "application/x-www-form-urlencoded");
            StringBuilder buf = new StringBuilder();
            String[] tags = thread_data_.server_def_.board_tag_.split("/");
            if (tags.length < 2) return false;
            
            String board_dir = tags[0];
            String board_id = tags[1];
            buf.append("DIR=").append(urlencode(board_dir));
            buf.append("&BBS=").append(urlencode(board_id));
            buf.append("&KEY=").append(thread_data_.thread_id_);
            buf.append("&TIME=").append(epoc_time);
            buf.append("&NAME=").append(urlencode(post_entry_data_.author_name_));
            buf.append("&MAIL=").append(urlencode(post_entry_data_.author_mail_));
            buf.append("&MESSAGE=").append(urlencode(post_entry_data_.entry_body_));
            buf.append("&submit=").append("%BD%F1%A4%AD%B9%FE%A4%E0");
            
            ((HttpPost) req).setEntity(new StringEntity(buf.toString(), getTextEncode()));
            req.setHeader("Referer", thread_data_.getPostEntryRefererURI());
            
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return true;
    }
    
    @Override
    protected void dispatchHttpTextResponse(HttpResponse res, BufferedReader reader) throws InterruptedException,
            IOException {
        
        try {
            StringBuilder buf = new StringBuilder();
            while (true) {
                String line = reader.readLine();
                if (line == null) break;
                buf.append(line);
            }
            String data = buf.toString();
            
            String title = "";
            String tag_2ch_x = "";
            String body = "";
            
            Matcher matcher_title = pattern_title_.matcher(data);
            matcher_title.reset();
            if (matcher_title.find()) title = matcher_title.group(1);
            
            Matcher matcher_tag_2ch_x = pattern_tag_2ch_x_.matcher(data);
            matcher_tag_2ch_x.reset();
            if (matcher_tag_2ch_x.find()) tag_2ch_x = matcher_tag_2ch_x.group(1);
            
            Matcher matcher_body = pattern_body_.matcher(data);
            matcher_body.reset();
            if (matcher_body.find()) body = matcher_body.group(1);
            
            if (checkPosted(title, tag_2ch_x, body)) return;
            
            onPostFailed(title, tag_2ch_x, body);
            
        }
        catch (Exception e) {
            if (callback_ != null) {
                callback_.onConnectionError(false);
                callback_ = null;
            }
        }
        finally {
            reader.close();
        }
        if (Thread.interrupted()) throw new InterruptedException();
    }
    
    // 書き込み完了
    private boolean checkPosted(final String title, final String tag_2ch_x, final String body)
            throws InterruptedException {
        if (title.indexOf("書きこみました") == -1 && tag_2ch_x.indexOf("true") == -1) return false;
        
        if (callback_ != null) {
            callback_.onPosted();
        }
        callback_ = null;
        return true;
    }
    
    private boolean onPostFailed(final String title, final String tag_2ch_x, final String body)
            throws InterruptedException {
        final String message = HtmlUtils.stripAllHtmls(body, true);
        if (callback_ != null) {
            callback_.onPostFailed(message);
        }
        callback_ = null;
        return true;
    }
    
    @Override
    protected void onConnectionError(boolean connection_failed) {
        if (callback_ != null) {
            callback_.onConnectionError(connection_failed);
        }
        callback_ = null;
    }
    
    @Override
    protected void onRequestCanceled() {
        onConnectionError(false);
    }
    
}
