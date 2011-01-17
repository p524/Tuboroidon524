package info.narazaki.android.tuboroid.agent;

import info.narazaki.android.tuboroid.R;
import info.narazaki.android.tuboroid.TuboroidApplication.AccountPref;
import info.narazaki.android.tuboroid.agent.task.HttpBoardLoginTask2chMaru;
import info.narazaki.android.tuboroid.agent.task.HttpBoardLoginTask2chMaru.MaruLoginCallback;
import info.narazaki.android.tuboroid.agent.task.HttpCreateNewThreadTask2ch;
import info.narazaki.android.tuboroid.data.BoardData;
import info.narazaki.android.tuboroid.data.NewThreadData;
import java.util.HashMap;

public class CreateNewThreadTask2ch extends CreateNewThreadTask {
    private static final String TAG = "CreateNewThreadTask2ch";
    
    public CreateNewThreadTask2ch(TuboroidAgentManager agent_manager) {
        super(agent_manager);
    }
    
    @Override
    public FutureCreateNewThread createNewThread(final BoardData board_data, final NewThreadData new_thread_data,
            final AccountPref account_pref, final String user_agent, final OnCreateNewThreadCallback callback) {
        if (!board_data.canSpecialCreateNewThread(account_pref)) {
            createNewThread(board_data, new_thread_data, null, callback);
        }
        else if (account_pref.use_maru_) {
            loginMaru(board_data, new_thread_data, account_pref, user_agent, callback);
        }
        else {
            createNewThread(board_data, new_thread_data, null, callback);
        }
        return new FutureCreateNewThread();
    }
    
    private void loginMaru(final BoardData board_data, final NewThreadData new_thread_data,
            final AccountPref account_pref, final String user_agent, final OnCreateNewThreadCallback callback) {
        HttpBoardLoginTask2chMaru task = new HttpBoardLoginTask2chMaru(account_pref, user_agent,
                new MaruLoginCallback() {
                    @Override
                    public void onLoginFailed() {
                        callback.onCreateFailed(agent_manager_.getContext().getString(
                                R.string.text_failed_to_login_maru));
                    }
                    
                    @Override
                    public void onLogin(String sessionKey) {
                        createNewThread(board_data, new_thread_data, sessionKey, callback);
                    }
                });
        task.sendTo(agent_manager_.getMaruHttpAgent());
    }
    
    private void createNewThread(final BoardData board_data, final NewThreadData new_thread_data,
            final String session_key, final OnCreateNewThreadCallback callback) {
        HashMap<String, String> hidden_form_map = new HashMap<String, String>();
        if (session_key != null && session_key.length() > 0) {
            hidden_form_map.put("sid", session_key);
        }
        
        HttpCreateNewThreadTask2ch task = new HttpCreateNewThreadTask2ch(board_data, board_data.getCreateNewThreadURI(),
                board_data.getCreateNewThreadURI(), new_thread_data, hidden_form_map,
                new HttpCreateNewThreadTask2ch.Callback() {
                    @Override
                    public void onConnectionError(boolean connectionFailed) {
                        callback.onConnectionError(connectionFailed);
                    }
                    
                    @Override
                    public void onCreated() {
                        callback.onCreated();
                    }
                    
                    @Override
                    public void onCreateFailed(String message) {
                        callback.onCreateFailed(message);
                    }
                    
                    @Override
                    public void onRetryNotice(NewThreadData retry_new_thread_data, String message) {
                        callback.onRetryNotice(retry_new_thread_data, message);
                    }
                    
                    @Override
                    public void onRetry(NewThreadData retry_new_thread_data) {
                        createNewThread(board_data, retry_new_thread_data, session_key, callback);
                    }
                });
        task.sendTo(agent_manager_.getSingleHttpAgent());
    }
}
