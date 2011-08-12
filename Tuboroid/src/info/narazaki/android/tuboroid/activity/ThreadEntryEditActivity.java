package info.narazaki.android.tuboroid.activity;

import info.narazaki.android.lib.dialog.SimpleDialog;
import info.narazaki.android.lib.dialog.SimpleProgressDialog;
import info.narazaki.android.lib.toast.ManagedToast;
import info.narazaki.android.tuboroid.R;
import info.narazaki.android.tuboroid.TuboroidApplication;
import info.narazaki.android.tuboroid.activity.base.TuboroidActivity;
import info.narazaki.android.tuboroid.agent.PostEntryTask;
import info.narazaki.android.tuboroid.agent.thread.SQLiteAgent;
import info.narazaki.android.tuboroid.data.PostEntryData;
import info.narazaki.android.tuboroid.data.ThreadData;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources.Theme;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ToggleButton;

public class ThreadEntryEditActivity extends TuboroidActivity {
    public static final String TAG = "ThreadEntryEditActivity";
    public static final String INTENT_KEY_THREAD_DEFAULT_TEXT = "KEY_THREAD_DEFAULT_TEXT";
    
    public static final String PREF_KEY_ENTRY_EDIT_NAME = "PREF_KEY_ENTRY_EDIT_NAME";
    public static final String PREF_KEY_ENTRY_EDIT_MAIL = "PREF_KEY_ENTRY_EDIT_MAIL";
    
    // メニュー
    public static final int MENU_KEY_CLEAR_BODY = 10;
    
    // スレ情報
    private Uri thread_uri_;
    private ThreadData thread_data_;
    
    private SimpleProgressDialog progress_dialog_;
    
    private PostEntryTask.FuturePostEntry pending_;
    
