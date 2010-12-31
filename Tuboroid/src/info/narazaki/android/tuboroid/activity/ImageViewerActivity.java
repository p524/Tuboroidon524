package info.narazaki.android.tuboroid.activity;

import info.narazaki.android.lib.activity.PickFileActivityBase;
import info.narazaki.android.lib.dialog.SimpleProgressDialog;
import info.narazaki.android.lib.system.MigrationSDK5;
import info.narazaki.android.lib.text.NFileNameInfo;
import info.narazaki.android.lib.toast.ManagedToast;
import info.narazaki.android.tuboroid.R;
import info.narazaki.android.tuboroid.activity.base.TuboroidActivity;
import info.narazaki.android.tuboroid.agent.ImageFetchAgent;
import info.narazaki.android.tuboroid.agent.thread.DataFileAgent;
import info.narazaki.android.tuboroid.agent.thread.SQLiteAgent;
import info.narazaki.android.tuboroid.data.ThreadData;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.Toast;

public class ImageViewerActivity extends TuboroidActivity {
    public static final String TAG = "ImageViewerActivity";
    
    public static final String INTENT_KEY_IMAGE_URI = "INTENT_KEY_IMAGE_URI";
    public static final String INTENT_KEY_IMAGE_FILENAME = "INTENT_KEY_IMAGE_FILENAME";
    
    public static final String INTENT_TAG_RECENT_DIR = "INTENT_TAG_RECENT_DIR";
    
    public static final int MENU_KEY_SAVE = 10;
    public static final int MENU_KEY_SHARE = 20;
    
    private File image_local_file_;
    private String image_uri_;
    
    private boolean is_fill_parent_mode_;
    private SimpleProgressDialog progress_dialog_;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        setContentView(R.layout.image_viewer);
        
