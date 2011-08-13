package info.narazaki.android.tuboroid.dialog;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import info.narazaki.android.lib.activity.PickFileActivityBase;
import info.narazaki.android.lib.dialog.SimpleProgressDialog;
import info.narazaki.android.lib.system.MigrationSDK5;
import info.narazaki.android.lib.text.NFileNameInfo;
import info.narazaki.android.lib.toast.ManagedToast;
import info.narazaki.android.tuboroid.R;
import info.narazaki.android.tuboroid.TuboroidApplication;
import info.narazaki.android.tuboroid.TuboroidApplication.ViewConfig;
import info.narazaki.android.tuboroid.activity.PickFileActivity;
import info.narazaki.android.tuboroid.activity.ThreadEntryListActivity;
import info.narazaki.android.tuboroid.agent.ImageFetchAgent;
import info.narazaki.android.tuboroid.agent.thread.DataFileAgent;
import info.narazaki.android.tuboroid.data.ThreadData;
import info.narazaki.android.tuboroid.data.ThreadEntryData;
import info.narazaki.android.tuboroid.view.ImageViewerFooter;
import info.narazaki.android.tuboroid.view.ScrollImageView;
import info.narazaki.android.tuboroid.view.ScrollImageView.OnMoveImageListner;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.MenuItem.OnMenuItemClickListener;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

public class ImageViewerDialog extends Dialog {
    public static final String TAG = "ImageViewerDialog";
    
    public static final String INTENT_TAG_RECENT_DIR = "INTENT_TAG_RECENT_DIR";
    
    public static final int MENU_KEY_SAVE = 10;
    public static final int MENU_KEY_SHARE = 20;

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
						
						ThreadEntryData thread_entry = activity_.getEntryData(image_.entry_id_);
						thread_entry.setImageCheckEnabled(image_.image_index_);
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
						image_viewer_footer.setErrorMessage(getContext().getString(R.string.dialog_image_load_failed));
						image_view_tmp.setImageBitmap(null);
						progress_dialog_.hide();
						
						ThreadEntryData thread_entry = activity_.getEntryData(image_.entry_id_);
						thread_entry.setImageCheckEnabled(image_.image_index_);
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

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);
        
        MenuItem share_item = menu.add(0, MENU_KEY_SHARE, MENU_KEY_SHARE, activity_.getString(R.string.label_menu_share_image));
        
        share_item.setIcon(android.R.drawable.ic_menu_share);
        share_item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (image_.path == null) return true;
                
                Uri file_uri = Uri.fromFile(new File(image_.path));
                String extention = MimeTypeMap.getFileExtensionFromUrl(file_uri.toString());
                MimeTypeMap mime_map = MimeTypeMap.getSingleton();
                
                if (!mime_map.hasExtension(extention)) return true;
                String mime_type = mime_map.getMimeTypeFromExtension(extention);
                
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType(mime_type);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.putExtra(Intent.EXTRA_STREAM, file_uri);
                MigrationSDK5.Intent_addFlagNoAnimation(intent);
                activity_.startActivity(intent);
                return false;
            }
        });
        
        MenuItem save_item = menu.add(0, MENU_KEY_SAVE, MENU_KEY_SAVE, activity_.getString(R.string.label_menu_save_image));
        
        save_item.setIcon(android.R.drawable.ic_menu_save);
        save_item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (image_.path == null) return true;
                if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) return true;
                
                File image_local_file = new File(image_.path);
                NFileNameInfo file_info = new NFileNameInfo(image_local_file);
                
                Intent intent = new Intent(activity_, PickFileActivity.class);
                intent.putExtra(PickFileActivityBase.INTENT_KEY_ALLOW_NEW_DIR, true);
                intent.putExtra(PickFileActivityBase.INTENT_KEY_ALLOW_NEW_FILE, true);
                intent.putExtra(PickFileActivityBase.INTENT_KEY_ALERT_OVERWRITE, true);
                intent.putExtra(PickFileActivityBase.INTENT_KEY_FILE_EXTENTION, file_info.getExtention());
                intent.putExtra(PickFileActivityBase.INTENT_KEY_CHECK_WRITABLE, true);
                
                Matcher m = Pattern.compile("^[^:]*://[^/]*/([^?]*/)?([^?/]*)(\\?.*)?").matcher(image_.uri);
                String filename = "";
                if(m.find()){
                	filename = m.group(2);
                }
                if(filename == null || filename == ""){
                	filename = image_local_file.getName();
                }
                intent.putExtra(PickFileActivityBase.INTENT_KEY_DEFAULT_NEW_FILENAME, filename);
                intent.putExtra(PickFileActivityBase.INTENT_KEY_ROOT, Environment.getExternalStorageDirectory()
                        .getAbsolutePath());
                intent.putExtra(PickFileActivityBase.INTENT_KEY_NEW_FILE_CAPTION,
                		activity_.getString(R.string.label_filepicker_save_here));
                intent.putExtra(PickFileActivityBase.INTENT_KEY_FONT_SIZE,
                		activity_.getTuboroidApplication().view_config_.entry_body_ * 3 / 2);
                intent.putExtra(PickFileActivityBase.INTENT_KEY_TITLE, activity_.getString(R.string.label_menu_save_image));
                
                intent.putExtra(PickFileActivityBase.INTENT_KEY_RECENT_DIR_KEEP_TAG, this.getClass().getName()
                        + INTENT_TAG_RECENT_DIR);
                
                MigrationSDK5.Intent_addFlagNoAnimation(intent);
                activity_.startActivityForResult(intent, MENU_KEY_SHARE);
                return false;
            }
        });
        
        return result;
    }
    
	//ThreadEntryListActivity.onActivityResult から呼ばれる
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MENU_KEY_SHARE) {
            if (resultCode == Activity.RESULT_OK) {
                Uri uri = data.getData();
                if (uri == null) return;
                String target_path = uri.getPath();
                if (target_path == null) return;
                
                activity_.getAgent().copyFile(image_.path, target_path,
                        new DataFileAgent.FileWroteCallback() {
                            @Override
                            public void onFileWrote(boolean succeeded) {
                                if (succeeded) {
                                    ManagedToast.raiseToast(activity_.getApplicationContext(), R.string.toast_image_saved,
                                            Toast.LENGTH_LONG);
                                }
                                else {
                                    ManagedToast.raiseToast(activity_.getApplicationContext(),
                                            R.string.toast_image_failed_to_save, Toast.LENGTH_LONG);
                                }
                            }
                        });
            }
            return;
        }
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem save_item = menu.findItem(MENU_KEY_SAVE);
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            // SDカードがある
            save_item.setVisible(true);
        }
        else {
            save_item.setVisible(false);
        }
        
        return super.onPrepareOptionsMenu(menu);
    }

	public void setImage(String imageLocalFilename, String imageUri
    		, long entry_id, int image_index, int image_count) {
		image_.image_count_ = image_count;
		image_.image_index_ = image_index;
		image_.path = imageLocalFilename;
		image_.uri = imageUri;
		image_.entry_id_ = entry_id;
	}
	
	public void setThreadData(ThreadData thread_data) {
		thread_data_ = thread_data;
	}
}
