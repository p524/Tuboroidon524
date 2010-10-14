package info.narazaki.android.tuboroid.agent;

import info.narazaki.android.lib.agent.db.SQLiteAgentBase;
import info.narazaki.android.tuboroid.agent.FavoriteCacheListAgent.FavoriteThreadListFetchedCallback;
import info.narazaki.android.tuboroid.agent.FavoriteCacheListAgent.UpdateCheckListFetchedCallback;
import info.narazaki.android.tuboroid.data.BoardData;
import info.narazaki.android.tuboroid.data.ThreadData;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.database.Cursor;

public class FavoriteListAgent {
    private static final String TAG = "FavoriteListAgent";
    
    private TuboroidAgentManager agent_manager_;
    
    public FavoriteListAgent(TuboroidAgentManager agent_manager) {
        super();
        agent_manager_ = agent_manager;
    }
    
    public void fetchBoardOfFavoriteThreadList(final UpdateCheckListFetchedCallback callback) {
        agent_manager_.getDBAgent().getBoardOfFavoriteThreadList(new SQLiteAgentBase.DbResultReceiver() {
            
            @Override
            public void onQuery(Cursor cursor) {
                final List<BoardData> data_list = new LinkedList<BoardData>();
                if (cursor.getCount() != 0) {
                    cursor.moveToFirst();
                    while (true) {
                        data_list.add(BoardData.factory(cursor));
                        if (cursor.moveToNext() == false) break;
                    }
                }
                cursor.close();
                callback.onUpdateCheckListFetched(data_list);
            }
            
            @Override
            public void onError() {
                callback.onUpdateCheckListFetched(new ArrayList<BoardData>());
            }
            
        });
    }
    
    public void fetchBoardOfRecentThreadList(final UpdateCheckListFetchedCallback callback) {
        agent_manager_.getDBAgent().getBoardOfRecentThreadList(new SQLiteAgentBase.DbResultReceiver() {
            
            @Override
            public void onQuery(Cursor cursor) {
                final List<BoardData> data_list = new LinkedList<BoardData>();
                if (cursor.getCount() != 0) {
                    cursor.moveToFirst();
                    while (true) {
                        data_list.add(BoardData.factory(cursor));
                        if (cursor.moveToNext() == false) break;
                    }
                }
                cursor.close();
                callback.onUpdateCheckListFetched(data_list);
            }
            
            @Override
            public void onError() {
                callback.onUpdateCheckListFetched(new ArrayList<BoardData>());
            }
            
        });
    }
    
    public void fetchFavoriteThreadList(final FavoriteThreadListFetchedCallback callback) {
        agent_manager_.getDBAgent().getFavoriteThreadList(new SQLiteAgentBase.DbResultReceiver() {
            @Override
            public void onQuery(Cursor cursor) {
                ArrayList<ThreadData> result_list = new ArrayList<ThreadData>();
                if (cursor.getCount() != 0) {
                    cursor.moveToFirst();
                    while (true) {
                        result_list.add(ThreadData.factory(cursor));
                        if (cursor.moveToNext() == false) break;
                    }
                }
                cursor.close();
                callback.onThreadListFetched(result_list);
            }
            
            @Override
            public void onError() {
                callback.onThreadListFetched(new ArrayList<ThreadData>());
            }
            
        });
    }
}
