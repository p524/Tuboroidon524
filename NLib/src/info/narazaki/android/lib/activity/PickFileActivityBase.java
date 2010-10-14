package info.narazaki.android.lib.activity;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.regex.Pattern;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import info.narazaki.android.lib.R;

public class PickFileActivityBase extends ListActivity {
    private static final String TAG = "FilePckerBase";
    
    public static final String INTENT_KEY_LIGHT_THEME = "info.narazaki.android.lib.extra.LIGHT_THEME";
    
    public static final String INTENT_KEY_TITLE = "info.narazaki.android.lib.extra.TITLE";
    public static final String INTENT_KEY_ROOT = "info.narazaki.android.lib.extra.ROOT";
    public static final String INTENT_KEY_CURRENT = "info.narazaki.android.lib.extra.CURRENT";
    public static final String INTENT_KEY_FONT_SIZE = "info.narazaki.android.lib.extra.FONT_SIZE";
    public static final String INTENT_KEY_DEFAULT_NEW_FILENAME = "info.narazaki.android.lib.extra.NEW_FILENAME";
    public static final String INTENT_KEY_NEW_FILE_HINT = "info.narazaki.android.lib.extra.NEW_FILE_HINT";
    public static final String INTENT_KEY_ALLOW_NEW_DIR = "info.narazaki.android.lib.extra.ALLOW_NEW_DIR";
    public static final String INTENT_KEY_ALLOW_NEW_FILE = "info.narazaki.android.lib.extra.ALLOW_NEW_FILE";
    
    public static final String INTENT_KEY_CHECK_WRITABLE = "info.narazaki.android.lib.extra.CHECK_WRITABLE";
    public static final String INTENT_KEY_WRITE_FAILED_MESSAGE = "info.narazaki.android.lib.extra.WRITE_FAILED_MESSAGE";
    
    public static final String INTENT_KEY_NEW_FILE_CAPTION = "info.narazaki.android.lib.extra.NEW_FILE_CAPTION";
    public static final String INTENT_KEY_NEW_FILE_TITLE = "info.narazaki.android.lib.extra.NEW_FILE_TITLE";
    
    public static final String INTENT_KEY_NEW_DIR_CAPTION = "info.narazaki.android.lib.extra.NEW_DIR_CAPTION";
    public static final String INTENT_KEY_NEW_DIR_TITLE = "info.narazaki.android.lib.extra.NEW_DIR_TITLE";
    
    public static final String INTENT_KEY_FILE_PATTERN = "info.narazaki.android.lib.extra.FILE_PATTERN";
    public static final String INTENT_KEY_FILE_EXTENTION = "info.narazaki.android.lib.extra.FILE_EXTENTION";
    
    public static final String INTENT_KEY_ALERT_OVERWRITE = "info.narazaki.android.lib.extra.ALERT_OVERWRITE";
    public static final String INTENT_KEY_SLECTION_ALERT_TITLE = "info.narazaki.android.lib.extra.SLECTION_ALERT_TITLE";
    public static final String INTENT_KEY_SLECTION_ALERT_MESSAGE = "info.narazaki.android.lib.extra.SLECTION_ALERT_MESSAGE";
    
    public static final String INTENT_KEY_RECENT_DIR_KEEP_TAG = "info.narazaki.android.lib.extra.RECENT_DIR_KEEP_TAG";
    
    public static final String INTENT_KEY_PICK_DIRECTORY = "info.narazaki.android.lib.extra.PICK_DIRECTORY";
    public static final String INTENT_KEY_PICK_DIR_CAPTION = "info.narazaki.android.lib.extra.PICK_DIR_CAPTION";
    
    public static final int FILE_TYPE_PARENT = 0;
    public static final int FILE_TYPE_NEW = 1;
    public static final int FILE_TYPE_NEW_DIRECTORY = 2;
    public static final int FILE_TYPE_DIRECTORY = 1000;
    public static final int FILE_TYPE_FILE = 1001;
    public static final int FILE_TYPE_PICK_DIRECTORY = 2000;
    
    protected boolean is_light_theme_ = false;
    
    protected File root_directory_ = null;
    protected File current_directory_ = null;
    protected int list_font_size_ = 0;
    
