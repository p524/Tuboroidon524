package info.narazaki.android.tuboroid.dialog;

import info.narazaki.android.tuboroid.R;
import info.narazaki.android.tuboroid.data.ThreadData;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.view.*;
import android.widget.Button;
import android.widget.TextView;

public class ThreadInfoDialog extends Dialog {
	ThreadData thread_data_;
	
	public ThreadInfoDialog(Context context, ThreadData thread_data) {
		super(context, true, null);
		thread_data_ = thread_data;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);  
    	setContentView(R.layout.thread_info_dialog);
    	    	
    	TextView thread_title_text = (TextView) findViewById(R.id.thread_title_text);
    	thread_title_text.setText(thread_data_.thread_name_);
    	
    	Button copy_thread_title = (Button) findViewById(R.id.copy_thread_title);
    	copy_thread_title.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v){
				ClipboardManager cm = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
				cm.setText(thread_data_.thread_name_);
			}
		});

    	TextView thread_url = (TextView) findViewById(R.id.thread_url);
    	thread_url.setText(thread_data_.getThreadURI());
    	
    	Button copy_thread_url = (Button) findViewById(R.id.copy_thread_url);
    	copy_thread_url.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v){
				ClipboardManager cm = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
				cm.setText(thread_data_.getThreadURI());
			}
		});
    	

    	Button share_link = (Button) findViewById(R.id.share_link);
    	share_link.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v){
				Intent intent = new Intent(android.content.Intent.ACTION_SEND); 
				intent.setType("text/plain"); 

				intent.putExtra(Intent.EXTRA_TEXT, thread_data_.getThreadURI()); 
				getContext().startActivity(intent); 
			}
		});
    	

    	Button open_with_other_app = (Button) findViewById(R.id.open_with_other_app);
    	open_with_other_app.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v){
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(thread_data_.getThreadURI()));  
				getContext().startActivity(intent); 
			}
		});
	}
}
