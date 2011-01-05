package info.narazaki.android.tuboroid.agent;

import info.narazaki.android.lib.agent.http.HttpMultiTaskAgent;
import info.narazaki.android.lib.agent.http.HttpSingleTaskAgent;
import info.narazaki.android.lib.agent.http.HttpTaskAgent.SaveCookieStoreCallback;
import info.narazaki.android.lib.aplication.NSimpleApplication;
import info.narazaki.android.tuboroid.R;
import info.narazaki.android.tuboroid.TuboroidApplication;
import info.narazaki.android.tuboroid.agent.http.HttpMaruTaskAgent;
import info.narazaki.android.tuboroid.agent.thread.DataFileAgent;
import info.narazaki.android.tuboroid.agent.thread.SQLiteAgent;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.PreferenceManager;

public class TuboroidAgentManager {
    private static final String TAG = "TuboroidAgentManager";
    
    private Context context_;
    
    private DataFileAgent file_agent_ = null;
    private SQLiteAgent db_agent_ = null;
    
    private HttpMultiTaskAgent multi_http_agent_ = null;
    private HttpSingleTaskAgent single_http_agent_ = null;
    private HttpSingleTaskAgent maru_http_agent_ = null;
    
    private Find2chAgent find_2ch_agent_ = null;
    
    private BoardListAgent board_list_cache_ = null;
    private ThreadListAgent thread_list_cache_ = null;
    private ThreadEntryListAgent thread_entry_list_agent_ = null;
    
    private FavoriteCacheListAgent favorite_cache_list_agent_ = null;
    private FavoriteListAgent favorite_list_agent_ = null;
    
    private IgnoreListAgent ignore_list_agent_ = null;
    
    private ImageFetchAgent image_fetch_agent_ = null;
    
    private String USER_AGENT = "Monazilla/1.00";
    private String MARU_USER_AGENT = "DOLIB/1.00";
    private int POST_ENTRY_TIMEOUT_MS = 30000;
    private int FILE_THREADS = 1;
    
    public TuboroidAgentManager(Context context) {
        context_ = context;
        
        try {
            USER_AGENT = "Monazilla/1.00 (compatible; " + context.getString(R.string.app_name) + " "
                    + context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName
                    + "; Android)";
        }
        catch (NameNotFoundException e) {
        }
        
        getIgnoreListAgent();
    }
    
    public void onResetProxyPreference() {
    	multi_http_agent_ = null;
        single_http_agent_ = null;
        maru_http_agent_ = null;
    }
    
    public String getHttpUserAgentName() {
        return USER_AGENT;
    }
    
    public Context getContext() {
        return context_;
    }
    
    public DataFileAgent getFileAgent() {
        if (file_agent_ == null) {
            synchronized (this) {
                if (file_agent_ == null) {
                    file_agent_ = new DataFileAgent(FILE_THREADS);
                }
            }
        }
        return file_agent_;
    }
    
    public SQLiteAgent getDBAgent() {
        if (db_agent_ == null) {
            synchronized (this) {
                if (db_agent_ == null) {
                    db_agent_ = new SQLiteAgent(context_);
                    db_agent_.open();
                }
            }
        }
        return db_agent_;
    }
    
    public HttpMultiTaskAgent getMultiHttpAgent() {
        if (multi_http_agent_ == null) {
            synchronized (this) {
                if (multi_http_agent_ == null) {
                    multi_http_agent_ = new HttpMultiTaskAgent(context_, USER_AGENT
                    	, ((TuboroidApplication)context_.getApplicationContext()).getProxy());
                    
                }
            }
        }
        return multi_http_agent_;
    }
    
    public HttpSingleTaskAgent getSingleHttpAgent() {
        if (single_http_agent_ == null) {
            synchronized (this) {
                if (single_http_agent_ == null) {
                    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context_);
                    String cookie_data = pref.getString("pref_cookie_data", "");
                    single_http_agent_ = new HttpSingleTaskAgent(context_, USER_AGENT
                    	, ((TuboroidApplication)context_.getApplicationContext()).getProxy());
                    single_http_agent_.setCookieStoreData(cookie_data);
                    single_http_agent_.setSaveCookieCallback(new SaveCookieStoreCallback() {
                        @Override
                        public void saveCookieStore(String cookieBareData) {
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context_)
                                    .edit();
                            editor.putString("pref_cookie_data", cookieBareData);
                            editor.commit();
                        }
                    });
                    single_http_agent_.setTimeoutMS(POST_ENTRY_TIMEOUT_MS);
                }
            }
        }
        return single_http_agent_;
    }
    
    public HttpSingleTaskAgent getMaruHttpAgent() {
        if (maru_http_agent_ == null) {
            synchronized (this) {
                if (maru_http_agent_ == null) {
                    maru_http_agent_ = new HttpMaruTaskAgent(context_, MARU_USER_AGENT
                    	, ((TuboroidApplication)context_.getApplicationContext()).getProxy());
                    maru_http_agent_.setTimeoutMS(POST_ENTRY_TIMEOUT_MS);
                }
            }
        }
        return maru_http_agent_;
    }
    
    public Find2chAgent getFind2chAgent() {
        if (find_2ch_agent_ == null) {
            synchronized (this) {
                if (find_2ch_agent_ == null) {
                    find_2ch_agent_ = new Find2chAgent(this);
                }
            }
        }
        return find_2ch_agent_;
    }
    
    public BoardListAgent getBoardListAgent() {
        if (board_list_cache_ == null) {
            synchronized (this) {
                if (board_list_cache_ == null) {
                    board_list_cache_ = new BoardListAgent(this);
                }
            }
        }
        return board_list_cache_;
    }
    
    public ThreadListAgent getThreadListAgent() {
        if (thread_list_cache_ == null) {
            synchronized (this) {
                if (thread_list_cache_ == null) {
                    thread_list_cache_ = new ThreadListAgent(this);
                }
            }
        }
        return thread_list_cache_;
    }
    
    public FavoriteCacheListAgent getFavoriteCacheListAgent() {
        if (favorite_cache_list_agent_ == null) {
            synchronized (this) {
                if (favorite_cache_list_agent_ == null) {
                    favorite_cache_list_agent_ = new FavoriteCacheListAgent(this);
                }
            }
        }
        return favorite_cache_list_agent_;
    }
    
    public FavoriteListAgent getFavoriteListAgent() {
        if (favorite_list_agent_ == null) {
            synchronized (this) {
                if (favorite_list_agent_ == null) {
                    favorite_list_agent_ = new FavoriteListAgent(this);
                }
            }
        }
        return favorite_list_agent_;
    }
    
    public IgnoreListAgent getIgnoreListAgent() {
        if (ignore_list_agent_ == null) {
            synchronized (this) {
                if (ignore_list_agent_ == null) {
                    ignore_list_agent_ = new IgnoreListAgent(this);
                }
            }
        }
        return ignore_list_agent_;
    }
    
    public ImageFetchAgent getImageFetchAgent() {
        if (image_fetch_agent_ == null) {
            synchronized (this) {
                if (image_fetch_agent_ == null) {
                    image_fetch_agent_ = new ImageFetchAgent(this);
                }
            }
        }
        return image_fetch_agent_;
    }
    
    public ThreadEntryListAgent getEntryListAgent() {
        if (thread_entry_list_agent_ == null) {
            synchronized (this) {
                if (thread_entry_list_agent_ == null) {
                    thread_entry_list_agent_ = new ThreadEntryListAgent(this);
                }
            }
        }
        return thread_entry_list_agent_;
    }
    
    public boolean isOnline() {
        return NSimpleApplication.isOnline(getContext());
    }
}
