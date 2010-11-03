package info.narazaki.android.tuboroid.activity;

import info.narazaki.android.lib.adapter.SimpleListAdapterBase;
import info.narazaki.android.lib.dialog.SimpleDialog;
import info.narazaki.android.lib.system.MigrationSDK5;
import info.narazaki.android.lib.toast.ManagedToast;
import info.narazaki.android.lib.view.SimpleSpanTextViewOnTouchListener;
import info.narazaki.android.tuboroid.R;
import info.narazaki.android.tuboroid.TuboroidApplication;
import info.narazaki.android.tuboroid.TuboroidApplication.ViewConfig;
import info.narazaki.android.tuboroid.activity.base.SearchableListActivity;
import info.narazaki.android.tuboroid.adapter.ThreadEntryListAdapter;
import info.narazaki.android.tuboroid.agent.ThreadEntryListAgent;
import info.narazaki.android.tuboroid.agent.FavoriteCacheListAgent.NextFavoriteThreadFetchedCallback;
import info.narazaki.android.tuboroid.agent.thread.SQLiteAgent;
import info.narazaki.android.tuboroid.data.IgnoreData;
import info.narazaki.android.tuboroid.data.ThreadData;
import info.narazaki.android.tuboroid.data.ThreadEntryData;
import info.narazaki.android.tuboroid.dialog.ThreadEntryListConfigDialog;
import info.narazaki.android.tuboroid.service.ITuboroidService;
import info.narazaki.android.tuboroid.service.TuboroidService;
import info.narazaki.android.tuboroid.service.TuboroidServiceTask;
import info.narazaki.android.tuboroid.service.TuboroidServiceTask.ServiceSender;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import jp.syoboi.android.ListViewEx;
import jp.syoboi.android.ListViewScrollButton;
import jp.syoboi.android.ListViewScroller;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.OvershootInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.FrameLayout.LayoutParams;
import android.widget.TextView.BufferType;

public class ThreadEntryListActivity extends SearchableListActivity {
    public static final String TAG = "ThreadEntryListActivity";
    
    public static final String INTENT_KEY_URL = "KEY_URL";
    public static final String INTENT_KEY_MAYBE_ONLINE_COUNT = "KEY_MAYBE_ONLINE_COUNT";
    public static final String INTENT_KEY_MAYBE_THREAD_NAME = "KEY_MAYBE_THREAD_NAME";
    public static final String INTENT_KEY_FILTER_PARCELABLE = "KEY_FILTER_PARCELABLE";
    
    public static final String INTENT_KEY_RESUME_ENTRY_ID = "RESUME_ENTRY_ID";
    public static final String INTENT_KEY_RESUME_Y = "KEY_RESUME_Y";
    public static final String INTENT_KEY_ANCHOR_JUMP_STACK = "KEY_ANCHOR_JUMP_STACK";
    
    public static final int INTENT_ID_SHOW_ENTRY_EDITOR = 1;
    
    // コンテキストメニュー
    private final static int CTX_MENU_REPLY_TO_ENTRY = 1;
    private final static int CTX_MENU_FIND_BY_ENTRY_ID = 2;
    private final static int CTX_MENU_FIND_RELATED_ENTRIES = 3;
    private final static int CTX_MENU_ADD_IGNORE = 4;
    private final static int CTX_MENU_DELETE_IGNORE = 5;
    private final static int CTX_MENU_COPY_TO_CLIPBOARD = 6;
    private final static int CTX_MENU_DELETE_IMAGES = 10;
    
    // メニュー
    // ツールバーの出し入れ
    public static final int MENU_KEY_TOOLBAR_1 = 10;
    public static final int MENU_KEY_TOOLBAR_2 = 11;
    
    // サーチバーの出し入れ
    public static final int MENU_KEY_SEARCH_BAR_1 = 15;
    public static final int MENU_KEY_SEARCH_BAR_2 = 16;
    
    //
    public static final int MENU_KEY_COMPOSE = 20;
    public static final int MENU_KEY_SIMILAR = 30;
    
    public static final int MENU_KEY_COPY_INFO = 40;
    
    // スレ情報
    private Uri thread_uri_;
    private ThreadData thread_data_;
    private int maybe_online_count_;
    
    // onResumeで再読み込みする(エディットから戻った時等)
    private boolean reload_on_resume_ = false;
    private boolean jump_on_resume_after_post_ = false;
    
    private PositionData global_resume_data_ = null;
    private PositionData cache_resumed_pos_data_ = null;
    
    // 検索・抽出から脱出した時の戻り先(MAXなら無効)
    private long resume_entry_id_ = Long.MAX_VALUE;
    private int resume_entry_y_ = 0;
    
    // アンカージャンプ中管理フラグ
    private LinkedList<Integer> anchor_jump_stack_ = null;
    private int restore_position;
    private int restore_position_y;
    
    // フィルタ情報
    private ParcelableFilterData filter_ = null;
    
    // プログレスバー
    private final static int DEFAULT_MAX_PROGRESS = 1000;
    private final static int POST_PROCESS_PROGRESS = 300;
    private int reload_progress_max_ = DEFAULT_MAX_PROGRESS;
    private int reload_progress_cur_ = 0;
    
    // スクロールキー
    private boolean use_scroll_key_ = false;

    // スクロール装置
    private ListViewScroller scroller = new ListViewScroller();
    
    // サービスクライアント
    private BroadcastReceiver service_intent_receiver_;
    private TuboroidServiceTask service_task_ = null;
    
    // フッタ
    private View footer_view_;
    private ThreadData next_thread_data_;
    private boolean favorite_check_update_progress_;
    private int unread_thread_count_;
    
