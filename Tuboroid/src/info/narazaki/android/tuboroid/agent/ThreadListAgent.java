package info.narazaki.android.tuboroid.agent;

import info.narazaki.android.lib.list.ListUtils;
import info.narazaki.android.lib.text.LevenshteinDistanceCalc;
import info.narazaki.android.lib.text.TextUtils;
import info.narazaki.android.tuboroid.TuboroidApplication;
import info.narazaki.android.tuboroid.agent.task.HttpGetThreadListTask;
import info.narazaki.android.tuboroid.agent.thread.DataFileAgent;
import info.narazaki.android.tuboroid.agent.thread.SQLiteAgent;
import info.narazaki.android.tuboroid.data.BoardData;
import info.narazaki.android.tuboroid.data.ThreadData;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.database.Cursor;

public class ThreadListAgent {
    private static final String TAG = "ThreadListAgent";
    private static final int DEFAULT_THREAD_LIST_CACHE_SECOND = 60 * 60;
    
    private TuboroidAgentManager agent_manager_;
    
    private final ExecutorService executor_;
    
    public ThreadListAgent(TuboroidAgentManager agent_manager) {
        super();
        agent_manager_ = agent_manager;
        executor_ = Executors.newSingleThreadExecutor();
    }
    
    public static interface ThreadListFetchedCallback {
        public void onThreadListFetchedCache(final List<ThreadData> data_list);
        
        public void onThreadListFetched(final List<ThreadData> data_list);
        
        public void onThreadListFetchCompleted();
        
        public void onInterrupted();
        
        public void onThreadListFetchFailed(final boolean maybe_moved);
        
        public void onConnectionOffline();
    }
    
    public static interface RecentListFetchedCallback {
        public void onRecentListFetched(final ArrayList<ThreadData> data_list);
    }
    
    public void fetchThreadList(final BoardData board_data, final boolean force_reload,
            final ThreadListFetchedCallback callback) {
        reloadThreadList(board_data, force_reload, callback);
    }
    
    private void reloadThreadList(final BoardData board_data, final boolean force_reload,
            final ThreadListFetchedCallback callback) {
        agent_manager_.getDBAgent().getThreadList(board_data, new SQLiteAgent.DbResultReceiver() {
            @Override
            public void onQuery(Cursor cursor) {
                final ArrayList<ThreadData> data_list_db = new ArrayList<ThreadData>();
                if (cursor.getCount() != 0) {
                    cursor.moveToFirst();
                    while (true) {
                        data_list_db.add(ThreadData.factory(cursor));
                        if (cursor.moveToNext() == false) break;
                    }
                }
                cursor.close();
                if (!force_reload && reloadCachedThreadList(board_data, data_list_db, callback)) return;
                
                callback.onThreadListFetchedCache(data_list_db);
                reloadOnlineThreadList(board_data, data_list_db, callback);
            }
            
            @Override
            public void onError() {
                reloadOnlineThreadList(board_data, new ArrayList<ThreadData>(), callback);
            }
        });
    }
    
    private boolean reloadCachedThreadList(final BoardData board_data, final List<ThreadData> data_list_db,
            final ThreadListFetchedCallback callback) {
        final boolean use_external_storage = TuboroidApplication
                .getExternalStoragePathName(agent_manager_.getContext()) != null ? true : false;
        
        File cache_file = board_data.getLocalSubjectFile(agent_manager_.getContext(), use_external_storage);
        try {
            if (!cache_file.canRead() || !cache_file.canWrite()) return false;
            if (System.currentTimeMillis() - cache_file.lastModified() > DEFAULT_THREAD_LIST_CACHE_SECOND * 1000) {
                return false;
            }
        }
        catch (SecurityException e) {
            return false;
        }
        
        agent_manager_.getFileAgent().readFile(cache_file.getAbsolutePath(), new DataFileAgent.FileReadUTF8Callback() {
            @Override
            public void read(BufferedReader reader) {
                if (reader == null) {
                    ArrayList<ThreadData> data_list_result = new ArrayList<ThreadData>();
                    data_list_result.addAll(data_list_db);
                    callback.onThreadListFetchedCache(data_list_result);
                    callback.onThreadListFetched(new ArrayList<ThreadData>());
                    callback.onThreadListFetchCompleted();
                }
                else {
                    processCachedThreadList(board_data, data_list_db, reader, callback);
                }
            }
        });
        
        return true;
    }
    