    // ////////////////////////////////////////////////////////////
    // ステート管理系
    // ////////////////////////////////////////////////////////////
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.entry_edit);
        
        pending_ = null;
        
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        String default_name = pref.getString(PREF_KEY_ENTRY_EDIT_NAME, "");
        String default_mail = pref.getString(PREF_KEY_ENTRY_EDIT_MAIL, "sage");
        EditText name_view = (EditText) findViewById(R.id.entry_edit_name);
        name_view.setText(default_name);
        
        EditText mail_view = (EditText) findViewById(R.id.entry_edit_mail);
        mail_view.setText(default_mail);
        
        EditText body_view = (EditText) findViewById(R.id.entry_edit_body);

        for(EditText edit_text : new EditText[] {name_view, mail_view, body_view}){
        	edit_text.getInputExtras(true).putBoolean("allowEmoji", true);
        }
        	
        // スレッド情報の取得(URLから作れる範囲の暫定のもの)
        thread_uri_ = getIntent().getData();
        thread_data_ = ThreadData.factory(thread_uri_);
        if (thread_data_ == null) return;
        getAgent().initNewThreadData(thread_data_, null);

        // デフォルトのテキストを設定
        String default_text = getIntent().getStringExtra(INTENT_KEY_THREAD_DEFAULT_TEXT);
        if (default_text == null) {
        	default_text = getIntent().getStringExtra(Intent.EXTRA_TEXT);
        }
        if (default_text != null) {
        	body_view.setText(default_text);
        }
        
        // スレッド情報の読み込み
        getAgent().getThreadData(thread_data_, new SQLiteAgent.GetThreadDataResult() {
            @Override
            public void onQuery(final ThreadData thread_data) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        thread_data_ = thread_data;
                        setTitle(thread_data_.thread_name_);
                        EditText entry_edit_body = (EditText) findViewById(R.id.entry_edit_body);
                        if (entry_edit_body.getText().length() == 0) entry_edit_body.setText(thread_data_.edit_draft_);
                    }
                });
            }
        });
        progress_dialog_ = new SimpleProgressDialog();
        
        createButtons();
        
        initAAButton();
    }
    
 	@Override
    protected void onResume() {
        super.onResume();
        if (thread_data_ == null) {
            finish();
            return;
        }
        EditText entry_edit_body = (EditText) findViewById(R.id.entry_edit_body);
        entry_edit_body.requestFocus();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // abort
        progress_dialog_.hide();
        
        if (pending_ != null) {
            pending_.abort();
        }
        
        // save name/mail
        EditText entry_edit_name = (EditText) findViewById(R.id.entry_edit_name);
        EditText entry_edit_mail = (EditText) findViewById(R.id.entry_edit_mail);
        
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putString(PREF_KEY_ENTRY_EDIT_NAME, entry_edit_name.getText().toString());
        editor.putString(PREF_KEY_ENTRY_EDIT_MAIL, entry_edit_mail.getText().toString());
        editor.commit();
        
        // draft
        EditText entry_edit_body = (EditText) findViewById(R.id.entry_edit_body);
        if (thread_data_ != null) {
            getAgent().savePostEntryDraft(thread_data_, entry_edit_body.getText().toString());
        }
    }
    
    private void createButtons() {
        // ボタンの初期化
        ImageButton button_edit_name_clear = (ImageButton) findViewById(R.id.entry_edit_name_clear);
        button_edit_name_clear.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText entry_edit_name = (EditText) findViewById(R.id.entry_edit_name);
                entry_edit_name.setText("");
            }
        });
        
        ImageButton button_edit_mail_clear = (ImageButton) findViewById(R.id.entry_edit_mail_clear);
        button_edit_mail_clear.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText entry_edit_mail = (EditText) findViewById(R.id.entry_edit_mail);
                if (entry_edit_mail.getText().length() == 0) {
                    entry_edit_mail.setText("sage");
                }
                else {
                    entry_edit_mail.setText("");
                }
            }
        });
        
        Button button_edit_submit = (Button) findViewById(R.id.entry_edit_submit);
        button_edit_submit.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		checkSubmitEntryPosting(null, null);
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
                Builder builder = new AlertDialog.Builder(ThreadEntryEditActivity.this);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                	@Override
                	public void onClick(DialogInterface dialog, int which) {
                		EditText entry_edit_body = (EditText) findViewById(R.id.entry_edit_body);
                		entry_edit_body.setText("");
                	}
                });
                builder.setNegativeButton(android.R.string.no, null);

                builder.setTitle(R.string.dialog_clear_body_title);
                builder.setMessage(R.string.dialog_clear_body);
            
                builder.setCancelable(true);
                builder.show();
                return false;
            }
        });
        

        // sage
        MenuItem sage_item = menu.add(0, MENU_KEY_CLEAR_BODY, MENU_KEY_CLEAR_BODY,
                getString(R.string.label_menu_sage));
        sage_item.setIcon(R.drawable.ic_menu_sage);
        sage_item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
            	EditText entry_edit_mail = (EditText) findViewById(R.id.entry_edit_mail);
            	if(entry_edit_mail.getText().toString().equals("sage")) {
            		entry_edit_mail.setText("");
            	}else {
            		entry_edit_mail.append("sage");
            	}
            	return false;
            }
        });
        
        // 書き込み
        MenuItem submit_item = menu.add(0, MENU_KEY_CLEAR_BODY, MENU_KEY_CLEAR_BODY,
                getString(R.string.label_menu_submit));
        submit_item.setIcon(R.drawable.ic_menu_compose);
        submit_item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
        	@Override
        	public boolean onMenuItemClick(MenuItem item) {
        		checkSubmitEntryPosting(null, null);
        		return false;
        	}
        });
        return true;
    }

    private void checkSubmitEntryPosting(final String name, final String value) {
    	EditText entry_edit_body = (EditText) findViewById(R.id.entry_edit_body);
        if (entry_edit_body.getText().length() == 0) {
        	ManagedToast.raiseToast(this, R.string.toast_empty_body);
            return;
        }
        
    	if(getTuboroidApplication().getAccountPref().use_p2_){
    		SimpleDialog.showYesEtcNo(this, R.string.dialog_post_notice_title, R.string.dialog_do_you_post_it_title
    				, R.string.dialog_label_normal, R.string.dialog_label_with_p2, 
    				new DialogInterface.OnClickListener() {
    			@Override
    			public void onClick(DialogInterface dialog, int which) {
    				submitEntryPosting(null, null, false, false);
    			}
    		},new DialogInterface.OnClickListener() {
    			@Override
    			public void onClick(DialogInterface dialog, int which) {
    				submitEntryPosting(null, null, false, true);
    			}
    		}, new DialogInterface.OnCancelListener() {
    			@Override
    			public void onCancel(DialogInterface dialog) {}
    		});
    	}else{
    		SimpleDialog.showYesNo(this, R.string.dialog_post_notice_title, R.string.dialog_do_you_post_it_title,
    				new DialogInterface.OnClickListener() {
    			@Override
    			public void onClick(DialogInterface dialog, int which) {
    				submitEntryPosting(null, null, false, true);
    			}
    		}, new DialogInterface.OnCancelListener() {
    			@Override
    			public void onCancel(DialogInterface dialog) {}
    		});
    	}
    }
    /*
    public void retrySubmitEntryPosting(final String name, final String value, String message) {
        if (!is_active_) return;
        progress_dialog_.hide();
        SimpleDialog.showYesNo(this, R.string.dialog_post_notice_title, message, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                submitEntryPosting(name, value, true);
            }
        }, new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                finish();
            }
        });
    }
    */
    private void submitEntryPosting(final String name, final String value, final boolean is_retry, final boolean with_p2) {
        if (!is_active_) return;
        
        EditText entry_edit_name = (EditText) findViewById(R.id.entry_edit_name);
        EditText entry_edit_mail = (EditText) findViewById(R.id.entry_edit_mail);
        EditText entry_edit_body = (EditText) findViewById(R.id.entry_edit_body);
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(entry_edit_body.getWindowToken(), 0);
        
        PostEntryData post_entry_data = new PostEntryData(entry_edit_name.getText().toString(), entry_edit_mail
                .getText().toString(), entry_edit_body.getText().toString());
        postEntry(post_entry_data, with_p2);
    }
    
    private void postEntry(final PostEntryData post_entry_data, final boolean with_p2) {
        progress_dialog_.show(this, R.string.dialog_posting_progress, new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                finish();
            }
        });
        
        pending_ = getAgent().postEntry(thread_data_, post_entry_data
        		, with_p2 ? getTuboroidApplication().getAccountPref() : null,
                new PostEntryTask.OnPostEntryCallback() {
                    
                    @Override
                    public void onPosted() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ThreadEntryEditActivity.this.onPosted();
                            }
                        });
                    }
                    
                    @Override
                    public void onPostRetryNotice(final PostEntryData retryPostEntryData, final String message) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ThreadEntryEditActivity.this.onPostRetryNotice(retryPostEntryData, message, with_p2);
                            }
                        });
                    }
                    
                    @Override
                    public void onPostFailed(final String message) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ThreadEntryEditActivity.this.onPostFailed(message);
                            }
                        });
                    }
                    
                    @Override
                    public void onConnectionError(final boolean connectionFailed) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ThreadEntryEditActivity.this.onConnectionError();
                            }
                        });
                    }
                });
    }
    
    private void onPosted() {
        if (!is_active_) return;
        progress_dialog_.hide();
        pending_ = null;
        ManagedToast.raiseToast(this, R.string.toast_post_succeeded);
        EditText entry_edit_body = (EditText) findViewById(R.id.entry_edit_body);
        entry_edit_body.setText("");
        getAgent().savePostEntryRecentTime(thread_data_);
        setResult(RESULT_OK);
        finish();
    }
    
    private void onPostRetryNotice(final PostEntryData retryPostEntryData, String message, final boolean with_p2) {
        if (!is_active_) return;
        pending_ = null;
        
        if (getTuboroidApplication().isSkipAgreementNotice()) {
            postEntry(retryPostEntryData, with_p2);
            return;
        }
        
        progress_dialog_.hide();
        
        SimpleDialog.showYesNo(this, R.string.dialog_post_notice_title, message, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                getTuboroidApplication().setSkipAgreementNotice(true);
                postEntry(retryPostEntryData, with_p2);
            }
        }, new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                finish();
            }
        });
    }
    
    private void onPostFailed(final String message) {
        if (!is_active_) return;
        progress_dialog_.hide();
        pending_ = null;
        SimpleDialog.showNotice(this, R.string.dialog_post_failed_title, message, null);
    }
    
    private void onConnectionError() {
        if (!is_active_) return;
        progress_dialog_.hide();
        SimpleDialog.showNotice(this, R.string.dialog_post_failed_title, R.string.dialog_post_error_summary, null);
    }
    
    private void initAAButton() {
        final ToggleButton toggle_aa = (ToggleButton)findViewById(R.id.toggle_aa);
    	final EditText body = (EditText) findViewById(R.id.entry_edit_body);
        final InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
        final Typeface aaFont = ((TuboroidApplication)getApplication()).view_config_.getAAFont(); 
        if (aaFont == null) {
        	toggle_aa.setVisibility(View.GONE);
        } 
        else {
        	final Typeface origFont = body.getTypeface();
        	
	        toggle_aa.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (toggle_aa.isChecked()) {
						body.setTypeface(aaFont);
						body.setHorizontallyScrolling(true);
					} else {
						body.setTypeface(origFont);
						body.setHorizontallyScrolling(false);
					}
					imm.hideSoftInputFromWindow(body.getWindowToken(), 0);
				}
			});
        }
	}

}
