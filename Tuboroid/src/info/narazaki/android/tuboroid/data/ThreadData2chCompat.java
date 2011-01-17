package info.narazaki.android.tuboroid.data;

import info.narazaki.android.tuboroid.TuboroidApplication.AccountPref;

import java.util.List;

import android.database.Cursor;
import android.net.Uri;

public class ThreadData2chCompat extends ThreadData2ch {
    private static final String TAG = "ThreadData2chCompat";
    
    public static boolean is2chCompat(Uri uri) {
        try {
            List<String> segments = uri.getPathSegments();
            for (int i = 0; i < segments.size(); i++) {
                if (segments.get(i).equals("test") && segments.get(i + 1).equals("read.cgi")) {
                    long thread_id = Long.parseLong(segments.get(i + 3));
                    if (thread_id > 0) return true;
                }
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
    public ThreadData2chCompat(ThreadData threadData) {
        super(threadData);
    }
    
    public ThreadData2chCompat(BoardData boardData, int sortOrder, long threadId, String threadName, int onlineCount,
            int online_speed_x10) {
        super(boardData, sortOrder, threadId, threadName, onlineCount, online_speed_x10);
    }
    
    public ThreadData2chCompat(String boardName, BoardIdentifier server_def, int sortOrder, long threadId,
            String threadName, int onlineCount, int online_speed_x10) {
        super(boardName, server_def, sortOrder, threadId, threadName, onlineCount, online_speed_x10);
    }
    
    public ThreadData2chCompat(Cursor cursor) {
        super(cursor);
    }
    
    static public ThreadData factory(Uri uri) {
        BoardIdentifier board_server = BoardData2chCompat.createBoardIdentifier(uri);
        if (board_server.board_server_.length() > 0 && board_server.board_tag_.length() > 0) {
            return new ThreadData2chCompat("", board_server, 0, board_server.thread_id_, "", 0, 0);
        }
        return null;
    }
    
    @Override
    public boolean canRetryWithMaru(AccountPref account_pref) {
        return false;
    }
    
    @Override
    public boolean canSpecialPost(AccountPref account_pref) {
        return account_pref.use_p2_;
    }
    
    @Override
    public int getJumpEntryNum(Uri uri) {
        BoardIdentifier board_server = BoardData2chCompat.createBoardIdentifier(uri);
        return board_server.entry_id_;
    }
}