    protected boolean check_writable_ = false;
    protected String write_failed_message_ = null;
    
    protected String new_filename_ = "";
    protected String new_file_hint_ = "";
    
    protected String new_file_caption_ = null;
    protected String new_file_title_ = null;
    
    protected String new_dir_caption_ = null;
    protected String new_dir_title_ = null;
    
    protected String file_pattern_string_ = null;
    protected Pattern file_pattern_ = null;
    
    protected String file_extention_ = null;
    
    protected boolean allow_new_dir_ = false;
    protected boolean allow_new_file_ = false;
    
    protected boolean alert_overwrite_ = false;
    protected String selection_alert_title_ = null;
    protected String selection_alert_message_ = null;
    
    protected String recent_dir_keep_tag_ = null;
    
    protected boolean pick_directory_mode_ = false;
    protected String pick_dir_caption_ = null;
    
    // ////////////////////////////////////////////////////////////
    // 設定系
    // ////////////////////////////////////////////////////////////
    protected int getLayoutViewID() {
        return R.layout.file_pikcer_base;
    }
    
    protected int getRowViewID() {
        return R.layout.file_picker_row_base;
    }
    
    protected File getDefaultRoot() {
        return new File("/");
    }
    
    protected boolean checkVisibleFile(final File file) {
        if (file.isDirectory()) return true;
        if (pick_directory_mode_) return false;
        
        String name = file.getName();
        if (file_pattern_ != null) {
            if (!file_pattern_.matcher(name).find()) return false;
        }
        if (file_extention_ != null) {
            if (name.length() < file_extention_.length() + 1) return false;
            if (!name.endsWith("." + file_extention_)) return false;
        }
        return true;
    }
    
    protected int getFileIconID(final boolean is_light_theme, final FileData file_data) {
        switch (file_data.getFileType()) {
        case FILE_TYPE_PARENT:
            return is_light_theme ? R.drawable.folder_parent_black : R.drawable.folder_parent_white;
        case FILE_TYPE_DIRECTORY:
            return is_light_theme ? R.drawable.folder_close_black : R.drawable.folder_close_white;
        case FILE_TYPE_NEW_DIRECTORY:
            return is_light_theme ? R.drawable.new_folder_black : R.drawable.new_folder_white;
        case FILE_TYPE_NEW:
            return R.drawable.new_document;
        }
        return R.drawable.unknown_document;
    }
    
