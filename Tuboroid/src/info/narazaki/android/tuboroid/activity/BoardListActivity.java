package info.narazaki.android.tuboroid.activity;

import info.narazaki.android.lib.activity.base.NSimpleExpandableListActivity;
import info.narazaki.android.lib.adapter.SimpleExpandableListAdapterBase;
import info.narazaki.android.lib.dialog.SimpleDialog;
import info.narazaki.android.lib.dialog.SimpleEditTextDialog;
import info.narazaki.android.lib.dialog.SimpleProgressDialog;
import info.narazaki.android.lib.system.MigrationSDK5;
import info.narazaki.android.lib.toast.ManagedToast;
import info.narazaki.android.tuboroid.R;
import info.narazaki.android.tuboroid.TuboroidApplication;
import info.narazaki.android.tuboroid.activity.base.TuboroidExpandableListActivityBase;
import info.narazaki.android.tuboroid.agent.BoardListAgent;
import info.narazaki.android.tuboroid.data.BoardData;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.LinearLayout;
import android.widget.TextView;

public class BoardListActivity extends TuboroidExpandableListActivityBase {
    private static final String TAG = "BoardListActivity";
    
    // コンテキストメニュー
    private final static int CTX_MENU_DELETE_BOARD = 1;
    private final static int CTX_MENU_EDIT_BOARD = 2;
    
    public static final int MENU_KEY_RELOAD = 1;
    public static final int MENU_KEY_ADD_BOARD = 2;
    public static final int MENU_KEY_FIND2CH = 3;
    public static final int MENU_KEY_SETTING = 90;
    public static final int MENU_KEY_HELP = 99;
    public static final int MENU_KEY_ABOUT = 100;
    
