package info.narazaki.android.tuboroid.agent;

import info.narazaki.android.lib.activity.base.NSimpleExpandableListActivity;
import info.narazaki.android.lib.agent.db.SQLiteAgentBase;
import info.narazaki.android.tuboroid.R;
import info.narazaki.android.tuboroid.TuboroidApplication.AccountPref;
import info.narazaki.android.tuboroid.agent.FavoriteCacheListAgent.FavoriteListFetchedCallback;
import info.narazaki.android.tuboroid.agent.FavoriteCacheListAgent.FavoriteThreadListFetchedCallback;
import info.narazaki.android.tuboroid.agent.FavoriteCacheListAgent.NextFavoriteThreadFetchedCallback;
import info.narazaki.android.tuboroid.agent.FavoriteCacheListAgent.UpdateCheckListFetchedCallback;
import info.narazaki.android.tuboroid.agent.PostEntryTask.OnPostEntryCallback;
import info.narazaki.android.tuboroid.agent.ThreadListAgent.RecentListFetchedCallback;
import info.narazaki.android.tuboroid.agent.ThreadListAgent.ThreadListFetchedCallback;
import info.narazaki.android.tuboroid.agent.thread.DataFileAgent;
import info.narazaki.android.tuboroid.agent.thread.SQLiteAgent;
import info.narazaki.android.tuboroid.data.BoardData;
import info.narazaki.android.tuboroid.data.FavoriteItemData;
import info.narazaki.android.tuboroid.data.Find2chKeyData;
import info.narazaki.android.tuboroid.data.PostEntryData;
import info.narazaki.android.tuboroid.data.ThreadData;
import info.narazaki.android.tuboroid.data.ThreadEntryData;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class TuboroidAgent {
    private static final String TAG = "TuboroidAgent";
    
    public static final String THREAD_DATA_UPDATED_ACTION = "info.narazaki.android.tuboroid.service.TuboroidService.THREAD_DATA_UPDATED";
    
    private Context context_;
    private TuboroidAgentManager man_;
    
    public TuboroidAgent(Context context) {
        super();
        context_ = context;
        
        man_ = new TuboroidAgentManager(context);
    }
    
    public void onCreate() {}
    
    public void onTerminate() {}
    
    public void clearCookie() {
        man_.getSingleHttpAgent().setCookieStoreData(null);
    }
    
    public void onResetProxyPreference() {
    	man_.onResetProxyPreference();
    }
    
    public boolean fetchBoardList(final boolean no_cache, final boolean force_reload,
            final BoardListAgent.BoardListFetchedCallback callback) {
        return man_.getBoardListAgent().fetchBoardList(no_cache, force_reload, callback);
    }
    
    public BoardData getBoardData(final Uri uri, final boolean is_new_external,
            final BoardListAgent.BoardFetchedCallback callback) {
        return man_.getBoardListAgent().getBoardData(uri, is_new_external, callback);
    }
    
    public void deleteBoard(final BoardData board_data, final Runnable callback) {
        man_.getBoardListAgent().deleteBoard(board_data, callback);
    }
    
    public void updateBoard(final BoardData board_data, final Runnable callback) {
        man_.getBoardListAgent().updateBoard(board_data, callback);
    }
    
    public void saveBoardListStat(NSimpleExpandableListActivity.StatData stat_data, Runnable callback) {
        man_.getBoardListAgent().saveBoardListStat(stat_data, callback);
    }
    
    public void fetchThreadList(final BoardData board_data, final boolean force_reload,
            final ThreadListFetchedCallback callback) {
        man_.getThreadListAgent().fetchThreadList(board_data, force_reload, callback);
    }
    
    public void fetchSimilarThreadList(final BoardData board_data, final long thread_id, final String search_key,
            final boolean force_reload, final ThreadListFetchedCallback callback) {
        man_.getThreadListAgent().fetchSimilarThreadList(board_data, thread_id, search_key, force_reload, callback);
    }
    
    public void reloadThreadEntryList(final ThreadData thread_data, final boolean reload,
            final ThreadEntryListAgent.ThreadEntryListAgentCallback callback) {
        man_.getEntryListAgent().reloadThreadEntryList(thread_data, reload, callback);
    }
    
    public void reloadSpecialThreadEntryList(final ThreadData thread_data, final AccountPref account_pref,
            final ThreadEntryListAgent.ThreadEntryListAgentCallback callback) {
        man_.getEntryListAgent().reloadSpecialThreadEntryList(thread_data, account_pref, callback);
    }
    
    public void storeThreadEntryListAnalyzedCache(final ThreadData thread_data, List<ThreadEntryData> new_cache_list) {
        man_.getEntryListAgent().storeThreadEntryListAnalyzedCache(thread_data, new_cache_list);
    }
    
    public void initNewThreadData(final ThreadData thread_data, final Runnable callback) {
        man_.getDBAgent().insertThreadData(thread_data, new SQLiteAgentBase.DbTransactionDelegate(callback));
    }
    
    public void updateThreadData(final ThreadData thread_data, final Runnable callback) {
        man_.getDBAgent().updateThreadData(thread_data, new SQLiteAgentBase.DbTransaction() {
            @Override
            public void run() {
                if (callback != null) callback.run();
                Intent intent = new Intent(THREAD_DATA_UPDATED_ACTION);
                context_.sendBroadcast(intent);
            }
            
            @Override
            public void onError() {
                if (callback != null) callback.run();
            }
        });
    }
    
    public void updateThreadRecentPos(final ThreadData thread_data, final Runnable callback) {
        man_.getDBAgent().updateThreadRecentPos(thread_data, new SQLiteAgentBase.DbTransactionDelegate(callback));
    }
    
    public void getThreadData(final ThreadData thread_data, final SQLiteAgent.GetThreadDataResult callback) {
        man_.getDBAgent().getThreadData(thread_data, callback);
    }
    
    public void deleteThreadEntryListCache(final ThreadData thread_data, final Runnable callback) {
        man_.getEntryListAgent().deleteThreadEntryListCache(thread_data, callback);
    }
    
    public void fetchFavoriteList(final FavoriteListFetchedCallback callback) {
        man_.getFavoriteCacheListAgent().fetchFavoriteList(callback);
    }
    
    public void fetchNextFavoriteThread(final ThreadData thread_data, final NextFavoriteThreadFetchedCallback callback) {
        man_.getFavoriteCacheListAgent().fetchNextFavoriteThread(thread_data, callback);
    }
    
    public void fetchRecentList(final int recent_order, final RecentListFetchedCallback callback) {
        man_.getThreadListAgent().fetchRecentList(recent_order, callback);
    }
    
    public void deleteRecentList(final ArrayList<ThreadData> delete_list, final Runnable callback) {
        man_.getEntryListAgent().deleteRecentList(delete_list, callback);
    }
    
    public void fetchBoardOfRecentThreadList(final UpdateCheckListFetchedCallback callback) {
        man_.getFavoriteListAgent().fetchBoardOfRecentThreadList(callback);
    }
    
    public void fetchBoardOfFavoriteThreadList(final UpdateCheckListFetchedCallback callback) {
        man_.getFavoriteListAgent().fetchBoardOfFavoriteThreadList(callback);
    }
    
    public void fetchFavoriteThreadList(final FavoriteThreadListFetchedCallback callback) {
        man_.getFavoriteListAgent().fetchFavoriteThreadList(callback);
    }
    
    public void addFavorite(final BoardData board_data, final int rule, final Runnable callback) {
        man_.getFavoriteCacheListAgent().addFavorite(board_data, rule, callback);
    }
    
    public void delFavorite(final BoardData board_data, final Runnable callback) {
        man_.getFavoriteCacheListAgent().delFavorite(board_data, callback);
    }
    
    public void addFavorite(final ThreadData thread_data, final int rule, final Runnable callback) {
        man_.getFavoriteCacheListAgent().addFavorite(thread_data, rule, callback);
    }
    
    public void delFavorite(final ThreadData thread_data, final Runnable callback) {
        man_.getFavoriteCacheListAgent().delFavorite(thread_data, callback);
    }
    
    public void deleteFavoriteList(final ArrayList<FavoriteItemData> delete_list, final Runnable callback) {
        man_.getFavoriteCacheListAgent().deleteFavoriteList(delete_list, callback);
    }
    
    public void setNewFavoriteListOrder(final ArrayList<FavoriteItemData> new_item_list, final Runnable callback) {
        man_.getFavoriteCacheListAgent().setNewFavoriteListOrder(new_item_list, callback);
    }
    
    // find2chブクマ
    
    public void addFavorite(final Find2chKeyData key_data, final int rule, final Runnable callback) {
        man_.getFavoriteCacheListAgent().addFavorite(key_data, rule, callback);
    }
    
    public void delFavorite(final Find2chKeyData key_data, final Runnable callback) {
        man_.getFavoriteCacheListAgent().delFavorite(key_data, callback);
    }
    
    public void getFind2chKeyData(final String keyword, final SQLiteAgent.GetFind2chKeyDataResult callback) {
        man_.getDBAgent().getFind2chKeyData(keyword, callback);
    }
    
    public void setFind2chKeyData(final Find2chKeyData key_data) {
        man_.getDBAgent().setFind2chKeyData(key_data);
    }
    
    public Find2chTask searchViaFind2ch(final String key, final int order, final boolean force_reload,
            final Find2chTask.Find2chFetchedCallback callback) {
        return man_.getFind2chAgent().searchViaFind2ch(key, order, force_reload, callback);
    }
    
    public PostEntryTask.FuturePostEntry postEntry(final ThreadData thread_data, final PostEntryData post_entry_data,
            final AccountPref account_pref, final OnPostEntryCallback callback) {
        PostEntryTask task = thread_data.factoryPostEntryTask(man_);
        return task.post(thread_data, post_entry_data, account_pref, man_.getHttpUserAgentName(), callback);
    }
    
    public void savePostEntryDraft(final ThreadData thread_data, final String draft) {
        man_.getDBAgent().savePostEntryDraft(thread_data, draft);
    }
    
    public void savePostEntryRecentTime(final ThreadData thread_data) {
        man_.getDBAgent().savePostEntryRecentTime(thread_data);
    }
    
    public void addNGID(final String author_id, final int type) {
        man_.getIgnoreListAgent().addNGID(author_id, type);
    }
    
    public void addNGWord(final String word, final int type) {
        man_.getIgnoreListAgent().addNGWord(word, type);
    }
    
    public void deleteNG(final ThreadEntryData entry_data) {
        man_.getIgnoreListAgent().deleteNG(entry_data);
    }
    
    public void clearNG() {
        man_.getIgnoreListAgent().clearNG();
    }
    
    // 画像処理
    public boolean hasImageCacheFile(final ThreadData thread_data, final ThreadEntryData entry_data,
            final int image_index) {
        return man_.getImageFetchAgent().hasImageCacheFile(thread_data, entry_data, image_index);
    }
    
    public boolean fetchImage(final ImageFetchAgent.BitmapFetchedCallback callback, final File image_local_file,
            final String image_uri, final int max_width, final int max_height, final boolean can_cache) {
        return man_.getImageFetchAgent().fetchImage(callback, image_local_file, image_uri, max_width, max_height,
                can_cache);
    }
    
    public void copyFile(final String orig, final String dist, final DataFileAgent.FileWroteCallback callback) {
        man_.getFileAgent().copyFile(orig, dist, true, callback);
    }
    
    public void deleteImage(final File image_local_file) {
        man_.getImageFetchAgent().deleteImage(image_local_file);
    }
    
    // 外部フォント
    public Future<?> downloadExternalFont(final File local_file, final FontFetchAgent.FontFetchedCallback callback) {
        FontFetchAgent agent = new FontFetchAgent(man_.getMultiHttpAgent(), callback);
        return agent.fetchFile(local_file, context_.getString(R.string.const_font_file_url));
    }
}
