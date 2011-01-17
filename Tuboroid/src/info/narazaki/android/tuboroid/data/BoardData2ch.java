package info.narazaki.android.tuboroid.data;

import info.narazaki.android.tuboroid.TuboroidApplication.AccountPref;
import info.narazaki.android.tuboroid.agent.CreateNewThreadTask;
import info.narazaki.android.tuboroid.agent.CreateNewThreadTask2ch;
import info.narazaki.android.tuboroid.agent.TuboroidAgentManager;
import info.narazaki.android.tuboroid.agent.task.HttpGetBoardDataTask;
import info.narazaki.android.tuboroid.agent.task.HttpGetBoardDataTask2ch;
import info.narazaki.android.tuboroid.agent.task.HttpGetThreadListTask;
import info.narazaki.android.tuboroid.agent.task.HttpGetThreadListTask2ch;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.net.Uri;

public class BoardData2ch extends BoardData {
    private String subjects_uri_ = null;
    
    public static boolean is2ch(String board_server) {
        if (board_server.indexOf(".2ch.net") != -1) return true;
        if (board_server.indexOf(".bbspink.com") != -1) return true;
        return false;
    }
    
    @Override
    public int getSortOrder() {
        return 1;
    }
    
    protected BoardData2ch(BoardData2ch boardData) {
        super(boardData);
    }
    
    protected BoardData2ch(long id, boolean isFavorite, boolean is_external, String boardName,
            BoardIdentifier server_def) {
        super(id, isFavorite, is_external, boardName, server_def);
    }
    
    protected BoardData2ch(long orderId, String boardName, String boardCategory, BoardIdentifier server_def) {
        super(orderId, boardName, boardCategory, server_def);
    }
    
    static public BoardIdentifier createBoardIdentifier(Uri uri) {
        return BoardData2ch.createBoardIdentifierSub(uri, "test", null);
    }
    
    static public BoardIdentifier createBoardIdentifierSub(Uri uri, final String test_token, final String alt_cgi) {
        String board_server;
        String board_tag = "";
        long thread_id = 0;
        int entry_id = 0;
        try {
            board_server = uri.getHost();
            List<String> segments = uri.getPathSegments();
            if (segments.get(0).equals(test_token)
                    && (segments.get(1).equals("read.cgi") || (alt_cgi != null && segments.get(1).equals(alt_cgi)))) {
                // スレは /test/read.cgi/[板タグ]/[スレID]/[レス番指定] になる
                board_tag = segments.get(2);
                if (segments.size() > 3 && segments.get(3).length() > 0) {
                    thread_id = Long.parseLong(segments.get(3));
                }
                if (segments.size() > 4 && segments.get(4).length() > 0) {
                    // l : 指定件数の最新レスを表示する（レス１も表示）→ めんどくさいのでデフォルト表示
                    // n : １を除外する → 無視して良い
                    String opt = segments.get(4);
                    Pattern pattern = Pattern.compile("^([n|l]*)(\\d+)?(-)?(\\d+)?$");
                    Matcher matcher = pattern.matcher(opt);
                    if (matcher.find() && matcher.group(1) != null && matcher.group(1).indexOf('l') == -1) {
                        String target = matcher.group(2);
                        if (target != null && target.length() > 0) {
                            entry_id = Integer.parseInt(target);
                        }
                    }
                }
            }
            else {
                board_tag = segments.get(0);
            }
            return new BoardIdentifier(board_server, board_tag, thread_id, entry_id);
        }
        catch (IndexOutOfBoundsException e) {
        }
        return new BoardIdentifier("", "", 0, 0);
    }
    
    @Override
    public HttpGetThreadListTask factoryGetThreadListTask(HttpGetThreadListTask.Callback callback) {
        return new HttpGetThreadListTask2ch(this, callback);
    }
    
    @Override
    public HttpGetBoardDataTask factoryHttpGetBoardDataTask(HttpGetBoardDataTask.Callback callback) {
        return new HttpGetBoardDataTask2ch(this, callback);
    }
    
    @Override
    public ThreadData factoryThreadData(int sort_order, long thread_id, String thread_name, int online_count,
            int online_speed_x10) {
        return new ThreadData2ch(this, sort_order, thread_id, thread_name, online_count, online_speed_x10);
    }
    
    @Override
    public synchronized String getSubjectsURI() {
        if (subjects_uri_ == null) {
            subjects_uri_ = "http://" + server_def_.board_server_ + "/" + server_def_.board_tag_ + "/subject.txt";
        }
        return subjects_uri_;
    }
    
    @Override
    public String getBoardTopURI() {
        return "http://" + server_def_.board_server_ + "/" + server_def_.board_tag_ + "/";
    }
    
    @Override
    public BoardData clone() {
        return new BoardData2ch(this);
    }
    
    @Override
    public String getCreateNewThreadURI() {
        return "http://" + server_def_.board_server_ + "/test/bbs.cgi";
    }
    
    @Override
    public boolean canCreateNewThread() {
        return true;
    }
    
    @Override
    public boolean canSpecialCreateNewThread(AccountPref account_pref) {
        if (server_def_.board_server_.indexOf("2ch.net") != -1
                || server_def_.board_server_.indexOf("bbspink.com") != -1) {
            return account_pref.use_maru_;
        }
        return false;
    }
    
    @Override
    public CreateNewThreadTask factoryCreateNewThreadTask(TuboroidAgentManager agent_manager) {
        return new CreateNewThreadTask2ch(agent_manager);
    }
    
}
