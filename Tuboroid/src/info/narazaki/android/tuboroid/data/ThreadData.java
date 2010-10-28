package info.narazaki.android.tuboroid.data;

import info.narazaki.android.lib.adapter.NListAdapterDataInterface;
import info.narazaki.android.tuboroid.R;
import info.narazaki.android.tuboroid.TuboroidApplication;
import info.narazaki.android.tuboroid.TuboroidApplication.AccountPref;
import info.narazaki.android.tuboroid.agent.PostEntryTask;
import info.narazaki.android.tuboroid.agent.ThreadEntryListTask;
import info.narazaki.android.tuboroid.agent.TuboroidAgentManager;
import info.narazaki.android.tuboroid.agent.task.HttpGetThreadEntryListTask;

import java.io.File;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Comparator;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

abstract public class ThreadData implements NListAdapterDataInterface {
    private static final String TAG = "ThreadData";
    
    public static class KEY {
        // //////////////////////////////////////////////////
        // スレッドリスト
        // //////////////////////////////////////////////////
        public static final String TABLE = "threads";
        
        public static final String PID = "_id";
        
        public static final String ID = "id";
        public static final String NAME = "name";
        
        // 板情報
        public static final String BOARD_NAME = "board_name";
        public static final String BOARD_SERVER = "board_server";
        public static final String BOARD_TAG = "board_tag";
        
        // オンラインの(板一覧で見た)レス数
        public static final String ONLINE_COUNT = "online_count";
        
        // ローカルに保持しているレス数
        public static final String CACHE_COUNT = "cache_count";
        // ローカルに保持している(2chオリジナルエンコードでの)サイズ
        public static final String CACHE_SIZE = "cache_size";
        public static final String CACHE_ETAG = "cache_etag";
        public static final String CACHE_TIMESTAMP = "cache_timestamp";
        
        // 既読のレス数
        public static final String RECENT_TIME = "recent_time";
        public static final String READ_COUNT = "read_count";
        public static final String RECENT_POS = "recent_pos";
        public static final String RECENT_POS_Y = "recent_pos_y";
        
        public static final String IS_FAVORITE = "is_favorite";
        public static final String IS_ALIVE = "is_alive";
        public static final String IS_DROPPED = "is_dropped";
        
        public static final String ON_EXT_STORAGE = "on_ext_storage";
        
        public static final String EDIT_DRAFT = "edit_draft";
        public static final String RECENT_POST_TIME = "recent_post_time";
        
        public static final String[] FIELD_LIST = new String[] { //
        PID, //
                ID, NAME, //
                BOARD_NAME, BOARD_SERVER, BOARD_TAG, //
                ONLINE_COUNT, //
                CACHE_COUNT, CACHE_SIZE, RECENT_TIME, CACHE_ETAG, CACHE_TIMESTAMP, //
                READ_COUNT, RECENT_POS, RECENT_POS_Y, //
                IS_FAVORITE, //
                IS_ALIVE, //
                IS_DROPPED, //
                ON_EXT_STORAGE, //
                EDIT_DRAFT, //
                RECENT_POST_TIME //
        };
        
        public static final int RECENT_ORDER_READ = 0;
        public static final int RECENT_ORDER_WRITE = 1;
    }
    
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    
    public int sort_order_;
    public long thread_pid_;
    public long thread_id_;
    
    public String thread_name_;
    
    public String board_name_;
    
    public BoardIdentifier server_def_;
    
    public int online_count_;
    public int online_speed_x10_;
    
    public int new_online_count_;
    
    public int cache_count_;
    public int cache_size_;
    public String cache_etag_;
    public String cache_timestamp_;
    
    public int working_cache_count_;
    public int working_cache_size_;
    public String working_cache_etag_;
    public String working_cache_timestamp_;
    
    public long recent_time_;
    public int read_count_;
    public long recent_pos_;
    public int recent_pos_y_;
    
    public boolean is_favorite_;
    public boolean is_dropped_;
    public boolean on_ext_storage_;
    
    public String edit_draft_;
    public long recent_post_time_;
    
