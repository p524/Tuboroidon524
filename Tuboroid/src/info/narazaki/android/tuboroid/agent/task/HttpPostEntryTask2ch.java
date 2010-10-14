package info.narazaki.android.tuboroid.agent.task;

import info.narazaki.android.lib.agent.http.task.TextHttpPostTaskBase;
import info.narazaki.android.lib.text.HtmlUtils;
import info.narazaki.android.tuboroid.data.PostEntryData;
import info.narazaki.android.tuboroid.data.ThreadData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;

public class HttpPostEntryTask2ch extends TextHttpPostTaskBase {
    private static final String TAG = "HttpPostEntryTask2ch";
    
    static public interface Callback {
        void onPosted();
        
        void onPostFailed(final String message);
        
        void onConnectionError(final boolean connection_failed);
        
        void onPostRetryNotice(final PostEntryData retry_post_entry_data, final String message);
        
        void onPostRetry(final PostEntryData retry_post_entry_data);
    }
    
    private ThreadData thread_data_;
    private PostEntryData post_entry_data_;
    private HashMap<String, String> hidden_form_map_;
    private String referer_uri_;
    private Callback callback_;
    
    static Pattern pattern_title_;
    static Pattern pattern_tag_2ch_x_;
    static Pattern pattern_body_;
    static Pattern pattern_hidden_form_;
    static Pattern pattern_check_message_;
    static String[] form_names_list_;
    static {
        pattern_title_ = Pattern.compile("<title.*?\\>(.+?)</title\\>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
                | Pattern.DOTALL);
        pattern_tag_2ch_x_ = Pattern.compile("<!-- 2ch_X:(.+?) --\\>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
                | Pattern.DOTALL);
        pattern_body_ = Pattern.compile("<body.*?\\>(.+?)</body\\>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
                | Pattern.DOTALL);
        
        pattern_hidden_form_ = Pattern.compile(
                "<input .*?type=\"?hidden\"? .*?name=\"?(.+?)\"? .*?value=\"?([^\\>\"]*)\"?.*?\\>",
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
        
        pattern_check_message_ = Pattern.compile("投稿確認(.+)<form", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
                | Pattern.DOTALL);
    }
    
    @Override
    protected String getTextEncode() {
        return "MS932";
    }
    
    public HttpPostEntryTask2ch(ThreadData thread_data, String post_entry_uri, String referer_uri,
            PostEntryData post_entry_data, final HashMap<String, String> hidden_form_map, Callback callback) {
        super(post_entry_uri);
        thread_data_ = thread_data;
        referer_uri_ = referer_uri;
        post_entry_data_ = post_entry_data;
        hidden_form_map_ = hidden_form_map;
        callback_ = callback;
    }
    
    @Override
    protected boolean setRequestParameters(HttpRequestBase req) {
        try {
            long epoc_time = System.currentTimeMillis() / 1000;
            epoc_time -= 60 * 15;
            
            req.setHeader("Content-type", "application/x-www-form-urlencoded");
            StringBuilder buf = new StringBuilder();
            
            HashMap<String, String> form_map = new HashMap<String, String>();
            
            if (post_entry_data_.hidden_form_map_ != null) {
                form_map.putAll(post_entry_data_.hidden_form_map_);
            }
            if (hidden_form_map_ != null) {
                form_map.putAll(hidden_form_map_);
            }
            
            form_map.put("bbs", thread_data_.server_def_.board_tag_);
            form_map.put("key", String.valueOf(thread_data_.thread_id_));
            form_map.put("time", String.valueOf(epoc_time));
            form_map.put("FROM", post_entry_data_.author_name_);
            form_map.put("mail", post_entry_data_.author_mail_);
            form_map.put("MESSAGE", post_entry_data_.entry_body_);
            
            for (Entry<String, String> data : form_map.entrySet()) {
                if (buf.length() > 0) buf.append("&");
                buf.append(data.getKey()).append("=").append(urlencode(data.getValue()));
            }
            
            buf.append("&submit=").append("%8F%91%82%AB%8D%9E%82%DE");
            
            ((HttpPost) req).setEntity(new StringEntity(buf.toString(), getTextEncode()));
            req.setHeader("Referer", referer_uri_);
            
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
            
            if (checkRetryPost(title, tag_2ch_x, body)) return;
            
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
        
        if (callback_ != null) callback_.onPosted();
        
        callback_ = null;
        return true;
    }
    
    // 書き込み確認画面
    private boolean checkRetryPost(final String title, final String tag_2ch_x, final String body)
            throws InterruptedException {
        if (post_entry_data_.is_retry_) return false;
        if (title.indexOf("書き込み確認") == -1 && tag_2ch_x.indexOf("cookie") == -1) {
            return false;
        }
        
        Matcher matcher_check_message = pattern_check_message_.matcher(body);
        matcher_check_message.reset();
        final String message = (matcher_check_message.find()) ? HtmlUtils.stripAllHtmls(matcher_check_message.group(1),
                true) : "";
        
        Matcher matcher_form_hidden = pattern_hidden_form_.matcher(body);
        matcher_form_hidden.reset();
        while (matcher_form_hidden.find()) {
            if (Thread.interrupted()) throw new InterruptedException();
            
            String form_name = matcher_form_hidden.group(1);
            String form_value = matcher_form_hidden.group(2);
            post_entry_data_.hidden_form_map_.put(form_name, form_value);
        }
        
        if (callback_ != null) {
            post_entry_data_.is_retry_ = true;
            callback_.onPostRetryNotice(post_entry_data_, message);
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
