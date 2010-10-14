package info.narazaki.android.tuboroid.agent;

import info.narazaki.android.tuboroid.TuboroidApplication.AccountPref;
import info.narazaki.android.tuboroid.agent.task.HttpBoardLoginTask2chMaru;
import info.narazaki.android.tuboroid.agent.task.HttpBoardLoginTask2chMaru.MaruLoginCallback;
import info.narazaki.android.tuboroid.data.ThreadData;

public class ThreadEntryListTask2ch extends ThreadEntryListTask {
    private static final String TAG = "ThreadEntryListTask2ch";
    
    public ThreadEntryListTask2ch(TuboroidAgentManager agent_manager) {
        super(agent_manager);
    }
    
    @Override
    protected void doReloadSpecialThreadEntryList(final ThreadData thread_data, final AccountPref account_pref,
            final String user_agent, final ThreadEntryListFetchedCallback callback) {
        HttpBoardLoginTask2chMaru task = new HttpBoardLoginTask2chMaru(account_pref, user_agent,
                new MaruLoginCallback() {
                    @Override
                    public void onLoginFailed() {
                        callback.onConnectionFailed(false);
                        popFetchTask();
                    }
                    
                    @Override
                    public void onLogin(final String session_key) {
                        clearThreadEntryListCache(thread_data, new Runnable() {
                            @Override
                            public void run() {
                                reloadOnlineThreadEntryList(thread_data, session_key, callback);
                            }
                        });
                    }
                });
        task.sendTo(agent_manager_.getMaruHttpAgent());
    }
    
}
