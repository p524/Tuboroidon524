package info.narazaki.android.tuboroid.agent.thread;

import info.narazaki.android.lib.agent.db.SQLiteAgentBase;
import info.narazaki.android.tuboroid.agent.BoardListAgent;
import info.narazaki.android.tuboroid.data.BoardData;
import info.narazaki.android.tuboroid.data.BoardData2ch;
import info.narazaki.android.tuboroid.data.Find2chKeyData;
import info.narazaki.android.tuboroid.data.IgnoreData;
import info.narazaki.android.tuboroid.data.ThreadData;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class SQLiteAgent extends SQLiteAgentBase {
    private static final String TAG = "SQLiteAgent";
    
    private static final String DB_NAME = "tuboroid";
    private static final int DB_VERSION = 16;
    
    // //////////////////////////////////////////////////
    private static final String SQL_CREATE_BOARDS_TABLE = //
    "create table " + BoardData.KEY.TABLE + " (" + //
            BoardData.KEY.PID + " integer primary key autoincrement," + //
            BoardData.KEY.NAME + " text not null," + //
            BoardData.KEY.SERVER + " text not null," + //
            BoardData.KEY.TAG + " text not null," + //
            BoardData.KEY.IS_EXTERNAL + " integer default 0," + //
            BoardData.KEY.IS_FAVORITE + " integer default 0," + //
            " unique ( " + //
            BoardData.KEY.SERVER + " , " + BoardData.KEY.TAG + //
            " ) on conflict ignore" + //
            ");";
    private static final String SQL_DROP_BOARDS_TABLE = //
    "drop table if exists " + BoardData.KEY.TABLE;
    
    private static final String SQL_CREATE_BOARDS_INDEX_1 = //
    "create index BOARDS_INDEX_1 on " + BoardData.KEY.TABLE + " ( " + //
            BoardData.KEY.SERVER + ", " + //
            BoardData.KEY.TAG + //
            " );";
    
    private static final String SQL_CREATE_BOARDS_INDEX_2 = //
    "create index BOARDS_INDEX_2 on " + BoardData.KEY.TABLE + " ( " + //
            BoardData.KEY.IS_FAVORITE + //
            " );";
    
    // //////////////////////////////////////////////////
    private static final String SQL_CREATE_THREADS_TABLE = //
    "create table " + ThreadData.KEY.TABLE + " (" + //
            ThreadData.KEY.PID + " integer primary key autoincrement," + //
            ThreadData.KEY.ID + " integer, " + //
            ThreadData.KEY.NAME + " text not null, " + //
            ThreadData.KEY.BOARD_NAME + " text not null, " + //
            ThreadData.KEY.BOARD_SERVER + " text not null, " + //
            ThreadData.KEY.BOARD_TAG + " text not null, " + //
            ThreadData.KEY.ONLINE_COUNT + " integer default 0," + //
            ThreadData.KEY.CACHE_COUNT + " integer default 0," + //
            ThreadData.KEY.CACHE_SIZE + " integer default 0," + //
            ThreadData.KEY.RECENT_TIME + " text," + //
            ThreadData.KEY.CACHE_ETAG + " text," + //
            ThreadData.KEY.CACHE_TIMESTAMP + " datetime default null," + //
            ThreadData.KEY.READ_COUNT + " integer default 0," + //
            ThreadData.KEY.IS_FAVORITE + " integer default 0," + //
            ThreadData.KEY.IS_ALIVE + " integer default 1," + //
            ThreadData.KEY.IS_DROPPED + " integer default 0," + //
            ThreadData.KEY.ON_EXT_STORAGE + " integer default 0," + //
            ThreadData.KEY.EDIT_DRAFT + " text," + //
            " unique ( " + //
            ThreadData.KEY.BOARD_SERVER + " , " + ThreadData.KEY.BOARD_TAG + " , " + ThreadData.KEY.ID + //
            " ) on conflict ignore" + //
            ");";
    
    private static final String SQL_DROP_THREADS_TABLE = "drop table if exists " + ThreadData.KEY.TABLE;
    
    private static final String SQL_CREATE_THREADS_INDEX_1 = //
    "create index THREADS_INDEX_1 on "
            + //
            ThreadData.KEY.TABLE + " ( "
            + //
            ThreadData.KEY.IS_ALIVE + " , " + ThreadData.KEY.IS_FAVORITE + " , " + ThreadData.KEY.CACHE_TIMESTAMP
            + " desc" + //
            " );";
    
    private static final String SQL_CREATE_THREADS_INDEX_2 = //
    "create index THREADS_INDEX_2 on " + //
            ThreadData.KEY.TABLE + " ( " + //
            ThreadData.KEY.IS_ALIVE + " , " + ThreadData.KEY.IS_FAVORITE + " , " + //
            ThreadData.KEY.BOARD_NAME + " , " + ThreadData.KEY.CACHE_TIMESTAMP + " desc" + //
            " );";
    
    private static final String[] SQL_ALTER_THREADS_TABLE_14_15 = new String[] { //
    //
            "alter table " + ThreadData.KEY.TABLE + " add column " + //
                    ThreadData.KEY.RECENT_POS + " integer default 0;", //
            //
            "alter table " + ThreadData.KEY.TABLE + " add column " + //
                    ThreadData.KEY.RECENT_POS_Y + " integer default 0;" //
    };
    private static final String[] SQL_ALTER_THREADS_TABLE_15_16 = new String[] { //
    //
            "alter table " + ThreadData.KEY.TABLE + " add column " + //
                    ThreadData.KEY.RECENT_POST_TIME + " integer default 0;", //
            //
            "create index THREADS_INDEX_3 on " + //
                    ThreadData.KEY.TABLE + " ( " + //
                    ThreadData.KEY.IS_ALIVE + " , " + //
                    ThreadData.KEY.RECENT_POST_TIME + " desc, " + //
                    ThreadData.KEY.CACHE_TIMESTAMP + " desc" + //
                    " );"
    //
    };
    // //////////////////////////////////////////////////
    private static final String SQL_CREATE_IGNORES_TABLE = //
    "create table " + IgnoreData.KEY.TABLE + " (" + //
            IgnoreData.KEY.PID + " integer primary key autoincrement," + //
            IgnoreData.KEY.TYPE + " integer, " + //
            IgnoreData.KEY.TOKEN + " text not null, " + //
            " unique ( " + //
            IgnoreData.KEY.TYPE + " , " + IgnoreData.KEY.TOKEN + //
            " ) on conflict ignore" + //
            ");";
    private static final String SQL_DROP_IGNORES_TABLE = "drop table if exists " + IgnoreData.KEY.TABLE;
    
    // //////////////////////////////////////////////////
    private static final String SQL_CREATE_SEARCH_KEY_TABLE = //
    "create table " + Find2chKeyData.KEY.TABLE + " (" + //
            Find2chKeyData.KEY.PID + " integer primary key autoincrement," + //
            Find2chKeyData.KEY.KEYWORD + " text not null, " + //
            Find2chKeyData.KEY.HIT_COUNT + " integer, " + //
            Find2chKeyData.KEY.IS_FAVORITE + " integer default 0," + //
            Find2chKeyData.KEY.PREV_TIME + " integer, " + //
            " unique ( " + //
            Find2chKeyData.KEY.IS_FAVORITE + " , " + Find2chKeyData.KEY.KEYWORD + //
            " ) on conflict ignore" + //
            ");";
    
    private static final String SQL_DROP_SEARCH_KEY_TABLE = "drop table if exists " + Find2chKeyData.KEY.TABLE;
    
    // //////////////////////////////////////////////////
    public SQLiteAgent(Context context) {
        super(context);
    }
    
    private static class DBHelper extends SQLiteOpenHelper {
        public DBHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }
        
        @Override
        public void onCreate(SQLiteDatabase db) {
            onUpgrade(db, 0, DB_VERSION);
        }
        
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading Database from Ver." + oldVersion + " to Ver." + newVersion);
            int current_version = oldVersion;
            if (current_version < 14 || current_version > newVersion) {
                Log.w(TAG, "All Data will be destroyed");
                clearDb(db);
                createDb(db);
                current_version = 14;
            }
            
            current_version = upgradeTables(db, current_version, newVersion, 15, SQL_ALTER_THREADS_TABLE_14_15);
            current_version = upgradeTables(db, current_version, newVersion, 16, SQL_ALTER_THREADS_TABLE_15_16);
            
        }
        
        private int upgradeTables(SQLiteDatabase db, int current_version, int new_version, final int target_version,
                final String[] alter_table_query) {
            if (current_version != target_version - 1) return current_version;
            
            Log.w(TAG, "migrate " + current_version + " to " + target_version);
            
            for (String sql : alter_table_query) {
                db.execSQL(sql);
            }
            return target_version;
        }
        
        private void clearDb(SQLiteDatabase db) {
            db.execSQL(SQL_DROP_BOARDS_TABLE);
            db.execSQL(SQL_DROP_THREADS_TABLE);
            db.execSQL(SQL_DROP_IGNORES_TABLE);
            db.execSQL(SQL_DROP_SEARCH_KEY_TABLE);
        }
        
        private void createDb(SQLiteDatabase db) {
            db.execSQL(SQL_CREATE_BOARDS_TABLE);
            db.execSQL(SQL_CREATE_BOARDS_INDEX_1);
            db.execSQL(SQL_CREATE_BOARDS_INDEX_2);
            
            db.execSQL(SQL_CREATE_THREADS_TABLE);
            db.execSQL(SQL_CREATE_THREADS_INDEX_1);
            db.execSQL(SQL_CREATE_THREADS_INDEX_2);
            
            db.execSQL(SQL_CREATE_IGNORES_TABLE);
            db.execSQL(SQL_CREATE_SEARCH_KEY_TABLE);
            
        }
    }
    
    @Override
    protected SQLiteOpenHelper createSQLiteOpenHelper(Context context) {
        return new DBHelper(context);
    }
    
    // //////////////////////////////////////////////////
    // 板
    // //////////////////////////////////////////////////
    
    public void insertBoardData(final BoardData board_data, final BoardListAgent.BoardFetchedCallback callback_created,
            final Runnable callback_reload, final Runnable callback_exists) {
        submitTransaction(new DbTransaction() {
            @Override
            public void run() {
                BoardData new_board_data = board_data;
                boolean board_exists = false;
                
                ContentValues values = board_data.getContentValues();
                String where = BoardData.KEY.SERVER + " = ? AND " + BoardData.KEY.TAG + " = ?";
                String[] bind = new String[] { board_data.server_def_.board_server_, board_data.server_def_.board_tag_ };
                
                Cursor cursor = getDB().query(BoardData.KEY.TABLE, BoardData.KEY.FIELD_LIST, where, bind, null, null,
                        null);
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    new_board_data = BoardData.factory(cursor);
                    board_exists = true;
                }
                cursor.close();
                
                if (!board_exists) {
                    getDB().insert(BoardData.KEY.TABLE, null, values);
                }
                
                if (!board_exists && new_board_data.board_name_.length() > 0) {
                    if (callback_created != null) {
                        callback_created.onBoardFetched(new_board_data);
                        return;
                    }
                }
                else if (!board_exists || new_board_data.board_name_.length() == 0) {
                    if (callback_reload != null) {
                        callback_reload.run();
                        return;
                    }
                }
                if (callback_exists != null) callback_exists.run();
            }
            
            @Override
            public void onError() {
                if (callback_reload != null) callback_reload.run();
            }
        });
    }
    
    public void getBoardList(final DbResultReceiver callback) {
        submitTransaction(new DbTransaction() {
            @Override
            public void run() {
                Cursor cursor = getDB().query(BoardData.KEY.TABLE, BoardData.KEY.FIELD_LIST, null, null, null, null,
                        BoardData.KEY.PID);
                callback.onQuery(cursor);
                cursor.close();
            }
            
            @Override
            public void onError() {
                callback.onError();
            }
        });
    }
    
    public void updateBoardData(final BoardData board_data, final DbTransaction callback) {
        submitTransaction(new DbTransaction() {
            @Override
            public void run() {
                ContentValues values = board_data.getContentValues();
                String where = BoardData.KEY.SERVER + " = ? AND " + BoardData.KEY.TAG + " = ?";
                String[] bind = new String[] { board_data.server_def_.board_server_, board_data.server_def_.board_tag_ };
                getDB().update(BoardData.KEY.TABLE, values, where, bind);
                if (callback != null) callback.run();
            }
            
            @Override
            public void onError() {
                if (callback != null) callback.onError();
            }
        });
    }
    
    public void deleteBoardData(final BoardData board_data, final DbTransaction callback) {
        submitTransaction(new DbTransaction() {
            @Override
            public void run() {
                String where = BoardData.KEY.SERVER + " = ? AND " + BoardData.KEY.TAG + " = ?";
                String[] bind = new String[] { board_data.server_def_.board_server_, board_data.server_def_.board_tag_ };
                getDB().delete(BoardData.KEY.TABLE, where, bind);
                
                where = ThreadData.KEY.BOARD_SERVER + " = ? AND " + ThreadData.KEY.BOARD_TAG + " = ?";
                bind = new String[] { board_data.server_def_.board_server_, board_data.server_def_.board_tag_ };
                getDB().delete(ThreadData.KEY.TABLE, where, bind);
                
                if (callback != null) callback.run();
            }
            
            @Override
            public void onError() {
                if (callback != null) callback.onError();
            }
        });
    }
    
    // //////////////////////////////////////////////////
    // 板移転チェック
    // //////////////////////////////////////////////////
    
    public static interface MigrateBoardCallback {
        public void onMigrated(final BoardData from_data, final BoardData to_data);
        
        public void onNoMigrated();
    }
    
    public void migrateBoardData(final BoardData to_data, final MigrateBoardCallback callback) {
        submitTransaction(new DbTransaction() {
            @Override
            public void run() {
                String where = BoardData.KEY.NAME + " = ? AND " + BoardData.KEY.TAG + " = ?";
                String[] bind = new String[] { to_data.board_name_, to_data.server_def_.board_tag_ };
                
                Cursor cursor = getDB().query(BoardData.KEY.TABLE, BoardData.KEY.FIELD_LIST, where, bind, null, null,
                        null, null);
                if (cursor.getCount() != 1) {
                    cursor.close();
                    callback.onNoMigrated();
                    return;
                }
                
                cursor.moveToFirst();
                BoardData from_data = BoardData.factory(cursor);
                cursor.close();
                if (from_data instanceof BoardData2ch && !from_data.server_def_.equals(to_data.server_def_)) {
                    Log.i(TAG, "Migrate Board Data : " + from_data.server_def_.toString() + " -> "
                            + to_data.server_def_.toString());
                    
                    // 板情報の書き換え
                    String from_where = BoardData.KEY.SERVER + " = ? AND " + BoardData.KEY.TAG + " = ?";
                    String[] from_bind = new String[] { from_data.server_def_.board_server_,
                            from_data.server_def_.board_tag_ };
                    
                    ContentValues new_board_values = to_data.getContentValues();
                    getDB().update(BoardData.KEY.TABLE, new_board_values, from_where, from_bind);
                    
                    // スレ情報の書き換え
                    ContentValues new_thread_values = new ContentValues();
                    new_thread_values.put(ThreadData.KEY.BOARD_SERVER, to_data.server_def_.board_server_);
                    new_thread_values.put(ThreadData.KEY.BOARD_TAG, to_data.server_def_.board_tag_);
                    
                    from_where = ThreadData.KEY.BOARD_SERVER + " = ? AND " + ThreadData.KEY.BOARD_TAG + " = ?";
                    from_bind = new String[] { from_data.server_def_.board_server_, from_data.server_def_.board_tag_ };
                    getDB().update(ThreadData.KEY.TABLE, new_thread_values, from_where, from_bind);
                    
                    callback.onMigrated(from_data, to_data);
                }
                else {
                    callback.onNoMigrated();
                }
                
            }
            
            @Override
            public void onError() {
                callback.onNoMigrated();
            }
            
        });
    }
    
    // //////////////////////////////////////////////////
    // スレ
    // //////////////////////////////////////////////////
    static public interface GetThreadDataResult {
        public void onQuery(final ThreadData thread_data);
    }
    
    public void insertThreadData(final ThreadData thread_data, final DbTransaction callback) {
        submitTransaction(new DbTransaction() {
            @Override
            public void run() {
                ContentValues values = thread_data.getContentValues();
                String where = ThreadData.KEY.BOARD_SERVER + " = ? AND " + ThreadData.KEY.BOARD_TAG + " = ? AND "
                        + ThreadData.KEY.ID + " = ?";
                String[] bind = new String[] { thread_data.server_def_.board_server_,
                        thread_data.server_def_.board_tag_, String.valueOf(thread_data.thread_id_) };
                
                Cursor cursor = getDB().query(ThreadData.KEY.TABLE, ThreadData.KEY.FIELD_LIST, where, bind, null, null,
                        null);
                boolean no_entry = cursor.getCount() == 0;
                cursor.close();
                
                if (no_entry) {
                    getDB().insert(ThreadData.KEY.TABLE, null, values);
                }
                if (callback != null) callback.run();
            }
            
            @Override
            public void onError() {
                if (callback != null) callback.onError();
            }
        });
    }
    
    public void getThreadData(final ThreadData thread_data, final GetThreadDataResult callback) {
        submitTransaction(new DbTransaction() {
            @Override
            public void run() {
                String where = ThreadData.KEY.BOARD_SERVER + " = ? AND " + ThreadData.KEY.BOARD_TAG + " = ? AND "
                        + ThreadData.KEY.ID + " = ?";
                String[] bind = new String[] { thread_data.server_def_.board_server_,
                        thread_data.server_def_.board_tag_, String.valueOf(thread_data.thread_id_) };
                Cursor cursor = getDB().query(ThreadData.KEY.TABLE, ThreadData.KEY.FIELD_LIST, where, bind, null, null,
                        null);
                
                ThreadData result_data = thread_data;
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    result_data = ThreadData.factory(cursor);
                }
                cursor.close();
                
                callback.onQuery(result_data);
            }
            
            @Override
            public void onError() {
                callback.onQuery(thread_data);
            }
        });
    }
    
    public void getThreadList(final BoardData board_data, final DbResultReceiver callback) {
        submitTransaction(new DbTransaction() {
            @Override
            public void run() {
                String where = ThreadData.KEY.BOARD_SERVER + " = ? AND " + ThreadData.KEY.BOARD_TAG + " = ?";
                String[] bind = new String[] { board_data.server_def_.board_server_, board_data.server_def_.board_tag_ };
                Cursor cursor = getDB().query(ThreadData.KEY.TABLE, ThreadData.KEY.FIELD_LIST, where, bind, null, null,
                        ThreadData.KEY.PID);
                callback.onQuery(cursor);
                cursor.close();
            }
            
            @Override
            public void onError() {
                callback.onError();
            }
        });
    }
    
    public void updateThreadData(final ThreadData thread_data, final DbTransaction callback) {
        submitTransaction(new DbTransaction() {
            @Override
            public void run() {
                ContentValues values = thread_data.getContentValues();
                String where = ThreadData.KEY.BOARD_SERVER + " = ? AND " + ThreadData.KEY.BOARD_TAG + " = ? AND "
                        + ThreadData.KEY.ID + " = ?";
                String[] bind = new String[] { thread_data.server_def_.board_server_,
                        thread_data.server_def_.board_tag_, String.valueOf(thread_data.thread_id_) };
                getDB().update(ThreadData.KEY.TABLE, values, where, bind);
                if (callback != null) callback.run();
            }
            
            @Override
            public void onError() {
                if (callback != null) callback.onError();
            }
        });
    }
    
    public void updateThreadRecentPos(final ThreadData thread_data, final DbTransaction callback) {
        submitTransaction(new DbTransaction() {
            @Override
            public void run() {
                ContentValues values = new ContentValues();
                values.put(ThreadData.KEY.RECENT_POS, thread_data.recent_pos_);
                values.put(ThreadData.KEY.RECENT_POS_Y, thread_data.recent_pos_y_);
                String where = ThreadData.KEY.BOARD_SERVER + " = ? AND " + ThreadData.KEY.BOARD_TAG + " = ? AND "
                        + ThreadData.KEY.ID + " = ?";
                String[] bind = new String[] { thread_data.server_def_.board_server_,
                        thread_data.server_def_.board_tag_, String.valueOf(thread_data.thread_id_) };
                getDB().update(ThreadData.KEY.TABLE, values, where, bind);
                if (callback != null) callback.run();
            }
            
            @Override
            public void onError() {
                if (callback != null) callback.onError();
            }
        });
    }
    
    public void updateThreadDataList(final List<ThreadData> thread_data_list, final DbTransaction callback) {
        submitTransaction(new DbTransaction() {
            @Override
            public void run() {
                for (ThreadData thread_data : thread_data_list) {
                    ContentValues values = thread_data.getContentValues();
                    String where = ThreadData.KEY.BOARD_SERVER + " = ? AND " + ThreadData.KEY.BOARD_TAG + " = ? AND "
                            + ThreadData.KEY.ID + " = ?";
                    String[] bind = new String[] { thread_data.server_def_.board_server_,
                            thread_data.server_def_.board_tag_, String.valueOf(thread_data.thread_id_) };
                    getDB().update(ThreadData.KEY.TABLE, values, where, bind);
                }
                if (callback != null) callback.run();
            }
            
            @Override
            public void onError() {
                if (callback != null) callback.onError();
            }
        });
    }
    
    public void deleteThreadData(final ThreadData thread_data, final DbTransaction callback) {
        submitTransaction(new DbTransaction() {
            @Override
            public void run() {
                String where = ThreadData.KEY.BOARD_SERVER + " = ? AND " + ThreadData.KEY.BOARD_TAG + " = ? AND "
                        + ThreadData.KEY.ID + " = ?";
                String[] bind = new String[] { thread_data.server_def_.board_server_,
                        thread_data.server_def_.board_tag_, String.valueOf(thread_data.thread_id_) };
                getDB().delete(ThreadData.KEY.TABLE, where, bind);
                if (callback != null) callback.run();
            }
            
            @Override
            public void onError() {
                if (callback != null) callback.onError();
            }
        });
    }
    
    public void getRecentList(final int recent_order, final DbResultReceiver callback) {
        submitTransaction(new DbTransaction() {
            @Override
            public void run() {
                String where = ThreadData.KEY.IS_ALIVE + " = 1";
                String order = "";
                if (recent_order == ThreadData.KEY.RECENT_ORDER_WRITE) {
                    order += ThreadData.KEY.RECENT_POST_TIME + " desc,";
                }
                order += ThreadData.KEY.RECENT_TIME + " desc";
                Cursor cursor = getDB().query(ThreadData.KEY.TABLE, ThreadData.KEY.FIELD_LIST, where, null, null, null,
                        order);
                callback.onQuery(cursor);
                cursor.close();
            }
            
            @Override
            public void onError() {
                callback.onError();
            }
        });
    }
    
    public void savePostEntryDraft(final ThreadData thread_data, final String draft) {
        submitTransaction(new DbTransactionBase() {
            @Override
            public void run() {
                ContentValues values = new ContentValues();
                values.put(ThreadData.KEY.EDIT_DRAFT, draft);
                String where = ThreadData.KEY.BOARD_SERVER + " = ? AND " + ThreadData.KEY.BOARD_TAG + " = ? AND "
                        + ThreadData.KEY.ID + " = ?";
                String[] bind = new String[] { thread_data.server_def_.board_server_,
                        thread_data.server_def_.board_tag_, String.valueOf(thread_data.thread_id_) };
                getDB().update(ThreadData.KEY.TABLE, values, where, bind);
            }
        });
    }
    
    public void savePostEntryRecentTime(final ThreadData thread_data) {
        submitTransaction(new DbTransactionBase() {
            @Override
            public void run() {
                ContentValues values = new ContentValues();
                long current_time = System.currentTimeMillis() / 1000;
                values.put(ThreadData.KEY.RECENT_POST_TIME, current_time);
                String where = ThreadData.KEY.BOARD_SERVER + " = ? AND " + ThreadData.KEY.BOARD_TAG + " = ? AND "
                        + ThreadData.KEY.ID + " = ?";
                String[] bind = new String[] { thread_data.server_def_.board_server_,
                        thread_data.server_def_.board_tag_, String.valueOf(thread_data.thread_id_) };
                getDB().update(ThreadData.KEY.TABLE, values, where, bind);
            }
        });
    }
    
    // //////////////////////////////////////////////////
    // ブクマ
    // //////////////////////////////////////////////////
    
    public void updateFavoriteList(final ArrayList<BoardData> board_list, final ArrayList<ThreadData> thread_list,
            final ArrayList<Find2chKeyData> search_key_list, final DbTransaction callback) {
        submitTransaction(new DbTransaction() {
            @Override
            public void run() {
                for (BoardData board_data : board_list) {
                    ContentValues values = new ContentValues();
                    values.put(BoardData.KEY.IS_FAVORITE, board_data.is_favorite_ ? 1 : 0);
                    String where = BoardData.KEY.SERVER + " = ? AND " + BoardData.KEY.TAG + " = ?";
                    String[] bind = new String[] { board_data.server_def_.board_server_,
                            board_data.server_def_.board_tag_ };
                    getDB().update(BoardData.KEY.TABLE, values, where, bind);
                }
                for (ThreadData thread_data : thread_list) {
                    ContentValues values = new ContentValues();
                    values.put(ThreadData.KEY.IS_FAVORITE, thread_data.is_favorite_ ? 1 : 0);
                    String where = ThreadData.KEY.BOARD_SERVER + " = ? AND " + ThreadData.KEY.BOARD_TAG + " = ? AND "
                            + ThreadData.KEY.ID + " = ?";
                    String[] bind = new String[] { thread_data.server_def_.board_server_,
                            thread_data.server_def_.board_tag_, String.valueOf(thread_data.thread_id_) };
                    getDB().update(ThreadData.KEY.TABLE, values, where, bind);
                }
                for (Find2chKeyData search_key_data : search_key_list) {
                    String where = Find2chKeyData.KEY.KEYWORD + " = ?";
                    String[] bind = new String[] { search_key_data.keyword_ };
                    getDB().delete(Find2chKeyData.KEY.TABLE, where, bind);
                }
                if (callback != null) callback.run();
            }
            
            @Override
            public void onError() {
                if (callback != null) callback.onError();
            }
        });
    }
    
    public void updateBoardDataFavorite(final BoardData board_data, final DbTransaction callback) {
        submitTransaction(new DbTransaction() {
            @Override
            public void run() {
                ContentValues values = new ContentValues();
                values.put(BoardData.KEY.IS_FAVORITE, board_data.is_favorite_ ? 1 : 0);
                String where = BoardData.KEY.SERVER + " = ? AND " + BoardData.KEY.TAG + " = ?";
                String[] bind = new String[] { board_data.server_def_.board_server_, board_data.server_def_.board_tag_ };
                getDB().update(BoardData.KEY.TABLE, values, where, bind);
                if (callback != null) callback.run();
            }
            
            @Override
            public void onError() {
                if (callback != null) callback.onError();
            }
        });
    }
    
    public void updateThreadDataFavorite(final ThreadData thread_data, final DbTransaction callback) {
        submitTransaction(new DbTransaction() {
            @Override
            public void run() {
                ContentValues values = new ContentValues();
                values.put(ThreadData.KEY.IS_FAVORITE, thread_data.is_favorite_ ? 1 : 0);
                String where = ThreadData.KEY.BOARD_SERVER + " = ? AND " + ThreadData.KEY.BOARD_TAG + " = ? AND "
                        + ThreadData.KEY.ID + " = ?";
                String[] bind = new String[] { thread_data.server_def_.board_server_,
                        thread_data.server_def_.board_tag_, String.valueOf(thread_data.thread_id_) };
                getDB().update(ThreadData.KEY.TABLE, values, where, bind);
                if (callback != null) callback.run();
            }
            
            @Override
            public void onError() {
                if (callback != null) callback.onError();
            }
        });
    }
    
    public void getFavoriteBoardList(final DbResultReceiver callback) {
        submitTransaction(new DbTransaction() {
            @Override
            public void run() {
                String where = BoardData.KEY.IS_FAVORITE + " = 1";
                Cursor cursor = getDB().query(BoardData.KEY.TABLE, BoardData.KEY.FIELD_LIST, where, null, null, null,
                        null);
                callback.onQuery(cursor);
                cursor.close();
            }
            
            @Override
            public void onError() {
                if (callback != null) callback.onError();
            }
        });
    }
    
    public void getFavoriteThreadList(final DbResultReceiver callback) {
        submitTransaction(new DbTransaction() {
            @Override
            public void run() {
                String where = ThreadData.KEY.IS_FAVORITE + " = 1";
                Cursor cursor = getDB().query(ThreadData.KEY.TABLE, ThreadData.KEY.FIELD_LIST, where, null, null, null,
                        null);
                callback.onQuery(cursor);
                cursor.close();
            }
            
            @Override
            public void onError() {
                if (callback != null) callback.onError();
            }
        });
    }
    
    public void getBoardOfFavoriteThreadList(final DbResultReceiver callback) {
        submitTransaction(new DbTransaction() {
            @Override
            public void run() {
                String query = //
                "select distinct " + BoardData.KEY.TABLE + ".* " + //
                        " from " + BoardData.KEY.TABLE + ", " + ThreadData.KEY.TABLE + //
                        " where " + ThreadData.KEY.TABLE + "." + ThreadData.KEY.IS_FAVORITE + " = 1" + //
                        " and " + BoardData.KEY.TABLE + "." + BoardData.KEY.SERVER + //
                        " = " + ThreadData.KEY.TABLE + "." + ThreadData.KEY.BOARD_SERVER + //
                        " and " + BoardData.KEY.TABLE + "." + BoardData.KEY.TAG + //
                        " = " + ThreadData.KEY.TABLE + "." + ThreadData.KEY.BOARD_TAG;
                Cursor cursor = getDB().rawQuery(query, null);
                callback.onQuery(cursor);
                cursor.close();
            }
            
            @Override
            public void onError() {
                if (callback != null) callback.onError();
            }
        });
    }
    
    public void getBoardOfRecentThreadList(final DbResultReceiver callback) {
        submitTransaction(new DbTransaction() {
            @Override
            public void run() {
                String query = //
                "select distinct " + BoardData.KEY.TABLE + ".* " + //
                        " from " + BoardData.KEY.TABLE + ", " + ThreadData.KEY.TABLE + //
                        " where " + ThreadData.KEY.TABLE + "." + ThreadData.KEY.IS_ALIVE + " = 1" + //
                        " and " + BoardData.KEY.TABLE + "." + BoardData.KEY.SERVER + //
                        " = " + ThreadData.KEY.TABLE + "." + ThreadData.KEY.BOARD_SERVER + //
                        " and " + BoardData.KEY.TABLE + "." + BoardData.KEY.TAG + //
                        " = " + ThreadData.KEY.TABLE + "." + ThreadData.KEY.BOARD_TAG;
                Cursor cursor = getDB().rawQuery(query, null);
                callback.onQuery(cursor);
                cursor.close();
            }
            
            @Override
            public void onError() {
                if (callback != null) callback.onError();
            }
        });
    }
    
    // //////////////////////////////////////////////////
    // NGID/NGワード
    // //////////////////////////////////////////////////
    
    public void getIgnoreList(final DbResultReceiver callback) {
        submitTransaction(new DbTransaction() {
            @Override
            public void run() {
                Cursor cursor = getDB().query(IgnoreData.KEY.TABLE, IgnoreData.KEY.FIELD_LIST, null, null, null, null,
                        IgnoreData.KEY.PID);
                callback.onQuery(cursor);
                cursor.close();
            }
            
            @Override
            public void onError() {
                if (callback != null) callback.onError();
            }
        });
    }
    
    public void addIgnoreData(final IgnoreData ignore_data) {
        submitTransaction(new DbTransactionBase() {
            @Override
            public void run() {
                ContentValues values = ignore_data.getContentValues();
                getDB().insert(IgnoreData.KEY.TABLE, null, values);
            }
        });
    }
    
    public void deleteIgnoreData(final IgnoreData ignore_data) {
        submitTransaction(new DbTransactionBase() {
            @Override
            public void run() {
                String where = IgnoreData.KEY.TYPE + " = ? AND " + IgnoreData.KEY.TOKEN + " = ?";
                String[] bind = new String[] { String.valueOf(ignore_data.type_), ignore_data.token_ };
                getDB().delete(IgnoreData.KEY.TABLE, where, bind);
            }
        });
    }
    
    public void clearIgnoreData() {
        submitTransaction(new DbTransactionBase() {
            @Override
            public void run() {
                getDB().delete(IgnoreData.KEY.TABLE, null, null);
            }
        });
    }
    
    // //////////////////////////////////////////////////
    // 板検索キー
    // //////////////////////////////////////////////////
    static public interface GetFind2chKeyDataResult {
        public void onQuery(final Find2chKeyData search_key_data);
    }
    
    public void getFind2chKeyList(final DbResultReceiver callback) {
        submitTransaction(new DbTransaction() {
            @Override
            public void run() {
                String where = Find2chKeyData.KEY.IS_FAVORITE + " = 1";
                Cursor cursor = getDB().query(Find2chKeyData.KEY.TABLE, Find2chKeyData.KEY.FIELD_LIST, where, null,
                        null, null, null);
                callback.onQuery(cursor);
                cursor.close();
            }
            
            @Override
            public void onError() {
                if (callback != null) callback.onError();
            }
        });
    }
    
    public void getFind2chKeyData(final String keyword, final GetFind2chKeyDataResult callback) {
        submitTransaction(new DbTransaction() {
            @Override
            public void run() {
                String where = Find2chKeyData.KEY.KEYWORD + " = ? AND " + Find2chKeyData.KEY.IS_FAVORITE + " = 1";
                String[] bind = new String[] { keyword };
                Cursor cursor = getDB().query(Find2chKeyData.KEY.TABLE, Find2chKeyData.KEY.FIELD_LIST, where, bind,
                        null, null, null);
                
                Find2chKeyData result_data = null;
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    result_data = Find2chKeyData.factory(cursor);
                }
                cursor.close();
                
                callback.onQuery(result_data);
            }
            
            @Override
            public void onError() {
                callback.onQuery(null);
            }
        });
    }
    
    public void setFind2chKeyData(final Find2chKeyData key_data) {
        submitTransaction(new DbTransactionBase() {
            @Override
            public void run() {
                ContentValues values = key_data.getContentValues();
                String where = Find2chKeyData.KEY.KEYWORD + " = ?";
                String[] bind = new String[] { key_data.keyword_ };
                
                int count = getDB().update(Find2chKeyData.KEY.TABLE, values, where, bind);
                
                if (count == 0) {
                    getDB().insert(Find2chKeyData.KEY.TABLE, null, values);
                }
            }
        });
    }
    
    public void deleteFind2chKeyData(final String keyword) {
        submitTransaction(new DbTransactionBase() {
            @Override
            public void run() {
                String where = Find2chKeyData.KEY.KEYWORD + " = ?";
                String[] bind = new String[] { keyword };
                getDB().delete(Find2chKeyData.KEY.TABLE, where, bind);
            }
        });
    }
}