    /**
     * Copy Constructor
     */
    public ThreadData(ThreadData threadData) {
        sort_order_ = threadData.sort_order_;
        thread_pid_ = threadData.thread_pid_;
        thread_id_ = threadData.thread_id_;
        thread_name_ = threadData.thread_name_;
        board_name_ = threadData.board_name_;
        server_def_ = threadData.server_def_;
        online_count_ = threadData.online_count_;
        online_speed_x10_ = threadData.online_speed_x10_;
        new_online_count_ = threadData.new_online_count_;
        cache_count_ = threadData.cache_count_;
        cache_size_ = threadData.cache_size_;
        recent_time_ = threadData.recent_time_;
        cache_etag_ = threadData.cache_etag_;
        cache_timestamp_ = threadData.cache_timestamp_;
        working_cache_count_ = threadData.working_cache_count_;
        working_cache_size_ = threadData.working_cache_size_;
        working_cache_etag_ = threadData.working_cache_etag_;
        working_cache_timestamp_ = threadData.working_cache_timestamp_;
        read_count_ = threadData.read_count_;
        recent_pos_ = threadData.recent_pos_;
        recent_pos_y_ = threadData.recent_pos_y_;
        is_favorite_ = threadData.is_favorite_;
        is_dropped_ = threadData.is_dropped_;
        on_ext_storage_ = threadData.on_ext_storage_;
        edit_draft_ = threadData.edit_draft_;
        recent_post_time_ = threadData.recent_post_time_;
    }
    
    protected ThreadData(BoardData board_data, int sort_order, long thread_id, String thread_name, int online_count,
            int online_speed_x10) {
        initData(board_data.board_name_, board_data.server_def_, sort_order, thread_id, thread_name, online_count,
                online_speed_x10);
    }
    
    protected ThreadData(String board_name, BoardIdentifier server_def, int sort_order, long thread_id,
            String thread_name, int online_count, int online_speed_x10) {
        initData(board_name, server_def, sort_order, thread_id, thread_name, online_count, online_speed_x10);
    }
    
    protected void initData(String board_name, BoardIdentifier server_def, int sort_order, long thread_id,
            String thread_name, int online_count, int online_speed_x10) {
        sort_order_ = sort_order;
        
        board_name_ = board_name;
        server_def_ = server_def;
        
        thread_id_ = thread_id;
        thread_name_ = thread_name;
        
        online_count_ = online_count;
        online_speed_x10_ = online_speed_x10;
        
        new_online_count_ = 0;
        
        cache_count_ = 0;
        
        cache_size_ = 0;
        recent_time_ = 0;
        cache_etag_ = "";
        cache_timestamp_ = "";
        
        working_cache_count_ = 0;
        working_cache_size_ = 0;
        working_cache_etag_ = "";
        working_cache_timestamp_ = "";
        
        read_count_ = 0;
        recent_pos_ = 0;
        recent_pos_y_ = 0;
        
        is_favorite_ = false;
        is_dropped_ = false;
        on_ext_storage_ = false;
        
        edit_draft_ = "";
        
        recent_post_time_ = 0;
    }
    
    protected ThreadData(Cursor cursor) {
        sort_order_ = 0;
        
        board_name_ = cursor.getString(cursor.getColumnIndex(KEY.BOARD_NAME));
        String board_server = cursor.getString(cursor.getColumnIndex(KEY.BOARD_SERVER));
        String board_tag = cursor.getString(cursor.getColumnIndex(KEY.BOARD_TAG));
        server_def_ = new BoardIdentifier(board_server, board_tag, 0, 0);
        
        thread_pid_ = cursor.getLong(cursor.getColumnIndex(KEY.PID));
        thread_id_ = cursor.getLong(cursor.getColumnIndex(KEY.ID));
        thread_name_ = cursor.getString(cursor.getColumnIndex(KEY.NAME));
        
        online_count_ = cursor.getInt(cursor.getColumnIndex(KEY.ONLINE_COUNT));
        online_speed_x10_ = 0;
        
        new_online_count_ = 0;
        
        cache_count_ = cursor.getInt(cursor.getColumnIndex(KEY.CACHE_COUNT));
        
        recent_time_ = cursor.getLong(cursor.getColumnIndex(KEY.RECENT_TIME));
        
        cache_size_ = cursor.getInt(cursor.getColumnIndex(KEY.CACHE_SIZE));
        cache_etag_ = cursor.getString(cursor.getColumnIndex(KEY.CACHE_ETAG));
        cache_timestamp_ = cursor.getString(cursor.getColumnIndex(KEY.CACHE_TIMESTAMP));
        
        working_cache_count_ = cache_count_;
        working_cache_size_ = cache_size_;
        working_cache_etag_ = cache_etag_;
        working_cache_timestamp_ = cache_timestamp_;
        
        read_count_ = cursor.getInt(cursor.getColumnIndex(KEY.READ_COUNT));
        recent_pos_ = cursor.getInt(cursor.getColumnIndex(KEY.RECENT_POS));
        recent_pos_y_ = cursor.getInt(cursor.getColumnIndex(KEY.RECENT_POS_Y));
        
        is_favorite_ = cursor.getInt(cursor.getColumnIndex(KEY.IS_FAVORITE)) != 0;
        is_dropped_ = cursor.getInt(cursor.getColumnIndex(KEY.IS_DROPPED)) != 0;
        on_ext_storage_ = cursor.getInt(cursor.getColumnIndex(KEY.ON_EXT_STORAGE)) != 0;
        
        edit_draft_ = cursor.getString(cursor.getColumnIndex(KEY.EDIT_DRAFT));
        recent_post_time_ = cursor.getLong(cursor.getColumnIndex(KEY.RECENT_POST_TIME));
    }
    
