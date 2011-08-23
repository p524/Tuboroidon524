package info.narazaki.android.tuboroid.data;

import info.narazaki.android.lib.adapter.NListAdapterDataInterface;
import info.narazaki.android.tuboroid.TuboroidApplication;
import android.view.View;

abstract public class FavoriteItemData implements NListAdapterDataInterface {
    public int order_id_ = 0;
    
    abstract public String getURI();
    
    abstract public boolean isBoard();
    
    abstract public boolean isThread();
    
    abstract public boolean isSearchKey();
    
    abstract public ThreadData getThreadData();
    
    abstract public BoardData getBoardData();
    
    abstract public Find2chKeyData getSearchKey();
    
    abstract public View setView(View view, TuboroidApplication.ViewConfig view_config, ThreadData.ViewStyle style);
    
    abstract public BoardIdentifier getServerDef();
    
}
