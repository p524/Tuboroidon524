package info.narazaki.android.tuboroid.agent;

import info.narazaki.android.lib.agent.db.SQLiteAgentBase;
import info.narazaki.android.lib.list.ListUtils;
import info.narazaki.android.lib.text.HtmlUtils;
import info.narazaki.android.tuboroid.TuboroidApplication.AccountPref;
import info.narazaki.android.tuboroid.agent.task.HttpGetThreadEntryListTask;
import info.narazaki.android.tuboroid.agent.thread.DataFileAgent;
import info.narazaki.android.tuboroid.agent.thread.SQLiteAgent;
import info.narazaki.android.tuboroid.data.IgnoreData;
import info.narazaki.android.tuboroid.data.ThreadData;
import info.narazaki.android.tuboroid.data.ThreadEntryData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.util.Log;

abstract public class ThreadEntryListTask {
    private static final String TAG = "ThreadEntryListTask";
    
    protected final TuboroidAgentManager agent_manager_;
    
    public LinkedList<Runnable> fetch_task_list_;
    
    public ThreadEntryListTask(TuboroidAgentManager agent_manager) {
        super();
        agent_manager_ = agent_manager;
        
        fetch_task_list_ = new LinkedList<Runnable>();
    }
    
    private synchronized void pushFetchTask(final Runnable runnable) {
        fetch_task_list_.addLast(runnable);
        if (fetch_task_list_.size() == 1) {
            runnable.run();
            return;
        }
    }
    
    protected synchronized void popFetchTask() {
        if (fetch_task_list_.size() == 0) return;
        fetch_task_list_.removeFirst();
        if (fetch_task_list_.size() == 0) return;
        
        Runnable runnable = fetch_task_list_.getFirst();
        runnable.run();
    }
    
    public static interface ThreadEntryListFetchedCallback {
        public void onThreadEntryListFetchedByCache(final List<ThreadEntryData> data_list);
        
        public void onThreadEntryListClear();
        
        public void onThreadEntryListFetchStarted(final ThreadData thread_data);
        
        public void onThreadEntryListFetched(final List<ThreadEntryData> data_list);
        
        public void onThreadEntryListFetchedCompleted(final ThreadData thread_data);
        
        public void onInterrupted();
        
        public void onDatDropped(final boolean is_permanently);
        
        public void onConnectionFailed(final boolean connectionFailed);
        
        public void onConnectionOffline(final ThreadData thread_data);
    }
    
    protected void clearThreadEntryListCache(final ThreadData thread_data, final Runnable callback) {
        thread_data.cache_count_ = 0;
        thread_data.cache_size_ = 0;
        thread_data.working_cache_count_ = 0;
        thread_data.working_cache_size_ = 0;
        agent_manager_.getFileAgent().deleteFile(
                thread_data.getLocalDatFile(agent_manager_.getContext()).getAbsolutePath(),
                new DataFileAgent.FileWroteCallback() {
                    @Override
                    public void onFileWrote(boolean succeeded) {
                        agent_manager_.getDBAgent().updateThreadData(thread_data,
                                new SQLiteAgentBase.DbTransactionDelegate(callback));
                    }
                });
    }
    
    public final void reloadThreadEntryList(final ThreadData thread_data, final boolean use_ext_storage,
            final boolean reload, final ThreadEntryListFetchedCallback callback) {
        pushFetchTask(new Runnable() {
            @Override
            public void run() {
                doReloadThreadEntryList(thread_data, use_ext_storage, reload, callback);
            }
        });
    }
    
    protected void doReloadThreadEntryList(final ThreadData thread_data, final boolean use_ext_storage,
            final boolean reload, final ThreadEntryListFetchedCallback callback) {
        agent_manager_.getDBAgent().getThreadData(thread_data, new SQLiteAgent.GetThreadDataResult() {
            @Override
            public void onQuery(ThreadData thread_data) {
                thread_data.initWorkingCacheData();
                if (thread_data.on_ext_storage_ != use_ext_storage && thread_data.cache_count_ > 0) {
                    thread_data.on_ext_storage_ = use_ext_storage;
                    clearAndReloadThreadEntryList(thread_data, callback, reload);
                }
                thread_data.on_ext_storage_ = use_ext_storage;
                reloadThreadEntryListCache(thread_data, reload, callback);
            }
        });
    }
    
    private void clearAndReloadThreadEntryList(final ThreadData thread_data,
            final ThreadEntryListFetchedCallback callback, final boolean reload) {
        clearThreadEntryListCache(thread_data, new Runnable() {
            @Override
            public void run() {
                reloadThreadEntryListCache(thread_data, reload, callback);
            }
        });
    }
    
