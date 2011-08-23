package info.narazaki.android.tuboroid.agent;

import info.narazaki.android.lib.activity.base.NSimpleExpandableListActivity;
import info.narazaki.android.lib.activity.base.NSimpleExpandableListActivity.StatData;
import info.narazaki.android.lib.agent.db.SQLiteAgentBase;
import info.narazaki.android.lib.agent.db.SQLiteAgentBase.DbTransaction;
import info.narazaki.android.lib.list.ListUtils;
import info.narazaki.android.lib.text.TextUtils;
import info.narazaki.android.tuboroid.R;
import info.narazaki.android.tuboroid.agent.task.HttpGetBoardDataTask;
import info.narazaki.android.tuboroid.agent.task.HttpGetBoardListTask;
import info.narazaki.android.tuboroid.agent.thread.DataFileAgent;
import info.narazaki.android.tuboroid.agent.thread.SQLiteAgent;
import info.narazaki.android.tuboroid.data.BoardData;
import info.narazaki.android.tuboroid.data.BoardIdentifier;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;

public class BoardListAgent {
    private static final String TAG = "BoardListAgent";
    
    public static interface BoardListFetchedCallback {
        public void onBoardListFetched(final ArrayList<String> board_groups,
                final ArrayList<ArrayList<BoardData>> board_list, NSimpleExpandableListActivity.StatData board_list_stat);
        
        public void onBoardListConnectionFailed();
        
        public void onConnectionOffline();
    }
    
    public static interface BoardFileFetchedCallback {
        public void onBoardFileFetched(final List<BoardData> data_list);
        
        public void onBoardListConnectionFailed();
        
        public void onConnectionOffline();
    }
    
    public static interface BoardFetchedCallback {
        public void onBoardFetched(final BoardData new_board_data);
    }
    
    private TuboroidAgentManager agent_manager_;
    
    private volatile boolean data_loaded_;
    
    private final String board_list_filepath_;
    private final String board_list_stat_filepath_;
    private volatile NSimpleExpandableListActivity.StatData board_list_stat_;
    
    public ArrayList<String> board_groups_;
    public ArrayList<ArrayList<BoardData>> board_list_;
    public HashMap<BoardIdentifier, BoardData> board_map_;
    
    private final ExecutorService executor_;
    
    private LinkedList<Runnable> task_queue_;
    
    public BoardListAgent(TuboroidAgentManager agent_manager) {
        super();
        agent_manager_ = agent_manager;
        
        data_loaded_ = false;
        board_list_filepath_ = createBoardListFilePath();
        board_list_stat_filepath_ = createBoardListStatFilePath();
        board_groups_ = new ArrayList<String>();
        board_list_ = new ArrayList<ArrayList<BoardData>>();
        board_map_ = new HashMap<BoardIdentifier, BoardData>();
        
        board_list_stat_ = null;
        
        executor_ = Executors.newSingleThreadExecutor();
        task_queue_ = new LinkedList<Runnable>();
    }
    
    private void pushTask(final Runnable runnable) {
        if (!data_loaded_) {
            execPushTask(new Runnable() {
                @Override
                public void run() {
                    execFetchBoardList(false, false, null);
                }
            });
        }
        execPushTask(runnable);
    }
    
    private synchronized void execPushTask(final Runnable runnable) {
        task_queue_.addLast(runnable);
        
        if (task_queue_.size() == 1) {
            executor_.submit(runnable);
        }
    }
    
    private synchronized void popTask() {
        if (task_queue_.size() == 0) return;
        task_queue_.removeFirst();
        
        if (task_queue_.size() > 0) {
            final Runnable next = task_queue_.getFirst();
            executor_.submit(next);
        }
    }
    
    public boolean fetchBoardList(final boolean no_cache, final boolean force_reload,
            final BoardListFetchedCallback callback) {
        pushTask(new Runnable() {
            @Override
            public void run() {
                execFetchBoardList(no_cache, force_reload, callback);
            }
        });
        
        return !no_cache && !force_reload && data_loaded_;
    }
    
