package info.narazaki.android.tuboroid.activity;

import info.narazaki.android.lib.dialog.SimpleDialog;
import info.narazaki.android.lib.dialog.SimpleProgressDialog;
import info.narazaki.android.lib.toast.ManagedToast;
import info.narazaki.android.tuboroid.R;
import info.narazaki.android.tuboroid.activity.base.TuboroidActivity;
import info.narazaki.android.tuboroid.agent.CreateNewThreadTask;
import info.narazaki.android.tuboroid.agent.CreateNewThreadTask.FutureCreateNewThread;
import info.narazaki.android.tuboroid.data.BoardData;
import info.narazaki.android.tuboroid.data.NewThreadData;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;

public class NewThreadEditActivity extends TuboroidActivity {
    public static final String TAG = "NewThreadEditActivity";
    
    // メニュー
    public static final int MENU_KEY_CLEAR_BODY = 10;
    
    // 板情報
    private Uri board_uri_;
    private BoardData board_data_;
    
    private SimpleProgressDialog progress_dialog_;
    
    private FutureCreateNewThread pending_;
    
    // ////////////////////////////////////////////////////////////
    // ステート管理系
    // ////////////////////////////////////////////////////////////
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_thread_edit);
        
        pending_ = null;
        
        // 板情報の取得
        board_uri_ = getIntent().getData();
        
        board_data_ = getAgent().getBoardData(board_uri_, false, null);
        if (board_data_ == null) return;
        
        progress_dialog_ = new SimpleProgressDialog();
        
        createButtons();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (board_data_ == null) {
            finish();
            return;
        }
        EditText thread_title = (EditText) findViewById(R.id.new_thread_title);
        thread_title.requestFocus();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // abort
        progress_dialog_.hide();
        
        if (pending_ != null) {
            pending_.abort();
        }
    }
    
    private void createButtons() {
        // ボタンの初期化
        ImageButton button_submit = (ImageButton) findViewById(R.id.new_thread_submit);
        button_submit.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText thread_body = (EditText) findViewById(R.id.new_thread_body);
                EditText thread_title = (EditText) findViewById(R.id.new_thread_title);
                if (thread_body.getText().length() > 0 && thread_title.getText().length() > 0) {
                    checkSubmitCreateingThread(null, null);
                }
            }
        });
    }
    
    // ////////////////////////////////////////////////////////////
    // オプションメニュー
    // ////////////////////////////////////////////////////////////
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        
        // クリア
        MenuItem compose_item = menu.add(0, MENU_KEY_CLEAR_BODY, MENU_KEY_CLEAR_BODY,
                getString(R.string.label_menu_clear_composing_body));
        compose_item.setIcon(android.R.drawable.ic_menu_delete);
        compose_item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                EditText thread_body = (EditText) findViewById(R.id.new_thread_body);
                EditText thread_title = (EditText) findViewById(R.id.new_thread_title);
                EditText thread_name = (EditText) findViewById(R.id.new_thread_name);
                EditText thread_mail = (EditText) findViewById(R.id.new_thread_mail);
                thread_body.setText("");
                thread_title.setText("");
                thread_name.setText("");
                thread_mail.setText("");
                return false;
            }
        });
        
        return true;
    }
    
    private void checkSubmitCreateingThread(final String name, final String value) {
        SimpleDialog.showYesNo(this, R.string.dialog_create_new_thread_notice_title,
                R.string.dialog_do_you_create_thread_title, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        submitCreatingThread(null, null, false);
                    }
                }, new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {}
                });
    }
    
    public void retrySubmitCreatingThread(final String name, final String value, String message) {
        if (!is_active_) return;
        progress_dialog_.hide();
        SimpleDialog.showYesNo(this, R.string.dialog_create_new_thread_notice_title, message,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        submitCreatingThread(name, value, true);
                    }
                }, new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        finish();
                    }
                });
    }
    
    private void submitCreatingThread(final String name, final String value, final boolean is_retry) {
        if (!is_active_) return;
        
        EditText thread_body = (EditText) findViewById(R.id.new_thread_body);
        EditText thread_title = (EditText) findViewById(R.id.new_thread_title);
        EditText thread_name = (EditText) findViewById(R.id.new_thread_name);
        EditText thread_mail = (EditText) findViewById(R.id.new_thread_mail);
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(thread_body.getWindowToken(), 0);
        
        NewThreadData new_thread_data = new NewThreadData(thread_name.getText().toString(), thread_mail.getText()
                .toString(), thread_title.getText().toString(), thread_body.getText().toString());
        createThread(new_thread_data);
    }
    
    private void createThread(final NewThreadData new_thread_data) {
        progress_dialog_.show(this, R.string.dialog_creating_thread_progress, new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                finish();
            }
        });
        
        pending_ = getAgent().createNewThread(board_data_, new_thread_data, getTuboroidApplication().getAccountPref(),
                new CreateNewThreadTask.OnCreateNewThreadCallback() {
                    
                    @Override
                    public void onConnectionError(final boolean connectionFailed) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                NewThreadEditActivity.this.onConnectionError();
                            }
                        });
                    }
                    
                    @Override
                    public void onCreated() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                NewThreadEditActivity.this.onCreated();
                            }
                        });
                    }
                    
                    @Override
                    public void onCreateFailed(final String message) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                NewThreadEditActivity.this.onCreateFailed(message);
                            }
                        });
                    }
                    
                    @Override
                    public void onRetryNotice(final NewThreadData retry_new_thread_data, final String message) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                NewThreadEditActivity.this.onRetryNotice(retry_new_thread_data, message);
                            }
                        });
                    }
                });
    }
    
    private void onCreated() {
        if (!is_active_) return;
        progress_dialog_.hide();
        pending_ = null;
        ManagedToast.raiseToast(this, R.string.toast_post_succeeded);
        EditText thread_body = (EditText) findViewById(R.id.new_thread_body);
        thread_body.setText("");
        setResult(RESULT_OK);
        finish();
    }
    
    private void onRetryNotice(final NewThreadData new_thread_data, String message) {
        if (!is_active_) return;
        pending_ = null;
        
        progress_dialog_.hide();
        
        SimpleDialog.showYesNo(this, R.string.dialog_create_new_thread_notice_title, message,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getTuboroidApplication().setSkipAgreementNotice(true);
                        createThread(new_thread_data);
                    }
                }, new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        finish();
                    }
                });
    }
    
    private void onCreateFailed(final String message) {
        if (!is_active_) return;
        progress_dialog_.hide();
        pending_ = null;
        SimpleDialog.showNotice(this, R.string.dialog_create_new_thread_failed_title, message, null);
    }
    
    private void onConnectionError() {
        if (!is_active_) return;
        progress_dialog_.hide();
        SimpleDialog.showNotice(this, R.string.dialog_create_new_thread_failed_title,
                R.string.dialog_create_new_thread_error_summary, null);
    }
    
}
