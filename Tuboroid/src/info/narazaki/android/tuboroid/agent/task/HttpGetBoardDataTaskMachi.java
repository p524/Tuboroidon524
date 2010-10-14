package info.narazaki.android.tuboroid.agent.task;

import info.narazaki.android.tuboroid.data.BoardData;

public class HttpGetBoardDataTaskMachi extends HttpGetBoardDataTask {
    private static final String TAG = "HttpGetBoardDataTaskMachi";
    
    public HttpGetBoardDataTaskMachi(BoardData boardData, Callback callback) {
        super(boardData, callback);
    }
    
    @Override
    protected String getTextEncode() {
        return "MS932";
    }
    
}