    private void execFetchBoardList(final boolean no_cache, final boolean force_reload,
            final BoardListFetchedCallback callback_orig) {
        final BoardListFetchedCallback callback = new BoardListFetchedCallback() {
            @Override
            public void onBoardListConnectionFailed() {
                if (callback_orig != null) callback_orig.onBoardListConnectionFailed();
                popTask();
            }
            
            @Override
            public void onBoardListFetched(ArrayList<String> boardGroups, ArrayList<ArrayList<BoardData>> boardList,
                    StatData boardListStat) {
                if (callback_orig != null) callback_orig.onBoardListFetched(boardGroups, boardList, boardListStat);
                popTask();
            }
            
            @Override
            public void onConnectionOffline() {
                if (callback_orig != null) callback_orig.onConnectionOffline();
                popTask();
            }
        };
        
        if (!no_cache && !force_reload && data_loaded_) {
            returnBoardList(true, callback);
        }
        
        final BoardFileFetchedCallback file_fetched_callback = new BoardFileFetchedCallback() {
            @Override
            public void onBoardFileFetched(List<BoardData> data_list) {
                if (data_list == null) {
                    returnBoardList(true, callback);
                }
                else {
                    mergeCachedBoardList(data_list, callback);
                }
            }
            
            @Override
            public void onBoardListConnectionFailed() {
                callback.onBoardListConnectionFailed();
            }
            
            @Override
            public void onConnectionOffline() {
                callback.onConnectionOffline();
            }
        };
        
        if (force_reload) {
            reloadBBSMenu(file_fetched_callback);
        }
        else {
            loadBoardListFile(true, file_fetched_callback);
        }
    }
    
    private void returnBoardList(final boolean can_retry, final BoardListFetchedCallback callback) {
        if (board_list_stat_ == null && can_retry) {
            loadBoardListStat(new Runnable() {
                @Override
                public void run() {
                    returnBoardList(false, callback);
                }
            });
        }
        else {
            callback.onBoardListFetched(board_groups_, board_list_, board_list_stat_);
        }
    }
    
    private synchronized void setBoardCache(ArrayList<String> board_groups, ArrayList<ArrayList<BoardData>> board_list,
            HashMap<BoardIdentifier, BoardData> board_map) {
        data_loaded_ = true;
        board_groups_ = board_groups;
        board_list_ = board_list;
        board_map_ = board_map;
    }
    
    private void loadBoardListFile(final boolean can_reload, final BoardFileFetchedCallback callback) {
        agent_manager_.getFileAgent().readFile(board_list_filepath_, new DataFileAgent.FileReadUTF8Callback() {
            @Override
            public void read(final BufferedReader reader) {
                try {
                    if (reader == null) {
                        if (can_reload) {
                            reloadBBSMenu(callback);
                            return;
                        }
                        else {
                            callback.onBoardFileFetched(null);
                            return;
                        }
                    }
                    List<BoardData> data_list = new LinkedList<BoardData>();
                    int order_id = 1;
                    while (true) {
                        String line;
                        line = reader.readLine();
                        if (line == null) break;
                        String[] line_splitted = ListUtils.split("\t", line);
                        if (line_splitted.length == 4) {
                            BoardData data = BoardData.factory(order_id, line_splitted[0], line_splitted[1],
                                    new BoardIdentifier(line_splitted[2], line_splitted[3], 0, 0));
                            data_list.add(data);
                            order_id++;
                        }
                    }
                    callback.onBoardFileFetched(data_list);
                }
                catch (IOException e) {
                    e.printStackTrace();
                    callback.onBoardFileFetched(null);
                }
            }
        });
    }
    
    private void reloadBBSMenu(final BoardFileFetchedCallback callback) {
        // Offlineチェック
        if (!agent_manager_.isOnline()) {
            callback.onConnectionOffline();
            return;
        }
        
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(agent_manager_.getContext()
                .getApplicationContext());
        String default_bbsmenu_url = agent_manager_.getContext().getResources()
                .getStringArray(R.array.bbsmenu_url_values)[0];
        String bbsmenu_url = pref.getString("pref_bbsmenu_url_list", default_bbsmenu_url);
        
        HttpGetBoardListTask task = new HttpGetBoardListTask(bbsmenu_url, new HttpGetBoardListTask.Callback() {
            @Override
            public void onConnectionFailed() {
                callback.onBoardListConnectionFailed();
                loadBoardListFile(false, callback);
            }
            
            @Override
            public void onBoardListReceived(List<BoardData> data_list) {
                saveBoardListFile(data_list, callback);
            }
        });
        agent_manager_.getSingleHttpAgent().send(task);
    }
    