    // ////////////////////////////////////////////////////////////
    // ステート管理系
    // ////////////////////////////////////////////////////////////
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.entry_list);
        
        registerForContextMenu(getListView());

        if (savedInstanceState == null) {
            reload_on_resume_ = true;
        }
        else {
            reload_on_resume_ = false;
        }
        service_intent_receiver_ = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, final Intent intent) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onCheckUpdateFinished(intent);
                    }
                });
            }
        };
        
        // スレッド情報の取得(URLから作れる範囲の暫定のもの)
        thread_uri_ = getIntent().getData();
        if (thread_uri_ == null) {
            if (savedInstanceState != null && savedInstanceState.containsKey(INTENT_KEY_URL)) {
                thread_uri_ = Uri.parse(savedInstanceState.getString(INTENT_KEY_URL));
            }
        }
        if (thread_uri_ == null) return;
        thread_data_ = ThreadData.factory(thread_uri_);
        if (thread_data_ == null) return;
        
        // 板情報がない時のために作成処理
        getAgent().getBoardData(Uri.parse(thread_data_.getBoardIndexURI()), false, null);
        
        // 暫定スレ情報
        maybe_online_count_ = DEFAULT_MAX_PROGRESS;
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            if (extras.containsKey(INTENT_KEY_MAYBE_ONLINE_COUNT)) {
                maybe_online_count_ = extras.getInt(INTENT_KEY_MAYBE_ONLINE_COUNT);
            }
            else if (savedInstanceState != null && savedInstanceState.containsKey(INTENT_KEY_MAYBE_ONLINE_COUNT)) {
                maybe_online_count_ = savedInstanceState.getInt(INTENT_KEY_MAYBE_ONLINE_COUNT);
            }
            if (extras.containsKey(INTENT_KEY_MAYBE_THREAD_NAME)) {
                thread_data_.thread_name_ = extras.getString(INTENT_KEY_MAYBE_THREAD_NAME);
            }
            else if (savedInstanceState != null && savedInstanceState.containsKey(INTENT_KEY_MAYBE_THREAD_NAME)) {
                thread_data_.thread_name_ = savedInstanceState.getString(INTENT_KEY_MAYBE_THREAD_NAME);
            }
        }
        
        // フィルタ
        filter_ = new ParcelableFilterData();
        resume_entry_id_ = Long.MAX_VALUE;
        resume_entry_y_ = 0;
        
        anchor_jump_stack_ = new LinkedList<Integer>();
        
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(INTENT_KEY_RESUME_ENTRY_ID)) {
                resume_entry_id_ = savedInstanceState.getLong(INTENT_KEY_RESUME_ENTRY_ID);
            }
            if (savedInstanceState.containsKey(INTENT_KEY_RESUME_Y)) {
                resume_entry_y_ = savedInstanceState.getInt(INTENT_KEY_RESUME_Y);
            }
            if (savedInstanceState.containsKey(INTENT_KEY_ANCHOR_JUMP_STACK)) {
                anchor_jump_stack_ = new LinkedList<Integer>(
                        savedInstanceState.getIntegerArrayList(INTENT_KEY_ANCHOR_JUMP_STACK));
            }
            if (savedInstanceState.containsKey(INTENT_KEY_FILTER_PARCELABLE)) {
                filter_ = savedInstanceState.getParcelable(INTENT_KEY_FILTER_PARCELABLE);
            }
        }
        
        // スレ情報読み込み
        getAgent().initNewThreadData(thread_data_, null);
        
        // リロード時ジャンプ
        int jump_on_reloaded_num = thread_data_.getJumpEntryNum(thread_uri_);
        if (jump_on_reloaded_num > 0) {
            setResumeItemPos(jump_on_reloaded_num - 1, 0);
        }
        
        // アンカーバー初期化
        updateAnchorBar();
        
        global_resume_data_ = null;
        
        // フッタ
        next_thread_data_ = null;
        favorite_check_update_progress_ = false;
        unread_thread_count_ = 0;
        View footer_row = LayoutInflater.from(this).inflate(R.layout.entry_list_footer_row, null);
        footer_view_ = footer_row.findViewById(R.id.entry_footer_box);
        
        ImageView footer_button = (ImageView) footer_view_.findViewById(R.id.entry_footer_image_view);
        footer_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onFooterClicked();
            }
        });
        footer_view_.setVisibility(View.GONE);
        getListView().addFooterView(footer_row);

		//TypedArray ta = obtainStyledAttributes(new int [] { R.attr.toolbarDarkColor });
		//getListView().setDivider(new ColorDrawable(ta.getColor(0, 0x40888888)));
		getListView().setDivider(new ColorDrawable(0x80606060));

        ListViewScrollButton btn = (ListViewScrollButton)findViewById(R.id.button_scroll);
        if (btn != null) {
        	btn.setListView(getListView());
        	btn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					scrollDown(0);
				}
			});
        }

        // ダブルタップ
        getListView().setOnTouchListener(new OnTouchListener() {
            int doubleTapPosition;
            long doubleTapTime;
            float downX;
            float downY;
            boolean doubleTap;
            final ListView listView = getListView();
            final int DOUBLE_TAP_TIMEOUT = ViewConfiguration.getDoubleTapTimeout();
                
            @Override
			public boolean onTouch(View v, MotionEvent event) {
            	switch (event.getAction()) {
            	case MotionEvent.ACTION_DOWN:
            		doubleTap = (event.getEventTime() - doubleTapTime) < DOUBLE_TAP_TIMEOUT;
            		if (doubleTap) {
            			// ダブルタップの時間制限内に押され、
            			// 1回目のダウンと2回目のダウンで同じアイテムがクリックされたか判定して
            			// 同じならばダブルタップ判定
            			int firstPosition = pointToPosition(downX, downY);
            			doubleTapPosition = pointToPosition(event.getX(), event.getY());
            			doubleTap = firstPosition == doubleTapPosition; 
            		}
            		downX = event.getX();
            		downY = event.getY();
            		doubleTapTime = event.getEventTime();
            		break;
            	case MotionEvent.ACTION_MOVE:
            		if (Math.abs(event.getX() - downX) > 20 ||
            				Math.abs(event.getY() - downY) > 20) {
            			doubleTap = false;
            		}
            		break;
            	case MotionEvent.ACTION_UP:
            		if (doubleTap) {
            			// ダブルタップ
			    		Toast.makeText(ThreadEntryListActivity.this, 
			    				getString(R.string.ctx_menu_find_related_entries),
			    				Toast.LENGTH_SHORT).show();
			    		if (doubleTapPosition != ListView.INVALID_POSITION) {
				            ThreadEntryData entry_data = ((ThreadEntryListAdapter) list_adapter_).getData(doubleTapPosition);
				            if (entry_data != null) {
				            	updateFilterByRelation(entry_data.entry_id_);
				            }
		            		return true;
			    		}
            		}
            		break;
            	}
				return false;
			}
            public int pointToPosition(float x, float y) {
            	return listView.pointToPosition((int)x, (int)y);
            }
		});
        
        // スクロールキー
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        use_scroll_key_ = pref.getBoolean("pref_use_page_up_down_key", true);
        
        applyViewConfig(getListFontPref());
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (thread_data_ != null && thread_uri_ != null) {
            outState.putString(INTENT_KEY_URL, thread_uri_.toString());
            outState.putInt(INTENT_KEY_MAYBE_ONLINE_COUNT,
                    thread_data_.online_count_ > maybe_online_count_ ? thread_data_.online_count_ : maybe_online_count_);
            outState.putString(INTENT_KEY_MAYBE_THREAD_NAME, thread_data_.thread_name_);
        }
        
        if (filter_ != null) {
            outState.putParcelable(INTENT_KEY_FILTER_PARCELABLE, filter_);
        }
        if (hasResumeItemPos()) {
            outState.putLong(INTENT_KEY_RESUME_ENTRY_ID, resume_entry_id_);
            outState.putInt(INTENT_KEY_RESUME_Y, resume_entry_y_);
        }
        outState.putIntegerArrayList(INTENT_KEY_ANCHOR_JUMP_STACK, new ArrayList<Integer>(anchor_jump_stack_));
        
        super.onSaveInstanceState(outState);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        ((ThreadEntryListAdapter) list_adapter_).setFontSize(getTuboroidApplication().view_config_);
        
        registerReceiver(service_intent_receiver_, new IntentFilter(TuboroidService.CHECK_UPDATE.ACTION_FINISHED));
        service_task_ = new TuboroidServiceTask(getApplicationContext());
        service_task_.bind();
        if (thread_data_ == null) {
            finish();
            return;
        }
        
        // スレッド情報の読み込み
        getAgent().getThreadData(thread_data_, new SQLiteAgent.GetThreadDataResult() {
            @Override
            public void onQuery(final ThreadData thread_data) {
                postListViewAndUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!is_active_) return;
                        thread_data_ = thread_data;
                        ((ThreadEntryListAdapter) list_adapter_).setReadCount(thread_data_.read_count_);
                        ((ThreadEntryListAdapter) list_adapter_).setThreadData(thread_data_);
                        int pos = (int) thread_data_.recent_pos_;
                        int pos_y = thread_data_.recent_pos_y_;
                        if (pos > 0) {
                            global_resume_data_ = new PositionData(pos, pos_y);
                        }
                        onFavoriteUpdated();
                        setTitle(thread_data_.thread_name_);
                        updateFooterRow(false, null);
                        
                        if (!isReloadInProgress()) {
                            inflateMappedPosition();
                            resumeItemPos(null);
                            clearResumeItemPos();
                        }
                    }
                });
            }
        });
        
        updateAnchorBar();
        applyViewConfig(getListFontPref());
    }
    
    @Override
    protected void onPause() {
        if (thread_data_ != null && list_adapter_ != null && list_adapter_.getCount() > 0 && hasInitialData()) {
            int bottom_pos = getListView().getLastVisiblePosition();
            if (bottom_pos == list_adapter_.getCount()) {
                ThreadEntryData bottom_entry_data = ((ThreadEntryListAdapter) list_adapter_).getData(bottom_pos - 1);
                if (bottom_entry_data != null) {
                    long bottom_id = bottom_entry_data.entry_id_ - 1;
                    thread_data_.recent_pos_ = bottom_id;
                    thread_data_.recent_pos_y_ = 0;
                    getAgent().updateThreadRecentPos(thread_data_, null);
                }
            }
            else {
                int pos = getListView().getFirstVisiblePosition();
                ThreadEntryData top_entry_data = ((ThreadEntryListAdapter) list_adapter_).getData(pos);
                if (top_entry_data != null) {
                    thread_data_.recent_pos_ = top_entry_data.entry_id_ - 1;
                    thread_data_.recent_pos_y_ = getListView().getChildAt(0).getTop();
                    getAgent().updateThreadRecentPos(thread_data_, null);
                }
            }
        }
        
        reload_progress_max_ = DEFAULT_MAX_PROGRESS;
        reload_progress_cur_ = 0;
        showProgressBar(false);
        
        unregisterReceiver(service_intent_receiver_);
        service_task_ = null;
        
        if (list_adapter_ != null) {
            favorite_check_update_progress_ = false;
            if (thread_data_ != null && list_adapter_.getCount() > 0 && hasValidData()) {
                final ThreadData thread_data = thread_data_;
                ((ThreadEntryListAdapter) list_adapter_)
                        .getInnerDataList(new ThreadEntryListAdapter.GetInnerDataListCallback<ThreadEntryData>() {
                            @Override
                            public void onFetched(final ArrayList<ThreadEntryData> dataList) {
                                if (!dataList.isEmpty()) {
                                    getAgent().storeThreadEntryListAnalyzedCache(thread_data, dataList);
                                }
                            }
                        });
            }
        }
        
        if (footer_view_ != null) footer_view_.setVisibility(View.GONE);
        super.onPause();
    }
    
    @Override
    protected void onActivityResult(int request_code, int result_code, Intent data) {
        switch (request_code) {
        case INTENT_ID_SHOW_ENTRY_EDITOR:
            if (result_code == RESULT_OK) {
                reload_on_resume_ = true;
                SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
                if (pref.getBoolean("pref_jump_bottom_on_posted", true)) {
                    jump_on_resume_after_post_ = true;
                }
            }
            break;
        default:
            super.onActivityResult(request_code, result_code, data);
            break;
        }
    }
    
    @Override
    protected SimpleListAdapterBase<?> createListAdapter() {
        ThreadEntryListAdapter list_adapter = new ThreadEntryListAdapter(this, getAgent(), getListFontPref(), 
        		new ThreadEntryData.ImageViewerLauncher() {
    		        @Override
    		        public void onRequired(ThreadData threadData, String imageLocalFilename, String imageUri) {
    		            Intent intent = new Intent(ThreadEntryListActivity.this, ImageViewerActivity.class);
    		            intent.setData(Uri.parse(threadData.getThreadURI()));
    		            intent.putExtra(ImageViewerActivity.INTENT_KEY_IMAGE_FILENAME, imageLocalFilename);
    		            intent.putExtra(ImageViewerActivity.INTENT_KEY_IMAGE_URI, imageUri);
    		            MigrationSDK5.Intent_addFlagNoAnimation(intent);
    		            startActivity(intent);
    		        }
        	    }, new ThreadEntryData.OnAnchorClickedCallback() {
        	        
        	        @Override
        	        public void onNumberAnchorClicked(int jumpFrom, int jumpTo) {
        	            if (jumpTo > 0) {
        	                jumpToAnchor(jumpFrom, jumpTo);
        	            }
        	        }
        	        
        	        @Override
        	        public void onThreadLinkClicked(Uri uri) {
        	            Intent intent = new Intent(ThreadEntryListActivity.this, ThreadEntryListActivity.class);
        	            intent.setData(uri);
        	            MigrationSDK5.Intent_addFlagNoAnimation(intent);
        	            startActivity(intent);
        	        }
        	        
        	        @Override
        	        public void onBoardLinkClicked(Uri uri) {
        	            Intent intent = new Intent(ThreadEntryListActivity.this, ThreadListActivity.class);
        	            intent.setData(uri);
        	            MigrationSDK5.Intent_addFlagNoAnimation(intent);
        	            startActivity(intent);
        	        }
        	    });
        list_adapter.setThreadData(thread_data_);
        return list_adapter;
    }
    
    @Override
    protected void onFirstDataRequired() {
        if (thread_data_ == null) return;
        updateParcelableFilter(filter_);
        onResumeDataRequired();
    }
    
    @Override
    protected void onResumeDataRequired() {
        if (thread_data_ == null) return;
        if (reload_on_resume_) {
            reloadList(true);
        }
        else {
            reloadList(false);
        }
        reload_on_resume_ = false;
    }
    
    // ////////////////////////////////////////////////////////////
    // キー管理系
    // ////////////////////////////////////////////////////////////
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	if (keyCode == KeyEvent.KEYCODE_BACK) {
    		// BACKキーが押されたときの処理
    		
    		// アンカの履歴があれば戻す
	        if (anchor_jump_stack_.size() > 0) {
	            exitAnchorJumpMode();
	            return true;
	        }
	        // フィルタされていれば戻す
	        if (filter_.type_ != ParcelableFilterData.TYPE_NONE || hasVisibleSearchBar()) {
	            cancelSearchBar();
	            return true;
	        }
    	}
        return super.onKeyDown(keyCode, event);
    }
    
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP && !isToobarForcused()) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (use_scroll_key_) {
                	int scrollAmount = getScrollingAmount();
                	if (scrollAmount == 0) {
                        setListPageUp();
                	} else {
                		scroller.scroll(getListView(), -scrollAmount/110f,
                				(event.getRepeatCount() == 0 ? true : false));
                	}
                }
                else {
                    //setListRollUp(null);
                	// dividerHeight==0の場合の問題回避
                	ListView lv = (ListView)getListView();
                	int pos = lv.getFirstVisiblePosition();
                	if (lv.getChildCount() > 0) {
                		if (lv.getChildAt(0).getTop() == 0) pos--;
                	}
                	lv.setSelection(pos);
                }
            }
            return true;
        }
        if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN && !isToobarForcused()) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
            	scrollDown(event.getRepeatCount());
            }
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    private void scrollDown(int repeatCount) {
        if (use_scroll_key_) {
        	int scrollAmount = getScrollingAmount();
        	if (scrollAmount == 0) {
                setListPageDown();
        	} else {
        		scroller.scroll(getListView(), getScrollingAmount()/110f, 
        				(repeatCount == 0 ? true : false));
        	}
        }
        else {
            //setListRollDown(null);
        	ListView lv = (ListView)getListView();
        	int pos = lv.getFirstVisiblePosition() + 1;
        	if (lv.getChildCount() > 1) {
        		// dividerHeightが0のときは、setSelection(pos)すると、
        		// getFirstVisiblePosition() は pos のままになるので、+1しただけではスクロールしない
        		if (lv.getChildAt(1).getTop()==0) pos++;
        	}
        	lv.setSelection(pos);
        }
    }
    
    private boolean isToobarForcused() {
        View forcused_view = getCurrentFocus();
        if (forcused_view == null) return false;
        ListView list_view = getListView();
        if (list_view.findFocus() == forcused_view) return false;
        
        return true;
    }
    
    // ////////////////////////////////////////////////////////////
    // アイテムタップ
    // ////////////////////////////////////////////////////////////
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menu_info) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menu_info;
        ThreadEntryData entry_data = ((ThreadEntryListAdapter) list_adapter_).getData(info.position);
        if (entry_data == null) return;
        
        menu.clear();
        menu.setHeaderTitle(String.format(getString(R.string.ctx_menu_title_entry), entry_data.entry_id_));
        menu.add(0, CTX_MENU_REPLY_TO_ENTRY, CTX_MENU_REPLY_TO_ENTRY, R.string.ctx_menu_reply_to);
        menu.add(0, CTX_MENU_COPY_TO_CLIPBOARD, CTX_MENU_COPY_TO_CLIPBOARD, R.string.ctx_menu_copy_to_clipboard);
        
        if (entry_data.canAddNGID()) {
            // ?が入ったIDはNG不可
            menu.add(0, CTX_MENU_FIND_BY_ENTRY_ID, CTX_MENU_FIND_BY_ENTRY_ID,
                    String.format(getString(R.string.ctx_menu_find_by_entry_id), entry_data.author_id_));
        }
        if (entry_data.isNG()) {
            menu.add(0, CTX_MENU_DELETE_IGNORE, CTX_MENU_DELETE_IGNORE, R.string.ctx_menu_delete_ignore);
        }
        else {
            menu.add(0, CTX_MENU_ADD_IGNORE, CTX_MENU_ADD_IGNORE, R.string.ctx_menu_add_ignore);
        }
        if (entry_data.hasShownThumbnails()) {
            menu.add(0, CTX_MENU_DELETE_IMAGES, CTX_MENU_DELETE_IMAGES, R.string.ctx_menu_delete_thumbnail_images);
        }
        
        menu.add(0, CTX_MENU_FIND_RELATED_ENTRIES, CTX_MENU_FIND_RELATED_ENTRIES,
                R.string.ctx_menu_find_related_entries);
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        ThreadEntryData entry_data = ((ThreadEntryListAdapter) list_adapter_).getData(info.position);
        if (entry_data == null) return false;
        
        switch (item.getItemId()) {
        case CTX_MENU_REPLY_TO_ENTRY:
            showDialogReplyTo(entry_data);
            break;
        case CTX_MENU_FIND_BY_ENTRY_ID:
            updateFilterByAuthorID(entry_data);
            break;
        case CTX_MENU_FIND_RELATED_ENTRIES:
            updateFilterByRelation(entry_data.entry_id_);
            break;
        case CTX_MENU_ADD_IGNORE:
            showDialogAddIgnore(entry_data);
            break;
        case CTX_MENU_DELETE_IGNORE:
            getAgent().deleteNG(entry_data);
            reloadList(false);
            break;
        case CTX_MENU_COPY_TO_CLIPBOARD:
            showDialogCopyToClipboard(entry_data);
            break;
        case CTX_MENU_DELETE_IMAGES:
            entry_data.deleteThumbnails(this, getAgent(), thread_data_);
            ((ThreadEntryListAdapter) list_adapter_).notifyDataSetChanged();
            break;
        default:
            break;
        }
        return true;
    }
    
    private void showDialogReplyTo(final ThreadEntryData entry_data) {
        String[] menu_strings = new String[] { getString(R.string.ctx_submenu_reply_to_this_entry),
                getString(R.string.ctx_submenu_quote_and_reply_to_this_entry) };
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.ctx_submenu_reply_to_title);
        builder.setItems(menu_strings, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent;
                
                switch (which) {
                case 0:
                    intent = new Intent(ThreadEntryListActivity.this, ThreadEntryEditActivity.class);
                    intent.setData(Uri.parse(thread_data_.getThreadURI()));
                    intent.putExtra(ThreadEntryEditActivity.INTENT_KEY_THREAD_DEFAULT_TEXT, ">>" + entry_data.entry_id_
                            + "\n");
                    startActivityForResult(intent, INTENT_ID_SHOW_ENTRY_EDITOR);
                    break;
                case 1:
                    intent = new Intent(ThreadEntryListActivity.this, ThreadEntryEditActivity.class);
                    intent.setData(Uri.parse(thread_data_.getThreadURI()));
                    String quoted_entry = getQuotedEntry(entry_data.entry_id_, entry_data.entry_body_);
                    intent.putExtra(ThreadEntryEditActivity.INTENT_KEY_THREAD_DEFAULT_TEXT, quoted_entry);
                    startActivityForResult(intent, INTENT_ID_SHOW_ENTRY_EDITOR);
                    break;
                }
            }
        });
        builder.create().show();
    }
    
    private void showDialogAddIgnore(final ThreadEntryData entry_data) {
        final String author_id = entry_data.author_id_;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.ctx_menu_add_ignore);
        
        if (entry_data.canAddNGID()) {
            String[] menu_strings = new String[] {
                    String.format(getString(R.string.ctx_menu_add_ignore_id_normal), entry_data.author_id_),
                    String.format(getString(R.string.ctx_menu_add_ignore_id_gone), entry_data.author_id_),
                    getString(R.string.ctx_menu_add_ignore_word_normal),
                    getString(R.string.ctx_menu_add_ignore_word_gone) };
            builder.setItems(menu_strings, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                    case 0:
                        getAgent().addNGID(author_id, IgnoreData.TYPE.NGID);
                        reloadList(false);
                        break;
                    case 1:
                        getAgent().addNGID(author_id, IgnoreData.TYPE.NGID_GONE);
                        reloadList(false);
                        break;
                    case 2:
                        showDialogAddNGWord(entry_data, false);
                        break;
                    case 3:
                        showDialogAddNGWord(entry_data, true);
                        break;
                    }
                }
            });
        }
        else {
            String[] menu_strings = new String[] { getString(R.string.ctx_menu_add_ignore_word_normal),
                    getString(R.string.ctx_menu_add_ignore_word_gone) };
            builder.setItems(menu_strings, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                    case 0:
                        showDialogAddNGWord(entry_data, false);
                        break;
                    case 1:
                        showDialogAddNGWord(entry_data, true);
                        break;
                    }
                }
            });
        }
        
        builder.create().show();
    }
    
    private void showDialogAddNGWord(final ThreadEntryData entry_data, final boolean gone) {
        // ビュー作成
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (!gone) {
            builder.setTitle(R.string.ctx_menu_add_ignore_word_normal);
        }
        else {
            builder.setTitle(R.string.ctx_menu_add_ignore_word_gone);
        }
        
        LayoutInflater layout_inflater = LayoutInflater.from(this);
        LinearLayout layout_view = (LinearLayout) layout_inflater.inflate(R.layout.add_ngword_dialog, null);
        builder.setView(layout_view);
        
        final int type = gone ? IgnoreData.TYPE.NGWORD_GONE : IgnoreData.TYPE.NGWORD;
        
        final EditText ngword_token = (EditText) layout_view.findViewById(R.id.add_ngword_token);
        final EditText ngword_orig = (EditText) layout_view.findViewById(R.id.add_ngword_orig);
        StringBuilder orig_text = new StringBuilder();
        orig_text.append(entry_data.author_name_);
        orig_text.append("\n");
        orig_text.append(entry_data.entry_body_);
        ngword_orig.setText(orig_text);
        ngword_orig.setSingleLine(false);
        
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                getAgent().addNGWord(ngword_token.getText().toString(), type);
                reloadList(false);
            }
        });
        builder.setCancelable(true);
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {}
        });
        builder.create().show();
    }
    
    private void showDialogCopyToClipboard(final ThreadEntryData entry_data) {
        String[] menu_strings = new String[] { getString(R.string.ctx_submenu_copy_to_clipboard_id),
                getString(R.string.ctx_submenu_copy_to_clipboard_name),
                getString(R.string.ctx_submenu_copy_to_clipboard_body) };
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.ctx_menu_copy_to_clipboard);
        builder.setItems(menu_strings, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                switch (which) {
                case 0:
                    cm.setText(entry_data.author_id_);
                    break;
                case 1:
                    cm.setText(entry_data.author_name_);
                    break;
                case 2:
                    showDialogCopyEntryBody(entry_data);
                    return;
                default:
                    return;
                }
                ManagedToast.raiseToast(getApplicationContext(), R.string.toast_copied);
            }
        });
        builder.create().show();
    }
    
    private void showDialogCopyThreadInfoToClipboard() {
        String[] menu_strings = new String[] { getString(R.string.label_submenu_copy_thread_info_title),
                getString(R.string.label_submenu_copy_thread_info_url),
                getString(R.string.label_submenu_copy_thread_info_title_url) };
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.label_menu_copy_thread_info);
        builder.setItems(menu_strings, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                switch (which) {
                case 0:
                    cm.setText(thread_data_.thread_name_);
                    break;
                case 1:
                    cm.setText(thread_data_.getThreadURI());
                    break;
                case 2:
                    cm.setText(thread_data_.thread_name_ + "\n" + thread_data_.getThreadURI());
                    break;
                default:
                    return;
                }
                ManagedToast.raiseToast(getApplicationContext(), R.string.toast_copied);
            }
        });
        builder.create().show();
    }
    
    // レス部分コピー
    private void showDialogCopyEntryBody(final ThreadEntryData entry_data) {
        // ビュー作成
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater layout_inflater = LayoutInflater.from(this);
        LinearLayout layout_view = (LinearLayout) layout_inflater.inflate(R.layout.copy_entry_body_dialog, null);
        builder.setView(layout_view);
        
        final EditText copy_orig = (EditText) layout_view.findViewById(R.id.copy_orig);
        copy_orig.setText(entry_data.entry_body_);
        copy_orig.setSingleLine(false);
        
        builder.setCancelable(true);
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {}
        });
        
        builder.create().show();
    }
    
    // ////////////////////////////////////////////////////////////
    // オプションメニュー
    // ////////////////////////////////////////////////////////////
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        
        // ツールバー
        createToolBarOptionMenu(menu, MENU_KEY_TOOLBAR_1, MENU_KEY_TOOLBAR_2, MENU_KEY_SEARCH_BAR_1,
                MENU_KEY_SEARCH_BAR_2);
        
        // 書き込み
        MenuItem compose_item = menu.add(0, MENU_KEY_COMPOSE, MENU_KEY_COMPOSE, getString(R.string.label_menu_compose));
        compose_item.setIcon(R.drawable.ic_menu_compose);
        compose_item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent intent = new Intent(ThreadEntryListActivity.this, ThreadEntryEditActivity.class);
                intent.setData(Uri.parse(thread_data_.getThreadURI()));
                startActivityForResult(intent, INTENT_ID_SHOW_ENTRY_EDITOR);
                return false;
            }
        });
        
        // 類似検索
        MenuItem similar_item = menu.add(0, MENU_KEY_SIMILAR, MENU_KEY_SIMILAR,
                getString(R.string.label_menu_find_similar_thread));
        similar_item.setIcon(android.R.drawable.ic_menu_gallery);
        similar_item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent intent = new Intent(ThreadEntryListActivity.this, SimilarThreadListActivity.class);
                intent.setData(Uri.parse(thread_data_.getBoardSubjectsURI()));
                intent.putExtra(SimilarThreadListActivity.KEY_SEARCH_KEY_NAME, thread_data_.thread_name_);
                intent.putExtra(SimilarThreadListActivity.KEY_SEARCH_THREAD_ID, thread_data_.thread_id_);
                MigrationSDK5.Intent_addFlagNoAnimation(intent);
                startActivity(intent);
                return false;
            }
        });
        
        // スレ情報コピー
        MenuItem copy_info_item = menu.add(0, MENU_KEY_COPY_INFO, MENU_KEY_COPY_INFO,
                getString(R.string.label_menu_copy_thread_info));
        copy_info_item.setIcon(android.R.drawable.ic_menu_agenda);
        copy_info_item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                showDialogCopyThreadInfoToClipboard();
                return false;
            }
        });
        
        getMenuInflater().inflate(R.menu.thread_entry_list_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    	case R.id.menu_size_setting:
    		showSizeSettingDialog();
    		break;
    	}
    	return super.onOptionsItemSelected(item);
    }
    
    // ////////////////////////////////////////////////////////////
    // ダイアログ
    // ////////////////////////////////////////////////////////////
    private void showSizeSettingDialog() {
    	
    	final ListView listView = getListView();
    	final int pos = listView.getFirstVisiblePosition();
    	
    	ThreadEntryListConfigDialog dlg = new ThreadEntryListConfigDialog(this,
    			null,
    			new ThreadEntryListConfigDialog.OnChangedListener() {
					@Override
					public void onChanged(ViewConfig config) {
						listView.setSelectionFromTop(pos, 0);
				    	ThreadEntryListAdapter adapter = (ThreadEntryListAdapter) list_adapter_;
				    	adapter.setFontSize(new ViewConfig(config));
				    	applyViewConfig(config);
				    	listView.invalidateViews();
				    	
				    	listView.setSelectionFromTop(pos, 0);
					}
    			}
    	);
    	dlg.show();
    }
    
    private void applyViewConfig(ViewConfig view_config) {
    	ListView lv = getListView();
    	Resources res = getResources();
    	
    	lv.setDividerHeight(view_config.entry_divider > 0 ? 
    			res.getDimensionPixelSize(R.dimen.entryDividerHeight) : 0);

    	ListViewScrollButton sb = (ListViewScrollButton)findViewById(R.id.button_scroll);
    	if (sb != null) {
    		FrameLayout.LayoutParams lp = (LayoutParams) sb.getLayoutParams();
    		lp.bottomMargin = res.getDimensionPixelSize(R.dimen.scrollButtonMargin);
    		lp.leftMargin = lp.rightMargin = lp.bottomMargin;
    		boolean visibility = true;
    		boolean reverse = true;
    		switch (view_config.scroll_button_position) {
    		case ViewConfig.SCROLL_BUTTON_CENTER:
        		lp.bottomMargin = res.getDimensionPixelSize(R.dimen.scrollButtonBottomMargin);
        		lp.gravity = Gravity.CENTER | Gravity.BOTTOM;
        		reverse = false;
        		break;
    		case ViewConfig.SCROLL_BUTTON_BOTTOM:
        		lp.gravity = Gravity.CENTER | Gravity.BOTTOM;
    			break;
    		case ViewConfig.SCROLL_BUTTON_LB:
    			lp.gravity = Gravity.LEFT | Gravity.BOTTOM;
    			break;
    		case ViewConfig.SCROLL_BUTTON_RB:
    			lp.gravity = Gravity.RIGHT | Gravity.BOTTOM;
    			break;
    		case ViewConfig.SCROLL_BUTTON_NONE:
    			visibility = false;
    			break;
    		}
    		sb.setReverse(reverse);
    		sb.setLayoutParams(lp);
    		sb.setVisibility(visibility ? View.VISIBLE : View.GONE);
    	}
    }
    
    // ////////////////////////////////////////////////////////////
    // ツールバー
    // ////////////////////////////////////////////////////////////
    
    @Override
    protected void createToolbarButtons() {
        super.createToolbarButtons();
        
        ImageButton button_thread_list = (ImageButton) findViewById(R.id.button_toolbar_thread_list);
        button_thread_list.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ThreadEntryListActivity.this, ThreadListActivity.class);
                intent.setData(Uri.parse(thread_data_.getBoardSubjectsURI()));
                startActivityForResult(intent, INTENT_ID_SHOW_ENTRY_EDITOR);
            }
        });
    }
    
    // ////////////////////////////////////////////////////////////
    // フッタ
    // ////////////////////////////////////////////////////////////
    
    private void updateFooterRow(final boolean maybe_has_new_unread, final Runnable callback) {
        if (!is_active_ || thread_data_ == null) {
            if (callback != null) callback.run();
            return;
        }
        
        getAgent().fetchNextFavoriteThread(thread_data_, new NextFavoriteThreadFetchedCallback() {
            @Override
            public void onNextFavoriteThreadFetched(final int unread_thread_count, final ThreadData next_thread_data,
                    final boolean current_has_unread) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!is_active_ || thread_data_ == null) {
                            if (callback != null) callback.run();
                            return;
                        }
                        if (maybe_has_new_unread) {
                            if (unread_thread_count > 0) {
                                String message = String.valueOf(unread_thread_count) + " "
                                        + getString(R.string.toast_new_entry_found_at_favorite_threads);
                                ManagedToast.raiseToast(ThreadEntryListActivity.this, message);
                            }
                            if (current_has_unread) {
                                reloadList(true);
                                if (callback != null) callback.run();
                                return;
                            }
                        }
                        if (unread_thread_count == 0) {
                            NotificationManager notif_manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                            notif_manager.cancel(TuboroidApplication.NOTIF_ID_BACKGROUND_UPDATED);
                        }
                        
                        unread_thread_count_ = unread_thread_count;
                        next_thread_data_ = next_thread_data;
                        setFooterView();
                        
                        if (callback != null) callback.run();
                    }
                });
            }
        });
    }
    
    private void setFooterView() {
        if (thread_data_ == null || list_adapter_ == null || list_adapter_.getCount() <= 0) return;
        
        if (footer_view_.getVisibility() == View.GONE) footer_view_.setVisibility(View.VISIBLE);
        ImageView button = (ImageView) footer_view_.findViewById(R.id.entry_footer_image_view);
        TextView entry_footer_header = (TextView) footer_view_.findViewById(R.id.entry_footer_header);
        TextView entry_footer_body = (TextView) footer_view_.findViewById(R.id.entry_footer_body);
        if (favorite_check_update_progress_) {
            button.setImageResource(R.drawable.toolbar_btn_reload);
            entry_footer_header.setText(R.string.text_check_update_unread_favorite_threads);
            entry_footer_body.setText("");
        }
        else if (unread_thread_count_ > 0 && next_thread_data_ != null) {
            button.setImageResource(R.drawable.toolbar_btn_jump_right);
            String message = String.valueOf(unread_thread_count_) + " "
                    + getString(R.string.text_new_entry_found_at_favorite_threads);
            entry_footer_header.setText(message);
            entry_footer_body.setText(next_thread_data_.thread_name_);
        }
        else {
            button.setImageResource(R.drawable.toolbar_btn_reload);
            entry_footer_header.setText(R.string.text_no_new_entry_found_at_favorite_threads);
            entry_footer_body.setText(R.string.text_no_new_entry_found_at_favorite_threads_func);
        }
        footer_view_.invalidate();
    }
    
    private void showFooterView() {
        if (footer_view_.getVisibility() == View.GONE) setFooterView();
    }
    
    public void onFooterClicked() {
        if (!is_active_) return;
        if (service_task_ == null) return;
        if (list_adapter_ == null) return;
        if (next_thread_data_ == null) {
            if (favorite_check_update_progress_) return;
            favorite_check_update_progress_ = true;
            setFooterView();
            
            ManagedToast.raiseToast(ThreadEntryListActivity.this, R.string.toast_check_unread_favorite_threads,
                    Toast.LENGTH_SHORT);
            service_task_.send(new ServiceSender() {
                @Override
                public void send(ITuboroidService service) throws RemoteException {
                    service.checkUpdateFavorites(false);
                }
            });
        }
        else {
            Intent intent = new Intent(ThreadEntryListActivity.this, ThreadEntryListActivity.class);
            String uri = next_thread_data_.getThreadURI();
            intent.setData(Uri.parse(uri));
            MigrationSDK5.Intent_addFlagNoAnimation(intent);
            startActivity(intent);
        }
    }
    
    private void onCheckUpdateFinished(final Intent intent) {
        if (list_adapter_ == null) return;
        favorite_check_update_progress_ = false;
        int unread_threads = intent.getIntExtra(TuboroidService.CHECK_UPDATE.NUM_UNREAD_THREADS, 0);
        String message = null;
        Runnable callback = new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (list_adapter_ == null) return;
                        ((ThreadEntryListAdapter) list_adapter_).notifyDataSetChanged();
                    }
                });
            }
        };
        if (unread_threads == 0) {
            message = getString(R.string.toast_no_new_entry_found_at_favorite_threads);
            ManagedToast.raiseToast(this, message);
            updateFooterRow(false, callback);
        }
        else {
            updateFooterRow(true, callback);
        }
    }
    
    // ////////////////////////////////////////////////////////////
    // その他
    // ////////////////////////////////////////////////////////////
    private String getQuotedEntry(long entry_id, String body) {
        StringBuilder buf = new StringBuilder();
        buf.append(">>");
        buf.append(entry_id);
        buf.append("\n");
        for (String data : body.split("(\\r\\n|\\r|\\n)")) {
            buf.append("> ");
            buf.append(data);
            buf.append("\n");
        }
        return buf.toString();
    }
    
    @Override
    protected boolean isFavorite() {
        if (thread_data_ == null) return false;
        return thread_data_.is_favorite_;
    }
    
    @Override
    protected void addFavorite() {
        if (thread_data_ == null) return;
        getAgent().addFavorite(thread_data_, 0, new Runnable() {
            @Override
            public void run() {
                onFavoriteUpdated();
            }
        });
    }
    
    @Override
    protected void deleteFavorite() {
        if (thread_data_ == null) return;
        thread_data_.is_favorite_ = false;
        getAgent().delFavorite(thread_data_, new Runnable() {
            @Override
            public void run() {
                onFavoriteUpdated();
            }
        });
    }
    
    // ////////////////////////////////////////////////////////////
    // 内部インデックス(フィルタ前)からフィルタ後のインデックスを得る
    // ////////////////////////////////////////////////////////////
    
    public PositionData getMappedPosition(PositionData orig) {
        PositionData data = new PositionData(orig);
        if (list_adapter_ == null) return null;
        int pos = ((ThreadEntryListAdapter) list_adapter_).getMappedPosition(orig.position_);
        if (pos == -1) return null;
        data.position_ = pos;
        return data;
    }
    
    public void setMappedListPosition(final int position, final Runnable callback) {
        if (list_adapter_ == null) return;
        final int pos = ((ThreadEntryListAdapter) list_adapter_).getMappedPosition(position);
        if (pos == -1) return;
        
        // ハイライト表示
        ListView lv = getListView();
        if (lv instanceof ListViewEx) {
        	final ListViewEx lvx = (ListViewEx)lv;
        	lvx.setHighlight(pos, 750);
        	if (!lvx.isVisiblePosition(pos)) {
        		setListPosition(pos, callback);
        	} else {
        		if (callback != null) callback.run();
        	}
        }
        else {
        	setListPosition(pos, callback);
        }
        
    }
    
    // ////////////////////////////////////////////////////////////
    // レジューム(絞込みなどからフルモードへの復帰)
    // ////////////////////////////////////////////////////////////
    /**
     * しおりを保存
     */
    private void saveResumeEntryNum(long seved_entry_id, int saved_y) {
        if (resume_entry_id_ == Long.MAX_VALUE) {
            resume_entry_id_ = seved_entry_id;
            resume_entry_y_ = saved_y;
        	//Log.d(TAG, "resume_entry_id: " + resume_entry_id_ + " y:"+resume_entry_y_);
        }
    }
    
    /**
     * しおりのレジューム
     */
    private void resumeSavedEntryNum() {
        if (resume_entry_id_ != Long.MAX_VALUE) {
        	
        	final int pos = ((ThreadEntryListAdapter) list_adapter_).getMappedPosition(
        			(int)resume_entry_id_ - 1);
        	//Log.d(TAG, "resume_entry_id: " + resume_entry_id_ + " y:"+resume_entry_y_);
            ListViewEx lvx = (ListViewEx)getListView();
            lvx.setHighlight(pos, 750);
            if (hasResumeItemPos()) {
                setResumeItemPos(pos, resume_entry_y_);
            }
            else {
                setListPositionFromTop(pos, resume_entry_y_, null);
            }
            
            resume_entry_id_ = Long.MAX_VALUE;
            resume_entry_y_ = 0;
        }
    }
    
    // ////////////////////////////////////////////////////////////
    // フィルタモード
    // ////////////////////////////////////////////////////////////
    
    private void onEntryFilterMode(long seved_entry_id, int saved_y) {
        saveResumeEntryNum(seved_entry_id, saved_y);
        updateAnchorBar();
    }
    
    // ////////////////////////////////////////////////////////////
    // アンカー管理
    // ////////////////////////////////////////////////////////////
    private void jumpToAnchor(int current_num, final int num) {
        if (anchor_jump_stack_.size() == 0) {
            anchor_jump_stack_.add(current_num);
            ListView lv = getListView();
            restore_position = lv.getFirstVisiblePosition();
            restore_position_y = getListView().getChildAt(0).getTop();
        }
        if (anchor_jump_stack_.indexOf(num) == -1) {
            anchor_jump_stack_.add(num);
            updateAnchorBar();
        }
        postListViewAndUiThread(new Runnable(){
			@Override
			public void run() {
	        	setMappedListPosition(num - 1, null);
			}
		});
    }
    
    private void exitAnchorJumpMode() {
        if (anchor_jump_stack_.size() == 0) return;
        int entry_id = disableAnchorBar();
        updateAnchorBar();
        setMappedListPosition(entry_id - 1, null);
        
        postListViewAndUiThread(new Runnable() {
			@Override
			public void run() {
		        setListPositionFromTop(restore_position, restore_position_y, null);
			}
        });
    }
    
    private int disableAnchorBar() {
        int entry_id = 0;
        if (anchor_jump_stack_.size() > 0) {
            entry_id = anchor_jump_stack_.getFirst();
            anchor_jump_stack_.clear();
        }
        return entry_id;
    }
    
    private void updateAnchorBar() {
        class JumpAnchorSpan extends ClickableSpan {
            private int num_;
            
            public JumpAnchorSpan(int num) {
                num_ = num;
            }
            
            @Override
            public void onClick(View widget) {
                if (num_ > 0) ThreadEntryListActivity.this.onAnchorBarClicked(num_);
            }
        }
        
        TextView text_view = (TextView) findViewById(R.id.entry_anchor_stack);
        text_view.setTextSize(getTuboroidApplication().view_config_.entry_header_ * 3 / 2);
        HorizontalScrollView box = (HorizontalScrollView) findViewById(R.id.entry_anchor_stack_box);
        
        if (anchor_jump_stack_.size() == 0) {
            disableAnchorBar();
            text_view.setText("", BufferType.SPANNABLE);
            box.setVisibility(View.GONE);
            return;
        }
        
        boolean animation = (box.getVisibility() == View.GONE); 
        box.setVisibility(View.VISIBLE);
        
        
        SpannableStringBuilder text = new SpannableStringBuilder();
        
        for (int num : anchor_jump_stack_) {
            text.append("[ ");
            String num_str = String.valueOf(num);
            int start_index = text.length();
            text.append(String.valueOf(num));
            text.setSpan(new JumpAnchorSpan(num), start_index, start_index + num_str.length(),
                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            text.append(" ]");
        }
        
        text_view.setText(text, BufferType.SPANNABLE);
        
        if (animation) {
        	ScaleAnimation ta = new ScaleAnimation(1, 1,            
        			0, 1,
                    0, box.getMeasuredHeight()
            );
        	ta.setInterpolator(new OvershootInterpolator());
        	ta.setDuration(300);
        	box.startAnimation(ta);
        }

        int on_clicked_bgcolor = obtainStyledAttributes(R.styleable.Theme).getColor(
                R.styleable.Theme_entryLinkClickedBgColor, 0);
        text_view.setOnTouchListener(new SimpleSpanTextViewOnTouchListener(
                getTuboroidApplication().view_config_.touch_margin_, on_clicked_bgcolor));
    }
    
    private void onAnchorBarClicked(int num) {
        if (anchor_jump_stack_.size() == 0) return;
        int index = anchor_jump_stack_.indexOf(num);
        
        if (index == 0) {
        	// アンカの先頭がクリックされたら終了
        	exitAnchorJumpMode();
        } else if (index > 0) {
        	// クリックされたアンカーの階層まで残して、下の階層を削除
            while (anchor_jump_stack_.size() - 1 > index) {
                anchor_jump_stack_.removeLast();
            }
            
            updateAnchorBar();
            setMappedListPosition(num - 1, null);
        }
    }
    
    // ////////////////////////////////////////////////////////////
    // リロード
    // ////////////////////////////////////////////////////////////
    @Override
    protected void reloadList(final boolean force_reload) {
        if (!is_active_) return;
        if (!onBeginReload()) return;
        
        reload_progress_max_ = maybe_online_count_;
        reload_progress_cur_ = 0;
        setProgress(0);
        setSecondaryProgress(0);
        ((ThreadEntryListAdapter) list_adapter_).setQuickShow(true);
        
        if (force_reload) showProgressBar(true);
        getAgent().reloadThreadEntryList(thread_data_.clone(), force_reload, getFetchTask(force_reload));
    }
    
    private ThreadEntryListAgent.ThreadEntryListAgentCallback getFetchTask(final boolean force_reload) {
        final ReloadTerminator reload_terminator = getNewReloadTerminator();
        return new ThreadEntryListAgent.ThreadEntryListAgentCallback() {
            
            @Override
            public void onThreadEntryListFetchedCompleted(final ThreadData thread_data, final boolean is_analyzed) {
                // 読み込みとの待ち合わせ
                postListViewAndUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (reload_terminator.is_terminated_) return;
                        maybe_online_count_ = DEFAULT_MAX_PROGRESS;
                        ThreadEntryListActivity.this.onReloadCompleted(thread_data, force_reload, is_analyzed);
                    }
                });
            }
            
            @Override
            public void onThreadEntryListFetchedByCache(final List<ThreadEntryData> data_list) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (reload_terminator.is_terminated_) return;
                        final int data_size = data_list.size();
                        ((ThreadEntryListAdapter) list_adapter_).setDataList(data_list, new Runnable() {
                            @Override
                            public void run() {
                                reload_progress_cur_ += data_size;
                                setProgressBar(reload_progress_cur_, reload_progress_max_ + POST_PROCESS_PROGRESS);
                                onReloadCacheCompleted();
                            }
                        });
                    }
                });
            }
            
            @Override
            public void onThreadEntryListFetchStarted(final ThreadData thread_data) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (reload_terminator.is_terminated_) return;
                        setSecondaryProgress(10000);
                        setProgressBar(reload_progress_cur_, reload_progress_max_ + POST_PROCESS_PROGRESS);
                        thread_data_ = thread_data;
                        onThreadFetchStarted();
                    }
                });
            }
            
            @Override
            public void onThreadEntryListFetched(final List<ThreadEntryData> data_list) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (reload_terminator.is_terminated_) return;
                        final int data_size = data_list.size();
                        ((ThreadEntryListAdapter) list_adapter_).addDataList(data_list, new Runnable() {
                            @Override
                            public void run() {
                                reload_progress_cur_ += data_size;
                                setProgressBar(reload_progress_cur_, reload_progress_max_ + POST_PROCESS_PROGRESS);
                            }
                        });
                    }
                });
            }
            
            @Override
            public void onThreadEntryListClear() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (reload_terminator.is_terminated_) return;
                        reload_progress_cur_ = 0;
                        setProgressBar(reload_progress_cur_, reload_progress_max_ + POST_PROCESS_PROGRESS);
                        ((ThreadEntryListAdapter) list_adapter_).clearData();
                    }
                });
            }
            
            @Override
            public void onInterrupted() {
                postListViewAndUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (reload_terminator.is_terminated_) return;
                        ((ThreadEntryListAdapter) list_adapter_).clearData();
                        onEndReload();
                    }
                });
            }
            
            @Override
            public void onDatDropped(final boolean is_permanently) {
                postListViewAndUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (reload_terminator.is_terminated_) return;
                        ThreadEntryListActivity.this.onDatDropped(is_permanently);
                        ThreadEntryListActivity.this.onReloadCompleted(null, force_reload, false);
                    }
                });
            }
            
            @Override
            public void onConnectionFailed(final boolean connectionFailed) {
                postListViewAndUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (reload_terminator.is_terminated_) return;
                        ThreadEntryListActivity.this.onConnectionFailed(connectionFailed);
                        ThreadEntryListActivity.this.onReloadCompleted(null, force_reload, false);
                    }
                });
            }
            
            @Override
            public void onConnectionOffline(ThreadData threadData) {
                onThreadEntryListFetchedCompleted(threadData, false);
            }
            
        };
    }
    
    private void onThreadFetchStarted() {}
    
    private void inflateMappedPosition() {
        if (global_resume_data_ == null) return;
        if (hasResumeItemPos()) {
            global_resume_data_ = null;
            return;
        }
        PositionData data = getMappedPosition(global_resume_data_);
        if (data == null) return;
        setResumeItemPos(data.position_, data.y_);
        global_resume_data_ = null;
    }
    
    private void onReloadCacheCompleted() {
        showFooterView();
        hasInitialData(true);
        inflateMappedPosition();
        
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                resumeItemPos(new Runnable() {
                    @Override
                    public void run() {
                        postListViewAndUiThread(new Runnable() {
                            @Override
                            public void run() {
                                cache_resumed_pos_data_ = getCurrentItemPos();
                            }
                        });
                    }
                });
            }
        });
    }
    
    private void onReloadCompleted(final ThreadData thread_data, final boolean force_reload, final boolean is_analyzed) {
        if (thread_data != null) thread_data_ = thread_data;
        
        if (thread_data_.online_count_ < thread_data_.cache_count_) {
            thread_data_.online_count_ = thread_data_.cache_count_;
        }
        if (force_reload) {
            ((ThreadEntryListAdapter) list_adapter_).setReadCount(thread_data_.read_count_);
            thread_data_.read_count_ = thread_data_.cache_count_;
        }
        ((ThreadEntryListAdapter) list_adapter_).setThreadData(thread_data_);
        long current_time = System.currentTimeMillis() / 1000;
        thread_data_.recent_time_ = current_time;
        setTitle(thread_data_.thread_name_);
        
        getAgent().updateThreadData(thread_data_, new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setFooterView();
                        inflateMappedPosition();
                        if (list_adapter_ != null && jump_on_resume_after_post_) {
                            clearResumeItemPos();
                            setResumeItemPos(((ThreadEntryListAdapter) list_adapter_).getCount() - 1, 0);
                            cache_resumed_pos_data_ = null;
                        }
                        
                        if (is_analyzed) {
                            onEndReload();
                        }
                        else {
                            analyzeForEndReload();
                        }
                    }
                });
            }
        });
    }
    
    @Override
    protected void onEndReload() {
        updateFooterRow(false, new Runnable() {
            @Override
            public void run() {
                ((ThreadEntryListAdapter) list_adapter_).setQuickShow(false);
                reapplyFilterOnReloaded(new Runnable() {
                    @Override
                    public void run() {
                        postListViewAndUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (cache_resumed_pos_data_ != null
                                        && !cache_resumed_pos_data_.equals(getCurrentItemPos())) {
                                    clearResumeItemPos();
                                }
                                cache_resumed_pos_data_ = null;
                                
                                ThreadEntryListActivity.super.onEndReload();
                                reload_on_resume_ = false;
                                jump_on_resume_after_post_ = false;
                                setProgress(10000);
                                showProgressBar(false);
                            }
                        });
                    }
                });
            }
        });
    }
    
    protected void analyzeForEndReload() {
        ((ThreadEntryListAdapter) list_adapter_).analyzeThreadEntryList(new Runnable() {
            @Override
            public void run() {
                onEndReload();
            }
        }, new ThreadEntryData.AnalyzeThreadEntryListProgressCallback() {
            @Override
            public void onProgress(final int current, final int max) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setProgressBar(reload_progress_max_ + (POST_PROCESS_PROGRESS * current / max),
                                reload_progress_max_ + POST_PROCESS_PROGRESS);
                    }
                });
            }
        });
    }
    
    @Override
    protected void onEndReloadJumped() {
        getListView().post(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!is_active_ || list_adapter_ == null) return;
                        ThreadEntryListAdapter list_adapter = (ThreadEntryListAdapter) list_adapter_;
                        list_adapter.notifyDataSetChanged();
                    }
                });
            }
        });
        super.onEndReloadJumped();
    }
    
    private void onDatDropped(final boolean is_permanently) {
        if (!is_active_) return;
        
        if (is_permanently || !thread_data_.canSpecialRetry(getTuboroidApplication().getAccountPref())) {
            onDatDroppedWithoutMaru();
        }
        else {
            onDatDroppedWithMaru();
        }
    }
    
    private void cancelDatDropped() {
        updateFilter(null);
        if (((ThreadEntryListAdapter) list_adapter_).getCount() == 0 && thread_data_.cache_count_ == 0
                && thread_data_.read_count_ == 0) {
            getAgent().deleteThreadEntryListCache(thread_data_, null);
            finish();
        }
    }
    
    private void onDatDroppedWithoutMaru() {
        if (!is_active_) return;
        
        if (thread_data_.thread_name_.length() > 0) {
            SimpleDialog.showYesNo(this, R.string.dialog_dat_dropped_title, R.string.dialog_dat_dropped_summary,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(ThreadEntryListActivity.this, SimilarThreadListActivity.class);
                            intent.setData(Uri.parse(thread_data_.getBoardSubjectsURI()));
                            intent.putExtra(SimilarThreadListActivity.KEY_SEARCH_KEY_NAME, thread_data_.thread_name_);
                            intent.putExtra(SimilarThreadListActivity.KEY_SEARCH_THREAD_ID, thread_data_.thread_id_);
                            MigrationSDK5.Intent_addFlagNoAnimation(intent);
                            startActivity(intent);
                            cancelDatDropped();
                        }
                    }, new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            cancelDatDropped();
                        }
                    });
        }
        else {
            SimpleDialog.showNotice(this, R.string.dialog_dat_dropped_title,
                    R.string.dialog_dat_dropped_summary_no_name, new Runnable() {
                        @Override
                        public void run() {
                            cancelDatDropped();
                        }
                    });
        }
    }
    
    private void onDatDroppedWithMaru() {
        if (!is_active_) return;
        
        if (thread_data_.thread_name_.length() > 0) {
            SimpleDialog.showYesEtcNo(this, R.string.dialog_dat_dropped_title,
                    R.string.dialog_dat_dropped_summary_with_maru, R.string.dialog_label_dat_dropped_find_similar,
                    R.string.dialog_label_dat_dropped_use_maru, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(ThreadEntryListActivity.this, SimilarThreadListActivity.class);
                            intent.setData(Uri.parse(thread_data_.getBoardSubjectsURI()));
                            intent.putExtra(SimilarThreadListActivity.KEY_SEARCH_KEY_NAME, thread_data_.thread_name_);
                            intent.putExtra(SimilarThreadListActivity.KEY_SEARCH_THREAD_ID, thread_data_.thread_id_);
                            MigrationSDK5.Intent_addFlagNoAnimation(intent);
                            startActivity(intent);
                            cancelDatDropped();
                        }
                    }, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            getAgent().reloadSpecialThreadEntryList(thread_data_.clone(),
                                    getTuboroidApplication().getAccountPref(), getFetchTask(true));
                        }
                    }, new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            cancelDatDropped();
                        }
                    });
        }
        else {
            SimpleDialog.showYesNo(this, R.string.dialog_dat_dropped_title,
                    R.string.dialog_dat_dropped_summary_with_maru_no_name, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            getAgent().reloadSpecialThreadEntryList(thread_data_.clone(),
                                    getTuboroidApplication().getAccountPref(), getFetchTask(true));
                        }
                    }, new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            cancelDatDropped();
                        }
                    });
        }
    }
    
    private void onConnectionFailed(boolean connectionFailed) {
        if (!is_active_) return;
        
        if (connectionFailed) {
            ManagedToast.raiseToast(getApplicationContext(), R.string.toast_reload_entry_list_failed);
        }
    }
    
    protected TuboroidApplication.ViewConfig getListFontPref() {
        return getTuboroidApplication().view_config_;
    }
    
    // ////////////////////////////////////////////////////////////
    // フィルタ
    // ////////////////////////////////////////////////////////////
    public static class ParcelableFilterData implements Parcelable {
        final public int type_;
        final public String string_filter_word_;
        final public String author_id_;
        final public long target_entry_id_;
        
        public static int TYPE_NONE = 0;
        public static int TYPE_STRING = 1;
        public static int TYPE_AUTHOR_ID = 2;
        public static int TYPE_PELATION = 3;
        
        public ParcelableFilterData() {
            type_ = TYPE_NONE;
            string_filter_word_ = null;
            author_id_ = null;
            target_entry_id_ = 0;
        }
        
        public ParcelableFilterData(int type, String stringFilterWord, String author_id, long target_entry_id) {
            super();
            type_ = type;
            string_filter_word_ = stringFilterWord;
            author_id_ = author_id;
            target_entry_id_ = target_entry_id;
        }
        
        @Override
        public int describeContents() {
            return 0;
        }
        
        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(type_);
            dest.writeString(string_filter_word_);
            dest.writeString(author_id_);
            dest.writeLong(target_entry_id_);
        }
        
        public static final Parcelable.Creator<ParcelableFilterData> CREATOR = new Parcelable.Creator<ParcelableFilterData>() {
            @Override
            public ParcelableFilterData createFromParcel(Parcel in) {
                return new ParcelableFilterData(in);
            }
            
            @Override
            public ParcelableFilterData[] newArray(int size) {
                return new ParcelableFilterData[size];
            }
        };
        
        private ParcelableFilterData(Parcel in) {
            type_ = in.readInt();
            string_filter_word_ = in.readString();
            author_id_ = in.readString();
            target_entry_id_ = in.readLong();
        }
        
    }
    
    static class BaseFilter implements ThreadEntryListAdapter.Filter<ThreadEntryData> {
        @Override
        public boolean filter(ThreadEntryData data) {
            if (data.isGone()) return false;
            return true;
        }
    }
    
    @Override
    protected void updateFilter(String filter_string) {
        if (filter_string == null || filter_string.length() == 0) {
            updateFilterNone();
            return;
        }
        updateStringFilter(filter_string);
    }
    
    private void updateParcelableFilter(ParcelableFilterData filter) {
        if (!is_active_) return;
        
        if (filter.type_ == ParcelableFilterData.TYPE_STRING) {
            updateStringFilter(filter.string_filter_word_);
        }
        else if (filter.type_ == ParcelableFilterData.TYPE_AUTHOR_ID) {
            updateFilterByAuthorID(filter_.target_entry_id_, filter_.author_id_);
        }
        else if (filter.type_ == ParcelableFilterData.TYPE_PELATION) {
            updateFilterByRelation(filter_.target_entry_id_);
        }
        else {
            updateFilterNone();
        }
    }
    
    private void updateFilterNone() {
        if (!is_active_) return;
        
        filter_ = new ParcelableFilterData();
        ((ThreadEntryListAdapter) list_adapter_).setFilter(new BaseFilter(), new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        resumeSavedEntryNum();
                    }
                });
            }
        });
    }
    
    private void updateStringFilter(String filter_string) {
        if (!is_active_) return;
        
        
        filter_ = new ParcelableFilterData(ParcelableFilterData.TYPE_STRING, filter_string, null, 0);
        
        PositionData pos_data = getCurrentPosition();
        onEntryFilterMode(getListAdapter().getItemId(pos_data.position_), pos_data.y_);
        
        final String filter_lc = filter_.string_filter_word_.toLowerCase();
        ((ThreadEntryListAdapter) list_adapter_).setFilter(new BaseFilter() {
            @Override
            public boolean filter(ThreadEntryData data) {
                if (data.entry_body_.toLowerCase().indexOf(filter_lc) == -1) return false;
                return super.filter(data);
            }
        }, new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setListPositionTop(null);
                    }
                });
            }
        });
    }
    
    private void updateFilterByAuthorID(final ThreadEntryData entry_data) {
        if (!is_active_) return;
        
        updateFilterByAuthorID(entry_data.entry_id_, entry_data.author_id_);
    }
    
    private void updateFilterByAuthorID(final long target_entry_id, final String target_author_id) {
        if (!is_active_) return;
        
        ListViewEx lvx = (ListViewEx)getListView();
        onEntryFilterMode(target_entry_id, lvx.getViewTop((int)target_entry_id-1, 0));
        
        filter_ = new ParcelableFilterData(ParcelableFilterData.TYPE_AUTHOR_ID, null, target_author_id, target_entry_id);
        
        ((ThreadEntryListAdapter) list_adapter_).setFilter(new BaseFilter() {
            @Override
            public boolean filter(ThreadEntryData data) {
                if (!target_author_id.equals(data.author_id_)) return false;
                return super.filter(data);
            }
        }, new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                    	highlightByEntryId(target_entry_id, true);
                    }
                });
            }
        });
    }
    
    private void highlightByEntryId(long entry_id, boolean center) {
    	
        int position = ((ThreadEntryListAdapter) list_adapter_).getMappedPosition((int)entry_id - 1);
    	ListViewEx lvx = (ListViewEx)getListView();
    	lvx.setHighlight(position, 750);
        if (center) {
        	lvx.setSelectionFromTop(position, lvx.getMeasuredHeight()/2);
        }
    }
    
    private void updateFilterByRelation(final long target_entry_id) {
        if (!is_active_) return;
        
        updateFilterByRelation(target_entry_id, new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                    	highlightByEntryId(target_entry_id, true);
                    }
                });
            }
        });
    }
    
    private void reapplyFilterOnReloaded(final Runnable callback) {
        if (filter_.type_ == ParcelableFilterData.TYPE_PELATION) {
            updateFilterByRelation(filter_.target_entry_id_, callback);
        }
        else {
            if (callback != null) callback.run();
        }
    }
    
    // 関連レス抽出
    private void updateFilterByRelation(final long target_entry_id, final Runnable callback) {
        if (!is_active_) return;
        
        ListViewEx lvx = (ListViewEx)getListView();
        onEntryFilterMode(target_entry_id, lvx.getViewTop((int)target_entry_id-1,0));
        
        filter_ = new ParcelableFilterData(ParcelableFilterData.TYPE_PELATION, null, null, target_entry_id);
        
        
        final HashSet<Long> result_id_map = new HashSet<Long>();
        final HashMap<Long, ThreadEntryData> inner_data_map = new HashMap<Long, ThreadEntryData>();
        final HashMap<Long,Integer> indent_map = new HashMap<Long,Integer>();
        
        final ThreadEntryListAdapter adapter = (ThreadEntryListAdapter)list_adapter_;
        
        adapter.setFilter(new ThreadEntryListAdapter.PrepareFilter<ThreadEntryData>() {
        	private int indentMin = 0;
        	
            @Override
            public void prepare(ArrayList<ThreadEntryData> inner_data_list) {
                ThreadEntryData target_data = null;
                for (ThreadEntryData data : inner_data_list) {
                    inner_data_map.put(data.entry_id_, data);
                    if (data.entry_id_ == target_entry_id) {
                        target_data = data;
                    }
                }
                if (target_data == null) return;
                check(target_data, 0);
                adapter.setIndentMap(indent_map, indentMin);
            }
            
            private void check(ThreadEntryData target_data, int indent) {
                if (result_id_map.contains(target_data.entry_id_)) return;
                result_id_map.add(target_data.entry_id_);
                indent_map.put(target_data.entry_id_, indent);

                if (indent < indentMin) indentMin = indent; 
                
                for (Long next_id : target_data.back_anchor_list_) {
                    ThreadEntryData data = inner_data_map.get(next_id);
                    if (data != null) check(data, indent + 1);
                }
                
                for (Long next_id : target_data.forward_anchor_list_) {
                    ThreadEntryData data = inner_data_map.get(next_id);
                    if (data != null) check(data, indent - 1);
                }
            }
            
        }, new BaseFilter() {
            @Override
            public boolean filter(ThreadEntryData data) {
                if (!result_id_map.contains(data.entry_id_)) return false;
                return super.filter(data);
            }
        }, callback);
    }
}
