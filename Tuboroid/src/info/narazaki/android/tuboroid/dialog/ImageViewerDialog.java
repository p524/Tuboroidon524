package info.narazaki.android.tuboroid.dialog;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import info.narazaki.android.lib.dialog.SimpleProgressDialog;
import info.narazaki.android.tuboroid.R;
import info.narazaki.android.tuboroid.TuboroidApplication;
import info.narazaki.android.tuboroid.TuboroidApplication.ViewConfig;
import info.narazaki.android.tuboroid.activity.ThreadEntryListActivity;
import info.narazaki.android.tuboroid.agent.ImageFetchAgent;
import info.narazaki.android.tuboroid.data.ThreadData;
import info.narazaki.android.tuboroid.data.ThreadEntryData;
import info.narazaki.android.tuboroid.view.ImageViewerFooter;
import info.narazaki.android.tuboroid.view.ScrollImageView;
import info.narazaki.android.tuboroid.view.ScrollImageView.OnMoveImageListner;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

public class ImageViewerDialog extends Dialog {

	private ScrollImageView image_view;
	private ImageViewerFooter image_viewer_footer;
	private SimpleProgressDialog progress_dialog_;
	private ThreadEntryListActivity activity_;
	private ThreadData thread_data_;
	private Image image_;
	
	class Image {
		public Image(String imageLocalFilename, String imageUri, long entry_id,
				int image_index, int image_count) {
			uri = imageUri;
			path = imageLocalFilename;
			entry_id_ = entry_id;
			image_index_ = image_index;
			image_count_ = image_count;
		}
		

		public String uri;
		public String path;
		public long entry_id_;
		public int image_index_;
		public int image_count_;
	}

	public ImageViewerDialog(ThreadEntryListActivity activity,
			ThreadData thread_data) {
		super(activity, R.style.ImageViewerDialog);

		activity_ = activity;
		thread_data_ = thread_data;
		image_ = new Image("", "", 0, 0, 0);
	}
	
	private boolean moveToImage(long entry_id, int image_index) {
		ThreadEntryData thread_entry = activity_.getEntryData(entry_id);
		if(thread_entry != null) {
			if(image_index == -1) {
				image_index = thread_entry.getImageCount() - 1;
			}
			File image_file = thread_entry.getImageLocalFile(activity_, thread_data_, image_index);
			if(image_file != null) {
				image_.image_count_ = thread_entry.getImageCount();
				image_.image_index_ = image_index;
				image_.path = image_file.getAbsolutePath();
				image_.uri = thread_entry.getImageUri(image_.image_index_);
				image_.entry_id_ = entry_id;
				return true;
			}
		}
		return false;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.image_viewer_dialog);

		image_view = (ScrollImageView) findViewById(R.id.image_viewer_image);
		image_view.setOnMoveImageListner(new OnMoveImageListner() {
			
			@Override
			public void onMoveImage(boolean isNext){
				boolean image_changed = false;
				if(isNext) {
					if(image_.image_index_ == image_.image_count_ - 1) {
						for(int i = (int)image_.entry_id_ + 1; ; i++) {
							ThreadEntryData thread_entry = activity_.getEntryData(i);
							if(thread_entry == null) {
								break;
							}
							if(thread_entry.getImageCount() != 0 && !thread_entry.isNG()){
								image_changed = moveToImage(i, 0);
								break;
							}
						}

					}else {
						image_changed = moveToImage(image_.entry_id_, image_.image_index_ + 1);
					}
				}else {
					if(image_.image_index_ == 0) {
						for(int i = (int)image_.entry_id_ - 1; ; i--) {
							ThreadEntryData thread_entry = activity_.getEntryData(i);
							if(thread_entry == null) {
								break;
							}
							if(thread_entry.getImageCount() != 0 && !thread_entry.isNG()){
								image_changed = moveToImage(i, -1);
								break;
							}
			
						}
					}else {
						image_changed = moveToImage(image_.entry_id_, image_.image_index_ - 1);
					}
				
				}
				if(image_changed) {
					showThumbnail();
				}
			}
		});
		
		image_viewer_footer = (ImageViewerFooter) findViewById(R.id.image_viewer_footer);

		progress_dialog_ = new SimpleProgressDialog();
	}
	
	

	@Override
	protected void onStart(){
		super.onStart();
		
		showThumbnail();
	}

	private void showThumbnail(){
		Display display = activity_.getWindowManager().getDefaultDisplay();
		
		image_viewer_footer.setImageInfo(image_.path, image_.uri, image_view, image_.entry_id_, image_.image_index_, image_.image_count_, display.getWidth());
		
		final File image_local_file = new File(image_.path);
		
		
		progress_dialog_.show(activity_, R.string.dialog_loading_progress,
				new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog){
						// if (is_active_) {
						dismiss();
						// }
					}
				});

		ImageView image_view = (ImageView) findViewById(R.id.image_viewer_image);
		final WeakReference<ImageView> image_view_ref = new WeakReference<ImageView>(
				image_view);
		final Handler handler = new Handler();

		final ImageFetchAgent.BitmapFetchedCallback callback = new ImageFetchAgent.BitmapFetchedCallback() {

			@Override
			public void onCacheFetched(Bitmap bitmap){
				onFetched(bitmap);
			}

			@Override
			public void onFetched(final Bitmap bitmap){
				final ScrollImageView image_view_tmp = (ScrollImageView) image_view_ref
						.get();
				if(image_view_tmp == null)
					return;
				// このスレッドからImageView.postを呼ぶとtrueが返ってくるのにRunnableが実行されないという
				// 事態がまれに発生する。View自体がもつhandlerの代わりに自分で作ったhandlerを使うと大丈夫？

				handler.post(new Runnable() {
					@Override
					public void run(){
						image_view_tmp.setImageBitmap(bitmap);
						progress_dialog_.hide();
					}
				});
			}

			@Override
			public void onFailed(){
				final ScrollImageView image_view_tmp = (ScrollImageView) image_view_ref
						.get();
				if(image_view_tmp == null)
					return;
				image_view_tmp.post(new Runnable() {
					@Override
					public void run(){
						image_view_tmp.setImageBitmap(null);
						image_viewer_footer.setErrorMessage("a");
						progress_dialog_.hide();
					}
				});
			}

			@Override
			public void onBegeinNoCache(){
			}
		};

		activity_.getAgent().fetchImage(callback, image_local_file, image_.uri,
				-1, -1, false);
	}


	public void setImage(String imageLocalFilename, String imageUri
    		, long entry_id, int image_index, int image_count) {
		image_.image_count_ = image_count;
		image_.image_index_ = image_index;
		image_.path = imageLocalFilename;
		image_.uri = imageUri;
		image_.entry_id_ = entry_id;
	}
	

}
