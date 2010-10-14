package info.narazaki.android.tuboroid.agent.task;

import info.narazaki.android.lib.agent.http.HttpTaskAgentInterface;
import info.narazaki.android.lib.agent.http.task.TextHttpGetTaskBase;
import info.narazaki.android.lib.agent.http.task.TextHttpPostTaskBase;
import info.narazaki.android.tuboroid.TuboroidApplication.AccountPref;
import info.narazaki.android.tuboroid.data.ThreadData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;

import android.net.Uri;

public class HttpBoardLoginTask2chP2 {
    private static final String TAG = "HttpBoardLoginTask2chP2";
    
    static public interface P2LoginCallback {
        void onPostStandby(final String p2_host, final HashMap<String, String> hidden_form_map);
        
        void onLoginFailed();
    }
    
    public static final String P2_BASE_HOST = "p2.2ch.net";
    public static final String LOGIN_PATH = "/p2/";
    public static final String POST_FORM_PATH = "/p2/post_form.php";
    public static final String POST_PATH = "/p2/post.php";
    
    private final ExecutorService executor_;
    private HttpTaskAgentInterface http_agent_;
    
    private ThreadData thread_data_;
    private AccountPref account_pref_;
    private P2LoginCallback callback_;
    
    static Pattern pattern_error_form_;
    static Pattern pattern_hidden_form_;
    static Pattern pattern_is_login_form_;
    static Pattern pattern_is_post_form_;
    static {
        pattern_error_form_ = Pattern.compile("<p class=\"infomsg\"\\>Error:.*</p\\><p class=\"infomsg\"\\></p\\>",
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
        pattern_hidden_form_ = Pattern.compile(
                "<input .*?type=\"?hidden\"? .*?name=\"?(.+?)\"? .*?value=\"?([^\\>\"]*)\"?.*?\\>",
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
        pattern_is_login_form_ = Pattern
                .compile(
                        "<input type=\"text\".+?name=\"form_login_id\".*?\\>.*?<input type=\"password\".+?name=\"form_login_pass\".*?\\>",
                        Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
        pattern_is_post_form_ = Pattern.compile(
                "<input type=\"hidden\".+?name=\"key\".*?\\>.*?<input type=\"hidden\".+?name=\"host\".*?\\>",
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
    }
    
    static public String createFormInitUri(final String host_name, final ThreadData thread_data) {
        return "http://" + host_name + POST_FORM_PATH + "?host=" + thread_data.server_def_.board_server_ + "&bbs="
                + thread_data.server_def_.board_tag_ + "&key=" + thread_data.thread_id_;
    }
    
    static public String createFormPostUri(final String host_name) {
        return "http://" + host_name + POST_PATH;
    }
    
    private class LoginFormTask extends TextHttpGetTaskBase {
        public LoginFormTask(final String host_name) {
            super("http://" + host_name + LOGIN_PATH);
        }
        
        @Override
        protected String getTextEncode() {
            return "MS932";
        }
        
        @Override
        protected void dispatchHttpTextResponse(HttpResponse res, BufferedReader reader) throws InterruptedException,
                IOException {
            final HashMap<String, String> hidden_form_map = new HashMap<String, String>();
            StringBuilder buf = new StringBuilder();
            while (true) {
                String line = reader.readLine();
                if (line == null) break;
                buf.append(line);
            }
            
            // hidden回収
            Matcher matcher_hidden_form = pattern_hidden_form_.matcher(buf);
            matcher_hidden_form.reset();
            while (matcher_hidden_form.find()) {
                String form_name = matcher_hidden_form.group(1);
                String form_value = matcher_hidden_form.group(2);
                hidden_form_map.put(form_name, form_value);
            }
            
            final Uri real_uri = Uri.parse(getRealRequestUri());
            final String host_name = real_uri.getHost();
            
            // ログインフォームだよね?
            if (pattern_is_login_form_.matcher(buf).reset().find()) {
                executor_.submit(new Runnable() {
                    @Override
                    public void run() {
                        LoginRequestTask next_task = new LoginRequestTask(host_name, hidden_form_map);
                        next_task.sendTo(http_agent_);
                    }
                });
                return;
            }
            
            // なんかログイン済みっぽいな……
            executor_.submit(new Runnable() {
                @Override
                public void run() {
                    PostFormTask next_task = new PostFormTask(host_name);
                    next_task.sendTo(http_agent_);
                }
            });
        }
        
        @Override
        protected void onRequestCanceled() {
            callback_.onLoginFailed();
        }
        
        @Override
        protected void onConnectionError(boolean connectionFailed) {
            callback_.onLoginFailed();
        }
    }
    
    private class LoginRequestTask extends TextHttpPostTaskBase {
        private HashMap<String, String> login_form_map_;
        
        public LoginRequestTask(final String host_name, HashMap<String, String> login_form_map) {
            super("http://" + host_name + LOGIN_PATH);
            login_form_map_ = new HashMap<String, String>(login_form_map);
        }
        
        @Override
        protected String getTextEncode() {
            return "MS932";
        }
        
        @Override
        protected boolean setRequestParameters(HttpRequestBase req) {
            try {
                req.setHeader("Content-type", "application/x-www-form-urlencoded");
                StringBuilder buf = new StringBuilder();
                login_form_map_.put("form_login_id", account_pref_.p2_user_id_);
                login_form_map_.put("form_login_pass", account_pref_.p2_password_);
                login_form_map_.put("regist_cookie", "1");
                login_form_map_.put("ignore_cip", "1");
                login_form_map_.put("submit_userlogin", "%83%86%81%5B%83U%83%8D%83O%83C%83%93");
                
                if (login_form_map_ != null) {
                    for (Entry<String, String> data : login_form_map_.entrySet()) {
                        if (buf.length() > 0) buf.append("&");
                        buf.append(data.getKey()).append("=").append(urlencode(data.getValue()));
                    }
                }
                StringEntity string_entity = new StringEntity(buf.toString(), getTextEncode());
                ((HttpPost) req).setEntity(string_entity);
                req.setHeader("Referer", getRequestUri());
            }
            catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            return true;
        }
        
        @Override
        protected void dispatchHttpTextResponse(HttpResponse res, final BufferedReader reader)
                throws InterruptedException, IOException {
            final Uri real_uri = Uri.parse(getRealRequestUri());
            final String host_name = real_uri.getHost();
            
            executor_.submit(new Runnable() {
                @Override
                public void run() {
                    PostFormTask next_task = new PostFormTask(host_name);
                    next_task.sendTo(http_agent_);
                }
            });
        }
        
        @Override
        protected void onRequestCanceled() {
            callback_.onLoginFailed();
        }
        
        @Override
        protected void onConnectionError(boolean connectionFailed) {
            callback_.onLoginFailed();
        }
    }
    
    private class PostFormTask extends TextHttpGetTaskBase {
        public PostFormTask(final String host_name) {
            super(createFormInitUri(host_name, thread_data_));
        }
        
        @Override
        protected String getTextEncode() {
            return "MS932";
        }
        
        @Override
        protected void dispatchHttpTextResponse(HttpResponse res, BufferedReader reader) throws InterruptedException,
                IOException {
            final HashMap<String, String> hidden_form_map = new HashMap<String, String>();
            StringBuilder buf = new StringBuilder();
            while (true) {
                String line = reader.readLine();
                if (line == null) break;
                buf.append(line);
            }
            
            // hidden回収
            Matcher matcher_hidden_form = pattern_hidden_form_.matcher(buf);
            matcher_hidden_form.reset();
            while (matcher_hidden_form.find()) {
                String form_name = matcher_hidden_form.group(1);
                String form_value = matcher_hidden_form.group(2);
                hidden_form_map.put(form_name, form_value);
            }
            
            // 投稿フォームっぽいな……
            final Uri real_uri = Uri.parse(getRealRequestUri());
            final String host_name = real_uri.getHost();
            if (pattern_is_post_form_.matcher(buf).reset().find()) {
                if (hidden_form_map.size() != 0) {
                    callback_.onPostStandby(host_name, hidden_form_map);
                    return;
                }
            }
            callback_.onLoginFailed();
        }
        
        @Override
        protected void onRequestCanceled() {
            callback_.onLoginFailed();
        }
        
        @Override
        protected void onConnectionError(boolean connectionFailed) {
            callback_.onLoginFailed();
        }
    }
    
    public HttpBoardLoginTask2chP2(ThreadData thread_data, AccountPref account_pref, P2LoginCallback callback) {
        executor_ = Executors.newSingleThreadExecutor();
        thread_data_ = thread_data;
        account_pref_ = account_pref;
        callback_ = callback;
    }
    
    public void sendTo(HttpTaskAgentInterface http_agent) {
        http_agent_ = http_agent;
        
        executor_.submit(new Runnable() {
            @Override
            public void run() {
                LoginFormTask task = new LoginFormTask(P2_BASE_HOST);
                http_agent_.send(task);
            }
        });
    }
    
}
