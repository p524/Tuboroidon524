package info.narazaki.android.tuboroid.agent.task;

import info.narazaki.android.lib.agent.http.task.TextHttpGetTaskBase;
import info.narazaki.android.lib.text.HtmlUtils;
import info.narazaki.android.tuboroid.data.BoardData;
import info.narazaki.android.tuboroid.data.ThreadData;
import info.narazaki.android.tuboroid.data.ThreadData2ch;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.HttpResponse;

public class HttpGetThreadListTask2ch extends TextHttpGetTaskBase implements HttpGetThreadListTask {
    private static final String TAG = "HttpGetThreadListTask";
    private static final int RECV_PROGRESS_INTERVAL = 1000;
    
    private Callback callback_;
    private BoardData board_data_;
    
    @Override
    protected String getTextEncode() {
        return "MS932";
    }
    
    public HttpGetThreadListTask2ch(BoardData board_data, Callback callback) {
        super(board_data.getSubjectsURI());
        callback_ = callback;
        board_data_ = board_data;
    }
    
    @Override
    protected void dispatchHttpTextResponse(HttpResponse res, BufferedReader reader) throws InterruptedException,
            IOException {
        List<ThreadData> data_list = new LinkedList<ThreadData>();
        
        try {
            long current_time = System.currentTimeMillis() / 1000;
            int sort_order = 1;
            long progress = System.currentTimeMillis();
            while (true) {
                String line = reader.readLine();
                if (line == null) break;
                
                int index_dat = line.indexOf(".dat<>");
                if (index_dat <= 0) continue;
                int index_count = line.lastIndexOf("(");
                if (index_count <= 0) continue;
                
                long thread_id = 0;
                thread_id = Long.parseLong(line.substring(0, index_dat));
                String thread_name = HtmlUtils.stripAllHtmls(line.substring(index_dat + 6, index_count).trim(), false);
                int online_count = Integer.parseInt(line.substring(index_count + 1, line.length() - 1));
                
                long thread_age = current_time - thread_id;
                if (thread_age <= 0) thread_age = 1;
                int online_speed_x10 = (int) (online_count * 60 * 60 * 24 * 10 / thread_age);
                
                ThreadData data = new ThreadData2ch(board_data_, sort_order, thread_id, thread_name, online_count,
                        online_speed_x10);
                data_list.add(data);
                sort_order++;
                if (System.currentTimeMillis() - progress > RECV_PROGRESS_INTERVAL) {
                    progress = System.currentTimeMillis();
                    onRequestProgress(data_list);
                    data_list = new LinkedList<ThreadData>();
                }
            }
            if (data_list.size() > 0) {
                onRequestProgress(data_list);
            }
        }
        finally {
            reader.close();
        }
        if (Thread.interrupted()) throw new InterruptedException();
        
        onRequestFinished();
    }
    
    private void onRequestProgress(final List<ThreadData> data_list) {
        if (callback_ != null) callback_.onReceived(data_list);
    }
    
    private void onRequestFinished() {
        if (callback_ != null) {
            callback_.onCompleted();
        }
        callback_ = null;
        board_data_ = null;
    }
    
    @Override
    protected void onConnectionError(boolean connectionFailed) {
        if (callback_ != null) {
            callback_.onConnectionFailed();
        }
        callback_ = null;
        board_data_ = null;
    }
    
    @Override
    protected void onRequestCanceled() {
        onRequestFinished();
    }
    
}
