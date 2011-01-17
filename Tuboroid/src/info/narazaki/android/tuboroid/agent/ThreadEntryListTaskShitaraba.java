package info.narazaki.android.tuboroid.agent;

import info.narazaki.android.tuboroid.TuboroidApplication.AccountPref;
import info.narazaki.android.tuboroid.agent.task.HttpGetThreadEntryListTaskShitarabaStorage;
import info.narazaki.android.tuboroid.data.ThreadData;


public class ThreadEntryListTaskShitaraba extends ThreadEntryListTask {
    private static final String TAG = "ThreadEntryListTaskShitaraba";
    
    public ThreadEntryListTaskShitaraba(TuboroidAgentManager agent_manager) {
        super(agent_manager);
    }

    @Override
    protected void doReloadSpecialThreadEntryList(final ThreadData thread_data, final AccountPref account_pref,
    		final String user_agent, final ThreadEntryListFetchedCallback callback) {
    	HttpGetThreadEntryListTaskShitarabaStorage task = new HttpGetThreadEntryListTaskShitarabaStorage(thread_data,
    			factoryGetThreadEntryListCallback(thread_data, user_agent, false, callback));
    	task.sendTo(agent_manager_.getMultiHttpAgent());
    }
}
