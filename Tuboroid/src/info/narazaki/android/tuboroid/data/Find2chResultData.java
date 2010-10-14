package info.narazaki.android.tuboroid.data;

import info.narazaki.android.lib.adapter.NListAdapterDataInterface;
import info.narazaki.android.tuboroid.R;
import info.narazaki.android.tuboroid.TuboroidApplication;

import java.sql.Date;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Find2chResultData implements NListAdapterDataInterface {
    private static final String TAG = "Find2chResultData";
    
    public long thread_id_;
    
    public String thread_name_;
    public String board_name_;
    public int online_count_;
    
    public String uri_;
    
    public Find2chResultData(long thread_id, String thread_name, String board_name, int online_count, String uri) {
        thread_id_ = thread_id;
        board_name_ = board_name;
        thread_name_ = thread_name;
        online_count_ = online_count;
        uri_ = uri;
    }
    
    @Override
    public long getId() {
        return 0;
    }
    
    public static View initView(View view, TuboroidApplication.ViewConfig view_config) {
        TextView timestamp_view = (TextView) view.findViewById(R.id.find2ch_thread_timestamp);
        timestamp_view.setTextSize(view_config.entry_header_);
        
        TextView online_count_view = (TextView) view.findViewById(R.id.find2ch_thread_onlinecount);
        online_count_view.setTextSize(view_config.entry_header_);
        
        TextView board_name_view = (TextView) view.findViewById(R.id.find2ch_board_name);
        board_name_view.setTextSize(view_config.entry_header_);
        
        TextView thread_name_view = (TextView) view.findViewById(R.id.find2ch_thread_name);
        thread_name_view.setTextSize(view_config.thread_list_base_);
        thread_name_view.setMinLines(2);
        
        return view;
    }
    
    public View setView(View view, TuboroidApplication.ViewConfig view_config) {
        LinearLayout row_view = (LinearLayout) view;
        
        TextView timestamp_view = (TextView) view.findViewById(R.id.find2ch_thread_timestamp);
        Date date = new Date(thread_id_ * 1000);
        timestamp_view.setText(ThreadData.DATE_FORMAT.format(date));
        
        TextView online_count_view = (TextView) view.findViewById(R.id.find2ch_thread_onlinecount);
        online_count_view.setText("(" + String.valueOf(online_count_) + ")");
        
        // 板名
        TextView board_name_view = (TextView) row_view.findViewById(R.id.find2ch_board_name);
        board_name_view.setText("[" + board_name_ + "]");
        
        // スレのタイトル
        TextView thread_name_view = (TextView) row_view.findViewById(R.id.find2ch_thread_name);
        thread_name_view.setText(thread_name_);
        
        return view;
    }
    
}
