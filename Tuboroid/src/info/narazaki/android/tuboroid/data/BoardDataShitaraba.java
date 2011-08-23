package info.narazaki.android.tuboroid.data;

import info.narazaki.android.tuboroid.TuboroidApplication.AccountPref;
import info.narazaki.android.tuboroid.agent.CreateNewThreadTask;
import info.narazaki.android.tuboroid.agent.CreateNewThreadTaskShitaraba;
import info.narazaki.android.tuboroid.agent.TuboroidAgentManager;
import info.narazaki.android.tuboroid.agent.task.HttpGetBoardDataTask;
import info.narazaki.android.tuboroid.agent.task.HttpGetBoardDataTaskShitaraba;
import info.narazaki.android.tuboroid.agent.task.HttpGetThreadListTask;
import info.narazaki.android.tuboroid.agent.task.HttpGetThreadListTaskShitaraba;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.net.Uri;

public class BoardDataShitaraba extends BoardData {
    private String subjects_uri_ = null;
    static private final String CATEGORY_NAME = "したらば";
    
    public static boolean isShitaraba(String board_server) {
        if (board_server.equals("jbbs.livedoor.jp")) return true;
        return false;
    }
    
    @Override
    public int getSortOrder() {
        return 1000;
    }
    
    protected BoardDataShitaraba(BoardDataShitaraba boardData) {
        super(boardData);
        board_category_ = CATEGORY_NAME;
    }
    
    protected BoardDataShitaraba(long id, boolean isFavorite, boolean is_external, String boardName,
            BoardIdentifier server_def) {
        super(id, isFavorite, is_external, boardName, server_def);
        board_category_ = CATEGORY_NAME;
    }
    
    protected BoardDataShitaraba(long orderId, String boardName, String boardCategory, BoardIdentifier server_def) {
        super(orderId, boardName, CATEGORY_NAME, server_def);
    }
    
    static public BoardIdentifier createBoardIdentifier(Uri uri) {
        String board_server;
        String board_tag = "";
        long thread_id = 0;
        int entry_id = 0;
        try {
            board_server = uri.getHost();
            List<String> segments = uri.getPathSegments();
            if (segments.size() >= 4 && segments.get(0).equals("bbs")
                    && (segments.get(1).equals("read.cgi") || (segments.get(1).equals("rawmode.cgi")))) {
                // スレは /bbs/read.cgi/[板タグ(スラッシュが入る!!)]/[スレID]/[レス番指定] になる
                board_tag = segments.get(2) + "/" + segments.get(3);
                if (segments.size() > 4 && segments.get(4).length() > 0) {
                    thread_id = info.narazaki.android.lib.text.TextUtils.parseLong(segments.get(4));
                }
                if (segments.size() > 5 && segments.get(5).length() > 0) {
                    // l : 指定件数の最新レスを表示する（レス１も表示）→ めんどくさいのでデフォルト表示
                    // n : １を除外する → 無視して良い
                    String opt = segments.get(5);
                    Pattern pattern = Pattern.compile("^([n|l]*)(\\d+)?(-)?(\\d+)?$");
                    Matcher matcher = pattern.matcher(opt);
                    if (matcher.find() && matcher.group(1) != null && matcher.group(1).indexOf('l') == -1) {
                        String target = matcher.group(2);
                        if (target != null && target.length() > 0) {
                            entry_id = info.narazaki.android.lib.text.TextUtils.parseInt(target);
                        }
                    }
                }
            }
            else if (segments.size() >= 4 && segments.get(2).equals("storage") && segments.get(3).endsWith(".html")) {
            	// 過去ログ書庫は /[板タグ(スラッシュが入る!!)]/storage/[スレID].html
            	board_tag = segments.get(0) + "/" + segments.get(1);
            	String thread_id_seg = segments.get(3);
            	if (thread_id_seg.length() > 5) {
            		String thread_id_str = thread_id_seg.substring(0, thread_id_seg.indexOf(".html"));
            		thread_id = Long.parseLong(thread_id_str);
            	}
            }
            else {
            	board_tag = segments.get(0) + "/" + segments.get(1);
            }
            return new BoardIdentifier(board_server, board_tag, thread_id, entry_id);
        }
        catch (IndexOutOfBoundsException e) {
        }
        catch (NumberFormatException e) {
        }
        return new BoardIdentifier("", "", 0, 0);
    }
    
    @Override
    public HttpGetThreadListTask factoryGetThreadListTask(HttpGetThreadListTask.Callback callback) {
        return new HttpGetThreadListTaskShitaraba(this, callback);
    }
    
    @Override
    public HttpGetBoardDataTask factoryHttpGetBoardDataTask(HttpGetBoardDataTask.Callback callback) {
        return new HttpGetBoardDataTaskShitaraba(this, callback);
    }
    
    @Override
    public ThreadData factoryThreadData(int sort_order, long thread_id, String thread_name, int online_count,
            int online_speed_x10) {
        return new ThreadDataShitaraba(this, sort_order, thread_id, thread_name, online_count, online_speed_x10);
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
        return new BoardDataShitaraba(this);
    }
    
    @Override
    public String getCreateNewThreadURI() {
        return "http://" + server_def_.board_server_ + "/bbs/write.cgi/" + server_def_.board_tag_ + "/new/";
    }
    
    @Override
    public boolean canCreateNewThread() {
        return true;
    }
    
    @Override
    public boolean canSpecialCreateNewThread(AccountPref account_pref) {
        return false;
    }
    
    @Override
    public CreateNewThreadTask factoryCreateNewThreadTask(TuboroidAgentManager agent_manager) {
        return new CreateNewThreadTaskShitaraba(agent_manager);
    }
    
}
