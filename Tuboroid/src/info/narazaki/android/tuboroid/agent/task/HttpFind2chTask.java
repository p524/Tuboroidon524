package info.narazaki.android.tuboroid.agent.task;

import info.narazaki.android.lib.agent.http.task.TextHttpGetTaskBase;
import info.narazaki.android.lib.text.HtmlUtils;
import info.narazaki.android.lib.text.TextUtils;
import info.narazaki.android.tuboroid.R;
import info.narazaki.android.tuboroid.data.Find2chResultData;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;

import android.content.Context;

public class HttpFind2chTask extends TextHttpGetTaskBase {
    private static final String TAG = "HttpFind2chTask";
    
    public static final int FETCH_COUNT = 50;
    public static final int ORDER_CREATED = 0;
    public static final int ORDER_POSTED = 1;
    
    static public interface Callback {
        void onReceived(final ArrayList<Find2chResultData> data_list, final int found_items);
        
        void onConnectionFailed();
        
        void onInterrupted();
    }
    
    private static final Pattern pattern_result_;
    private static final Pattern pattern_search_items_;
    static {
        pattern_result_ = Pattern
                .compile("<a href=\"?(http://(?:.+?\\.2ch\\.net|.+?\\.bbspink\\.com)/test/read\\.cgi/\\w+/(\\d+)).*?\\>(.+?)</a\\>.*?\\((\\d+)\\).+?<a href=.*?\\>(.+?)</a\\>");
        pattern_search_items_ = Pattern
                .compile("  <td align=right\\><font color=white size=-1\\>(\\d+)スレ中.*?</font\\></td\\>");
    }
    
    private Callback callback_;
    
    @Override
    protected String getTextEncode() {
        return "EUC-JP";
    }
    
    public HttpFind2chTask(Context context, String key, int page, int order, Callback callback) {
        super(null);
        setRequestUri(context.getString(R.string.find_2ch_search_url) + urlencode(key) + "&SORT=" + getOrder(order)
                + "&COUNT=" + FETCH_COUNT + "&OFFSET=" + FETCH_COUNT * page);
        callback_ = callback;
    }
    
    static private String getOrder(int order) {
        switch (order) {
        case ORDER_POSTED:
            return "MODIFIED";
        }
        return "CREATED";
    }
    
    @Override
    protected void dispatchHttpTextResponse(HttpResponse res, BufferedReader reader) throws InterruptedException,
            IOException {
        ArrayList<Find2chResultData> data_list = new ArrayList<Find2chResultData>();
        
        int found_items = 0;
        try {
            while (true) {
                String line = reader.readLine();
                if (line == null) break;
                if (line.startsWith("<dt>")) {
                    Matcher matcher = pattern_result_.matcher(line);
                    matcher.reset();
                    if (matcher.find() && matcher.groupCount() >= 3) {
                        String thread_url = matcher.group(1);
                        long thread_id = TextUtils.parseLong(matcher.group(2));
                        String thread_name = HtmlUtils.unescapeHtml(HtmlUtils.stripAllHtmls(matcher.group(3), true))
                                .replace("\n", "");
                        int online_count = TextUtils.parseInt(matcher.group(4));
                        String board_name = HtmlUtils.unescapeHtml(HtmlUtils.stripAllHtmls(matcher.group(5), true))
                                .replace("\n", "");
                        data_list.add(new Find2chResultData(thread_id, thread_name, board_name, online_count,
                                thread_url));
                    }
                }
                else if (line.startsWith("  <td align=right>")) {
                    Matcher matcher = pattern_search_items_.matcher(line);
                    matcher.reset();
                    if (matcher.find() && matcher.groupCount() > 0) {
                        found_items = TextUtils.parseInt(matcher.group(1));
                    }
                }
            }
        }
        finally {
            reader.close();
        }
        
        onRequestFinished(data_list, found_items);
        
    }
    
    private void onRequestFinished(final ArrayList<Find2chResultData> data_list, final int found_items) {
        if (callback_ != null) {
            callback_.onReceived(data_list, found_items);
        }
        callback_ = null;
    }
    
    @Override
    protected void onConnectionError(boolean connectionFailed) {
        if (callback_ != null) {
            callback_.onConnectionFailed();
        }
        callback_ = null;
    }
    
    @Override
    protected void onRequestCanceled() {
        if (callback_ != null) {
            callback_.onInterrupted();
        }
        callback_ = null;
    }
    
}
