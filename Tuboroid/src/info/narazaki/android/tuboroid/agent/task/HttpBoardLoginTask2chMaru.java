package info.narazaki.android.tuboroid.agent.task;

import info.narazaki.android.lib.agent.http.task.TextHttpPostTaskBase;
import info.narazaki.android.tuboroid.TuboroidApplication.AccountPref;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;

public class HttpBoardLoginTask2chMaru extends TextHttpPostTaskBase {
    private static final String TAG = "HttpBoardLoginTask2chMaru";
    
    static public interface MaruLoginCallback {
        void onLogin(final String session_key);
        
        void onLoginFailed();
    }
    
    private static final String LOGIN_URI = "https://2chv.tora3.net/futen.cgi";
    private static final String MARU_REFERER = "https://2chv.tora3.net/";
    
    private static final String SESSION_PREFIX = "SESSION-ID=";
    private static final String ERROR_PREFIX = "ERROR";
    
    private AccountPref account_pref_;
    private String user_agent_;
    private MaruLoginCallback callback_;
    
    @Override
    protected String getTextEncode() {
        return "MS932";
    }
    
    public HttpBoardLoginTask2chMaru(AccountPref account_pref, String user_agent, MaruLoginCallback callback) {
        super(LOGIN_URI);
        account_pref_ = account_pref;
        user_agent_ = user_agent;
        callback_ = callback;
    }
    
    @Override
    protected boolean setRequestParameters(HttpRequestBase req) {
        try {
            req.setHeader("Content-type", "application/x-www-form-urlencoded");
            StringBuilder buf = new StringBuilder();
            buf.append("ID=").append(account_pref_.maru_user_id_);
            buf.append("&PW=").append(account_pref_.maru_password_);
            
            StringEntity string_entity = new StringEntity(buf.toString(), getTextEncode());
            ((HttpPost) req).setEntity(string_entity);
            req.setHeader("Referer", MARU_REFERER);
            req.setHeader("X-2ch-UA", user_agent_);
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
            String line = reader.readLine();
            if (line == null || line.indexOf(SESSION_PREFIX) != 0) {
                onConnectionError(false);
                return;
            }
            String session_key = line.substring(SESSION_PREFIX.length());
            if (session_key.length() == 0 || session_key.toUpperCase().indexOf(ERROR_PREFIX) == 0) {
                onConnectionError(false);
                return;
            }
            callback_.onLogin(session_key);
        }
        catch (Exception e) {
            if (callback_ != null) {
                callback_.onLoginFailed();
                callback_ = null;
            }
        }
        finally {
            reader.close();
        }
        if (Thread.interrupted()) throw new InterruptedException();
    }
    
    @Override
    protected void onConnectionError(boolean connection_failed) {
        if (callback_ != null) {
            callback_.onLoginFailed();
        }
        callback_ = null;
    }
    
    @Override
    protected void onRequestCanceled() {
        onConnectionError(false);
    }
    
}
