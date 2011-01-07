package info.narazaki.android.tuboroid.agent.task;

import info.narazaki.android.lib.text.CharsetInfo;
import info.narazaki.android.tuboroid.data.BoardData;

public class HttpGetBoardDataTask2ch extends HttpGetBoardDataTask {
    static private final String TAG = "HttpGetBoardDataTask2ch";
    
    public HttpGetBoardDataTask2ch(BoardData boardData, Callback callback) {
        super(boardData, callback);
    }
    
    @Override
    protected String getTextEncode() {
        return CharsetInfo.getEmojiShiftJis();
    }
    
}
