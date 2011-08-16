package info.narazaki.android.tuboroid.data;

import info.narazaki.android.lib.view.NLabelView;
import info.narazaki.android.tuboroid.R;
import info.narazaki.android.tuboroid.TuboroidApplication;
import android.database.Cursor;
import android.view.View;
import android.widget.TextView;

public class FavoriteItemBoardData extends FavoriteItemData {
    BoardData board_data_;
    
    public FavoriteItemBoardData(BoardData board_data) {
        super();
        board_data_ = board_data.clone();
    }
    
    public FavoriteItemBoardData(Cursor cursor) {
        super();
        board_data_ = BoardData.factory(cursor);
    }
    
    @Override
    public String getURI() {
        return board_data_.getSubjectsURI();
    }
    
    @Override
    public long getId() {
        return 0;
    }
    
    @Override
    public boolean isBoard() {
        return true;
    }
    
    @Override
    public BoardData getBoardData() {
        return board_data_;
    }
    
    @Override
    public ThreadData getThreadData() {
        return null;
    }
    
    @Override
    public boolean isThread() {
        return false;
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
    public BoardIdentifier getServerDef() {
        return board_data_.server_def_;
    }
    
    @Override
    public View setView(View view, TuboroidApplication.ViewConfig view_config, ThreadData.ViewStyle style) {
        NLabelView title_view = (NLabelView) view.findViewById(R.id.favorite_board_name);
        title_view.setTextSize(view_config.board_list_);
        title_view.setText(board_data_.board_name_);
        return view;
    }
    
}