    public void up2date(ThreadData online_data) {
        is_dropped_ = false;
        
        sort_order_ = online_data.sort_order_;
        
        board_name_ = online_data.board_name_;
        server_def_ = online_data.server_def_;
        
        thread_name_ = online_data.thread_name_;
        
        new_online_count_ = online_data.online_count_ - online_count_;
        
        online_count_ = online_data.online_count_;
        online_speed_x10_ = online_data.online_speed_x10_;
    }
    
    public ContentValues getContentValues() {
        ContentValues values = new ContentValues();
        
        values.put(KEY.BOARD_NAME, board_name_);
        values.put(KEY.BOARD_SERVER, server_def_.board_server_);
        values.put(KEY.BOARD_TAG, server_def_.board_tag_);
        
        values.put(KEY.ID, thread_id_);
        values.put(KEY.NAME, thread_name_);
        
        // values.put(KEY.IS_FAVORITE, is_favorite_ ? 1 : 0);
        
        values.put(KEY.IS_DROPPED, is_dropped_ ? 1 : 0);
        values.put(KEY.ON_EXT_STORAGE, on_ext_storage_ ? 1 : 0);
        
        values.put(KEY.ONLINE_COUNT, online_count_);
        
        values.put(KEY.CACHE_COUNT, cache_count_);
        
        values.put(KEY.CACHE_SIZE, cache_size_);
        values.put(KEY.CACHE_ETAG, cache_etag_);
        values.put(KEY.CACHE_TIMESTAMP, cache_timestamp_);
        
        values.put(KEY.RECENT_TIME, recent_time_);
        
        values.put(KEY.READ_COUNT, read_count_);
        // values.put(KEY.RECENT_POS, recent_pos_);
        // values.put(KEY.RECENT_POS_Y, recent_pos_y_);
        
        return values;
    }
    
    public ContentValues getCacheTagDataContentValues() {
        ContentValues values = new ContentValues();
        
        values.put(KEY.IS_DROPPED, is_dropped_ ? 1 : 0);
        values.put(KEY.ON_EXT_STORAGE, on_ext_storage_ ? 1 : 0);
        
        values.put(KEY.ONLINE_COUNT, online_count_);
        
        values.put(KEY.CACHE_COUNT, cache_count_);
        
        values.put(KEY.CACHE_SIZE, cache_size_);
        values.put(KEY.CACHE_ETAG, cache_etag_);
        values.put(KEY.CACHE_TIMESTAMP, cache_timestamp_);
        
        values.put(KEY.RECENT_TIME, recent_time_);
        
        return values;
    }
    
    // //////////////////////////////////////////////////
    // 掲示板サイト特有の処理
    // //////////////////////////////////////////////////
    @Override
    abstract public ThreadData clone();
    
    public final File getLocalDatFile(Context context) {
        String filename = "/" + server_def_.board_server_ + "/" + server_def_.board_tag_ + "/" + thread_id_ + ".dat";
        
        if (on_ext_storage_) {
            File file = TuboroidApplication.getExternalStoragePath(context, filename);
            if (file != null) return file;
        }
        return TuboroidApplication.getInternalStoragePath(context, filename);
    }
    
    public final File getLocalAttachFileDir(Context context) {
        String dirname = "/" + server_def_.board_server_ + "/" + server_def_.board_tag_ + "/" + thread_id_ + ".attach";
        
        File file = null;
        if (on_ext_storage_) {
            file = TuboroidApplication.getExternalStoragePath(context, dirname);
        }
        if (file == null) {
            file = TuboroidApplication.getInternalStoragePath(context, dirname);
        }
        return file;
    }
    
    public final File getLocalAttachFile(Context context, String filename) {
        File dir = getLocalAttachFileDir(context);
        dir.mkdirs();
        return new File(dir, filename);
    }
    
