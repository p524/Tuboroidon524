package info.narazaki.android.tuboroid.data;

import info.narazaki.android.tuboroid.agent.task.HttpGetBoardDataTask;
import info.narazaki.android.tuboroid.agent.task.HttpGetBoardDataTaskMachi;
import info.narazaki.android.tuboroid.agent.task.HttpGetThreadListTask;
import info.narazaki.android.tuboroid.agent.task.HttpGetThreadListTaskMachi;
import android.net.Uri;

public class BoardDataMachi extends BoardData {
    private String subjects_uri_ = null;
    static private final String CATEGORY_NAME = "まちBBS";
    
    public static boolean isMachiBBS(String board_server) {
        if (board_server.indexOf(".machi.to") != -1) return true;
        return false;
    }
    
    @Override
    public int getSortOrder() {
        return 100;
    }
    
    protected BoardDataMachi(BoardDataMachi boardData) {
        super(boardData);
        board_category_ = CATEGORY_NAME;
    }
    
    protected BoardDataMachi(long id, boolean isFavorite, boolean is_external, String boardName,
            BoardIdentifier server_def) {
        super(id, isFavorite, is_external, boardName, server_def);
        board_category_ = CATEGORY_NAME;
    }
    
    protected BoardDataMachi(long orderId, String boardName, String boardCategory, BoardIdentifier server_def) {
        super(orderId, boardName, CATEGORY_NAME, server_def);
    }
    
    static public BoardIdentifier createBoardIdentifier(Uri uri) {
        return BoardData2ch.createBoardIdentifierSub(uri, "bbs", "offlaw.cgi");
    }
    
    @Override
    public HttpGetThreadListTask factoryGetThreadListTask(HttpGetThreadListTask.Callback callback) {
        return new HttpGetThreadListTaskMachi(this, callback);
    }
    
    @Override
    public HttpGetBoardDataTask factoryHttpGetBoardDataTask(HttpGetBoardDataTask.Callback callback) {
        return new HttpGetBoardDataTaskMachi(this, callback);
    }
    
    @Override
    public ThreadData factoryThreadData(int sort_order, long thread_id, String thread_name, int online_count,
            int online_speed_x10) {
        return new ThreadDataMachi(this, sort_order, thread_id, thread_name, online_count, online_speed_x10);
    }
    
    @Override
    public synchronized String getSubjectsURI() {
        if (subjects_uri_ == null) {
            subjects_uri_ = "http://" + server_def_.board_server_ + "/bbs/offlaw.cgi/" + server_def_.board_tag_ + "/";
        }
        return subjects_uri_;
    }
    
    @Override
    public String getBoardTopURI() {
        return "http://" + server_def_.board_server_ + "/" + server_def_.board_tag_ + "/";
    }
    
    @Override
    public BoardData clone() {
        return new BoardDataMachi(this);
    }
    
}
