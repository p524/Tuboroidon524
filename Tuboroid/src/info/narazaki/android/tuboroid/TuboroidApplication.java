package info.narazaki.android.tuboroid;

import info.narazaki.android.lib.aplication.NSimpleApplication;
import info.narazaki.android.lib.system.MigrationSDK4;
import info.narazaki.android.lib.system.MigrationSDK5;
import info.narazaki.android.lib.text.TextUtils;
import info.narazaki.android.tuboroid.activity.BoardListActivity;
import info.narazaki.android.tuboroid.activity.FavoriteListActivity;
import info.narazaki.android.tuboroid.activity.RecentListActivity;
import info.narazaki.android.tuboroid.agent.TuboroidAgent;
import info.narazaki.android.tuboroid.agent.task.HttpBoardLoginTask2chP2;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.apache.http.HttpHost;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.ImageButton;

public class TuboroidApplication extends NSimpleApplication {
    private static final String TAG = "TuboroidApplication";
    
    public static final int NOTIF_ID_BACKGROUND_UPDATED = 1;
    public static final String INTENT_KEY_CURRENT_HOME_ACTIVITY_ID = "__TuboroidApplication_home_activity_id";
    
    private TuboroidAgent agent_;
    
    public TuboroidApplication() {
        super(10);
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        setting_invalidate_checker_ = new SettingInvalidateChecker();
        agent_ = new TuboroidAgent(this);
        agent_.onCreate();
        
        IntentFilter intent_filter = new IntentFilter();
        intent_filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        intent_filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        intent_filter.addAction(Intent.ACTION_MEDIA_REMOVED);
        intent_filter.addAction(Intent.ACTION_MEDIA_SHARED);
        
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i(TAG, "BroadcastReceiver : Media");
                reloadPreferences(false);
            }
        }, intent_filter);
        
        reloadPreferences(false);
    }
    
    @Override
    public void onTerminate() {
        agent_.onTerminate();
        agent_ = null;
        super.onTerminate();
    }
    
    public TuboroidAgent getAgent() {
        return agent_;
    }
    
    // //////////////////////////////////////////////////
    // 設定全般
    // //////////////////////////////////////////////////
    public void reloadPreferences(boolean invalidate) {
        view_config_ = new ViewConfig(this);
        
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        setupAAFont();
        
        view_config_.board_list_ = TextUtils.parseInt(pref.getString("pref_font_size_board_list", "24"));
        view_config_.thread_list_base_ = TextUtils.parseInt(pref.getString("pref_font_size_thread_list_base", "13"));
        view_config_.entry_header_ = TextUtils.parseInt(pref.getString("pref_font_size_entry_header", "11"));
        view_config_.entry_body_ = TextUtils.parseInt(pref.getString("pref_font_size_entry_body", "13"));
        
        view_config_.entry_aa_body_ = TextUtils.parseInt(pref.getString("pref_font_size_entry_aa_body", "12"));
        
        view_config_.entry_divider = pref.getInt(ViewConfig.PREF_ENTRY_DIVIDER, 1);
        view_config_.scroll_button_position = pref.getInt(ViewConfig.PREF_SCROLL_BUTTON_POSITION, ViewConfig.SCROLL_BUTTON_CENTER);
        view_config_.aa_mode = pref.getInt(ViewConfig.PREF_AA_MODE, ViewConfig.AA_MODE_DEFAULT);
        
        
        
        view_config_.use_back_anchor_ = pref.getBoolean("pref_use_back_anchor", false);
        
        final float scale = getResources().getDisplayMetrics().density;
        view_config_.thumbnail_size_ = TextUtils.parseInt(pref.getString("pref_thumbnail_size", "96"));
        view_config_.real_thumbnail_size_ = (int) (view_config_.thumbnail_size_ / scale);
        
        view_config_.touch_margin_ = pref.getBoolean("pref_touch_margin_wide", false) ? 1 : 0;
        view_config_.scrolling_amount_ = TextUtils.parseInt(pref.getString("pref_scrolling_amount", "100"));
        
        boolean use_maru = pref.getBoolean("pref_use_maru", false);
        String maru_user_id = pref.getString("pref_maru_user_id", "");
        String maru_password = pref.getString("pref_maru_password", "");
        boolean use_p2 = pref.getBoolean("pref_use_p2", false);
        String p2_user_id = pref.getString("pref_p2_user_id", "");
        String p2_password = pref.getString("pref_p2_password", "");
        
        account_pref_ = new AccountPref(use_maru, use_p2, maru_user_id, maru_password, p2_user_id, p2_password);
        
        // if (invalidate) getAgent().clearCookie();
        
        getAgent().onResetProxyPreference();
        
        setVolumeButtonScrolling(pref.getBoolean("pref_use_volume_button_scrolling", false));
        setCameraButtonScrolling(pref.getBoolean("pref_use_camera_button_scrolling", false));
        
        Intent timer_updater = new Intent(BackgroundTimerUpdater.ACTION);
        timer_updater.setPackage(getPackageName());
        sendBroadcast(timer_updater);
        
        if (invalidate) notifySettingUpdated();
        
        clearExternalStorageBaseDirCache();
    }
    
    private void setupAAFont() {
        view_config_.use_ext_aa_font_ = false;
        File ext_font_file = getAAFontFile();
        if (ext_font_file != null) {
            view_config_.use_ext_aa_font_ = true;
        }
    }

    private File getAAFontFile() {
    	try {
    		File ext_font_file = getExternalFontFile();
    		if (ext_font_file != null && ext_font_file.exists()) {
    			return ext_font_file;
    		}
    	}
    	catch (SecurityException e) {
    	}
    	return null;
    }
    
    public File getExternalFontFile() {
        return getExternalStoragePath(this, getString(R.string.const_filename_AAFont));
    }
    
    @Override
    public int getScrollingAmount() {
        return view_config_.scrolling_amount_;
    }
    
    public boolean isFullScreenMode() {
        int orientation = getResources().getConfiguration().orientation;
        
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        int full_screen_mode = TextUtils.parseInt(pref.getString("pref_full_screen_mode", "0"));
        
        switch (orientation) {
        case Configuration.ORIENTATION_PORTRAIT:
            if (full_screen_mode == 1 || full_screen_mode == 3) return true;
            break;
        case Configuration.ORIENTATION_LANDSCAPE:
            if (full_screen_mode == 2 || full_screen_mode == 3) return true;
            break;
        case Configuration.ORIENTATION_SQUARE:
            if (full_screen_mode != 0) return true;
            break;
        }
        return false;
    }
    
    public int getCurrentScreenOrientation() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        int orientation = TextUtils.parseInt(pref.getString("pref_screen_orientation", "0"));
        
        switch (orientation) {
        case 1:
            return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        case 2:
            return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        }
        return ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
    }
    
    public boolean isSkipAgreementNotice() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        return pref.getBoolean("skip_agreement_notice", false);
    }
    
    public void setSkipAgreementNotice(final boolean skip) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        Editor editor = pref.edit();
        editor.putBoolean("skip_agreement_notice", skip);
        editor.commit();
    }
    
    // //////////////////////////////////////////////////
    // ストレージ
    // //////////////////////////////////////////////////
    
    File exernal_storage_base = null;
    
    File getExternalStorageBaseDir(){
    	if(exernal_storage_base == null){
    		String path = getExternalStoragePathName(this);
            if (path == null) return null;
            
            File dir = new File(Environment.getExternalStorageDirectory(), path);
            if (!dir.isDirectory() && !dir.mkdirs()) return null;
            
            // create ".nomedia"
            File nomedia = new File(dir, ".nomedia");
            try {
                if (!nomedia.isFile()) nomedia.createNewFile();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            
            exernal_storage_base = dir;
    	}
    	return exernal_storage_base;
    }
    
    void clearExternalStorageBaseDirCache(){
    	exernal_storage_base = null;
    }
    
    
    static public File getInternalStoragePath(Context context, String name) {
        try {
            File dir = new File(context.getDir("data2ch", Context.MODE_PRIVATE).toString());
            if (!dir.isDirectory() && !dir.mkdirs()) return null;
            
            File result = new File(dir, name);
            File parent = result.getParentFile();
            if (parent == null) return null;
            if (!parent.isDirectory() && !parent.mkdirs()) return null;
            return result;
        }
        catch (SecurityException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    static public File getExternalStoragePath(Context context, String name) {
        try {
        	TuboroidApplication app = (TuboroidApplication) context.getApplicationContext();
            File dir = app.getExternalStorageBaseDir();
            
            File result = new File(dir, name);
            File parent = result.getParentFile();
            if (parent == null) return null;
            if (!parent.isDirectory() && !parent.mkdirs()) return null;
            return result;
        }
        catch (SecurityException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    static public String getExternalStoragePathName(Context context) {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            // SDカードがそもそも刺さっていない
            Log.i(TAG, "SD NOT EXISTS");
            return null;
        }
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        String path = pref.getString("pref_external_storage_path", context.getString(R.string.pref_default_external_storage_path));
        if (!pref.getBoolean("pref_use_external_storage", true) || path == null || path.length() == 0) {
            return null;
        }
        return path;
    }
    
    // //////////////////////////////////////////////////
    // 設定チェッカ
    // //////////////////////////////////////////////////
    public static class SettingInvalidateChecker {
        private boolean is_invalidated_ = false;
        
        public boolean isInvalidated() {
            return is_invalidated_;
        }
        
        public void invalidate() {
            is_invalidated_ = true;
        }
    }
    
    private volatile SettingInvalidateChecker setting_invalidate_checker_;
    
    public SettingInvalidateChecker getSettingInvalidateChecker() {
        return setting_invalidate_checker_;
    }
    
    public void notifySettingUpdated() {
        setting_invalidate_checker_.invalidate();
        setting_invalidate_checker_ = new SettingInvalidateChecker();
    }
    
    // //////////////////////////////////////////////////
    // テーマ
    // //////////////////////////////////////////////////
    
    private static HashMap<Integer, Integer> theme_id_map_;
    
    static {
        theme_id_map_ = new HashMap<Integer, Integer>();
        theme_id_map_.put(1, R.style.Theme_TuboroidLight);
        theme_id_map_.put(2, R.style.Theme_TuboroidDark);
        theme_id_map_.put(3, R.style.Theme_TuboroidLightSimple);
        theme_id_map_.put(4, R.style.Theme_TuboroidDarkSimple);
        theme_id_map_.put(5, R.style.Theme_TuboroidSepia);
        theme_id_map_.put(6, R.style.Theme_TuboroidIceBlue);
        theme_id_map_.put(7, R.style.Theme_TuboroidLime);
        theme_id_map_.put(8, R.style.Theme_TuboroidSunset);
        theme_id_map_.put(9, R.style.Theme_TuboroidMidnight);
        theme_id_map_.put(10, R.style.Theme_TuboroidForest);
        theme_id_map_.put(11, R.style.Theme_Tuboroidon);
    }
    
    public int applyTheme(final Activity activity) {
        int theme_id = getCurrentThemeID();
        activity.setTheme(theme_id);
        return theme_id;
    }
    
    public boolean isLightTheme() {
        int theme_id = getCurrentThemeID();
        switch (theme_id) {
        case R.style.Theme_TuboroidLight:
        case R.style.Theme_TuboroidLightSimple:
        case R.style.Theme_TuboroidSepia:
        case R.style.Theme_TuboroidIceBlue:
        case R.style.Theme_TuboroidLime:
        case R.style.Theme_Tuboroidon:
            return true;
        }
        return false;
    }
    
    public int getCurrentThemeID() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        int theme_index = TextUtils.parseInt(pref.getString("pref_theme_setting", "1"));
        Integer theme_id = theme_id_map_.get(theme_index);
        if (theme_id == null) {
            theme_id = R.style.Theme_TuboroidLight;
        }
        return theme_id;
    }
    
    // //////////////////////////////////////////////////
    // フォント
    // //////////////////////////////////////////////////
    public static class ViewConfig {
    	public static final String PREF_ENTRY_DIVIDER 			= "pref_entry_divider";
    	public static final String PREF_SCROLL_BUTTON_POSITION	= "pref_scroll_button_position";
    	public static final String PREF_AA_MODE	= "pref_aa_mode";
    	
    	public static final int SCROLL_BUTTON_NONE 		= 0;
    	public static final int SCROLL_BUTTON_CENTER 	= 1;
    	public static final int SCROLL_BUTTON_LB 		= 2;
    	public static final int SCROLL_BUTTON_BOTTOM	= 3;
    	public static final int SCROLL_BUTTON_RB 		= 4;
    	
        private final TuboroidApplication app_;
        public int board_list_;
        public int thread_list_base_;
        public int thread_list_speed_;
        public int entry_header_;
        public int entry_body_;
        public int entry_aa_body_;
        private Typeface aa_font_;
        public int scroll_button_position;
        public int entry_divider;
        public static final int AA_MODE_DEFAULT = 0;
        public static final int AA_MODE_ALL_NOT_AA = 1;
        public static final int AA_MODE_ALL_AA = 2;
        public int aa_mode;
        
        public int thumbnail_size_;
        public int real_thumbnail_size_;
        
        public boolean use_back_anchor_;
        
        public boolean use_ext_aa_font_;
        
        public int touch_margin_;
        
        public int scrolling_amount_;
        
        public ViewConfig(TuboroidApplication app) {
            aa_font_ = null;
            app_ = app;
        }
        
        public ViewConfig(ViewConfig obj) {
            app_ = obj.app_;
            board_list_ = obj.board_list_;
            thread_list_base_ = obj.thread_list_base_;
            thread_list_speed_ = obj.thread_list_speed_;
            entry_header_ = obj.entry_header_;
            entry_body_ = obj.entry_body_;
            entry_aa_body_ = obj.entry_aa_body_;
            aa_font_ = obj.aa_font_;
            use_ext_aa_font_ = obj.use_ext_aa_font_;
            use_back_anchor_ = obj.use_back_anchor_;
            thumbnail_size_ = obj.thumbnail_size_;
            real_thumbnail_size_ = obj.real_thumbnail_size_;
            touch_margin_ = obj.touch_margin_;
            scrolling_amount_ = obj.scrolling_amount_;
            scroll_button_position = obj.scroll_button_position;
            entry_divider = obj.entry_divider;
            aa_mode = obj.aa_mode;
        }
        
        public synchronized Typeface getAAFont() {
            if (aa_font_ != null) return aa_font_;
            
            File ext_font_file = app_.getAAFontFile();
            if (ext_font_file != null) {
                if (ext_font_file.canRead()) {
                aa_font_ = MigrationSDK4.Typeface_createFromFile(ext_font_file);
                    return aa_font_;
            }
            }
            AssetManager assets = app_.getAssets();
            aa_font_ = Typeface.createFromAsset(assets, "mona-outline.ttf");
            return aa_font_;
        }
    }
    
    public volatile ViewConfig view_config_;
    
    // //////////////////////////////////////////////////
    // 2ちゃんねるビューア ●
    // //////////////////////////////////////////////////
    public static class AccountPref {
        final public boolean use_maru_;
        final public String maru_user_id_;
        final public String maru_password_;
        final public boolean use_p2_;
        final public String p2_user_id_;
        final public String p2_password_;
        
        public volatile String p2_host_;
        
        public AccountPref(boolean useMaru, boolean use_p2, String maruUserId, String maruPassword, String p2_user_id,
                String p2_password) {
            super();
            maru_user_id_ = maruUserId;
            maru_password_ = maruPassword;
            if (maruUserId.length() > 0 && maruPassword.length() > 0) {
                use_maru_ = useMaru;
            }
            else {
                use_maru_ = false;
            }
            p2_user_id_ = p2_user_id;
            p2_password_ = p2_password;
            if (p2_user_id.length() > 0 && p2_password.length() > 0) {
                use_p2_ = use_p2;
            }
            else {
                use_p2_ = false;
            }
            
            p2_host_ = HttpBoardLoginTask2chP2.P2_BASE_HOST;
        }
    }
    
    private volatile AccountPref account_pref_;
    
    public AccountPref getAccountPref() {
        return account_pref_;
    }
    
    // //////////////////////////////////////////////////
    // ネットワーク設定
    // //////////////////////////////////////////////////
    public boolean useProxy() {
    	SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
    	boolean pref_use_proxy = pref.getBoolean("pref_use_proxy", false);
    	if(!pref_use_proxy) {
    		return false;
    	}
    	int port = TextUtils.parseInt(pref.getString("pref_proxy_port", "8080"));
    	if(pref.getString("pref_proxy_host", "") == "" || port < 0 || (1 << 16) <= port) {
    		return false;
    	}
    	return true;
    }
    
    public HttpHost getProxy() {
    	if(useProxy()) {
    		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        	return new HttpHost(pref.getString("pref_proxy_host", ""), TextUtils.parseInt(pref.getString("pref_proxy_port", "8080")));
    	}else {
    		return null;
    	}
    }
    

    // //////////////////////////////////////////////////
    //
    // //////////////////////////////////////////////////
    public boolean tool_bar_visible_ = true;
    
    public static final int KEY_HOME_ACTIVITY_BOARD_LIST = 1;
    public static final int KEY_HOME_ACTIVITY_FAVORITES = 2;
    public static final int KEY_HOME_ACTIVITY_RECENTS = 3;
    
    private int home_activity_id_ = KEY_HOME_ACTIVITY_BOARD_LIST;
    
    public void createMainTabButtons(final Activity activity, final Runnable on_search_callback,
            final Runnable on_check_update_callback, final Runnable on_check_update_width_entry_callback) {
        ImageButton button_board_list = (ImageButton) activity.findViewById(R.id.button_tab_board_list);
        ImageButton button_favorite = (ImageButton) activity.findViewById(R.id.button_tab_favorite);
        ImageButton button_recents = (ImageButton) activity.findViewById(R.id.button_tab_recents);
        
        if (button_board_list != null) {
            button_board_list.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    jumpHomeTabActivity(activity, KEY_HOME_ACTIVITY_BOARD_LIST);
                }
            });
        }
        if (button_favorite != null) {
            button_favorite.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    jumpHomeTabActivity(activity, KEY_HOME_ACTIVITY_FAVORITES);
                }
            });
        }
        if (button_recents != null) {
            button_recents.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    jumpHomeTabActivity(activity, KEY_HOME_ACTIVITY_RECENTS);
                }
            });
        }
        
        final ImageButton button_search = (ImageButton) activity.findViewById(R.id.button_tab_find2ch);
        final ImageButton button_check_update = (ImageButton) activity.findViewById(R.id.button_tab_check_update);
        
        if (button_search != null && on_search_callback != null) {
            button_search.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    on_search_callback.run();
                }
            });
        }
        if (button_check_update != null && on_check_update_callback != null) {
            button_check_update.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(TuboroidApplication.this);
                    boolean download = pref.getBoolean("manual_update_fetch_entries", true);
                    if (download) {
                        on_check_update_width_entry_callback.run();
                    }
                    else {
                        on_check_update_callback.run();
                    }
                }
            });
        }
    }
    
    public void setHomeTabActivity(int home_activity_id) {
        home_activity_id_ = home_activity_id;
    }
    
    public int getHomeTabActivityID() {
        return home_activity_id_;
    }
    
    public void jumpHomeTabActivity(Activity activity) {
        jumpHomeTabActivity(activity, home_activity_id_);
    }
    
    public void jumpHomeTabActivity(Activity activity, int home_activity_id) {
        setHomeTabActivity(home_activity_id);
        Intent intent;
        switch (home_activity_id) {
        case KEY_HOME_ACTIVITY_BOARD_LIST:
            intent = new Intent(activity, BoardListActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            MigrationSDK5.Intent_addFlagNoAnimation(intent);
            activity.startActivity(intent);
            break;
        
        case KEY_HOME_ACTIVITY_FAVORITES:
            intent = new Intent(activity, FavoriteListActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            MigrationSDK5.Intent_addFlagNoAnimation(intent);
            activity.startActivity(intent);
            break;
        
        case KEY_HOME_ACTIVITY_RECENTS:
            intent = new Intent(activity, RecentListActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            MigrationSDK5.Intent_addFlagNoAnimation(intent);
            activity.startActivity(intent);
            break;
        }
    }
    
    public int getHomeTabActivityIcon() {
        switch (home_activity_id_) {
        case KEY_HOME_ACTIVITY_BOARD_LIST:
            return R.drawable.toolbar_btn_board_list;
            
        case KEY_HOME_ACTIVITY_FAVORITES:
            return R.drawable.toolbar_btn_favorite_list;
            
        case KEY_HOME_ACTIVITY_RECENTS:
            return R.drawable.toolbar_btn_recent_list;
        }
        return R.drawable.toolbar_btn_board_list;
    }
    
    // //////////////////////////////////////////////////
    // ヘルプ
    // //////////////////////////////////////////////////
    public void showHelpDialog(Activity activity, String help_uri) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        LayoutInflater layout_inflater = LayoutInflater.from(activity);
        View help_view = layout_inflater.inflate(R.layout.help_view, null);
        WebView web_view = (WebView) help_view.findViewById(R.id.help_main_box);
        web_view.loadUrl(help_uri);
        builder.setView(help_view);
        builder.setCancelable(true);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {}
        });
        builder.setInverseBackgroundForced(true);
        builder.show();
    }
}