    // ////////////////////////////////////////////////////////////
    // ステート管理系
    // ////////////////////////////////////////////////////////////
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutViewID());
        
        Bundle bundle = getIntent().getExtras();
        
        is_light_theme_ = getInstanceStateBoolean(bundle, savedInstanceState, INTENT_KEY_LIGHT_THEME, false);
        
        String title = getInstanceStateString(bundle, savedInstanceState, INTENT_KEY_TITLE, null);
        if (title != null) {
            setTitle(title);
        }
        else {
            setTitle(R.string.title_file_picker_base);
        }
        
        String root_directory_name = getInstanceStateString(bundle, savedInstanceState, INTENT_KEY_ROOT, null);
        root_directory_ = root_directory_name != null ? new File(root_directory_name) : getDefaultRoot();
        
        String current_directory_name = getInstanceStateString(bundle, savedInstanceState, INTENT_KEY_CURRENT, null);
        current_directory_ = current_directory_name != null ? new File(current_directory_name) : root_directory_;
        
        check_writable_ = getInstanceStateBoolean(bundle, savedInstanceState, INTENT_KEY_CHECK_WRITABLE, false);
        write_failed_message_ = getInstanceStateString(bundle, savedInstanceState, INTENT_KEY_WRITE_FAILED_MESSAGE,
                null);
        
        file_pattern_string_ = getInstanceStateString(bundle, savedInstanceState, INTENT_KEY_FILE_PATTERN, null);
        file_pattern_ = null;
        if (file_pattern_string_ != null) {
            file_pattern_ = Pattern.compile(file_pattern_string_);
        }
        file_extention_ = getInstanceStateString(bundle, savedInstanceState, INTENT_KEY_FILE_EXTENTION, null);
        
        list_font_size_ = getInstanceStateInt(bundle, savedInstanceState, INTENT_KEY_FONT_SIZE, 0);
        new_filename_ = getInstanceStateString(bundle, savedInstanceState, INTENT_KEY_DEFAULT_NEW_FILENAME, "");
        new_file_hint_ = getInstanceStateString(bundle, savedInstanceState, INTENT_KEY_NEW_FILE_HINT, "");
        allow_new_dir_ = getInstanceStateBoolean(bundle, savedInstanceState, INTENT_KEY_ALLOW_NEW_DIR, false);
        allow_new_file_ = getInstanceStateBoolean(bundle, savedInstanceState, INTENT_KEY_ALLOW_NEW_FILE, false);
        
        new_file_caption_ = getInstanceStateString(bundle, savedInstanceState, INTENT_KEY_NEW_FILE_CAPTION, null);
        new_file_title_ = getInstanceStateString(bundle, savedInstanceState, INTENT_KEY_NEW_FILE_TITLE, null);
        
        new_dir_caption_ = getInstanceStateString(bundle, savedInstanceState, INTENT_KEY_NEW_DIR_CAPTION, null);
        new_dir_title_ = getInstanceStateString(bundle, savedInstanceState, INTENT_KEY_NEW_DIR_TITLE, null);
        
        alert_overwrite_ = getInstanceStateBoolean(bundle, savedInstanceState, INTENT_KEY_ALERT_OVERWRITE, false);
        selection_alert_title_ = getInstanceStateString(bundle, savedInstanceState, INTENT_KEY_SLECTION_ALERT_TITLE,
                null);
        selection_alert_message_ = getInstanceStateString(bundle, savedInstanceState,
                INTENT_KEY_SLECTION_ALERT_MESSAGE, null);
        
        recent_dir_keep_tag_ = getInstanceStateString(bundle, savedInstanceState, INTENT_KEY_RECENT_DIR_KEEP_TAG, null);
        if (recent_dir_keep_tag_ != null) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
            current_directory_name = pref.getString(recent_dir_keep_tag_, null);
            if (current_directory_name != null) {
                current_directory_ = new File(current_directory_name);
            }
        }
        
        if (!current_directory_.getAbsolutePath().startsWith(root_directory_.getAbsolutePath())) {
            current_directory_ = root_directory_;
        }
        
        pick_directory_mode_ = getInstanceStateBoolean(bundle, savedInstanceState, INTENT_KEY_PICK_DIRECTORY, false);
        if (pick_directory_mode_) {
            allow_new_file_ = false;
        }
        pick_dir_caption_ = getInstanceStateString(bundle, savedInstanceState, INTENT_KEY_PICK_DIR_CAPTION, null);
        
        moveDirectory(current_directory_);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
    }
    
    @Override
    protected void onPause() {
        if (recent_dir_keep_tag_ != null) {
            String current_directory_name = null;
            if (current_directory_ != null) current_directory_name = current_directory_.getAbsolutePath();
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = pref.edit();
            editor.putString(recent_dir_keep_tag_, current_directory_name);
            editor.commit();
        }
        super.onPause();
    }
    
    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        
        state.putBoolean(INTENT_KEY_LIGHT_THEME, is_light_theme_);
        
        if (root_directory_ != null) state.putString(INTENT_KEY_ROOT, root_directory_.getAbsolutePath());
        if (current_directory_ != null) state.putString(INTENT_KEY_CURRENT, current_directory_.getAbsolutePath());
        
        state.putBoolean(INTENT_KEY_CHECK_WRITABLE, check_writable_);
        if (write_failed_message_ != null) state.putString(INTENT_KEY_WRITE_FAILED_MESSAGE, write_failed_message_);
        
        if (file_pattern_string_ != null) state.putString(INTENT_KEY_FILE_PATTERN, file_pattern_string_);
        if (file_extention_ != null) state.putString(INTENT_KEY_FILE_EXTENTION, file_extention_);
        
        state.putInt(INTENT_KEY_FONT_SIZE, list_font_size_);
        
        if (new_filename_ != null) state.putString(INTENT_KEY_DEFAULT_NEW_FILENAME, new_filename_);
        if (new_file_hint_ != null) state.putString(INTENT_KEY_NEW_FILE_HINT, new_file_hint_);
        
        state.putBoolean(INTENT_KEY_ALLOW_NEW_DIR, allow_new_dir_);
        state.putBoolean(INTENT_KEY_ALLOW_NEW_FILE, allow_new_file_);
        
        if (new_file_caption_ != null) state.putString(INTENT_KEY_NEW_FILE_CAPTION, new_file_caption_);
        if (new_file_title_ != null) state.putString(INTENT_KEY_NEW_FILE_TITLE, new_file_title_);
        
        if (new_dir_caption_ != null) state.putString(INTENT_KEY_NEW_DIR_CAPTION, new_dir_caption_);
        if (new_dir_title_ != null) state.putString(INTENT_KEY_NEW_DIR_TITLE, new_dir_title_);
        
        state.putBoolean(INTENT_KEY_ALERT_OVERWRITE, alert_overwrite_);
        if (selection_alert_title_ != null) state.putString(INTENT_KEY_SLECTION_ALERT_TITLE, selection_alert_title_);
        if (selection_alert_message_ != null) state.putString(INTENT_KEY_SLECTION_ALERT_MESSAGE,
                selection_alert_message_);
        
        if (recent_dir_keep_tag_ != null) {
            state.putString(INTENT_KEY_RECENT_DIR_KEEP_TAG, recent_dir_keep_tag_);
        }
        
        state.putBoolean(INTENT_KEY_PICK_DIRECTORY, pick_directory_mode_);
        if (pick_dir_caption_ != null) state.putString(INTENT_KEY_PICK_DIR_CAPTION, new_dir_caption_);
    }
    
    // ////////////////////////////////////////////////////////////
    // 選択
    // ////////////////////////////////////////////////////////////
    
    private void moveDirectory(final File directory) {
        current_directory_ = directory;
        TextView view = (TextView) findViewById(android.R.id.text1);
        String local_path = current_directory_.getAbsolutePath().substring(root_directory_.getAbsolutePath().length());
        if (local_path.length() == 0) local_path = "/";
        view.setText(local_path);
        view.setTextSize(list_font_size_);
        setListAdapter(new FileDataListAdapter(current_directory_));
    }
    
    protected void showWriteFailedDialog() {
        Builder builder = new AlertDialog.Builder(this);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {}
        });
        if (write_failed_message_ != null) {
            builder.setMessage(write_failed_message_);
        }
        else {
            builder.setMessage(R.string.text_file_picker_write_failed);
        }
        builder.setCancelable(true);
        builder.show();
    }
    
    protected void onAlertFileSelection(final File file) {
        if (check_writable_ && !file.canWrite()) {
            showWriteFailedDialog();
            return;
        }
        
        if (selection_alert_message_ == null && !alert_overwrite_) {
            onFileSelected(file);
            return;
        }
        Builder builder = new AlertDialog.Builder(this);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                onFileSelected(file);
            }
        });
        builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {}
        });
        if (selection_alert_message_ != null) {
            if (selection_alert_title_ != null) {
                builder.setTitle(selection_alert_title_);
            }
            builder.setMessage(selection_alert_message_);
        }
        else {
            builder.setTitle(R.string.text_file_picker_overwrite_title);
            builder.setMessage(R.string.text_file_picker_overwrite_message);
        }
        builder.setCancelable(true);
        builder.show();
    }
    
    protected void onFileSelected(final File file) {
        Intent intent = new Intent();
        intent.setData(Uri.fromFile(file));
        setResult(RESULT_OK, intent);
        finish();
    }
    
    private void onDirSelected() {
        Intent intent = new Intent();
        intent.setData(Uri.fromFile(current_directory_));
        setResult(RESULT_OK, intent);
        finish();
    }
    
    protected void onAlertNewFile() {
        Builder builder = new AlertDialog.Builder(this);
        
        final EditText edit_text = new EditText(this);
        edit_text.setText(new_filename_);
        if (new_file_hint_ != null) edit_text.setHint(new_file_hint_);
        edit_text.setSingleLine(true);
        edit_text.setImeOptions(EditorInfo.IME_ACTION_DONE);
        builder.setView(edit_text);
        
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                new_filename_ = edit_text.getText().toString();
                if (new_filename_.length() == 0) return;
                File target = new File(current_directory_, new_filename_);
                onNewFileSelected(target);
            }
        });
        builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                new_filename_ = edit_text.getText().toString();
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                new_filename_ = edit_text.getText().toString();
            }
        });
        if (new_file_title_ != null) {
            builder.setTitle(new_file_title_);
        }
        else {
            builder.setTitle(R.string.text_file_picker_new_file_title);
        }
        builder.setCancelable(true);
        builder.show();
    }
    
    protected void onAlertNewDir() {
        Builder builder = new AlertDialog.Builder(this);
        
        final EditText edit_text = new EditText(this);
        edit_text.setSingleLine(true);
        edit_text.setImeOptions(EditorInfo.IME_ACTION_DONE);
        builder.setView(edit_text);
        
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String dirname = edit_text.getText().toString();
                if (dirname.length() == 0) return;
                File target = new File(current_directory_, dirname);
                onNewDirSelected(target);
            }
        });
        builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {}
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {}
        });
        if (new_dir_title_ != null) {
            builder.setTitle(new_dir_title_);
        }
        else {
            builder.setTitle(R.string.text_file_picker_new_dir_title);
        }
        builder.setCancelable(true);
        builder.show();
    }
    
    protected void onNewFileSelected(final File file) {
        if (file.isDirectory()) {
            showWriteFailedDialog();
            return;
        }
        if (file.exists()) {
            onAlertFileSelection(file);
            return;
        }
        onFileSelected(file);
    }
    
    protected void onNewDirSelected(final File file) {
        if (file.exists() || !file.mkdir()) {
            showWriteFailedDialog();
            return;
        }
        moveDirectory(current_directory_);
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        FileData data = (FileData) ((FileDataListAdapter) getListAdapter()).getItem(position);
        File file = data.getFile();
        
        switch (data.getFileType()) {
        case FILE_TYPE_PARENT:
            moveDirectory(file);
            break;
        case FILE_TYPE_DIRECTORY:
            if (!file.canRead()) return;
            moveDirectory(file);
            break;
        case FILE_TYPE_FILE:
            if (!file.canRead()) return;
            if (checkVisibleFile(file)) onAlertFileSelection(file);
            break;
        case FILE_TYPE_NEW:
            onAlertNewFile();
            break;
        case FILE_TYPE_NEW_DIRECTORY:
            onAlertNewDir();
            break;
        case FILE_TYPE_PICK_DIRECTORY:
            onDirSelected();
            break;
        }
    }
    
    // ////////////////////////////////////////////////////////////
    // アダプタ
    // ////////////////////////////////////////////////////////////
    
    static class FileData {
        final int type_;
        final File file_;
        
        public FileData(int type, File file) {
            type_ = type;
            file_ = file;
        }
        
        public File getFile() {
            return file_;
        }
        
        public int getFileType() {
            return type_;
        }
    }
    
    class FileDataListAdapter extends BaseAdapter implements OnEditorActionListener {
        final ArrayList<FileData> data_list_;
        
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            new_filename_ = v.getText().toString();
            return true;
        }
        
        public FileDataListAdapter(final File directory) {
            data_list_ = new ArrayList<FileData>();
            if (!directory.isDirectory()) return;
            
            // pick dir!
            if (pick_directory_mode_) {
                data_list_.add(new FileData(FILE_TYPE_PICK_DIRECTORY, null));
            }
            
            // has parent?
            if (root_directory_.compareTo(directory) < 0) {
                data_list_.add(new FileData(FILE_TYPE_PARENT, directory.getParentFile()));
            }
            
            // new file!
            if (directory.canWrite() && allow_new_file_) {
                data_list_.add(new FileData(FILE_TYPE_NEW, null));
            }
            
            // new dir!
            if (directory.canWrite() && allow_new_dir_) {
                data_list_.add(new FileData(FILE_TYPE_NEW_DIRECTORY, null));
            }
            
            // files
            File[] base_list = directory.listFiles();
            ArrayList<FileData> dirs_list = new ArrayList<FileData>();
            ArrayList<FileData> files_list = new ArrayList<FileData>();
            for (File file : base_list) {
                if (checkVisibleFile(file)) {
                    if (file.isDirectory()) {
                        dirs_list.add(new FileData(FILE_TYPE_DIRECTORY, file));
                    }
                    else {
                        files_list.add(new FileData(FILE_TYPE_FILE, file));
                    }
                }
            }
            Collections.sort(dirs_list, new Comparator<FileData>() {
                @Override
                public int compare(FileData object1, FileData object2) {
                    return object1.getFile().compareTo(object2.getFile());
                }
            });
            Collections.sort(files_list, new Comparator<FileData>() {
                @Override
                public int compare(FileData object1, FileData object2) {
                    return object1.getFile().compareTo(object2.getFile());
                }
            });
            data_list_.addAll(dirs_list);
            data_list_.addAll(files_list);
            
        }
        
        @Override
        public int getCount() {
            return data_list_.size();
        }
        
        @Override
        public Object getItem(int position) {
            if (position >= data_list_.size()) return null;
            return data_list_.get(position);
        }
        
        @Override
        public long getItemId(int position) {
            return 0;
        }
        
        @Override
        public View getView(int position, View convert_view, ViewGroup parent) {
            if (convert_view == null) convert_view = createView();
            if (getCount() <= position || position < 0) return convert_view;
            FileData data = (FileData) getItem(position);
            return setView(convert_view, data);
        }
        
        private View createView() {
            LayoutInflater layout_inflater = LayoutInflater.from(PickFileActivityBase.this);
            View view = layout_inflater.inflate(getRowViewID(), null);
            
            TextView filename_view = (TextView) view.findViewById(R.id.filename);
            if (list_font_size_ != 0) {
                filename_view.setTextSize(list_font_size_);
            }
            return view;
        }
        
        private View setView(View view, FileData data) {
            ImageView icon_view = (ImageView) view.findViewById(R.id.icon);
            icon_view.setImageResource(getFileIconID(is_light_theme_, data));
            
            TextView filename_view = (TextView) view.findViewById(R.id.filename);
            
            int filetype = data.getFileType();
            if (filetype == FILE_TYPE_NEW) {
                if (new_file_caption_ != null) {
                    filename_view.setText(new_file_caption_);
                }
                else {
                    filename_view.setText(R.string.text_file_picker_new_file);
                }
            }
            else if (filetype == FILE_TYPE_NEW_DIRECTORY) {
                if (new_dir_caption_ != null) {
                    filename_view.setText(new_dir_caption_);
                }
                else {
                    filename_view.setText(R.string.text_file_picker_new_dir);
                }
            }
            else if (filetype == FILE_TYPE_PICK_DIRECTORY) {
                if (pick_dir_caption_ != null) {
                    filename_view.setText(pick_dir_caption_);
                }
                else {
                    filename_view.setText(R.string.text_file_picker_pick_dir);
                }
            }
            else {
                switch (filetype) {
                case FILE_TYPE_PARENT:
                    filename_view.setText(R.string.text_file_picker_parent_dir);
                    break;
                case FILE_TYPE_DIRECTORY:
                case FILE_TYPE_FILE:
                    filename_view.setText(data.getFile().getName());
                    break;
                }
            }
            
            return view;
        }
        
    }
    
    private Boolean getInstanceStateBoolean(Bundle intent_bundle, Bundle saved_instance_state, final String key,
            final Boolean default_data) {
        if (intent_bundle != null && intent_bundle.containsKey(key)) {
            return intent_bundle.getBoolean(key);
        }
        else if (saved_instance_state != null && saved_instance_state.containsKey(key)) {
            return saved_instance_state.getBoolean(key);
        }
        return default_data;
    }
    
    private int getInstanceStateInt(Bundle intent_bundle, Bundle saved_instance_state, final String key,
            final int default_data) {
        if (intent_bundle != null && intent_bundle.containsKey(key)) {
            return intent_bundle.getInt(key);
        }
        else if (saved_instance_state != null && saved_instance_state.containsKey(key)) {
            return saved_instance_state.getInt(key);
        }
        return default_data;
    }
    
    private String getInstanceStateString(Bundle intent_bundle, Bundle saved_instance_state, final String key,
            final String default_data) {
        if (intent_bundle != null && intent_bundle.containsKey(key)) {
            return intent_bundle.getString(key);
        }
        else if (saved_instance_state != null && saved_instance_state.containsKey(key)) {
            return saved_instance_state.getString(key);
        }
        return default_data;
    }
}