    private void reloadThreadEntryListCache(final ThreadData thread_data, final boolean reload,
            final ThreadEntryListFetchedCallback callback) {
        agent_manager_.getFileAgent().readFile(
                thread_data.getLocalDatFile(agent_manager_.getContext()).getAbsolutePath(),
                new DataFileAgent.FileReadUTF8Callback() {
                    @Override
                    public void read(BufferedReader reader) {
                        if (reader == null) {
                            thread_data.working_cache_count_ = 0;
                            thread_data.working_cache_size_ = 0;
                            reloadOnlineThreadEntryList(thread_data, null, callback);
                        }
                        else {
                            processThreadEntryListCache(thread_data, reader, reload, callback);
                        }
                    }
                });
    }
    
    private void processThreadEntryListCache(final ThreadData thread_data, final BufferedReader reader,
            final boolean reload, final ThreadEntryListFetchedCallback callback) {
        List<ThreadEntryData> data_list = new LinkedList<ThreadEntryData>();
        
        long thread_cur_count = 0;
        try {
            while (true) {
                String line = reader.readLine();
                if (line == null) break;
                
                thread_cur_count++;
                ArrayList<String> tokens = ListUtils.split("<>", line);
                
                ThreadEntryData data;
                if (tokens.size() >= 8) {
                    String author_name = tokens.get(0);
                    String author_mail = tokens.get(1);
                    String entry_body = tokens.get(2);
                    String author_id = tokens.get(3);
                    String author_be = tokens.get(4);
                    String entry_time = tokens.get(5);
                    String entry_is_aa = tokens.get(6);
                    String forward_anchor_list_str = tokens.get(7);
                    
                    if (thread_cur_count == 1 && tokens.size() >= 9 && tokens.get(8).length() > 0) {
                        thread_data.thread_name_ = tokens.get(8);
                    }
                    
                    entry_body = HtmlUtils.unescapeHtml(entry_body.replace("<br>", "\n"));
                    data = new ThreadEntryData(true, thread_cur_count, author_name, author_mail, entry_body, author_id,
                            author_be, entry_time, entry_is_aa, forward_anchor_list_str);
                }
                else {
                    Log.e(TAG, "Broken Cache : NUM=" + thread_cur_count);
                    data = new ThreadEntryData(true, thread_cur_count, "", "", "", "", "", "", "", "");
                }
                
                data_list.add(data);
            }
            if (thread_data.working_cache_count_ == thread_cur_count) {
                // DB上の数とファイルの内容が一致した時のみ通知(外部ストレージに変なデータが来た時のため)
                applyIgnoreList(data_list);
                callback.onThreadEntryListFetchedByCache(data_list);
            }
            else {
                Log.e(TAG, "Broken Cache : Data Clear : working_cache_count_=" + thread_data.working_cache_count_
                        + " but NUM=" + thread_cur_count);
                clearThreadEntryListCache(thread_data, null);
                data_list.clear();
                callback.onThreadEntryListFetchedByCache(data_list);
            }
        }
        catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            try {
                reader.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        if (reload) {
            reloadOnlineThreadEntryList(thread_data, null, callback);
        }
        else {
            callback.onThreadEntryListFetchedCompleted(thread_data);
            popFetchTask();
        }
    }
    
    private void appendThreadEntryListCache(final ThreadData thread_data, final List<ThreadEntryData> data_list) {
        agent_manager_.getFileAgent().writeFile(
                thread_data.getLocalDatFile(agent_manager_.getContext()).getAbsolutePath(),
                new DataFileAgent.FileWriteUTF8StreamCallback() {
                    
                    @Override
                    public void write(Writer writer) throws IOException {
                        for (ThreadEntryData data : data_list) {
                            writer.append(data.author_name_).append("<>");
                            writer.append(data.author_mail_).append("<>");
                            
                            String entry_body = HtmlUtils.escapeHtml(data.entry_body_).replace("\n", "<br>");
                            writer.append(entry_body).append("<>");
                            
                            writer.append(data.author_id_).append("<>");
                            writer.append(data.author_be_).append("<>");
                            writer.append(data.entry_time_).append("<>");
                            writer.append(data.entry_is_aa_ ? "1" : "0").append("<>");
                            writer.append(data.forward_anchor_list_str_);
                            if (data.entry_id_ == 1) {
                                writer.append("<>").append(thread_data.thread_name_);
                            }
                            writer.append("\n");
                        }
                    }
                }, true, true, true, null);
    }
    
    protected void applyIgnoreList(List<ThreadEntryData> data_list) {
        for (ThreadEntryData data : data_list) {
            int type = agent_manager_.getIgnoreListAgent().checkNG(data);
            if (type != IgnoreData.TYPE.NONE) {
                data.ng_flag_ = type;
            }
        }
    }
    
    protected void reloadOnlineThreadEntryList(final ThreadData thread_data_orig, final String session_key,
            final ThreadEntryListFetchedCallback callback) {
        final ThreadData thread_data = thread_data_orig.clone();
        
        // Offlineチェック
        if (!agent_manager_.isOnline()) {
            callback.onConnectionOffline(thread_data);
            popFetchTask();
            return;
        }
        
        final boolean can_abone = thread_data.working_cache_count_ > 0;
        final ArrayList<ThreadEntryData> received_list = new ArrayList<ThreadEntryData>();
        
        HttpGetThreadEntryListTask task = thread_data.factoryGetThreadHttpGetThreadEntryListTask(session_key,
                new HttpGetThreadEntryListTask.Callback() {
                    @Override
                    public void onFetchStarted() {
                        callback.onThreadEntryListFetchStarted(thread_data);
                    }
                    
                    @Override
                    public void onReceivedNew() {
                        thread_data.working_cache_size_ = 0;
                        thread_data.working_cache_count_ = 0;
                    }
                    
                    @Override
                    public void onReceived(List<ThreadEntryData> data_list) {
                        if (thread_data.working_cache_count_ == 0) {
                            agent_manager_.getFileAgent().deleteFile(
                                    thread_data.getLocalDatFile(agent_manager_.getContext()).getAbsolutePath(), null);
                            callback.onThreadEntryListClear();
                        }
                        thread_data.working_cache_count_ += data_list.size();
                        applyIgnoreList(data_list);
                        received_list.addAll(data_list);
                        callback.onThreadEntryListFetched(data_list);
                    }
                    
                    @Override
                    public void onNoUpdated() {
                        agent_manager_.getDBAgent().updateThreadData(thread_data, new SQLiteAgentBase.DbTransaction() {
                            
                            @Override
                            public void run() {
                                callback.onThreadEntryListFetchedCompleted(thread_data);
                                popFetchTask();
                            }
                            
                            @Override
                            public void onError() {
                                callback.onInterrupted();
                                popFetchTask();
                            }
                        });
                    }
                    
                    @Override
                    public void onInterrupted() {
                        callback.onInterrupted();
                    }
                    
                    @Override
                    public void onDatDropped() {
                        thread_data.is_dropped_ = true;
                        agent_manager_.getDBAgent().updateThreadData(thread_data, new SQLiteAgentBase.DbTransaction() {
                            @Override
                            public void run() {
                                callback.onDatDropped(session_key != null);
                                popFetchTask();
                            }
                            
                            @Override
                            public void onError() {
                                callback.onInterrupted();
                                popFetchTask();
                            }
                        });
                    }
                    
                    @Override
                    public void onConnectionFailed(boolean connectionFailed) {
                        if (connectionFailed) {
                            callback.onConnectionFailed(connectionFailed);
                            popFetchTask();
                        }
                        else {
                            onDatDropped();
                        }
                    }
                    
                    @Override
                    public void onDatBroken() {
                        onDatDropped();
                    }
                    
                    @Override
                    public void onCompleted() {
                        appendThreadEntryListCache(thread_data, received_list);
                        thread_data.flushWorkingCacheData();
                        agent_manager_.getDBAgent().updateThreadData(thread_data, new SQLiteAgentBase.DbTransaction() {
                            @Override
                            public void run() {
                                callback.onThreadEntryListFetchedCompleted(thread_data);
                                popFetchTask();
                            }
                            
                            @Override
                            public void onError() {
                                callback.onInterrupted();
                                popFetchTask();
                            }
                        });
                    }
                    
                    @Override
                    public void onAboneFound() {
                        if (can_abone) {
                            thread_data.working_cache_count_ = 0;
                            thread_data.working_cache_size_ = 0;
                            reloadOnlineThreadEntryList(thread_data, null, callback);
                            
                        }
                        else {
                            callback.onConnectionFailed(false);
                            popFetchTask();
                        }
                    }
                });
        task.sendTo(agent_manager_.getMultiHttpAgent());
    }
    
    public final void reloadSpecialThreadEntryList(final ThreadData thread_data, final AccountPref account_pref,
            final String user_agent, final ThreadEntryListFetchedCallback callback) {
        pushFetchTask(new Runnable() {
            @Override
            public void run() {
                doReloadSpecialThreadEntryList(thread_data, account_pref, user_agent, callback);
            }
        });
    }
    
    protected void doReloadSpecialThreadEntryList(final ThreadData thread_data, final AccountPref account_pref,
            final String user_agent, final ThreadEntryListFetchedCallback callback) {
        callback.onConnectionFailed(true);
    }
    
}