    private void saveBoardListFile(final List<BoardData> data_list, final BoardFileFetchedCallback callback) {
        agent_manager_.getFileAgent().writeFile(board_list_filepath_, new DataFileAgent.FileWriteUTF8StreamCallback() {
            @Override
            public void write(Writer writer) throws IOException {
                for (BoardData data : data_list) {
                    writer.append(data.board_name_);
                    writer.append('\t');
                    writer.append(data.board_category_);
                    writer.append('\t');
                    writer.append(data.server_def_.board_server_);
                    writer.append('\t');
                    writer.append(data.server_def_.board_tag_);
                    writer.append('\n');
                }
            }
        }, false, true, true, new DataFileAgent.FileWroteCallback() {
            @Override
            public void onFileWrote(boolean succeeded) {
                migrateBoardList(data_list, callback);
            }
        });
    }
    
    private void migrateBoardList(final List<BoardData> data_list, final BoardFileFetchedCallback callback) {
        ArrayList<BoardData> data_list_temp = new ArrayList<BoardData>(data_list);
        migrateBoardListImpl(data_list_temp, callback);
    }
    
    private void migrateBoardListImpl(final List<BoardData> data_list, final BoardFileFetchedCallback callback) {
        if (data_list.size() == 0) {
            loadBoardListFile(false, callback);
            return;
        }
        
        BoardData target_data = data_list.remove(0);
        agent_manager_.getDBAgent().migrateBoardData(target_data, new SQLiteAgent.MigrateBoardCallback() {
            
            @Override
            public void onNoMigrated() {
                migrateBoardListImpl(data_list, callback);
            }
            
            @Override
            public void onMigrated(BoardData fromData, BoardData toData) {
                LinkedList<String> from_dirs = fromData.getLocalDatDir(agent_manager_.getContext());
                LinkedList<String> to_dirs = toData.getLocalDatDir(agent_manager_.getContext());
                
                if (from_dirs.size() != to_dirs.size()) {
                    migrateBoardListImpl(data_list, callback);
                    return;
                }
                
                String from_dir = null;
                String to_dir = null;
                
                while (from_dirs.size() != 0) {
                    String from_dir_temp = from_dirs.removeFirst();
                    File from_dir_file = new File(from_dir_temp);
                    String to_dir_temp = to_dirs.removeFirst();
                    if (from_dir_file.exists() && from_dir_file.isDirectory()) {
                        from_dir = from_dir_temp;
                        to_dir = to_dir_temp;
                        break;
                    }
                }
                if (from_dir == null) {
                    migrateBoardListImpl(data_list, callback);
                    return;
                }
                
                agent_manager_.getFileAgent().moveFilesInDirectory(from_dir, to_dir, new Runnable() {
                    @Override
                    public void run() {
                        migrateBoardListImpl(data_list, callback);
                    }
                });
            }
        });
    }
    
    private String createBoardListFilePath() {
        String filename = agent_manager_.getContext().getDir("board_list", Context.MODE_PRIVATE).getAbsolutePath();
        filename += File.separator;
        filename += agent_manager_.getContext().getResources().getString(R.string.const_filename_bbsmenu_dat);
        
        return filename;
    }
    
    private String createBoardListStatFilePath() {
        String filename = agent_manager_.getContext().getDir("board_list", Context.MODE_PRIVATE).getAbsolutePath();
        filename += File.separator;
        filename += agent_manager_.getContext().getResources().getString(R.string.const_filename_bbsmenu_stat);
        
        return filename;
    }
    
