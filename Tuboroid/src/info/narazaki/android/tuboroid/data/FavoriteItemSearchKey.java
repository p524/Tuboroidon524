package info.narazaki.android.tuboroid.data;

import info.narazaki.android.tuboroid.TuboroidApplication;
import android.database.Cursor;
import android.view.View;

public class FavoriteItemSearchKey extends FavoriteItemData {
    Find2chKeyData key_data_;
    
    static private BoardIdentifier dummy_board_server_;
    static {
        dummy_board_server_ = new BoardIdentifier("__SEARCH_DUMMY", "__", 0, 0);
    }
    
    public FavoriteItemSearchKey(Find2chKeyData key_data) {
        super();
        key_data_ = new Find2chKeyData(key_data);
    }
    
    public FavoriteItemSearchKey(Cursor cursor) {
        super();
        key_data_ = Find2chKeyData.factory(cursor);
    }
    
    @Override
    public String getURI() {
        return key_data_.getSearchURI();
    }
    
    @Override
    public long getId() {
        return 0;
    }
    
    @Override
    public boolean isThread() {
        return false;
    }
    
    @Override
    public ThreadData getThreadData() {
        return null;
    }
    
    @Override
    public boolean isSearchKey() {
        return true;
    }
    
    @Override
    public Find2chKeyData getSearchKey() {
        return key_data_;
    }
    
    @Override
    public BoardData getBoardData() {
        return null;
    }
    
    @Override
    public boolean isBoard() {
        return false;
    }
    
    @Override
    public BoardIdentifier getServerDef() {
        return dummy_board_server_;
    }
    
    @Override
    public View setView(View view, TuboroidApplication.ViewConfig view_config) {
        key_data_.setView(view, view_config);
        return view;
    }
    
}
