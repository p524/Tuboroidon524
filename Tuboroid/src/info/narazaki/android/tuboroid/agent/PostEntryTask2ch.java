package info.narazaki.android.tuboroid.agent;

import info.narazaki.android.tuboroid.R;
import info.narazaki.android.tuboroid.TuboroidApplication.AccountPref;
import info.narazaki.android.tuboroid.agent.task.HttpBoardLoginTask2chMaru;
import info.narazaki.android.tuboroid.agent.task.HttpBoardLoginTask2chMaru.MaruLoginCallback;
import info.narazaki.android.tuboroid.agent.task.HttpBoardLoginTask2chP2;
import info.narazaki.android.tuboroid.agent.task.HttpPostEntryTask2ch;
import info.narazaki.android.tuboroid.data.PostEntryData;
import info.narazaki.android.tuboroid.data.ThreadData;

import java.util.HashMap;

public class PostEntryTask2ch extends PostEntryTask {
    private static final String TAG = "PostEntryTask2ch";
    
    public PostEntryTask2ch(TuboroidAgentManager agent_manager) {
        super(agent_manager);
    }
    
    @Override
    public FuturePostEntry post(final ThreadData thread_data, final PostEntryData post_entry_data,
            final AccountPref account_pref, final String user_agent, final OnPostEntryCallback callback) {
    	if(account_pref == null){
    		postEntry(thread_data, post_entry_data, null, callback);
    	}else if (!thread_data.canSpecialPost(account_pref)) {
            postEntry(thread_data, post_entry_data, null, callback);
        }
        else if (account_pref.use_maru_) {
            loginMaru(thread_data, post_entry_data, account_pref, user_agent, callback);
        }
        else if (account_pref.use_p2_) {
            if (post_entry_data.is_retry_) {
                postEntryByP2(thread_data, post_entry_data, account_pref, null, callback);
            }
            else {
                loginP2(thread_data, post_entry_data, account_pref, callback);
            }
        }
        else {
            postEntry(thread_data, post_entry_data, null, callback);
        }
        return new FuturePostEntry();
    }
    
    private void loginMaru(final ThreadData thread_data, final PostEntryData post_entry_data,
            final AccountPref account_pref, final String user_agent, final OnPostEntryCallback callback) {
        HttpBoardLoginTask2chMaru task = new HttpBoardLoginTask2chMaru(account_pref, user_agent,
                new MaruLoginCallback() {
                    @Override
                    public void onLoginFailed() {
                        callback.onPostFailed(agent_manager_.getContext().getString(R.string.text_failed_to_login_maru));
                    }
                    
                    @Override
                    public void onLogin(String sessionKey) {
                        postEntry(thread_data, post_entry_data, sessionKey, callback);
                    }
                });
        task.sendTo(agent_manager_.getMaruHttpAgent());
    }
    
    private void loginP2(final ThreadData thread_data, final PostEntryData post_entry_data,
            final AccountPref account_pref, final OnPostEntryCallback callback) {
        HttpBoardLoginTask2chP2 task = new HttpBoardLoginTask2chP2(thread_data, account_pref,
                new HttpBoardLoginTask2chP2.P2LoginCallback() {
                    @Override
                    public void onLoginFailed() {
                        callback.onPostFailed(agent_manager_.getContext().getString(R.string.text_failed_to_login_p2));
                    }
                    
                    @Override
                    public void onPostStandby(final String p2_host, final HashMap<String, String> hidden_form_map) {
                        account_pref.p2_host_ = p2_host;
                        postEntryByP2(thread_data, post_entry_data, account_pref, hidden_form_map, callback);
                    }
                });
        task.sendTo(agent_manager_.getSingleHttpAgent());
    }
    
    private void postEntry(final ThreadData thread_data, final PostEntryData post_entry_data, final String session_key,
            final OnPostEntryCallback callback) {
        HashMap<String, String> hidden_form_map = new HashMap<String, String>();
        if (session_key != null && session_key.length() > 0) {
            hidden_form_map.put("sid", session_key);
        }
        HttpPostEntryTask2ch task = new HttpPostEntryTask2ch(thread_data, thread_data.getPostEntryURI(),
                thread_data.getPostEntryRefererURI(), post_entry_data, hidden_form_map,
                new HttpPostEntryTask2ch.Callback() {
                    
                    @Override
                    public void onPosted() {
                        callback.onPosted();
                    }
                    
                    @Override
                    public void onPostRetryNotice(PostEntryData retryPostEntryData, String message) {
                        callback.onPostRetryNotice(retryPostEntryData, message);
                    }
                    
                    @Override
                    public void onPostRetry(PostEntryData retryPostEntryData) {
                        postEntry(thread_data, retryPostEntryData, session_key, callback);
                    }
                    
                    @Override
                    public void onPostFailed(String message) {
                        callback.onPostFailed(message);
                    }
                    
                    @Override
                    public void onConnectionError(boolean connectionFailed) {
                        callback.onConnectionError(connectionFailed);
                    }
                });
        task.sendTo(agent_manager_.getSingleHttpAgent());
    }
    
    private void postEntryByP2(final ThreadData thread_data, final PostEntryData post_entry_data,
            final AccountPref account_pref, final HashMap<String, String> hidden_form_map,
            final OnPostEntryCallback callback) {
        final String post_entry_uri = HttpBoardLoginTask2chP2.createFormPostUri(account_pref.p2_host_);
        final String referer_uri = HttpBoardLoginTask2chP2.createFormInitUri(account_pref.p2_host_, thread_data);
        HttpPostEntryTask2ch task = new HttpPostEntryTask2ch(thread_data, post_entry_uri, referer_uri, post_entry_data,
                hidden_form_map, new HttpPostEntryTask2ch.Callback() {
                    
                    @Override
                    public void onPosted() {
                        callback.onPosted();
                    }
                    
                    @Override
                    public void onPostRetryNotice(PostEntryData retryPostEntryData, String message) {
                        callback.onPostRetryNotice(retryPostEntryData, message);
                    }
                    
                    @Override
                    public void onPostRetry(PostEntryData retryPostEntryData) {
                        postEntryByP2(thread_data, retryPostEntryData, account_pref, hidden_form_map, callback);
                    }
                    
                    @Override
                    public void onPostFailed(String message) {
                        callback.onPostFailed(message);
                    }
                    
                    @Override
                    public void onConnectionError(boolean connectionFailed) {
                        callback.onConnectionError(connectionFailed);
                    }
                });
        task.sendTo(agent_manager_.getSingleHttpAgent());
    }
}
