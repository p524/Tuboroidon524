package info.narazaki.android.tuboroid.agent.task;

import info.narazaki.android.tuboroid.data.BoardData;

public class HttpGetBoardDataTaskShitaraba extends HttpGetBoardDataTask {
    private static final String TAG = "HttpGetBoardDataTaskShitaraba";
    
    public HttpGetBoardDataTaskShitaraba(BoardData boardData, Callback callback) {
        super(boardData, callback);
    }
    
    @Override
    protected String getTextEncode() {
        return "EUC_JP";
    }
    
}