    private void processCachedThreadList(final BoardData board_data, final List<ThreadData> data_list_db,
            final BufferedReader reader, final ThreadListFetchedCallback callback) {
        final ArrayList<ThreadData> data_list = new ArrayList<ThreadData>();
        
        try {
            int sort_order = 1;
            while (true) {
                String line = reader.readLine();
                if (line == null) break;
                
                String[] tokens = ListUtils.split("<>", line);
                
                if (tokens.length >= 4) {
                    long thread_id = TextUtils.parseLong(tokens[0]);
                    String thread_name = tokens[1].replace("<br>", "\n");
                    int online_count = TextUtils.parseInt(tokens[2]);
                    int online_speed_x10 = TextUtils.parseInt(tokens[3]);
                    ThreadData data = board_data.factoryThreadData(sort_order, thread_id, thread_name, online_count,
                            online_speed_x10);
                    data_list.add(data);
                }
                sort_order++;
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
        
        ArrayList<ThreadData> data_list_result = new ArrayList<ThreadData>();
        data_list_result.addAll(data_list_db);
        data_list_result.addAll(mergeThreadList(data_list, data_list_db));
        
        callback.onThreadListFetchedCache(data_list_result);
        callback.onThreadListFetched(new ArrayList<ThreadData>());
        callback.onThreadListFetchCompleted();
    }
    
    private void storeThreadListCache(final BoardData board_data, final List<ThreadData> data_list) {
        final boolean use_external_storage = TuboroidApplication
                .getExternalStoragePathName(agent_manager_.getContext()) != null ? true : false;
        File cache_file = board_data.getLocalSubjectFile(agent_manager_.getContext(), use_external_storage);
        
        if (data_list.size() == 0) {
            agent_manager_.getFileAgent().deleteFile(cache_file.getAbsolutePath(), null);
            return;
        }
        
        agent_manager_.getFileAgent().writeFile(cache_file.getAbsolutePath(),
                new DataFileAgent.FileWriteUTF8StreamCallback() {
                    
                    @Override
                    public void write(Writer writer) throws IOException {
                        for (ThreadData data : data_list) {
                            writer.append(String.valueOf(data.thread_id_)).append("<>");
                            String thread_name = data.thread_name_.replace("\n", "<br>");
                            writer.append(thread_name).append("<>");
                            writer.append(String.valueOf(data.online_count_)).append("<>");
                            writer.append(String.valueOf(data.online_speed_x10_));
                            writer.append("\n");
                        }
                    }
                }, false, true, true, null);
    }
    
    private void reloadOnlineThreadList(final BoardData board_data, final List<ThreadData> data_list_db,
            final ThreadListFetchedCallback callback) {
        // Offlineチェック
        if (!agent_manager_.isOnline()) {
            callback.onConnectionOffline();
            return;
        }
        
        final List<ThreadData> data_list_result = new ArrayList<ThreadData>();
        final List<ThreadData> data_list_cache_temp = new ArrayList<ThreadData>();
        data_list_result.addAll(data_list_db);
        
        HttpGetThreadListTask task = board_data.factoryGetThreadListTask(new HttpGetThreadListTask.Callback() {
            @Override
            public void onConnectionFailed() {
                callback.onThreadListFetchFailed(false);
            }
            
            @Override
            public void onCompleted() {
                updateDatDroppedThreads(data_list_result);
                boolean maybe_moved = data_list_cache_temp.size() == 0;
                executor_.submit(new Runnable() {
                    @Override
                    public void run() {
                        storeThreadListCache(board_data, data_list_cache_temp);
                    }
                });
                if (maybe_moved) {
                    callback.onThreadListFetchFailed(true);
                }
                else {
                    callback.onThreadListFetchCompleted();
                }
            }
            
            @Override
            public void onReceived(final List<ThreadData> data_list) {
                data_list_cache_temp.addAll(data_list);
                List<ThreadData> new_data_list = mergeThreadList(data_list, data_list_db);
                data_list_result.addAll(new_data_list);
                callback.onThreadListFetched(new_data_list);
            }
            
            @Override
            public void onInterrupted() {
                callback.onInterrupted();
            }
            
        });
        task.sendTo(agent_manager_.getMultiHttpAgent());
    }
    
    private List<ThreadData> mergeThreadList(final List<ThreadData> data_list, final List<ThreadData> data_list_db) {
        final List<ThreadData> data_list_result = new ArrayList<ThreadData>();
        final HashMap<Long, ThreadData> data_map_db = new HashMap<Long, ThreadData>();
        final List<ThreadData> new_data_list_db = new ArrayList<ThreadData>();
        for (ThreadData data : data_list_db) {
            data_map_db.put(data.thread_id_, data);
        }
        for (ThreadData data : data_list) {
            ThreadData db_data = data_map_db.get(data.thread_id_);
            if (db_data != null) {
                db_data.up2date(data);
                new_data_list_db.add(db_data);
            }
            else {
                data_list_result.add(data);
            }
        }
        agent_manager_.getDBAgent().updateThreadDataList(new_data_list_db, null);
        return data_list_result;
    }
    
    private void updateDatDroppedThreads(final List<ThreadData> data_list) {
        final List<ThreadData> dropped_list = new ArrayList<ThreadData>();
        for (ThreadData data : data_list) {
            if (data.sort_order_ == 0 && !data.is_dropped_) {
                data.is_dropped_ = true;
                dropped_list.add(data);
            }
        }
        
        if (dropped_list.size() > 0) {
            agent_manager_.getDBAgent().updateThreadDataList(dropped_list, null);
        }
    }
    
    public void fetchRecentList(final int recent_order, final RecentListFetchedCallback callback) {
        agent_manager_.getDBAgent().getRecentList(recent_order, new SQLiteAgent.DbResultReceiver() {
            @Override
            public void onQuery(Cursor cursor) {
                final ArrayList<ThreadData> data_list_db = new ArrayList<ThreadData>();
                if (cursor.getCount() != 0) {
                    cursor.moveToFirst();
                    while (true) {
                        data_list_db.add(ThreadData.factory(cursor));
                        if (cursor.moveToNext() == false) break;
                    }
                }
                cursor.close();
                if (callback != null) callback.onRecentListFetched(data_list_db);
            }
            
            @Override
            public void onError() {
                if (callback != null) callback.onRecentListFetched(new ArrayList<ThreadData>());
            }
            
        });
    }
    
    public void fetchSimilarThreadList(final BoardData board_data, final long thread_id, final String search_key,
            final boolean force_reload, final ThreadListFetchedCallback callback) {
        final List<ThreadData> result_list = new ArrayList<ThreadData>();
        reloadThreadList(board_data, force_reload, new ThreadListFetchedCallback() {
            
            @Override
            public void onThreadListFetchCompleted() {
                filterSimilarThreadList(result_list, thread_id, search_key, callback);
            }
            
            @Override
            public void onThreadListFetchedCache(List<ThreadData> dataList) {
                result_list.addAll(dataList);
            }
            
            @Override
            public void onThreadListFetched(List<ThreadData> dataList) {
                result_list.addAll(dataList);
            }
            
            @Override
            public void onInterrupted() {
                callback.onInterrupted();
            }
            
            @Override
            public void onThreadListFetchFailed(final boolean maybe_moved) {
                callback.onThreadListFetchFailed(maybe_moved);
            }
            
            @Override
            public void onConnectionOffline() {
                callback.onConnectionOffline();
            }
        });
    }
    
    private static class SimilarThreadItem {
        ThreadData thread_data_;
        int rate_;
        
        public SimilarThreadItem(ThreadData threadData, int rate) {
            super();
            thread_data_ = threadData;
            rate_ = rate;
        }
    }
    
    private void filterSimilarThreadList(List<ThreadData> data_list, final long thread_id, final String search_key,
            final ThreadListFetchedCallback callback) {
        LevenshteinDistanceCalc differ = new LevenshteinDistanceCalc();
        int max_rate = 0;
        
        TreeSet<SimilarThreadItem> ordered_list = new TreeSet<SimilarThreadItem>(new Comparator<SimilarThreadItem>() {
            @Override
            public int compare(SimilarThreadItem data1, SimilarThreadItem data2) {
                return data2.rate_ - data1.rate_;
            }
        });
        
        for (ThreadData data : data_list) {
            if (data.is_dropped_ || thread_id == data.thread_id_) continue;
            int rate = differ.similarity(search_key, data.thread_name_);
            if (rate > max_rate) max_rate = rate;
            // 一致度1/5以下は足切り
            if (rate > LevenshteinDistanceCalc.MAX_SIMILARITY_RATE / 5) {
                ordered_list.add(new SimilarThreadItem(data, rate));
            }
        }
        
        int border_rate = java.lang.Math.min(ordered_list.first().rate_ / 2,
                LevenshteinDistanceCalc.MAX_SIMILARITY_RATE / 3);
        
        List<ThreadData> result_list = new ArrayList<ThreadData>();
        for (SimilarThreadItem data : ordered_list) {
            if (data.rate_ >= border_rate) result_list.add(data.thread_data_);
        }
        
        callback.onThreadListFetchedCache(result_list);
        callback.onThreadListFetchCompleted();
    }
    
}
