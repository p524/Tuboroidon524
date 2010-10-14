package info.narazaki.android.tuboroid.data;

import info.narazaki.android.lib.adapter.NListAdapterDataInterface;
import android.content.ContentValues;
import android.database.Cursor;

public class IgnoreData implements NListAdapterDataInterface {
    private static final String TAG = "IgnoreData";
    
    public static class TYPE {
        public static final int NONE = 0;
        public static final int NGID = 1;
        public static final int NGID_GONE = 2;
        public static final int NGWORD = 11;
        public static final int NGWORD_GONE = 12;
    }
    
    public static boolean isNG(int type) {
        if (type != TYPE.NONE) return true;
        return false;
    }
    
    public static boolean isGone(int type) {
        if (type == TYPE.NGID_GONE || type == TYPE.NGWORD_GONE) return true;
        return false;
    }
    
    public static class KEY {
        // //////////////////////////////////////////////////
        // スレッドリスト
        // //////////////////////////////////////////////////
        public static final String TABLE = "ignores";
        
        public static final String PID = "_id";
        
        public static final String TYPE = "type";
        public static final String TOKEN = "token";
        
        public static final String[] FIELD_LIST = new String[] { //
        PID, //
                TYPE,//
                TOKEN //
        };
    }
    
    final public long pid_;
    final public int type_;
    
    final public String token_;
    
    public IgnoreData(int type, String token) {
        super();
        pid_ = 0;
        type_ = type;
        token_ = token;
    }
    
    public IgnoreData(Cursor cursor) {
        pid_ = cursor.getLong(cursor.getColumnIndex(KEY.PID));
        type_ = cursor.getInt(cursor.getColumnIndex(KEY.TYPE));
        token_ = cursor.getString(cursor.getColumnIndex(KEY.TOKEN));
    }
    
    public ContentValues getContentValues() {
        ContentValues values = new ContentValues();
        
        values.put(KEY.TYPE, type_);
        values.put(KEY.TOKEN, token_);
        
        return values;
    }
    
    @Override
    public long getId() {
        return pid_;
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof IgnoreData) {
            return ((IgnoreData) o).token_.equals(token_);
        }
        if (o instanceof String) {
            return ((String) o).equals(token_);
        }
        return super.equals(o);
    }
    
    @Override
    public int hashCode() {
        return token_.hashCode();
    }
    
}