    private SimpleProgressDialog progress_dialog_;
    protected boolean is_active_ = true;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.board_list);
        
        registerForContextMenu(getExpandableListView());
        
        setTitle(R.string.title_boards);
        
        progress_dialog_ = new SimpleProgressDialog();
        
        getTuboroidApplication().setHomeTabActivity(TuboroidApplication.KEY_HOME_ACTIVITY_BOARD_LIST);
        
        getTuboroidApplication().createMainTabButtons(this, new Runnable() {
            @Override
            public void run() {
                toggleSearchBar();
            }
        }, null, null);
    }
    
    @Override
    public boolean onSearchRequested() {
        toggleSearchBar();
        return true;
    }
    
    public void toggleSearchBar() {
        Intent intent = new Intent(this, Find2chSearchActivity.class);
        MigrationSDK5.Intent_addFlagNoAnimation(intent);
        startActivity(intent);
    }
    
    @Override
    protected void onResume() {
        ((BoardListAdapter) list_adapter_).setFontSize(getListFontSize());
        getTuboroidApplication().setHomeTabActivity(TuboroidApplication.KEY_HOME_ACTIVITY_BOARD_LIST);
        super.onResume();
    }
    
    @Override
    protected void onPause() {
        progress_dialog_.hide();
        super.onPause();
    }
    
    @Override
    protected void onSaveResumePositon(StatData stat) {
        super.onSaveResumePositon(stat);
        getAgent().saveBoardListStat(stat, null);
    }
    
    // ////////////////////////////////////////////////////////////
    // アイテムタップ
    // ////////////////////////////////////////////////////////////
    @Override
    public boolean onChildClick(ExpandableListView parent, View v, int group_position, int child_position, long id) {
        BoardData data = (BoardData) list_adapter_.getChild(group_position, child_position);
        if (data == null) return false;
        
        Intent intent = new Intent(this, ThreadListActivity.class);
        intent.setData(Uri.parse(data.getSubjectsURI()));
        MigrationSDK5.Intent_addFlagNoAnimation(intent);
        startActivity(intent);
        return true;
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menu_info) {
        menu.clear();
        
        ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menu_info;
        long packed_pos = info.packedPosition;
        
        if (ExpandableListView.getPackedPositionType(packed_pos) == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
            int group_pos = ExpandableListView.getPackedPositionGroup(packed_pos);
            int child_pos = ExpandableListView.getPackedPositionChild(packed_pos);
            BoardData data = (BoardData) list_adapter_.getChild(group_pos, child_pos);
            if (data == null) return;
            if (data.id_ != 0) {
                menu.setHeaderTitle(R.string.ctx_menu_title_board);
                menu.add(0, CTX_MENU_DELETE_BOARD, CTX_MENU_DELETE_BOARD, R.string.ctx_menu_delete_board);
                menu.add(0, CTX_MENU_EDIT_BOARD, CTX_MENU_EDIT_BOARD, R.string.ctx_menu_edit_board);
            }
        }
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item.getMenuInfo();
        long packed_pos = info.packedPosition;
        
        if (ExpandableListView.getPackedPositionType(packed_pos) == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
            int group_pos = ExpandableListView.getPackedPositionGroup(packed_pos);
            int child_pos = ExpandableListView.getPackedPositionChild(packed_pos);
            BoardData data = (BoardData) list_adapter_.getChild(group_pos, child_pos);
            
            switch (item.getItemId()) {
            case CTX_MENU_DELETE_BOARD:
                doDeleteBoard(data);
                break;
            case CTX_MENU_EDIT_BOARD:
                doEditBoard(data);
                break;
            }
        }
        return true;
    }
    
    private void doDeleteBoard(final BoardData board_data) {
        SimpleDialog.showYesNo(this, R.string.ctx_menu_delete_board, R.string.ctx_menu_delete_board_confirm,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getAgent().deleteBoard(board_data, new Runnable() {
                            @Override
                            public void run() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        reloadBoardList(true, false);
                                    }
                                });
                            }
                        });
                    }
                }, null);
    }
    
    private void doEditBoard(final BoardData board_data) {
        SimpleEditTextDialog.show(this, board_data.board_name_, R.string.ctx_menu_edit_board_confirm,
                android.R.string.ok, new SimpleEditTextDialog.OnSubmitListener() {
                    @Override
                    public void onSubmitted(String data) {
                        data = data.trim();
                        if (data.length() == 0) return;
                        board_data.board_name_ = data;
                        getAgent().updateBoard(board_data, new Runnable() {
                            @Override
                            public void run() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        reloadBoardList(true, false);
                                    }
                                });
                            }
                        });
                    }
                    
                    @Override
                    public void onCanceled() {}
                });
    }
    
    // ////////////////////////////////////////////////////////////
    // メニュー
    // ////////////////////////////////////////////////////////////
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);
        
        MenuItem reload_item = menu.add(0, MENU_KEY_RELOAD, MENU_KEY_RELOAD,
                getString(R.string.label_menu_reload_boards));
        
        reload_item.setIcon(R.drawable.ic_menu_refresh);
        reload_item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                reloadBoardList(true, true);
                return false;
            }
        });
        
        MenuItem add_board_item = menu.add(0, MENU_KEY_ADD_BOARD, MENU_KEY_ADD_BOARD,
                getString(R.string.label_menu_add_board));
        
        add_board_item.setIcon(android.R.drawable.ic_menu_add);
        add_board_item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                addExternalBoard();
                return false;
            }
        });
        
        MenuItem find2ch_item = menu.add(0, MENU_KEY_FIND2CH, MENU_KEY_FIND2CH,
                getString(R.string.label_menu_search_via_find2ch));
        
        find2ch_item.setIcon(android.R.drawable.ic_menu_search);
        find2ch_item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                toggleSearchBar();
                return false;
            }
        });
        
        MenuItem setting_item = menu.add(0, MENU_KEY_SETTING, MENU_KEY_SETTING, getString(R.string.label_menu_setting));
        setting_item.setIcon(android.R.drawable.ic_menu_preferences);
        setting_item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent intent = new Intent(BoardListActivity.this, SettingsActivity.class);
                MigrationSDK5.Intent_addFlagNoAnimation(intent);
                startActivity(intent);
                return false;
            }
        });
        
        MenuItem help_item = menu.add(0, MENU_KEY_HELP, MENU_KEY_HELP, getString(R.string.label_menu_help));
        help_item.setIcon(android.R.drawable.ic_menu_help);
        help_item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent intent = new Intent(BoardListActivity.this, HelpActivity.class);
                MigrationSDK5.Intent_addFlagNoAnimation(intent);
                startActivity(intent);
                return false;
            }
        });
        
        MenuItem about_item = menu.add(0, MENU_KEY_ABOUT, MENU_KEY_ABOUT, getString(R.string.label_menu_about));
        about_item.setIcon(android.R.drawable.ic_menu_info_details);
        about_item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                AlertDialog.Builder builder = new AlertDialog.Builder(BoardListActivity.this);
                LayoutInflater layout_inflater = LayoutInflater.from(BoardListActivity.this);
                View about_view = layout_inflater.inflate(R.layout.about_dialog, null);
                TextView version_view = (TextView) about_view.findViewById(R.id.about_version);
                try {
                    String version_string = getPackageManager().getPackageInfo("info.narazaki.android.tuboroid", 0).versionName;
                    version_view.setText(" " + version_string);
                    
                }
                catch (NameNotFoundException e) {
                }
                
                builder.setView(about_view);
                builder.setCancelable(true);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {}
                });
                builder.show();
                
                return false;
            }
        });
        
        return result;
    }
    
    private void reloadBoardList(boolean no_cache, boolean force_reload) {
        if (!is_active_) return;
        if (!onBeginReload()) return;
        
        boolean cached = getAgent().fetchBoardList(no_cache, force_reload,
                new BoardListAgent.BoardListFetchedCallback() {
                    @Override
                    public void onBoardListFetched(final ArrayList<String> board_groups,
                            final ArrayList<ArrayList<BoardData>> board_list,
                            final NSimpleExpandableListActivity.StatData board_list_stat) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (!is_active_) return;
                                ((BoardListAdapter) list_adapter_).setDataList(board_groups, board_list);
                                progress_dialog_.hide();
                                setResumePosition(board_list_stat);
                                onEndReload();
                            }
                        });
                    }
                    
                    @Override
                    public void onBoardListConnectionFailed() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (!is_active_) return;
                                ManagedToast
                                        .raiseToast(BoardListActivity.this, R.string.toast_reload_board_list_failed);
                                progress_dialog_.hide();
                                onEndReload();
                            }
                        });
                    }
                    
                    @Override
                    public void onConnectionOffline() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (!is_active_) return;
                                ManagedToast.raiseToast(BoardListActivity.this, R.string.toast_network_is_offline);
                                progress_dialog_.hide();
                                onEndReload();
                            }
                        });
                    }
                });
        
        if (!cached) {
            progress_dialog_.show(this, R.string.dialog_loading_progress, new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    if (is_active_) finish();
                }
            });
        }
    }
    
    private void addExternalBoard() {
        // ビュー作成
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_add_board_title);
        
        LayoutInflater layout_inflater = LayoutInflater.from(this);
        LinearLayout layout_view = (LinearLayout) layout_inflater.inflate(R.layout.add_board_dialog, null);
        builder.setView(layout_view);
        
        final EditText board_name = (EditText) layout_view.findViewById(R.id.add_board_name);
        final EditText board_uri = (EditText) layout_view.findViewById(R.id.add_board_uri);
        
        builder.setPositiveButton(R.string.dialog_add_board_submit, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                addExternalBoardImpl(board_name.getText().toString(), board_uri.getText().toString());
            }
        });
        builder.setCancelable(true);
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {}
        });
        builder.create().show();
    }
    
    private void addExternalBoardImpl(String board_name, String board_uri) {
        board_name = board_name.trim();
        board_uri = board_uri.trim();
        if (board_uri.length() == 0) return;
        Uri uri = Uri.parse(board_uri);
        if (uri == null) return;
        if (uri.getHost() == null || uri.getHost().length() == 0) return;
        if (uri.getPath() == null || uri.getPath().length() == 0) return;
        
        getAgent().getBoardData(uri, true, new BoardListAgent.BoardFetchedCallback() {
            @Override
            public void onBoardFetched(BoardData newBoardData) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        reloadBoardList(true, false);
                    }
                });
            }
        });
    }
    
    class BoardListAdapter extends SimpleExpandableListAdapterBase<BoardData> {
        float font_size_;
        
        public BoardListAdapter(float font_size) {
            super();
            setGroupList(new ArrayList<String>());
            setDataList(new ArrayList<ArrayList<BoardData>>());
            font_size_ = font_size;
        }
        
        public void setFontSize(float font_size) {
            font_size_ = font_size;
            notifyDataSetChanged();
        }
        
        @Override
        protected View createChildView() {
            LayoutInflater layout_inflater = LayoutInflater.from(BoardListActivity.this);
            View view = layout_inflater.inflate(R.layout.board_list_row, null);
            return view;
        }
        
        @Override
        protected View createGroupView() {
            LayoutInflater layout_inflater = LayoutInflater.from(BoardListActivity.this);
            View view = layout_inflater.inflate(R.layout.board_list_group_row, null);
            return view;
        }
        
        @Override
        protected View setChildView(View view, BoardData data) {
            LinearLayout row_view = (LinearLayout) view;
            TextView title_view = (TextView) row_view.getChildAt(0);
            title_view.setTextSize(font_size_);
            title_view.setText(data.board_name_);
            return view;
        }
        
        @Override
        protected View setGroupView(View view, String name) {
            if (name.length() == 0) {
                name = view.getResources().getString(R.string.text_board_category_others);
            }
            LinearLayout row_view = (LinearLayout) view;
            TextView title_view = (TextView) row_view.getChildAt(0);
            title_view.setTextSize(font_size_);
            title_view.setText(name);
            return view;
        }
    }
    
    @Override
    protected SimpleExpandableListAdapterBase<?> createListAdapter() {
        return new BoardListAdapter(getListFontSize());
    }
    
    protected float getListFontSize() {
        return getTuboroidApplication().view_config_.board_list_;
    }
    
    @Override
    protected void onFirstDataRequired() {
        onResumeDataRequired();
    }
    
    @Override
    protected void onResumeDataRequired() {
        reloadBoardList(false, false);
    }
    
}
