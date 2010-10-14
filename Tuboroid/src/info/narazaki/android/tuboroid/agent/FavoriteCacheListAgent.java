package info.narazaki.android.tuboroid.agent;

import info.narazaki.android.lib.agent.db.SQLiteAgentBase;
import info.narazaki.android.lib.list.ListUtils;
import info.narazaki.android.lib.text.LevenshteinDistanceCalc;
import info.narazaki.android.tuboroid.TuboroidApplication;
import info.narazaki.android.tuboroid.agent.thread.DataFileAgent;
import info.narazaki.android.tuboroid.data.BoardData;
import info.narazaki.android.tuboroid.data.FavoriteItemBoardData;
import info.narazaki.android.tuboroid.data.FavoriteItemData;
import info.narazaki.android.tuboroid.data.FavoriteItemSearchKey;
import info.narazaki.android.tuboroid.data.FavoriteItemThreadData;
import info.narazaki.android.tuboroid.data.Find2chKeyData;
import info.narazaki.android.tuboroid.data.ThreadData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.database.Cursor;

public class FavoriteCacheListAgent {
    private static final String TAG = "FavoriteCacheListAgent";
    private static final String FAVORITE_FILE = "favorite.dat";
    
    public static final int ADD_THREAD_RULE_FOLLOW_SIMILAR = 0;
    public static final int ADD_THREAD_RULE_FOLLOW_BOARD = 1;
    public static final int ADD_THREAD_RULE_BOARD_GROUP = 2;
    public static final int ADD_THREAD_RULE_TAIL = 3;
    
    public static final int ADD_BOARD_RULE_BOARD_GROUP = 0;
    public static final int ADD_BOARD_RULE_TAIL = 1;
    
    private TuboroidAgentManager agent_manager_;
    
    private final ExecutorService executor_;
    private HashMap<String, Integer> order_map_;
    private HashMap<String, FavoriteItemData> item_map_;
    private ArrayList<FavoriteItemData> item_list_;
    
    public static interface NextFavoriteThreadFetchedCallback {
        public void onNextFavoriteThreadFetched(final int unread_thread_count, final ThreadData thread_data,
                final boolean current_has_unread);
    }
    
    public static interface FavoriteListFetchedCallback {
        public void onFavoriteListFetched(final ArrayList<FavoriteItemData> data_list);
    }
    
    public static interface FavoriteThreadListFetchedCallback {
        public void onThreadListFetched(final ArrayList<ThreadData> data_list);
    }
    
    public static interface UpdateCheckListFetchedCallback {
        public void onUpdateCheckListFetched(final List<BoardData> data_list);
    }
    
    public FavoriteCacheListAgent(TuboroidAgentManager agent_manager) {
        super();
        agent_manager_ = agent_manager;
        
        // 初回ロード
        order_map_ = new HashMap<String, Integer>();
        item_map_ = new HashMap<String, FavoriteItemData>();
        item_list_ = new ArrayList<FavoriteItemData>();
        executor_ = Executors.newSingleThreadExecutor();
        task_list_ = new LinkedList<Runnable>();
        fetchFavoriteList(null);
    }
    
    public LinkedList<Runnable> task_list_;
    
    private synchronized void pushTask(final Runnable runnable) {
        task_list_.addLast(runnable);
        if (task_list_.size() == 1) {
            executor_.submit(runnable);
        }
    }
    
    private synchronized void popTask() {
        task_list_.removeFirst();
        if (task_list_.size() == 0) return;
        Runnable runnable = task_list_.getFirst();
        executor_.submit(runnable);
    }
    
    public void fetchFavoriteList(final FavoriteListFetchedCallback callback) {
        pushTask(new Runnable() {
            @Override
            public void run() {
                loadOrderMap(callback);
            }
        });
    }
    
    private void loadOrderMap(final FavoriteListFetchedCallback callback) {
        agent_manager_.getFileAgent().readFile(
                TuboroidApplication.getInternalStoragePath(agent_manager_.getContext(), FAVORITE_FILE)
                        .getAbsolutePath(), new DataFileAgent.FileReadUTF8Callback() {
                    @Override
                    public void read(BufferedReader reader) {
                        if (reader != null) {
                            processOrderMap(reader, callback);
                        }
                        else {
                            loadBoardDataList(callback);
                        }
                    }
                });
    }
    
