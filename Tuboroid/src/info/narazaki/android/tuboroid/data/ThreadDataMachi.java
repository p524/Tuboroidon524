package info.narazaki.android.tuboroid.data;

import info.narazaki.android.tuboroid.TuboroidApplication.AccountPref;
import info.narazaki.android.tuboroid.agent.PostEntryTask;
import info.narazaki.android.tuboroid.agent.PostEntryTaskMachi;
import info.narazaki.android.tuboroid.agent.ThreadEntryListTask;
import info.narazaki.android.tuboroid.agent.ThreadEntryListTaskMachi;
import info.narazaki.android.tuboroid.agent.TuboroidAgentManager;
import info.narazaki.android.tuboroid.agent.task.HttpGetThreadEntryListTask;
import info.narazaki.android.tuboroid.agent.task.HttpGetThreadEntryListTaskMachi;

import java.util.List;

import android.database.Cursor;
import android.net.Uri;

public class ThreadDataMachi extends ThreadData {
    private static final String TAG = "ThreadDataMachi";
    
    private String thread_uri_ = null;
    
    public static boolean isMachiBBS(Uri uri) {
        String board_server = uri.getHost();
        if (!BoardDataMachi.isMachiBBS(board_server)) return false;
        try {
            List<String> segments = uri.getPathSegments();
            if (segments.size() >= 4 && segments.get(0).equals("bbs") && segments.get(1).equals("read.cgi")) {
                return true;
            }
        }
        catch (IndexOutOfBoundsException e) {
        }
        catch (NumberFormatException e) {
        }
        return false;
    }
    
    /**
     * Copy Constructor
     */
    public ThreadDataMachi(ThreadData threadData) {
        super(threadData);
    }
    
    @Override
    public ThreadData clone() {
        return new ThreadDataMachi(this);
    }
    
    public ThreadDataMachi(BoardData boardData, int sortOrder, long threadId, String threadName, int onlineCount,
            int online_speed_x10) {
        super(boardData, sortOrder, threadId, threadName, onlineCount, online_speed_x10);
    }
    
    public ThreadDataMachi(String boardName, BoardIdentifier server_def, int sortOrder, long threadId,
            String threadName, int onlineCount, int online_speed_x10) {
        super(boardName, server_def, sortOrder, threadId, threadName, onlineCount, online_speed_x10);
    }
    
    public ThreadDataMachi(Cursor cursor) {
        super(cursor);
    }
    
    @Override
    public HttpGetThreadEntryListTask factoryGetThreadHttpGetThreadEntryListTask(String session_key,
            HttpGetThreadEntryListTask.Callback callback) {
        return new HttpGetThreadEntryListTaskMachi(this, null, callback);
    }
    
    @Override
    public PostEntryTask factoryPostEntryTask(TuboroidAgentManager agent_manager) {
        return new PostEntryTaskMachi(agent_manager);
    }
    
    @Override
    public ThreadEntryListTask factoryThreadEntryListTask(TuboroidAgentManager agent_manager) {
        return new ThreadEntryListTaskMachi(agent_manager);
    }
    
    @Override
    public synchronized String getThreadURI() {
        if (thread_uri_ == null) {
            thread_uri_ = "http://" + server_def_.board_server_ + "/bbs/read.cgi/" + server_def_.board_tag_ + "/"
                    + thread_id_ + "/";
        }
        return thread_uri_;
    }
    
    @Override
    public String getDatFileURI() {
        return "http://" + server_def_.board_server_ + "/bbs/offlaw.cgi/" + server_def_.board_tag_ + "/" + thread_id_
                + "/";
    }
    
    @Override
    public String getSpecialDatFileURI(String session_key) {
        return null;
    }
    
    @Override
    public String getBoardSubjectsURI() {
        return "http://" + server_def_.board_server_ + "/bbs/offlaw.cgi/" + server_def_.board_tag_ + "/";
    }
    
    @Override
    public String getBoardIndexURI() {
        return "http://" + server_def_.board_server_ + "/" + server_def_.board_tag_ + "/";
    }
    
    @Override
    public String getPostEntryURI() {
        return "http://" + server_def_.board_server_ + "/bbs/write.cgi";
    }
    
    @Override
    public String getPostEntryRefererURI() {
        return getThreadURI();
    }

    @Override
    public boolean isFilled() {
    	return is_dropped_;
    }

    @Override
    public boolean canRetryWithoutMaru() {
    	return false;
    }

    @Override
    public boolean canRetryWithMaru(AccountPref account_pref) {
    	return false;
    }

    @Override
    public boolean canSpecialPost(AccountPref account_pref) {
    	return false;
    }
    
    static public ThreadData factory(Uri uri) {
        BoardIdentifier board_server = BoardDataMachi.createBoardIdentifier(uri);
        if (board_server.board_server_.length() > 0 && board_server.board_tag_.length() > 0) {
            return new ThreadDataMachi("", board_server, 0, board_server.thread_id_, "", 0, 0);
        }
        return null;
    }
    
    @Override
    public int getJumpEntryNum(Uri uri) {
        BoardIdentifier board_server = BoardDataMachi.createBoardIdentifier(uri);
        return board_server.entry_id_;
    }
    
}