    private void mergeCachedBoardList(List<BoardData> data_list, final BoardListFetchedCallback callback) {
        final ArrayList<String> board_groups = new ArrayList<String>();
        final ArrayList<ArrayList<BoardData>> board_list = new ArrayList<ArrayList<BoardData>>();
        final HashMap<BoardIdentifier, BoardData> board_map = new HashMap<BoardIdentifier, BoardData>();
        
        String current_group = null;
        ArrayList<BoardData> current_list = null;
        for (BoardData data : data_list) {
            if (current_group == null || !data.board_category_.equals(current_group)) {
                current_group = data.board_category_;
                board_groups.add(current_group);
                current_list = new ArrayList<BoardData>();
                board_list.add(current_list);
            }
            current_list.add(data);
            board_map.put(data.server_def_, data);
        }
        
        // DBを引いて結合する
        agent_manager_.getDBAgent().getBoardList(new SQLiteAgentBase.DbResultReceiver() {
            @Override
            public void onQuery(Cursor cursor) {
                final List<BoardData> data_list_db = new LinkedList<BoardData>();
                if (cursor.getCount() != 0) {
                    cursor.moveToFirst();
                    while (true) {
                        data_list_db.add(BoardData.factory(cursor));
                        if (cursor.moveToNext() == false) break;
                    }
                }
                cursor.close();
                
                Collections.sort(data_list_db, new BoardData.comparator());
                
                // DBから引いたものをマージする(きたない……)
                String current_group = null;
                ArrayList<BoardData> current_list = null;
                for (BoardData data : data_list_db) {
                    BoardData target = board_map.get(data.server_def_);
                    if (target != null) {
                        // DBにもbbsmenuにもある。bbsmenu側優先でマージする
                        target.importData(data);
                    }
                    else {
                        if (current_group == null || !data.board_category_.equals(current_group)) {
                            current_group = data.board_category_;
                            board_groups.add(current_group);
                            current_list = new ArrayList<BoardData>();
                            board_list.add(current_list);
                        }
                        current_list.add(data);
                        board_map.put(data.server_def_, data);
                    }
                }
                setBoardCache(board_groups, board_list, board_map);
                returnBoardList(true, callback);
            }
            
            @Override
            public void onError() {
                returnBoardList(true, callback);
            }
        });
        
    }
    
    // 板リストの状態
    private void loadBoardListStat(final Runnable callback) {
        board_list_stat_ = new NSimpleExpandableListActivity.StatData();
        agent_manager_.getFileAgent().readFile(board_list_stat_filepath_, new DataFileAgent.FileReadCallback() {
            @Override
            public void read(final InputStream stream) {
                try {
                    if (stream == null) {
                        callback.run();
                        return;
                    }
                    BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"), 8 * 1024);
                    String line;
                    line = reader.readLine();
                    if (line == null) throw new IOException();
                    board_list_stat_.pos_data_.packed_pos_ = TextUtils.parseLong(line);
                    
                    line = reader.readLine();
                    if (line == null) throw new IOException();
                    board_list_stat_.pos_data_.y_ = TextUtils.parseInt(line);
                    
                    while (true) {
                        line = reader.readLine();
                        if (line == null) break;
                        board_list_stat_.expand_stat_list_.add(line.equals("1"));
                    }
                }
                catch (IOException e) {
                }
                catch (NumberFormatException e) {
                }
                callback.run();
            }
        });
        
    }
    
    public void saveBoardListStat(final NSimpleExpandableListActivity.StatData stat, final Runnable callback) {
        pushTask(new Runnable() {
            @Override
            public void run() {
                execSaveBoardListStat(stat, callback);
                popTask();
            }
        });
    }
    
    private void execSaveBoardListStat(final NSimpleExpandableListActivity.StatData stat, final Runnable callback) {
        board_list_stat_ = stat;
        
        agent_manager_.getFileAgent().writeFile(board_list_stat_filepath_,
                new DataFileAgent.FileWriteUTF8StreamCallback() {
                    @Override
                    public void write(Writer writer) throws IOException {
                        writer.append(String.valueOf(board_list_stat_.pos_data_.packed_pos_));
                        writer.append('\n');
                        writer.append(String.valueOf(board_list_stat_.pos_data_.y_));
                        writer.append('\n');
                        for (Boolean data : board_list_stat_.expand_stat_list_) {
                            writer.append(data ? "1" : "0");
                            writer.append('\n');
                        }
                    }
                }, false, true, true, new DataFileAgent.FileWroteCallback() {
                    @Override
                    public void onFileWrote(boolean succeeded) {
                        if (callback != null) callback.run();
                    }
                });
    }
    
    // 取得メソッド
    private BoardData getBoardData(BoardIdentifier server_def) {
        if (server_def.board_server_.length() == 0 || server_def.board_tag_.length() == 0) return null;
        
        BoardData data = board_map_.get(server_def);
        if (data != null) return data;
        
        return BoardData.factory(0, "", "", server_def);
    }
    
