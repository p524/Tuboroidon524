package info.narazaki.android.tuboroid.data;

import info.narazaki.android.lib.agent.http.task.HttpTaskBase;
import info.narazaki.android.tuboroid.TuboroidApplication.AccountPref;
import info.narazaki.android.tuboroid.agent.PostEntryTask;
import info.narazaki.android.tuboroid.agent.PostEntryTask2ch;
import info.narazaki.android.tuboroid.agent.ThreadEntryListTask;
import info.narazaki.android.tuboroid.agent.ThreadEntryListTask2ch;
import info.narazaki.android.tuboroid.agent.TuboroidAgentManager;
import info.narazaki.android.tuboroid.agent.task.HttpGetThreadEntryListTask;
import info.narazaki.android.tuboroid.agent.task.HttpGetThreadEntryListTask2ch;

import java.util.List;

import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class ThreadData2ch extends ThreadData {
    private static final String TAG = "ThreadData2ch";
    
    private static final int ASSUME_DROP_COUNT = 1000;
    private String thread_uri_ = null;
    
    public static boolean is2ch(Uri uri) {
        String board_server = uri.getHost();
        if (!BoardData2ch.is2ch(board_server)) return false;
        try {
            List<String> segments = uri.getPathSegments();
            if (segments.size() >= 4 && segments.get(0).equals("test") && segments.get(1).equals("read.cgi")) {
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
    public ThreadData2ch(ThreadData threadData) {
        super(threadData);
    }
    
    @Override
    public ThreadData clone() {
        return new ThreadData2ch(this);
    }
    
    public ThreadData2ch(BoardData boardData, int sortOrder, long threadId, String threadName, int onlineCount,
            int online_speed_x10) {
        super(boardData, sortOrder, threadId, threadName, onlineCount, online_speed_x10);
    }
    
    public ThreadData2ch(String boardName, BoardIdentifier server_def, int sortOrder, long threadId, String threadName,
            int onlineCount, int online_speed_x10) {

        super(boardName, server_def, sortOrder, threadId, threadName, onlineCount, online_speed_x10);
    	if(threadId >= 1309031694) {
    		Log.v("testtest", "thread data creaed");
    	}   }
    
    public ThreadData2ch(Cursor cursor) {
        super(cursor);
    }
    
    @Override
    public HttpGetThreadEntryListTask factoryGetThreadHttpGetThreadEntryListTask(String session_key,
            HttpGetThreadEntryListTask.Callback callback) {
        return new HttpGetThreadEntryListTask2ch(this, session_key, callback);
    }
    
    @Override
    public PostEntryTask factoryPostEntryTask(TuboroidAgentManager agent_manager) {
        return new PostEntryTask2ch(agent_manager);
    }
    
    @Override
    public ThreadEntryListTask factoryThreadEntryListTask(TuboroidAgentManager agent_manager) {
        return new ThreadEntryListTask2ch(agent_manager);
    }
    
    @Override
    public synchronized String getThreadURI() {
        if (thread_uri_ == null) {
            thread_uri_ = "http://" + server_def_.board_server_ + "/test/read.cgi/" + server_def_.board_tag_ + "/"
                    + thread_id_ + "/";
        }
        return thread_uri_;
    }
    
    @Override
    public String getDatFileURI() {
        return "http://" + server_def_.board_server_ + "/" + server_def_.board_tag_ + "/dat/" + thread_id_ + ".dat";
    }
    
    @Override
    public String getSpecialDatFileURI(String session_key) {
        return "http://" + server_def_.board_server_ + "/test/offlaw.cgi/" + server_def_.board_tag_ + "/" + thread_id_
                + "/?raw=0.0&sid=" + HttpTaskBase.urlencode(session_key, "UTF-8");
    }
    
    @Override
    public String getBoardSubjectsURI() {
        return "http://" + server_def_.board_server_ + "/" + server_def_.board_tag_ + "/subject.txt";
    }
    
    @Override
    public String getBoardIndexURI() {
        return "http://" + server_def_.board_server_ + "/" + server_def_.board_tag_ + "/";
    }
    
    @Override
    public String getPostEntryURI() {
        return "http://" + server_def_.board_server_ + "/test/bbs.cgi";
    }
    
    @Override
    public String getPostEntryRefererURI() {
        return getPostEntryURI() + "/" + server_def_.board_tag_ + "/" + thread_id_ + "/";
    }
    
    @Override
    public boolean isFilled() {
        return read_count_ >= ASSUME_DROP_COUNT || online_count_ >= ASSUME_DROP_COUNT || is_dropped_;
    }

    @Override
    public boolean canRetryWithoutMaru() {
    	return false;
    }

    @Override
    public boolean canRetryWithMaru(AccountPref account_pref) {
    	if (server_def_.board_server_.indexOf("2ch.net") != -1
    			|| server_def_.board_server_.indexOf("bbspink.com") != -1) {
    		return account_pref.use_maru_;
    	}
    	return false;
    }

    @Override
    public boolean canSpecialPost(AccountPref account_pref) {
        if (server_def_.board_server_.indexOf("2ch.net") != -1
                || server_def_.board_server_.indexOf("bbspink.com") != -1) {
            return account_pref.use_maru_ || account_pref.use_p2_;
        }
        return false;
    }
    
    static public ThreadData factory(Uri uri) {
        BoardIdentifier board_server = BoardData2ch.createBoardIdentifier(uri);
        if (board_server.board_server_.length() > 0 && board_server.board_tag_.length() > 0) {
            return new ThreadData2ch("", board_server, 0, board_server.thread_id_, "", 0, 0);
        }
        return null;
    }
    
    @Override
    public int getJumpEntryNum(Uri uri) {
        BoardIdentifier board_server = BoardData2ch.createBoardIdentifier(uri);
        return board_server.entry_id_;
    }
    
}