    private void processOrderMap(final BufferedReader reader, final FavoriteListFetchedCallback callback) {
        order_map_.clear();
        try {
            while (true) {
                String line = reader.readLine();
                if (line == null) break;
                
                ArrayList<String> tokens = ListUtils.split("<>", line);
                
                if (tokens.size() == 2) {
                    String uri = tokens.get(0);
                    String sort_order = tokens.get(1);
                    order_map_.put(uri, Integer.parseInt(sort_order));
                }
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
        loadBoardDataList(callback);
    }
    
    private void loadBoardDataList(final FavoriteListFetchedCallback callback) {
        agent_manager_.getDBAgent().getFavoriteBoardList(new SQLiteAgentBase.DbResultReceiver() {
            @Override
            public void onQuery(Cursor cursor) {
                final ArrayList<FavoriteItemData> new_item_list = new ArrayList<FavoriteItemData>();
                if (cursor.getCount() != 0) {
                    cursor.moveToFirst();
                    while (true) {
                        new_item_list.add(new FavoriteItemBoardData(cursor));
                        if (cursor.moveToNext() == false) break;
                    }
                }
                cursor.close();
                loadSearchKeyList(new_item_list, callback);
            }
            
            @Override
            public void onError() {
                if (callback != null) callback.onFavoriteListFetched(new ArrayList<FavoriteItemData>());
                popTask();
            }
        });
    }
    
    private void loadSearchKeyList(final ArrayList<FavoriteItemData> new_item_list,
            final FavoriteListFetchedCallback callback) {
        agent_manager_.getDBAgent().getFind2chKeyList(new SQLiteAgentBase.DbResultReceiver() {
            @Override
            public void onQuery(Cursor cursor) {
                if (cursor.getCount() != 0) {
                    cursor.moveToFirst();
                    while (true) {
                        new_item_list.add(new FavoriteItemSearchKey(cursor));
                        if (cursor.moveToNext() == false) break;
                    }
                }
                cursor.close();
                loadThreadDataList(new_item_list, callback);
            }
            
            @Override
            public void onError() {
                if (callback != null) callback.onFavoriteListFetched(new ArrayList<FavoriteItemData>());
                popTask();
            }
            
        });
    }
    
    private void loadThreadDataList(final ArrayList<FavoriteItemData> new_item_list,
            final FavoriteListFetchedCallback callback) {
        agent_manager_.getDBAgent().getFavoriteThreadList(new SQLiteAgentBase.DbResultReceiver() {
            @Override
            public void onQuery(Cursor cursor) {
                if (cursor.getCount() != 0) {
                    cursor.moveToFirst();
                    while (true) {
                        new_item_list.add(new FavoriteItemThreadData(cursor));
                        if (cursor.moveToNext() == false) break;
                    }
                }
                cursor.close();
                processNewDataList(new_item_list, callback);
            }
            
            @Override
            public void onError() {
                if (callback != null) callback.onFavoriteListFetched(new ArrayList<FavoriteItemData>());
                popTask();
            }
            
        });
    }
    
    private void processNewDataList(final ArrayList<FavoriteItemData> new_item_list,
            final FavoriteListFetchedCallback callback) {
        mergeAndSetOrderMap(new_item_list);
        if (callback != null) callback.onFavoriteListFetched(new_item_list);
        popTask();
    }
    
    private void mergeAndSetOrderMap(final ArrayList<FavoriteItemData> new_item_list) {
        for (FavoriteItemData data : new_item_list) {
            String uri = data.getURI();
            Integer order_id = order_map_.get(uri);
            if (order_id != null) data.order_id_ = order_id;
        }
        sortDataList(new_item_list);
        setDataList(new_item_list);
    }
    
    private void sortDataList(final ArrayList<FavoriteItemData> new_item_list) {
        Collections.sort(new_item_list, new Comparator<FavoriteItemData>() {
            @Override
            public int compare(FavoriteItemData object1, FavoriteItemData object2) {
                if (object1.order_id_ == 0) {
                    return object2.order_id_ == 0 ? 0 : 1;
                }
                return object1.order_id_ - object2.order_id_;
            }
        });
        reorderDataList(new_item_list);
    }
    
    private void reorderDataList(final ArrayList<FavoriteItemData> new_item_list) {
        int i = 1;
        for (FavoriteItemData data : new_item_list) {
            data.order_id_ = i;
            i++;
        }
        order_map_.clear();
        for (FavoriteItemData data : new_item_list) {
            String uri = data.getURI();
            order_map_.put(uri, data.order_id_);
        }
    }
    
    public void setNewFavoriteListOrder(final ArrayList<FavoriteItemData> new_item_list, final Runnable callback) {
        pushTask(new Runnable() {
            @Override
            public void run() {
                setNewFavoriteListOrderImpl(new_item_list, callback);
            }
        });
    }
    
    private void setNewFavoriteListOrderImpl(final ArrayList<FavoriteItemData> new_item_list, final Runnable callback) {
        int i = 1;
        for (FavoriteItemData data : new_item_list) {
            data.order_id_ = i;
            i++;
        }
        order_map_ = new HashMap<String, Integer>();
        for (FavoriteItemData data : new_item_list) {
            String uri = data.getURI();
            order_map_.put(uri, data.order_id_);
        }
        setDataList(new_item_list);
        saveOrderMap(callback);
    }
    
    private void saveOrderMap(final Runnable callback) {
        agent_manager_.getFileAgent().writeFile(
                TuboroidApplication.getInternalStoragePath(agent_manager_.getContext(), FAVORITE_FILE)
                        .getAbsolutePath(), new DataFileAgent.FileWriteUTF8StreamCallback() {
                    
                    @Override
                    public void write(Writer writer) throws IOException {
                        for (Entry<String, Integer> data : order_map_.entrySet()) {
                            writer.append(data.getKey()).append("<>");
                            writer.append(String.valueOf(data.getValue()));
                            writer.append("\n");
                        }
                    }
                }, false, true, true, new DataFileAgent.FileWroteCallback() {
                    @Override
                    public void onFileWrote(boolean succeeded) {
                        if (callback != null) callback.run();
                        popTask();
                    }
                });
    }
    
    private void setDataList(final ArrayList<FavoriteItemData> new_item_list) {
        item_map_ = new HashMap<String, FavoriteItemData>();
        item_list_ = new ArrayList<FavoriteItemData>();
        item_list_.addAll(new_item_list);
        for (FavoriteItemData data : item_list_) {
            String uri = data.getURI();
            item_map_.put(uri, data);
        }
    }
    
    public void fetchNextFavoriteThread(final ThreadData thread_data, final NextFavoriteThreadFetchedCallback callback) {
        fetchFavoriteList(new FavoriteListFetchedCallback() {
            @Override
            public void onFavoriteListFetched(ArrayList<FavoriteItemData> dataList) {
                fetchNextFavoriteThreadImpl(thread_data, dataList, callback);
            }
        });
    }
    
    private void fetchNextFavoriteThreadImpl(final ThreadData thread_data, ArrayList<FavoriteItemData> data_list,
            final NextFavoriteThreadFetchedCallback callback) {
        boolean found_self = false;
        boolean current_has_unread = false;
        boolean found_target = false;
        String thread_uri = thread_data.getThreadURI();
        ThreadData first_unread = null;
        int unread_thread_count = 0;
        for (FavoriteItemData data : data_list) {
            if (data.isThread()) {
                ThreadData target = data.getThreadData();
                if (!found_self && target.getThreadURI().equals(thread_uri)) {
                    found_self = true;
                    current_has_unread = target.hasUnread();
                }
                else if (target.hasUnread()) {
                    unread_thread_count++;
                    if (found_target) {
                    }
                    else if (found_self) {
                        first_unread = target;
                        found_target = true;
                    }
                    else if (first_unread == null) {
                        first_unread = target;
                    }
                }
            }
        }
        callback.onNextFavoriteThreadFetched(unread_thread_count, first_unread, current_has_unread);
    }
    
    public void deleteFavoriteList(final ArrayList<FavoriteItemData> delete_list, final Runnable callback) {
        pushTask(new Runnable() {
            @Override
            public void run() {
                deleteFavoriteListImpl(delete_list, callback);
            }
        });
    }
    
    private void deleteFavoriteListImpl(final ArrayList<FavoriteItemData> delete_list, final Runnable callback) {
        final ArrayList<BoardData> board_list = new ArrayList<BoardData>();
        final ArrayList<ThreadData> thread_list = new ArrayList<ThreadData>();
        final ArrayList<Find2chKeyData> search_key_list = new ArrayList<Find2chKeyData>();
        for (FavoriteItemData data : delete_list) {
            if (data.isBoard()) {
                BoardData board_data = data.getBoardData();
                board_data.is_favorite_ = false;
                board_list.add(board_data);
            }
            else if (data.isThread()) {
                ThreadData thread_data = data.getThreadData();
                thread_data.is_favorite_ = false;
                thread_list.add(thread_data);
            }
            else if (data.isSearchKey()) {
                Find2chKeyData search_key_data = data.getSearchKey();
                search_key_data.is_favorite_ = false;
                search_key_list.add(search_key_data);
            }
        }
        Runnable callback_with_pop = new Runnable() {
            @Override
            public void run() {
                if (callback != null) callback.run();
                popTask();
            }
        };
        agent_manager_.getDBAgent().updateFavoriteList(board_list, thread_list, search_key_list,
                new SQLiteAgentBase.DbTransactionDelegate(callback_with_pop));
    }
    
    public void addFavorite(final BoardData board_data, final int rule, final Runnable callback) {
        board_data.is_favorite_ = true;
        agent_manager_.getDBAgent().updateBoardDataFavorite(board_data, null);
        addFavorite(new FavoriteItemBoardData(board_data), rule, callback);
    }
    
    public void delFavorite(final BoardData board_data, final Runnable callback) {
        board_data.is_favorite_ = false;
        agent_manager_.getDBAgent().updateBoardDataFavorite(board_data, null);
        String uri = board_data.getSubjectsURI();
        delFavorite(uri, callback);
    }
    
    public void addFavorite(final ThreadData thread_data, final int rule, final Runnable callback) {
        thread_data.is_favorite_ = true;
        agent_manager_.getDBAgent().updateThreadDataFavorite(thread_data, null);
        addFavorite(new FavoriteItemThreadData(thread_data), rule, callback);
    }
    
    public void delFavorite(final ThreadData thread_data, final Runnable callback) {
        thread_data.is_favorite_ = false;
        agent_manager_.getDBAgent().updateThreadDataFavorite(thread_data, null);
        String uri = thread_data.getThreadURI();
        delFavorite(uri, callback);
    }
    
    public void addFavorite(final Find2chKeyData key_data, final int rule, final Runnable callback) {
        key_data.is_favorite_ = true;
        agent_manager_.getDBAgent().setFind2chKeyData(key_data);
        addFavorite(new FavoriteItemSearchKey(key_data), rule, callback);
    }
    
    public void delFavorite(final Find2chKeyData key_data, final Runnable callback) {
        key_data.is_favorite_ = false;
        agent_manager_.getDBAgent().deleteFind2chKeyData(key_data.keyword_);
        String uri = key_data.getSearchURI();
        delFavorite(uri, callback);
    }
    
    private void delFavorite(final String uri, final Runnable callback) {
        pushTask(new Runnable() {
            @Override
            public void run() {
                delFavoriteImpl(uri, callback);
            }
        });
    }
    
    private void delFavoriteImpl(final String uri, final Runnable callback) {
        FavoriteItemData data = item_map_.remove(uri);
        if (data != null) item_list_.remove(data);
        order_map_.remove(uri);
        saveOrderMap(callback);
    }
    
    private void addFavorite(final FavoriteItemData target, final int rule, final Runnable callback) {
        pushTask(new Runnable() {
            @Override
            public void run() {
                addFavoriteImpl(target, rule, callback);
            }
        });
    }
    
    private void addFavoriteImpl(final FavoriteItemData target, final int rule, final Runnable callback) {
        String uri = target.getURI();
        if (item_map_.containsKey(uri)) {
            if (callback != null) callback.run();
            popTask();
            return;
        }
        target.order_id_ = 0;
        item_map_.put(uri, target);
        if (target.isBoard()) {
            switch (rule) {
            case ADD_BOARD_RULE_BOARD_GROUP:
            case ADD_BOARD_RULE_TAIL:
            default:
                addFavoriteTail(target);
                break;
            }
        }
        else if (target.isThread()) {
            switch (rule) {
            case ADD_THREAD_RULE_FOLLOW_SIMILAR:
                addFavoriteSimilar(target);
                break;
            case ADD_THREAD_RULE_FOLLOW_BOARD:
                addFavoriteThreadFollowBoard(target);
                break;
            case ADD_THREAD_RULE_BOARD_GROUP:
                addFavoriteThreadAppendBoardGroup(target);
                break;
            case ADD_THREAD_RULE_TAIL:
            default:
                addFavoriteTail(target);
                break;
            }
        }
        else if (target.isSearchKey()) {
            addFavoriteTail(target);
        }
        saveOrderMap(callback);
    }
    
    private void addFavoriteTail(final FavoriteItemData target) {
        item_list_.add(target);
        sortDataList(item_list_);
    }
    
    private void addFavoriteThreadFollowBoard(final FavoriteItemData target) {
        if (item_list_.size() == 0) {
            addFavoriteTail(target);
            return;
        }
        ThreadData target_data = target.getThreadData();
        
        int same_board = -1;
        int first_same_board_thread = -1;
        int i = 0;
        for (FavoriteItemData data : item_list_) {
            if (data.isBoard() && data.getBoardData().isSameBoard(target_data)) {
                same_board = i;
                break;
            }
            else if (data.isThread() && data.getThreadData().isSameBoard(target_data)) {
                first_same_board_thread = i;
            }
            i++;
        }
        
        if (same_board != -1) {
            if (same_board + 1 <= item_list_.size()) {
                item_list_.add(same_board + 1, target);
            }
            else {
                addFavoriteTail(target);
                return;
            }
        }
        else if (first_same_board_thread != -1) {
            item_list_.add(first_same_board_thread, target);
        }
        else {
            addFavoriteTail(target);
            return;
        }
        sortDataList(item_list_);
    }
    
    private void addFavoriteThreadAppendBoardGroup(final FavoriteItemData target) {
        if (item_list_.size() == 0) {
            addFavoriteTail(target);
            return;
        }
        ThreadData target_data = target.getThreadData();
        
        boolean same_board_found = false;
        int last_same_board_thread = -1;
        int i = 0;
        for (FavoriteItemData data : item_list_) {
            if (data.isBoard() && data.getBoardData().isSameBoard(target_data)) {
                same_board_found = true;
                last_same_board_thread = i;
            }
            else if (data.isThread()) {
                if (data.getThreadData().isSameBoard(target_data)) {
                    last_same_board_thread = i;
                }
                else if (same_board_found) {
                    break;
                }
            }
            i++;
        }
        
        if (last_same_board_thread != -1) {
            if (last_same_board_thread + 1 <= item_list_.size()) {
                item_list_.add(last_same_board_thread + 1, target);
                sortDataList(item_list_);
                return;
            }
        }
        addFavoriteTail(target);
        return;
    }
    
    private void addFavoriteSimilar(final FavoriteItemData target) {
        if (item_list_.size() == 0) {
            addFavoriteTail(target);
            return;
        }
        ThreadData target_data = target.getThreadData();
        
        LevenshteinDistanceCalc differ = new LevenshteinDistanceCalc();
        int max = 0;
        int max_index = -1;
        boolean max_is_same_board = false;
        int i = 0;
        for (FavoriteItemData data : item_list_) {
            if (data.isThread()) {
                ThreadData thread_data = data.getThreadData();
                int rate = differ.similarity(thread_data.thread_name_, target_data.thread_name_);
                if (rate > LevenshteinDistanceCalc.MAX_SIMILARITY_RATE / 3 && rate >= max) {
                    boolean same_board = thread_data.isSameBoard(target_data);
                    if (!max_is_same_board || same_board) {
                        max = rate;
                        max_index = i;
                        max_is_same_board = same_board;
                    }
                }
            }
            else if (data.isBoard()) {
                BoardData board_data = data.getBoardData();
                boolean same_board = board_data.isSameBoard(target_data);
                if (!max_is_same_board || same_board) {
                    max = 0;
                    max_index = i;
                    max_is_same_board = same_board;
                }
            }
            i++;
        }
        if (max_index == -1) {
            addFavoriteThreadAppendBoardGroup(target);
            return;
        }
        else if (max_index + 1 >= item_list_.size()) {
            addFavoriteTail(target);
            return;
        }
        else {
            item_list_.add(max_index + 1, target);
        }
        reorderDataList(item_list_);
    }
}
