package info.narazaki.android.tuboroid.data;

import info.narazaki.android.lib.adapter.NExpandableListAdapterDataInterface;
import info.narazaki.android.tuboroid.TuboroidApplication;
import info.narazaki.android.tuboroid.TuboroidApplication.AccountPref;
import info.narazaki.android.tuboroid.agent.CreateNewThreadTask;
import info.narazaki.android.tuboroid.agent.TuboroidAgentManager;
import info.narazaki.android.tuboroid.agent.task.HttpGetBoardDataTask;
import info.narazaki.android.tuboroid.agent.task.HttpGetThreadListTask;

import java.io.File;
import java.util.Comparator;
import java.util.LinkedList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

abstract public class BoardData implements NExpandableListAdapterDataInterface {
    private static final String TAG = "BoardData";
    
    public static class TYPE {
        public static final int COMPAT = 1;
    }
    
    public static class KEY {
        // //////////////////////////////////////////////////
        // 板テーブル
        // //////////////////////////////////////////////////
        public static final String TABLE = "boards";
        
        public static final String PID = "_id";
        
        public static final String NAME = "name";
        public static final String SERVER = "server";
        public static final String TAG = "tag";
        
        public static final String IS_FAVORITE = "is_favorite";
        public static final String IS_EXTERNAL = "is_external";
        
        public static final String[] FIELD_LIST = new String[] { //
        PID, //
                NAME, //
                SERVER, TAG, //
                IS_FAVORITE, //
                IS_EXTERNAL //
        };
    }
    
    public long order_id_;
    public String board_name_;
    public String board_category_;
    
    final public BoardIdentifier server_def_;
    
    public long id_;
    public boolean is_favorite_;
    public boolean is_external_;
    
    // //////////////////////////////////////////////////
    // ファクトリメソッド軍団
    // //////////////////////////////////////////////////
    
    public static class comparator implements Comparator<BoardData> {
        @Override
        public int compare(BoardData object1, BoardData object2) {
            int a = object1.getSortOrder() - object2.getSortOrder();
            if (a != 0) return a;
            return (int) (object1.id_ - object2.id_);
        }
    }
    
    /**
     * Copy Constructor
     */
    protected BoardData(BoardData boardData) {
        order_id_ = boardData.order_id_;
        board_name_ = boardData.board_name_;
        board_category_ = boardData.board_category_;
        server_def_ = boardData.server_def_;
        id_ = boardData.id_;
        is_favorite_ = boardData.is_favorite_;
        is_external_ = boardData.is_external_;
    }
    
    static public BoardData factory(long order_id, String board_name, String board_category, BoardIdentifier server_def) {
        if (BoardData2ch.is2ch(server_def.board_server_)) {
            return new BoardData2ch(order_id, board_name, board_category, server_def);
        }
        else if (BoardDataShitaraba.isShitaraba(server_def.board_server_)) {
            return new BoardDataShitaraba(order_id, board_name, board_category, server_def);
        }
        else if (BoardDataMachi.isMachiBBS(server_def.board_server_)) {
            return new BoardDataMachi(order_id, board_name, board_category, server_def);
        }
        return new BoardData2chCompat(order_id, board_name, board_category, server_def);
    }
    
    static public BoardData factory(Cursor cursor) {
        long id = cursor.getLong(cursor.getColumnIndex(KEY.PID));
        
        boolean is_favorite = cursor.getInt(cursor.getColumnIndex(KEY.IS_FAVORITE)) != 0;
        
        boolean is_external = cursor.getInt(cursor.getColumnIndex(KEY.IS_EXTERNAL)) != 0;
        
        String board_name = cursor.getString(cursor.getColumnIndex(KEY.NAME));
        String board_server = cursor.getString(cursor.getColumnIndex(KEY.SERVER));
        String board_tag = cursor.getString(cursor.getColumnIndex(KEY.TAG));
        BoardIdentifier server_def = new BoardIdentifier(board_server, board_tag, 0, 0);
        
        if (BoardData2ch.is2ch(server_def.board_server_)) {
            return new BoardData2ch(id, is_favorite, is_external, board_name, server_def);
        }
        else if (BoardDataShitaraba.isShitaraba(server_def.board_server_)) {
            return new BoardDataShitaraba(id, is_favorite, is_external, board_name, server_def);
        }
        else if (BoardDataMachi.isMachiBBS(server_def.board_server_)) {
            return new BoardDataMachi(id, is_favorite, is_external, board_name, server_def);
        }
        return new BoardData2chCompat(id, is_favorite, is_external, board_name, server_def);
    }
    
    static public BoardIdentifier factoryBoardServer(Uri uri) {
        String board_server = uri.getHost();
        if (board_server == null) board_server = "";
        if (BoardData2ch.is2ch(board_server)) {
            return BoardData2ch.createBoardIdentifier(uri);
        }
        else if (BoardDataShitaraba.isShitaraba(board_server)) {
            return BoardDataShitaraba.createBoardIdentifier(uri);
        }
        else if (BoardDataMachi.isMachiBBS(board_server)) {
            return BoardDataMachi.createBoardIdentifier(uri);
        }
        return BoardData2chCompat.createBoardIdentifier(uri);
    }
    