    static public ThreadData factory(Uri uri) {
        String board_server = uri.getHost();
        if (BoardData2ch.is2ch(board_server)) {
            return ThreadData2ch.factory(uri);
        }
        else if (BoardDataShitaraba.isShitaraba(board_server)) {
            return ThreadDataShitaraba.factory(uri);
        }
        else if (BoardDataMachi.isMachiBBS(board_server)) {
            return ThreadDataMachi.factory(uri);
        }
        return ThreadData2chCompat.factory(uri);
    }
    
    static public ThreadData factory(Cursor cursor) {
        String board_server = cursor.getString(cursor.getColumnIndex(KEY.BOARD_SERVER));
        if (BoardData2ch.is2ch(board_server)) {
            return new ThreadData2ch(cursor);
        }
        else if (BoardDataShitaraba.isShitaraba(board_server)) {
            return new ThreadDataShitaraba(cursor);
        }
        else if (BoardDataMachi.isMachiBBS(board_server)) {
            return new ThreadDataMachi(cursor);
        }
        return new ThreadData2chCompat(cursor);
    }
    
    static public boolean isThreadUri(String uri_string) {
        Uri uri = Uri.parse(uri_string);
        String board_server = uri.getHost();
        if (board_server == null) return false;
        if (ThreadData2ch.is2ch(uri)) {
            return true;
        }
        else if (ThreadDataShitaraba.isShitaraba(uri)) {
            return true;
        }
        else if (ThreadDataMachi.isMachiBBS(uri)) {
            return true;
        }
        else if (ThreadData2chCompat.is2chCompat(uri)) {
            return true;
        }
        return false;
    }
    
    abstract public HttpGetThreadEntryListTask factoryGetThreadHttpGetThreadEntryListTask(String session_key,
            HttpGetThreadEntryListTask.Callback callback);
    
    abstract public PostEntryTask factoryPostEntryTask(TuboroidAgentManager agent_manager);
    
    abstract public ThreadEntryListTask factoryThreadEntryListTask(TuboroidAgentManager agent_manager);
    
    abstract public String getDatFileURI();
    
    abstract public String getSpecialDatFileURI(String session_key);
    
    abstract public String getBoardSubjectsURI();
    
    abstract public String getBoardIndexURI();
    
    abstract public String getThreadURI();
    
    abstract public String getPostEntryURI();
    
    abstract public String getPostEntryRefererURI();
    
    abstract public int getJumpEntryNum(Uri uri);
    
    abstract public boolean isFilled();
    
    abstract public boolean canSpecialRetry(AccountPref account_pref);
    
    abstract public boolean canSpecialPost(AccountPref account_pref);
    
    public boolean isSameBoard(BoardData target) {
        return server_def_.equals(target.server_def_);
    }
    
    public boolean isSameBoard(ThreadData target) {
        return server_def_.equals(target.server_def_);
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof ThreadData) {
            ThreadData target = (ThreadData) o;
            if (isSameBoard(target) && thread_id_ == target.thread_id_) return true;
            return false;
        }
        
