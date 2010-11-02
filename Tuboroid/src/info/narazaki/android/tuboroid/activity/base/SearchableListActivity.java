package info.narazaki.android.tuboroid.activity.base;

import info.narazaki.android.tuboroid.R;
import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

abstract public class SearchableListActivity extends TuboroidListActivity {
    public static final String TAG = "SearchableListActivity";
    
    private boolean search_bar_visible_;
    private int menu_id_tool_bar_show_ = 0;
    private int menu_id_tool_bar_hide_ = 0;
    
    private int menu_id_search_bar_show_ = 0;
    private int menu_id_search_bar_hide_ = 0;
    
    // ////////////////////////////////////////////////////////////
    // ステート管理系
    // ////////////////////////////////////////////////////////////
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        search_bar_visible_ = true;
        showToolBar(getTuboroidApplication().tool_bar_visible_);
        showSearchBar(false);
        createSearchButton();
    }
    
    // ////////////////////////////////////////////////////////////////
    // サーチバー
    // ////////////////////////////////////////////////////////////////
    
    @Override
    public boolean onSearchRequested() {
        toggleSearchBar();
        return true;
    }
    
    protected void toggleSearchBar() {
        if (search_bar_visible_) {
            showSearchBar(false);
        }
        else {
            showSearchBar(true);
        }
    }
    
    protected void showSearchBar(boolean show) {
        LinearLayout search_bar = getSearchBarView();
        if (show) {
            search_bar_visible_ = true;
            search_bar.setVisibility(View.VISIBLE);
            search_bar.requestFocus();
        }
        else {
            search_bar_visible_ = false;
            search_bar.setVisibility(View.GONE);
        }
    }
    
    public boolean isVisibleSearchBar() {
    	return search_bar_visible_;
    }
    
    protected void createSearchButton() {
        final EditText edit_text = getSearchEditView();
        edit_text.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int action_id, KeyEvent event) {
                if ((action_id | EditorInfo.IME_ACTION_DONE) != 0) {
                    onSubmitSearchBar();
                    return true;
                }
                return false;
            }
        });
        
        final ImageButton search_button = getSearchButtonView();
        search_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSubmitSearchBar();
            }
        });
        final ImageButton cancel_button = getSearchCancelButtonView();
        cancel_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCanceledSearchBar();
            }
        });
    }
    
    private void onSubmitSearchBar() {
        EditText edit_text = getSearchEditView();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(edit_text.getWindowToken(), 0);
        updateFilter(edit_text.getText().toString());
    }
    
    protected void cancelSearchBar() {
        onCanceledSearchBar();
    }
    
    private void onCanceledSearchBar() {
        EditText edit_text = getSearchEditView();
        if (edit_text.getText().length() == 0) {
            showSearchBar(false);
        }
        edit_text.setText("");
        updateFilter(null);
    }
    
    protected LinearLayout getSearchBarView() {
        return (LinearLayout) findViewById(R.id.search_bar);
    }
    
    protected EditText getSearchEditView() {
        return (EditText) findViewById(R.id.edit_search);
    }
    
    protected ImageButton getSearchButtonView() {
        return (ImageButton) findViewById(R.id.button_search);
    }
    
    protected ImageButton getSearchCancelButtonView() {
        return (ImageButton) findViewById(R.id.button_cancel_search);
    }
    
    abstract protected void updateFilter(final String filter);
    
    // ////////////////////////////////////////////////////////////////
    // ツールバー
    // ////////////////////////////////////////////////////////////////
    
    protected void toggleToolBar() {
        if (getTuboroidApplication().tool_bar_visible_) {
            showToolBar(false);
        }
        else {
            showToolBar(true);
        }
    }
    
    @Override
    protected void createToolbarButtons() {
        super.createToolbarButtons();
        
    }
    
    // ////////////////////////////////////////////////////////////////
    // メニュー
    // ////////////////////////////////////////////////////////////////
    
    protected void createToolBarOptionMenu(Menu menu, int menu_id_tool_bar_show, int menu_id_tool_bar_hide,
            int menu_id_search_bar_show, int menu_id_search_bar_hide) {
        menu_id_tool_bar_show_ = menu_id_tool_bar_show;
        menu_id_tool_bar_hide_ = menu_id_tool_bar_hide;
        menu_id_search_bar_show_ = menu_id_search_bar_show;
        menu_id_search_bar_hide_ = menu_id_search_bar_hide;
        
        MenuItem tool_bar_show = menu.add(0, menu_id_tool_bar_show_, menu_id_tool_bar_show_,
                getString(R.string.label_menu_show_toolbar));
        tool_bar_show.setIcon(R.drawable.ic_menu_show_toolbar);
        tool_bar_show.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                showToolBar(true);
                return false;
            }
        });
        
        MenuItem tool_bar_hide = menu.add(0, menu_id_tool_bar_hide_, menu_id_tool_bar_hide_,
                getString(R.string.label_menu_hide_toolbar));
        tool_bar_hide.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
        tool_bar_hide.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                showToolBar(false);
                return false;
            }
        });
        
        MenuItem search_bar_show = menu.add(0, menu_id_search_bar_show_, menu_id_search_bar_show_,
                getString(R.string.label_menu_show_searchbar));
        search_bar_show.setIcon(R.drawable.ic_menu_show_searchbar);
        search_bar_show.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                showSearchBar(true);
                return false;
            }
        });
        
        MenuItem search_bar_hide = menu.add(0, menu_id_search_bar_hide_, menu_id_search_bar_hide_,
                getString(R.string.label_menu_hide_searchbar));
        search_bar_hide.setIcon(R.drawable.ic_menu_hide_searchbar);
        search_bar_hide.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                showSearchBar(false);
                return false;
            }
        });
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        
        MenuItem tool_bar_show = menu.findItem(menu_id_tool_bar_show_);
        MenuItem tool_bar_hide = menu.findItem(menu_id_tool_bar_hide_);
        
        if (getTuboroidApplication().tool_bar_visible_) {
            tool_bar_show.setVisible(false);
            tool_bar_hide.setVisible(true);
        }
        else {
            tool_bar_show.setVisible(true);
            tool_bar_hide.setVisible(false);
        }
        
        MenuItem search_bar_show = menu.findItem(menu_id_search_bar_show_);
        MenuItem search_bar_hide = menu.findItem(menu_id_search_bar_hide_);
        
        if (search_bar_visible_) {
            search_bar_show.setVisible(false);
            search_bar_hide.setVisible(true);
        }
        else {
            search_bar_show.setVisible(true);
            search_bar_hide.setVisible(false);
        }
        
        return true;
    }
    
}