    static public boolean isBoardUri(String uri_string) {
        Uri uri = Uri.parse(uri_string);
        String board_server = uri.getHost();
        if (board_server == null) board_server = "";
        if (BoardData2ch.is2ch(board_server)) {
            return true;
        }
        else if (BoardDataShitaraba.isShitaraba(board_server)) {
            return true;
        }
        else if (BoardDataMachi.isMachiBBS(board_server)) {
            return true;
        }
        else if (BoardData2chCompat.is2chCompat(board_server)) {
            return true;
        }
        return false;
    }
    
    // //////////////////////////////////////////////////
    // コンストラクタ
    // //////////////////////////////////////////////////
    protected BoardData(long order_id, String board_name, String board_category, BoardIdentifier server_def) {
        order_id_ = order_id;
        board_name_ = board_name;
        board_category_ = board_category;
        server_def_ = server_def;
        is_favorite_ = false;
        is_external_ = false;
    }
    
    protected BoardData(long id, boolean is_favorite, boolean is_external, String board_name, BoardIdentifier server_def) {
        id_ = id;
        is_favorite_ = is_favorite;
        is_external_ = is_external;
        board_name_ = board_name;
        board_category_ = "";
        server_def_ = server_def;
        is_external_ = false;
    }
    
    // //////////////////////////////////////////////////
    // 掲示板サイト特有の処理
    // //////////////////////////////////////////////////
    @Override
    abstract public BoardData clone();
    
    abstract public int getSortOrder();
    
    abstract public HttpGetThreadListTask factoryGetThreadListTask(HttpGetThreadListTask.Callback callback);
    
    abstract public HttpGetBoardDataTask factoryHttpGetBoardDataTask(HttpGetBoardDataTask.Callback callback);
    
    abstract public ThreadData factoryThreadData(int sort_order, long thread_id, String thread_name, int online_count,
            int online_speed_x10);
    
    abstract public String getSubjectsURI();
    
    abstract public String getBoardTopURI();
    
    abstract public boolean canSpecialCreateNewThread(AccountPref account_pref);
    
    abstract public String getCreateNewThreadURI();
    
    abstract public boolean canCreateNewThread();
    
    abstract public CreateNewThreadTask factoryCreateNewThreadTask(TuboroidAgentManager agent_manager);
    
    public final LinkedList<String> getLocalDatDir(Context context) {
        String filename = "/" + server_def_.board_server_ + "/" + server_def_.board_tag_ + "/";
        LinkedList<String> list = new LinkedList<String>();
        
        File file = TuboroidApplication.getExternalStoragePath(context, filename);
        if (file != null) list.add(file.getAbsolutePath());
        file = TuboroidApplication.getInternalStoragePath(context, filename);
        if (file != null) list.add(file.getAbsolutePath());
        
        return list;
    }
    
    public final File getLocalSubjectFile(Context context, final boolean use_external_storage) {
        String filename = "/" + server_def_.board_server_ + "/" + server_def_.board_tag_ + "/subject.txt";
        File file = null;
        if (use_external_storage) {
            file = TuboroidApplication.getExternalStoragePath(context, filename);
        }
        if (file == null) {
            file = TuboroidApplication.getInternalStoragePath(context, filename);
        }
        return file;
    }
    
    public void importData(BoardData obj) {
        id_ = obj.id_;
        is_favorite_ = obj.is_favorite_;
        is_external_ = obj.is_external_;
    }
    
    public ContentValues getContentValues() {
        ContentValues values = new ContentValues();
        values.put(KEY.NAME, board_name_);
        values.put(KEY.SERVER, server_def_.board_server_);
        values.put(KEY.TAG, server_def_.board_tag_);
        // values.put(KEY.IS_FAVORITE, is_favorite_ ? 1 : 0);
        values.put(KEY.IS_EXTERNAL, is_external_ ? 1 : 0);
        return values;
    }
    
    @Override
    public long getId() {
        return order_id_;
    }
    
    @Override
    public String getGroupName() {
        return board_category_;
    }
    
    public boolean isSameBoard(BoardData target) {
        return server_def_.isSameBoard(target.server_def_);
    }
    
    public boolean isSameBoard(ThreadData target) {
        return server_def_.isSameBoard(target.server_def_);
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof BoardData) {
            BoardData target = (BoardData) o;
            if (isSameBoard(target)) return true;
            return false;
        }
        if (o instanceof ThreadData) {
            ThreadData target = (ThreadData) o;
            if (isSameBoard(target)) return true;
            return false;
        }
        
        return super.equals(o);
    }
    
    @Override
    public int hashCode() {
        return server_def_.hashCode();
    }
    
}