        return super.equals(o);
    }
    
    @Override
    public int hashCode() {
        return (int) (server_def_.hashCode() + thread_id_);
    }
    
    @Override
    public long getId() {
        return thread_id_;
    }
    
    public void initWorkingCacheData() {
        working_cache_count_ = cache_count_;
        working_cache_size_ = cache_size_;
        working_cache_etag_ = cache_etag_;
        working_cache_timestamp_ = cache_timestamp_;
    }
    
    public void flushWorkingCacheData() {
        cache_count_ = working_cache_count_;
        cache_size_ = working_cache_size_;
        cache_etag_ = working_cache_etag_;
        cache_timestamp_ = working_cache_timestamp_;
    }
    
    public int getResCount() {
        return (online_count_ == 0 && cache_count_ != 0) ? cache_count_ : online_count_;
    }
    
    public boolean hasUnread() {
        return getResCount() > read_count_;
    }
    
    public static View initView(View view, TuboroidApplication.ViewConfig view_config) {
        TextView thread_id_view = (TextView) view.findViewById(R.id.thread_list_timestamp);
        thread_id_view.setTextSize(view_config.thread_list_base_);
        
        TextView thread_name_view = (TextView) view.findViewById(R.id.thread_list_name);
        thread_name_view.setTextSize(view_config.thread_list_base_);
        thread_name_view.setMinLines(2);
        
        TextView online_count_view = (TextView) view.findViewById(R.id.thread_list_online_count);
        online_count_view.setTextSize(view_config.thread_list_base_);
        
        TextView cache_count_view = (TextView) view.findViewById(R.id.thread_list_online_count_diff);
        cache_count_view.setTextSize(view_config.thread_list_base_);
        
        TextView online_speed_view = (TextView) view.findViewById(R.id.thread_list_online_count_speed);
        online_speed_view.setTextSize(view_config.thread_list_speed_);
        
        return view;
    }
    
    public View setView(View view, TuboroidApplication.ViewConfig view_config) {
        LinearLayout row_view = (LinearLayout) view;
        
        // thread id(スレが立った時間)
        Date date = new Date(thread_id_ * 1000);
        TextView thread_id_view = (TextView) row_view.findViewById(R.id.thread_list_timestamp);
        thread_id_view.setText(DATE_FORMAT.format(date));
        
        // スレのタイトル
        TextView thread_name_view = (TextView) row_view.findViewById(R.id.thread_list_name);
        thread_name_view.setText(thread_name_);
        
        // スレのレス総数
        TextView online_count_view = (TextView) row_view.findViewById(R.id.thread_list_online_count);
        int res_count = getResCount();
        online_count_view.setText(String.valueOf(res_count));
        
        // 新着レス数
        TextView cache_count_view = (TextView) row_view.findViewById(R.id.thread_list_online_count_diff);
        if (read_count_ != 0) {
            if (res_count > read_count_) {
                cache_count_view.setText("(" + String.valueOf(res_count - read_count_) + ")");
            }
            else {
                cache_count_view.setText("(0)");
            }
        }
        else {
            cache_count_view.setText("");
        }
        
        // 勢い
        if (online_speed_x10_ > 0) {
            TextView online_speed_view = (TextView) row_view.findViewById(R.id.thread_list_online_count_speed);
            online_speed_view.setText("(" + (float) online_speed_x10_ / 10 + "/day)");
        }
        return view;
    }
    
    public static class Order {
        public static final int ORDER_DEFAULT = 0;
        public static final int ORDER_SPEED = 1;
        public static final int ORDER_NEW = 2;
        public static final int ORDER_CACHED = 3;
        
        static private class BaseComparator implements Comparator<ThreadData> {
            @Override
            public int compare(ThreadData object1, ThreadData object2) {
                if (object1.is_dropped_ != object2.is_dropped_) {
                    return object1.is_dropped_ ? 1 : -1;
                }
                return 0;
            }
            
            protected int defaultCompare(ThreadData object1, ThreadData object2) {
                if (object1.sort_order_ == object2.sort_order_) return 0;
                if (object1.sort_order_ == 0) return 1;
                if (object2.sort_order_ == 0) return -1;
                return object1.sort_order_ - object2.sort_order_;
            }
        }
        
        static private class DefaultComparator extends BaseComparator {
            @Override
            public int compare(ThreadData object1, ThreadData object2) {
                int base = super.compare(object1, object2);
                if (base != 0) return base;
                return defaultCompare(object1, object2);
            }
        }
        
        private static final Comparator<ThreadData> COMP_DEFAULT = new DefaultComparator();
        
        private static final Comparator<ThreadData> COMP_SPEED = new BaseComparator() {
            @Override
            public int compare(ThreadData object1, ThreadData object2) {
                int base = super.compare(object1, object2);
                if (base != 0) return base;
                return object2.online_speed_x10_ - object1.online_speed_x10_;
            }
        };
        
        private static final Comparator<ThreadData> COMP_NEW = new BaseComparator() {
            @Override
            public int compare(ThreadData object1, ThreadData object2) {
                int base = super.compare(object1, object2);
                if (base != 0) return base;
                return (int) (object2.thread_id_ - object1.thread_id_);
            }
        };
        
        private static final Comparator<ThreadData> COMP_CACHED = new BaseComparator() {
            @Override
            public int compare(ThreadData object1, ThreadData object2) {
                int base = super.compare(object1, object2);
                if (base != 0) return base;
                
                // 片方だけ既読
                if ((object1.read_count_ > 0) != (object2.read_count_ > 0)) {
                    return object2.read_count_ - object1.read_count_;
                }
                if (object1.read_count_ > 0) {
                    // 両方既読 未読数の多い順
                    int read_count = (object1.read_count_ - object1.online_count_)
                            - (object2.read_count_ - object2.online_count_);
                    if (read_count != 0) return read_count;
                    
                    // 未読数が同じなら最近読んだ順
                    return (int) (object2.recent_time_ - object1.recent_time_);
                }
                
                return defaultCompare(object1, object2);
            }
        };
        
        public static Comparator<ThreadData> getComparator(int sort_order) {
            switch (sort_order) {
            case 1:
                return COMP_SPEED;
            case 2:
                return COMP_NEW;
            case 3:
                return COMP_CACHED;
            default:
                return COMP_DEFAULT;
            }
        }
    }
    
}
