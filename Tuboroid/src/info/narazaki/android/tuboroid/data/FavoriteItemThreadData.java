package info.narazaki.android.tuboroid.data;

import info.narazaki.android.tuboroid.TuboroidApplication;
import android.database.Cursor;
import android.view.View;

public class FavoriteItemThreadData extends FavoriteItemData {
    ThreadData thread_data_;
    
    public FavoriteItemThreadData(ThreadData thread_data) {
        super();
        thread_data_ = thread_data.clone();
    }
    
    public FavoriteItemThreadData(Cursor cursor) {
        super();
        thread_data_ = ThreadData.factory(cursor);
    }
    
    @Override
    public String getURI() {
        return thread_data_.getThreadURI();
    }
    
    @Override
    public long getId() {
        return 0;
    }
    
    @Override
    public boolean isThread() {
        return true;
    }
    
    @Override
    public ThreadData getThreadData() {
        return thread_data_;
    }
    
    @Override
    public boolean isSearchKey() {
        return false;
    }
    
    @Override
    public Find2chKeyData getSearchKey() {
        return null;
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
        return thread_data_.server_def_;
    }
    
    @Override
    public View setView(View view, TuboroidApplication.ViewConfig view_config) {
        thread_data_.setView(view, view_config);
        return view;
    }
    
}
