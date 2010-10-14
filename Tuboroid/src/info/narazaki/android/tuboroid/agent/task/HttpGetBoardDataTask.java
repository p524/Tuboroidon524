package info.narazaki.android.tuboroid.agent.task;

import info.narazaki.android.lib.agent.http.task.TextHttpGetTaskBase;
import info.narazaki.android.tuboroid.data.BoardData;

import java.io.BufferedReader;
import java.io.IOException;

import org.apache.http.HttpResponse;

abstract public class HttpGetBoardDataTask extends TextHttpGetTaskBase {
    
    static public interface Callback {
        void onCompleted(final BoardData new_board_data);
        
        void onFailed();
    }
    
    BoardData board_data_;
    Callback callback_;
    
    public HttpGetBoardDataTask(BoardData board_data, Callback callback) {
        super(board_data.getBoardTopURI());
        board_data_ = board_data;
        callback_ = callback;
    }
    
    @Override
    protected void dispatchHttpTextResponse(HttpResponse res, BufferedReader reader) throws InterruptedException,
            IOException {
        try {
            StringBuilder buf = new StringBuilder();
            while (true) {
                String line = reader.readLine();
                if (line == null) break;
                if (line.toLowerCase().indexOf("</head>") != -1) break;
                buf.append(line);
            }
            String str = buf.toString();
            String str_lc = str.toLowerCase();
            int title_index = str_lc.indexOf("<title>");
            int title_end_index = str_lc.indexOf("</title>");
            if (title_index != -1 && title_end_index != -1) {
                board_data_.board_name_ = str.substring(title_index + 7, title_end_index);
            }
        }
        finally {
            reader.close();
        }
        if (Thread.interrupted()) throw new InterruptedException();
        
        if (callback_ != null) {
            callback_.onCompleted(board_data_);
        }
        callback_ = null;
        board_data_ = null;
    }
    
    @Override
    protected void onConnectionError(boolean connectionFailed) {
        onRequestCanceled();
    }
    
    @Override
    protected void onRequestCanceled() {
        if (callback_ != null) {
            callback_.onFailed();
        }
        callback_ = null;
        board_data_ = null;
    }
    
}
