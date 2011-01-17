package info.narazaki.android.tuboroid.agent;

import info.narazaki.android.tuboroid.TuboroidApplication.AccountPref;
import info.narazaki.android.tuboroid.agent.task.HttpCreateNewThreadTaskShitaraba;
import info.narazaki.android.tuboroid.data.BoardData;
import info.narazaki.android.tuboroid.data.NewThreadData;

public class CreateNewThreadTaskShitaraba extends CreateNewThreadTask {
    private static final String TAG = "CreateNewThreadTaskShitaraba";
    
    public CreateNewThreadTaskShitaraba(TuboroidAgentManager agent_manager) {
        super(agent_manager);
    }
    
    @Override
    public FutureCreateNewThread createNewThread(final BoardData board_data, final NewThreadData new_thread_data,
            final AccountPref account_pref, final String user_agent, final OnCreateNewThreadCallback callback) {
        createNewThread(board_data, new_thread_data, callback);
        return new FutureCreateNewThread();
    }
    
    private void createNewThread(final BoardData board_data, final NewThreadData new_thread_data,
            final OnCreateNewThreadCallback callback) {
        HttpCreateNewThreadTaskShitaraba task = new HttpCreateNewThreadTaskShitaraba(board_data, new_thread_data,
                new HttpCreateNewThreadTaskShitaraba.Callback() {
                    
                    @Override
                    public void onCreated() {
                        callback.onCreated();
                    }
                    
                    @Override
                    public void onCreateFailed(String message) {
                        callback.onCreateFailed(message);
                    }
                    
                    @Override
                    public void onConnectionError(boolean connectionFailed) {
                        callback.onConnectionError(connectionFailed);
                    }
                });
        task.sendTo(agent_manager_.getSingleHttpAgent());
    }
}