        image_local_file_ = null;
        is_fill_parent_mode_ = true;
        progress_dialog_ = new SimpleProgressDialog();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        final Uri thread_uri = getIntent().getData();
        String image_uri = null;
        String image_filename = null;
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            if (extras.containsKey(INTENT_KEY_IMAGE_FILENAME)) {
                image_filename = extras.getString(INTENT_KEY_IMAGE_FILENAME);
            }
            if (extras.containsKey(INTENT_KEY_IMAGE_URI)) {
                image_uri = extras.getString(INTENT_KEY_IMAGE_URI);
            }
        }
        final ThreadData thread_data_temp = ThreadData.factory(thread_uri);
        if (thread_data_temp == null || thread_uri == null || image_filename == null || image_filename.length() == 0
                || image_uri == null || image_uri.length() == 0) {
            finish();
            return;
        }
        
        final File image_local_file = new File(image_filename);
        final String const_image_uri = image_uri;
        image_local_file_ = image_local_file;
        image_uri_ = image_uri;
        
        ImageView image_view = (ImageView) findViewById(R.id.image_viewer_image);
        image_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setFillParentMode(!is_fill_parent_mode_);
            }
        });
        setFillParentMode(is_fill_parent_mode_);
        
        // スレッド情報の読み込み
        getAgent().getThreadData(thread_data_temp, new SQLiteAgent.GetThreadDataResult() {
            @Override
            public void onQuery(final ThreadData thread_data) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showThumbnail(thread_data, image_local_file, const_image_uri);
                    }
                });
            }
        });
        
    }
    
    @Override
    protected void onPause() {
        progress_dialog_.hide();
        ImageView image_view = (ImageView) findViewById(R.id.image_viewer_image);
        image_view.destroyDrawingCache();
        super.onPause();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);
        
        MenuItem share_item = menu.add(0, MENU_KEY_SHARE, MENU_KEY_SHARE, getString(R.string.label_menu_share_image));
        
        share_item.setIcon(android.R.drawable.ic_menu_share);
        share_item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (image_local_file_ == null) return true;
                
                Uri file_uri = Uri.fromFile(image_local_file_);
                String extention = MimeTypeMap.getFileExtensionFromUrl(file_uri.toString());
                MimeTypeMap mime_map = MimeTypeMap.getSingleton();
                
                if (!mime_map.hasExtension(extention)) return true;
                String mime_type = mime_map.getMimeTypeFromExtension(extention);
                
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType(mime_type);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.putExtra(Intent.EXTRA_STREAM, file_uri);
                MigrationSDK5.Intent_addFlagNoAnimation(intent);
                startActivity(intent);
                return false;
            }
        });
        
        MenuItem save_item = menu.add(0, MENU_KEY_SAVE, MENU_KEY_SAVE, getString(R.string.label_menu_save_image));
        
        save_item.setIcon(android.R.drawable.ic_menu_save);
        save_item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (image_local_file_ == null) return true;
                if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) return true;
                
                NFileNameInfo file_info = new NFileNameInfo(image_local_file_);
                
                Intent intent = new Intent(ImageViewerActivity.this, PickFileActivity.class);
                intent.putExtra(PickFileActivityBase.INTENT_KEY_ALLOW_NEW_DIR, true);
                intent.putExtra(PickFileActivityBase.INTENT_KEY_ALLOW_NEW_FILE, true);
                intent.putExtra(PickFileActivityBase.INTENT_KEY_ALERT_OVERWRITE, true);
                intent.putExtra(PickFileActivityBase.INTENT_KEY_FILE_EXTENTION, file_info.getExtention());
                intent.putExtra(PickFileActivityBase.INTENT_KEY_CHECK_WRITABLE, true);
                
                Matcher m = Pattern.compile("^[^:]*://[^/]*/([^?]*/)?([^?/]*)(\\?.*)?").matcher(image_uri_);
                String filename = "";
                if(m.find()){
                	filename = m.group(2);
                }
                if(filename == null || filename == ""){
                	filename = image_local_file_.getName();
                }
                intent.putExtra(PickFileActivityBase.INTENT_KEY_DEFAULT_NEW_FILENAME, filename);
                intent.putExtra(PickFileActivityBase.INTENT_KEY_ROOT, Environment.getExternalStorageDirectory()
                        .getAbsolutePath());
                intent.putExtra(PickFileActivityBase.INTENT_KEY_NEW_FILE_CAPTION,
                        getString(R.string.label_filepicker_save_here));
                intent.putExtra(PickFileActivityBase.INTENT_KEY_FONT_SIZE,
                        getTuboroidApplication().view_config_.entry_body_ * 3 / 2);
                intent.putExtra(PickFileActivityBase.INTENT_KEY_TITLE, getString(R.string.label_menu_save_image));
                
                intent.putExtra(PickFileActivityBase.INTENT_KEY_RECENT_DIR_KEEP_TAG, this.getClass().getName()
                        + INTENT_TAG_RECENT_DIR);
                
                MigrationSDK5.Intent_addFlagNoAnimation(intent);
                startActivityForResult(intent, MENU_KEY_SHARE);
                return false;
            }
        });
        
        return result;
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MENU_KEY_SHARE) {
            if (resultCode == RESULT_OK) {
                Uri uri = data.getData();
                if (uri == null) return;
                String target_path = uri.getPath();
                if (target_path == null) return;
                
                getAgent().copyFile(image_local_file_.getAbsolutePath(), target_path,
                        new DataFileAgent.FileWroteCallback() {
                            @Override
                            public void onFileWrote(boolean succeeded) {
                                if (succeeded) {
                                    ManagedToast.raiseToast(getApplicationContext(), R.string.toast_image_saved,
                                            Toast.LENGTH_LONG);
                                }
                                else {
                                    ManagedToast.raiseToast(getApplicationContext(),
                                            R.string.toast_image_failed_to_save, Toast.LENGTH_LONG);
                                }
                            }
                        });
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
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
    
    private void setFillParentMode(boolean fill_parent_mode) {
        ImageView image_view = (ImageView) findViewById(R.id.image_viewer_image);
        
        image_view.setAdjustViewBounds(true);
        
        Display display = getWindowManager().getDefaultDisplay();
        int width = display.getWidth();
        int height = display.getHeight();
        if (fill_parent_mode) {
            image_view.setMaxWidth(width);
            image_view.setMaxHeight(height);
        }
        else {
        	Rect r = image_view.getDrawable().getBounds();
            image_view.setMaxWidth(width * 16);
            image_view.setMaxHeight(height * 16);
        }
        image_view.requestLayout();
        is_fill_parent_mode_ = fill_parent_mode;
    }
    
    private void showThumbnail(final ThreadData thread_data, final File image_local_file, final String uri) {
        if (!is_active_) return;
        
        progress_dialog_.show(this, R.string.dialog_loading_progress, new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (is_active_) {
                    finish();
                }
            }
        });
        
        ImageView image_view = (ImageView) findViewById(R.id.image_viewer_image);
        final WeakReference<ImageView> image_view_ref = new WeakReference<ImageView>(image_view);
        
        final ImageFetchAgent.BitmapFetchedCallback callback = new ImageFetchAgent.BitmapFetchedCallback() {
            
            @Override
            public void onCacheFetched(Bitmap bitmap) {
                onFetched(bitmap);
            }
            
            @Override
            public void onFetched(final Bitmap bitmap) {
                final ImageView image_view_tmp = image_view_ref.get();
                if (image_view_tmp == null) return;
                image_view_tmp.post(new Runnable() {
                    @Override
                    public void run() {
                        image_view_tmp.setImageBitmap(bitmap);
                        progress_dialog_.hide();
                    }
                });
            }
            
            @Override
            public void onFailed() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (is_active_) {
                            finish();
                        }
                    }
                });
            }
            
            @Override
            public void onBegeinNoCache() {}
        };
        
        final float scale = getResources().getDisplayMetrics().density;
        Display display = getWindowManager().getDefaultDisplay();
        final int width = (int) (display.getWidth() / scale);
        final int height = (int) (display.getHeight() / scale);
        

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(image_local_file.getAbsolutePath(), options);
        
        getAgent().fetchImage(callback, image_local_file, uri, options.outWidth, options.outHeight, false);
    }
    
}
