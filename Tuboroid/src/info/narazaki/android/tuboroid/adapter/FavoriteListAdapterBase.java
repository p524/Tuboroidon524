package info.narazaki.android.tuboroid.adapter;

import info.narazaki.android.lib.adapter.FilterableListAdapterBase;
import info.narazaki.android.lib.adapter.NListAdapterDataInterface;
import info.narazaki.android.tuboroid.TuboroidApplication;

import java.util.ArrayList;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

// ////////////////////////////////////////////////////////////
// アダプタ
// ////////////////////////////////////////////////////////////
abstract public class FavoriteListAdapterBase<T_DATA extends NListAdapterDataInterface> extends
        FilterableListAdapterBase<T_DATA> {
    TuboroidApplication.ViewConfig view_config_;
    
    public FavoriteListAdapterBase(Activity activity, TuboroidApplication.ViewConfig view_config) {
        super(activity);
        setDataList(new ArrayList<T_DATA>());
        view_config_ = new TuboroidApplication.ViewConfig(view_config);
        
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());
        setShowUpdatedOnly(pref.getBoolean("favorite_recents_show_updated_only", false));
    }
    
    public void setFontSize(TuboroidApplication.ViewConfig view_config) {
        view_config_ = new TuboroidApplication.ViewConfig(view_config);
        notifyDataSetInvalidated();
    }
    
    abstract public void setShowUpdatedOnly(boolean updated_only);
}