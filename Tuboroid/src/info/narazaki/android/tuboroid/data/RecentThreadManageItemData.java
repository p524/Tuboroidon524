package info.narazaki.android.tuboroid.data;

import info.narazaki.android.lib.adapter.NListAdapterDataInterface;
import info.narazaki.android.tuboroid.R;
import info.narazaki.android.tuboroid.TuboroidApplication;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class RecentThreadManageItemData implements NListAdapterDataInterface {
    public boolean checked_;
    public ThreadData item_;
    
    public RecentThreadManageItemData(ThreadData item) {
        super();
        item_ = item;
        checked_ = false;
    }
    
    static public View initView(View view, TuboroidApplication.ViewConfig view_config) {
        return ThreadData.initView(view, view_config);
    }
    
    public View setView(View view, TuboroidApplication.ViewConfig view_config, ThreadData.ViewStyle style) {
        LinearLayout thread_row = (LinearLayout) view.findViewById(R.id.recent_thread_row);
        item_.setView(thread_row, view_config, style);
        
        final ImageView delete_button = (ImageView) view.findViewById(R.id.recent_thread_delete_button);
        final ImageView undelete_button = (ImageView) view.findViewById(R.id.recent_thread_undelete_button);
        LinearLayout delete_button_box = (LinearLayout) view.findViewById(R.id.recent_thread_button_box);
        delete_button_box.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checked_ = !checked_;
                setDeleteButton(delete_button, undelete_button);
            }
        });
        setDeleteButton(delete_button, undelete_button);
        
        return view;
    }
    
    private void setDeleteButton(ImageView delete_button, ImageView undelete_button) {
        if (checked_) {
            delete_button.setVisibility(View.GONE);
            undelete_button.setVisibility(View.VISIBLE);
        }
        else {
            delete_button.setVisibility(View.VISIBLE);
            undelete_button.setVisibility(View.GONE);
        }
    }
    
    @Override
    public long getId() {
        return 0;
    }
    
    public BoardIdentifier getServerDef() {
        return item_.server_def_;
    }
    
}
