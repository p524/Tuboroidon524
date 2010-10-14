package info.narazaki.android.tuboroid.agent;

import info.narazaki.android.tuboroid.TuboroidApplication.AccountPref;
import info.narazaki.android.tuboroid.agent.task.HttpPostEntryTaskShitaraba;
import info.narazaki.android.tuboroid.data.PostEntryData;
import info.narazaki.android.tuboroid.data.ThreadData;

public class PostEntryTaskShitaraba extends PostEntryTask {
    private static final String TAG = "PostEntryTaskShitaraba";
    
    public PostEntryTaskShitaraba(TuboroidAgentManager agent_manager) {
        super(agent_manager);
    }
    
    @Override
    public FuturePostEntry post(final ThreadData thread_data, final PostEntryData post_entry_data,
            final AccountPref account_pref, final String user_agent, final OnPostEntryCallback callback) {
        postEntry(thread_data, post_entry_data, callback);
        return new FuturePostEntry();
    }
    
    private void postEntry(final ThreadData thread_data, final PostEntryData post_entry_data,
            final OnPostEntryCallback callback) {
        HttpPostEntryTaskShitaraba task = new HttpPostEntryTaskShitaraba(thread_data, post_entry_data,
                new HttpPostEntryTaskShitaraba.Callback() {
                    
                    @Override
                    public void onPosted() {
                        callback.onPosted();
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