    // 板を取得、無ければ新しく作る
    public BoardData getBoardData(final Uri uri, final boolean is_new_external, final BoardFetchedCallback callback) {
        final BoardIdentifier server_def = BoardData.factoryBoardServer(uri);
        final BoardData board_data = BoardData.factory(0, "", "", server_def);
        
        pushTask(new Runnable() {
            @Override
            public void run() {
                execGetBoardData(server_def, is_new_external, new BoardFetchedCallback() {
                    @Override
                    public void onBoardFetched(BoardData newBoardData) {
                        if (callback != null) callback.onBoardFetched(newBoardData);
                        popTask();
                    }
                });
            }
        });
        return board_data;
    }
    
    private void execGetBoardData(final BoardIdentifier server_def, final boolean is_new_external,
            final BoardFetchedCallback callback) {
        final BoardData board_data = getBoardData(server_def);
        if (is_new_external) board_data.is_external_ = true;
        
        agent_manager_.getDBAgent().insertBoardData(board_data, callback, new Runnable() {
            @Override
            public void run() {
                HttpGetBoardDataTask task = board_data.factoryHttpGetBoardDataTask(new HttpGetBoardDataTask.Callback() {
                    @Override
                    public void onFailed() {
                        if (board_data.board_name_.length() == 0) {
                            deleteBoard(board_data, null);
                        }
                        callback.onBoardFetched(board_data);
                    }
                    
                    @Override
                    public void onCompleted(final BoardData newBoardData) {
                        agent_manager_.getDBAgent().updateBoardData(newBoardData, new DbTransaction() {
                            @Override
                            public void run() {
                                execFetchBoardList(true, false, new BoardListFetchedCallback() {
                                    @Override
                                    public void onBoardListFetched(ArrayList<String> boardGroups,
                                            ArrayList<ArrayList<BoardData>> boardList,
                                            NSimpleExpandableListActivity.StatData board_list_stat) {
                                        callback.onBoardFetched(newBoardData);
                                    }
                                    
                                    @Override
                                    public void onBoardListConnectionFailed() {
                                        callback.onBoardFetched(newBoardData);
                                    }
                                    
                                    @Override
                                    public void onConnectionOffline() {
                                        callback.onBoardFetched(newBoardData);
                                    }
                                });
                            }
                            
                            @Override
                            public void onError() {
                                callback.onBoardFetched(newBoardData);
                            }
                        });
                    }
                });
                task.sendTo(agent_manager_.getSingleHttpAgent());
            }
        }, new Runnable() {
            @Override
            public void run() {
                callback.onBoardFetched(board_data);
            }
        });
    }
    
    public void deleteBoard(final BoardData board_data, final Runnable callback) {
        pushTask(new Runnable() {
            @Override
            public void run() {
                execDeleteBoard(board_data, new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) callback.run();
                        popTask();
                    }
                });
            }
        });
    }
    
    private void execDeleteBoard(final BoardData board_data, final Runnable callback) {
        LinkedList<String> list = board_data.getLocalDatDir(agent_manager_.getContext());
        
        if (list.size() == 0) {
            agent_manager_.getDBAgent()
                    .deleteBoardData(board_data, new SQLiteAgentBase.DbTransactionDelegate(callback));
            return;
        }
        
        agent_manager_.getFileAgent().deleteFiles(list, new DataFileAgent.FileWroteCallback() {
            @Override
            public void onFileWrote(boolean succeeded) {
                agent_manager_.getDBAgent().deleteBoardData(board_data,
                        new SQLiteAgentBase.DbTransactionDelegate(callback));
            }
        });
    }
    
    public void updateBoard(final BoardData board_data, final Runnable callback) {
        pushTask(new Runnable() {
            @Override
            public void run() {
                execUpdateBoard(board_data, new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) callback.run();
                        popTask();
                    }
                });
            }
        });
    }
    
    private void execUpdateBoard(final BoardData board_data, final Runnable callback) {
        agent_manager_.getDBAgent().updateBoardData(board_data, new SQLiteAgentBase.DbTransactionDelegate(callback));
    }
    
}
