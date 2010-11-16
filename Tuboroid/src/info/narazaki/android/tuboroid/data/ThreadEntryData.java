package info.narazaki.android.tuboroid.data;

import info.narazaki.android.lib.adapter.NListAdapterDataInterface;
import info.narazaki.android.lib.list.ListUtils;
import info.narazaki.android.lib.text.HtmlUtils;
import info.narazaki.android.lib.text.SpanifyAdapter;
import info.narazaki.android.lib.text.SpanifyAdapter.PatternFilter;
import info.narazaki.android.lib.view.SimpleSpanTextViewOnTouchListener;
import info.narazaki.android.tuboroid.R;
import info.narazaki.android.tuboroid.TuboroidApplication;
import info.narazaki.android.tuboroid.TuboroidApplication.ViewConfig;
import info.narazaki.android.tuboroid.agent.ImageFetchAgent;
import info.narazaki.android.tuboroid.agent.TuboroidAgent;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.format.DateFormat;
import android.text.style.ClickableSpan;
import android.text.style.TextAppearanceSpan;
import android.text.style.URLSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView.BufferType;

public class ThreadEntryData implements NListAdapterDataInterface {
    private static final String TAG = "ThreadEntryData";
    
    public static final Pattern BODY_ANCHOR_PATTERN = Pattern.compile("\\>\\>?(\\d+)");
    
    private static final Pattern URL_PATTERN = Pattern.compile("h?ttps?://[-_.!~*'()a-zA-Z0-9;/?:@&=+$,%#]+");
    private static final Pattern IMG_URL_PATTERN = Pattern
            .compile("h?ttps?://[-_.!~*'()a-zA-Z0-9;/?:@&=+$,%#]+\\.(gif|jpe?g|png|GIF|JPE?G|PNG)(?![-_.!~*'()a-zA-Z0-9;/?:@&=+$,%#])");
    
    private static final int MIN_AA_MATCH_LINES = 2;
    
    private static final int MAX_BACK_LINKS = 10;
    
    // 簡易AA判定正規表現
    private static final Pattern AA_PATTERN = Pattern.compile("[" + "\\|/\\ﾟ｀´\\<\\>≪≫＜＞\\,\\.\"\\(\\)･・（）"
            + "●○∀⊂∧Ｖ 　／￣＼￣＿_≡" + "│┃┨┥┤┫┣┠┝├┌┏└┗┐┓┘┛┼╋┿╂─━┻┷┸┴┳┯┰┬┏┳┓┗┻┛┌┬┐└┴┘" + "]{4}.*?\n", Pattern.MULTILINE
            | Pattern.DOTALL);
    private static final Pattern SHRINK_WHITESPACE_PATTERN = Pattern.compile("  +");
    public static final String IS_AA = "1";
    
    // 年を表示するかどうかの判定に
    private static String dateBorder = getNewDateBorder();
    
    public long entry_id_;	// レス番
    
    public String author_name_;
    public String author_mail_;
    public String entry_body_;
    
    public String author_id_;
    public String author_be_;
    public String entry_time_;
    
    public int author_id_count_;
    
    public boolean entry_is_aa_;
    
    public String forward_anchor_list_str_;
    public ArrayList<Long> forward_anchor_list_;
    
    public ArrayList<Long> back_anchor_list_;
    private List<String> img_uri_list_;
    private List<Boolean> img_uri_enabled_list_;
    private List<Boolean> img_uri_check_enabled_list_;
    public int ng_flag_;
    
    static class SpannableCache {
        private Spannable cache_;
        private ViewStyle view_style_;
        private TuboroidApplication.ViewConfig view_config_;
        
        public SpannableCache(Spannable cache, ViewStyle viewStyle, ViewConfig viewConfig) {
            super();
            cache_ = cache;
            view_style_ = viewStyle;
            view_config_ = viewConfig;
        }
        
        public boolean isValid(ViewStyle viewStyle, ViewConfig viewConfig) {
            if (cache_ == null || view_style_ != viewStyle || view_config_ != viewConfig) return false;
            return true;
        }
        
        public Spannable get() {
            return cache_;
        }
        
    }
    
    private SpannableCache entry_header_cache_ = null;
    private SpannableCache entry_body_cache_ = null;
    private SpannableCache entry_rev_cache_ = null;
    
    public ThreadEntryData(boolean cached_data, long entry_id, String author_name, String author_mail,
            String entry_body, String author_id, String author_be, String entry_time, String entry_is_aa,
            String forward_anchor_list_str) {
        super();
        entry_id_ = entry_id;
        author_name_ = author_name;
        author_mail_ = author_mail;
        entry_body_ = entry_body;
        
        author_id_ = author_id;
        author_be_ = author_be;
        entry_time_ = entry_time;
        
        author_id_count_ = 0;
        entry_is_aa_ = entry_is_aa.equals(IS_AA);
        forward_anchor_list_str_ = forward_anchor_list_str;
        
        if (cached_data) {
            initCachedData();
        }
        else {
            initNewData();
        }
        
        back_anchor_list_ = new ArrayList<Long>();
        
        img_uri_list_ = Collections.synchronizedList(new ArrayList<String>());
        img_uri_enabled_list_ = Collections.synchronizedList(new ArrayList<Boolean>());
        img_uri_check_enabled_list_ = Collections.synchronizedList(new ArrayList<Boolean>());
        
        ng_flag_ = IgnoreData.TYPE.NONE;
    }
    
    private void initCachedData() {
        // アンカーリスト初期化
        ArrayList<String> anchor_tokens = ListUtils.split(",", forward_anchor_list_str_);
        forward_anchor_list_ = new ArrayList<Long>();
        for (String str : anchor_tokens) {
            try {
                forward_anchor_list_.add(Long.parseLong(str));
            }
            catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void initNewData() {
        // データのHTML削除
        author_name_ = HtmlUtils.stripAllHtmls(author_name_, false);
        author_mail_ = HtmlUtils.stripAllHtmls(author_mail_, false);
        entry_body_ = HtmlUtils.stripAllHtmls(entry_body_, true);
        entry_body_ = SHRINK_WHITESPACE_PATTERN.matcher(entry_body_).replaceAll(" ");
        
        author_id_ = HtmlUtils.stripAllHtmls(author_id_, false);
        author_be_ = HtmlUtils.stripAllHtmls(author_be_, false);
        entry_time_ = HtmlUtils.stripAllHtmls(entry_time_, false);
        
        entry_is_aa_ = false;
        Matcher aa_matcher = AA_PATTERN.matcher(entry_body_).reset();
        int aa_match_lines = 0;
        while (aa_matcher.find()) {
            aa_match_lines++;
            if (aa_match_lines >= MIN_AA_MATCH_LINES) {
                entry_is_aa_ = true;
                break;
            }
        }
        
        // アンカーリスト初期化
        HashSet<Long> forward_anchor_set = new HashSet<Long>();
        forward_anchor_list_ = new ArrayList<Long>();
        Matcher matcher = BODY_ANCHOR_PATTERN.matcher(entry_body_);
        matcher.reset();
        while (matcher.find()) {
            try {
                long id = Long.parseLong(matcher.group(1));
                if (id > 0 && id < entry_id_ && !forward_anchor_set.contains(id)) {
                    forward_anchor_list_.add(id);
                    forward_anchor_set.add(id);
                }
            }
            catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        forward_anchor_list_str_ = android.text.TextUtils.join(",", forward_anchor_list_);
    }
    
    public boolean hasShownThumbnails() {
        for (Boolean data : img_uri_enabled_list_) {
            if (data) return true;
        }
        return false;
    }
    
    private void parseImageUrl() {
        List<String> img_uri_list_ = this.img_uri_list_;
        List<Boolean> img_uri_enabled_list_ = this.img_uri_enabled_list_;
        List<Boolean> img_uri_check_enabled_list_ = this.img_uri_check_enabled_list_;
        img_uri_list_.clear();
        img_uri_enabled_list_.clear();
        img_uri_check_enabled_list_.clear();
        
        final String body = this.entry_body_;
        
        if (body.contains("://") && body.contains("ttp")) {
	        Matcher matcher = IMG_URL_PATTERN.matcher(body);
	        while (matcher.find()) {
	            String uri = matcher.group(0);
	            if (uri.startsWith("ttp")) {
	                img_uri_list_.add("h" + uri);
	            }
	            else {
	                img_uri_list_.add(uri);
	            }
	            img_uri_enabled_list_.add(false);
	            img_uri_check_enabled_list_.add(false);
	        }
        }
    }
    
    private boolean getImageEnabled(final TuboroidAgent agent, final ThreadData thread_data, final int index) {
        if (img_uri_check_enabled_list_.get(index)) {
            return img_uri_enabled_list_.get(index);
        }
        final boolean enabled = agent.hasImageCacheFile(thread_data, this, index);
        img_uri_enabled_list_.set(index, enabled);
        img_uri_check_enabled_list_.set(index, true);
        return enabled;
    }
    
    public File getImageLocalFile(Context context, ThreadData thread_data, int image_index) {
        if (img_uri_list_.size() < image_index) return null;
        String url = img_uri_list_.get(image_index);
        int dot_index = url.lastIndexOf('.');
        if (dot_index == -1) return null;
        
        String dot_ext = url.substring(dot_index).toLowerCase();
        
        String filename = "image_" + entry_id_ + "_" + image_index + dot_ext;
        
        return thread_data.getLocalAttachFile(context, filename);
    }
    
    public boolean isNG() {
        return IgnoreData.isNG(ng_flag_);
    }
    
    public boolean isGone() {
        return IgnoreData.isGone(ng_flag_);
    }
    
    public static interface AnalyzeThreadEntryListProgressCallback {
        public void onProgress(int current, int max);
    }
    
    public static void analyzeThreadEntryList(final TuboroidAgent agent, final ThreadData thread_data,
            final TuboroidApplication.ViewConfig view_config, final ViewStyle style,
            final List<ThreadEntryData> data_list_orig, final AnalyzeThreadEntryListProgressCallback callback) {
    	
    	final int data_list_size = data_list_orig.size();
    	final ThreadEntryData [] data_list = new ThreadEntryData [data_list_size];
        data_list_orig.toArray(data_list);
        HashMap<String, ThreadEntryData> author_id_map = new HashMap<String, ThreadEntryData>(data_list_size);
        
        dateBorder = getNewDateBorder();
        
        callback.onProgress(1, 8);
        for (ThreadEntryData data : data_list) {
            synchronized (thread_data) {
                // 状態クリア
                data.back_anchor_list_ = new ArrayList<Long>();
                
                // バックアンカーの抽出
                for (long id : data.forward_anchor_list_) {
                    int iid = (int) id;
                    if (data_list_size >= iid && iid > 0) {
                        data_list[iid - 1].back_anchor_list_.add(data.entry_id_);
                    }
                }
            }
        }
        
        // レス数を数える
        callback.onProgress(2, 8);
        for (ThreadEntryData data : data_list) {
            synchronized (thread_data) {
                ThreadEntryData orig = author_id_map.get(data.author_id_);
                if (orig != null) {
                    orig.author_id_count_++;
                }
                else {
                    author_id_map.put(data.author_id_, data);
                    data.author_id_count_ = 1;
                }
            }
        }
        
        // 画像チェック
        callback.onProgress(3, 8);
        for (ThreadEntryData data : data_list) {
            synchronized (data) {
                data.parseImageUrl();
            }
        }
        callback.onProgress(4, 8);
        for (ThreadEntryData data : data_list) {
            synchronized (data) {
            	int data_img_uri_list_size = data.img_uri_list_.size(); 
                for (int i = 0; i < data_img_uri_list_size; i++) {
                    data.getImageEnabled(agent, thread_data, i);
                }
            }
        }
        
        callback.onProgress(5, 8);
        for (ThreadEntryData data : data_list) {
            // 数えたレス数を反映させる
            data.author_id_count_ = author_id_map.get(data.author_id_).author_id_count_;
            // ヘッダを処理
            data.createSpannableEntryHeader(view_config, style);
        }
        
        // 逆アンカー
        callback.onProgress(6, 8);
        if (view_config.use_back_anchor_) {
            for (ThreadEntryData data : data_list) {
                synchronized (data) {
                    if (data.back_anchor_list_.size() > 0) {
                        data.createSpannableRevLink(view_config, style);
                    }
                }
            }
        }
        
        // ボディ
        callback.onProgress(7, 8);
        createSpannableEntryBodies(view_config, style, data_list);
    }
    
    private static ExecutorService body_creator_thread_executor_;
    
    private synchronized static void createSpannableEntryBodies(final TuboroidApplication.ViewConfig view_config,
            final ViewStyle style, final ThreadEntryData [] data_list) {
        if (body_creator_thread_executor_ == null) {
            body_creator_thread_executor_ = Executors.newSingleThreadExecutor();
        }
        
        body_creator_thread_executor_.submit(new Runnable() {
            @Override
            public void run() {
                for (ThreadEntryData data : data_list) {
                    // ボディを処理
                    data.getSpannableEntryBody(view_config, style);
                }
            }
        });
    }
    
    @Override
    public long getId() {
        return entry_id_;
    }
    
    static public interface OnAnchorClickedCallback {
        public void onNumberAnchorClicked(int jump_from, int jump_to);
        
        public void onThreadLinkClicked(Uri uri);
        
        public void onBoardLinkClicked(Uri uri);
    }
    
    static class ViewTag {
        TextView header_view;
        TextView entry_body_view;
        LinearLayout rev_anchor_box_view;
        TextView rev_anchor_view;
        TableLayout thumbnail_box;
        TuboroidApplication.ViewConfig view_config;
        int header_bgcolor = -1;
        
        void setHeaderBackgroundColor(int color) {
            if (header_bgcolor != color) {
                header_view.setBackgroundColor(color);
                header_bgcolor = color;
            }
        }
    }
    
    private static View.OnLongClickListener createLongClickDelegate(final View view) {
        return new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (v == null) return false;
                if (!v.isShown()) return false;
                return view.performLongClick();
            }
        };
    }
    
    public static View initView(final View view, TuboroidApplication.ViewConfig view_config, ViewStyle style,
            boolean is_aa) {
        View.OnLongClickListener long_click_delegate = createLongClickDelegate(view);
        
        SimpleSpanTextViewOnTouchListener on_touch_listener = new SimpleSpanTextViewOnTouchListener(
                view_config.touch_margin_, style.on_clicked_bgcolor_, true);
        
        // FIXME : スクロール停止問題暫定対応
        // HorizontalScrollViewにタッチイベントが食うため?か
        // LongClickイベントをTextViewで引っ掛けないとコンテキストメニューが出ない。
        // 一方、LongClickを有効にしているとスクロールがシングルタップで止まらない。
        //
        // そのためHorizontalScrollViewが出る時は諦めてLongClickを引っ掛け、
        // 出さない時はLongClickを引っ掛けるのもオフにしている。
        
        ViewTag tag = new ViewTag();
        view.setTag(tag);
        
        tag.view_config = view_config;
        
        tag.header_view = (TextView) view.findViewById(R.id.entry_header);
        tag.header_view.setTextSize(view_config.entry_header_);
        tag.header_view.setOnTouchListener(on_touch_listener);
        tag.header_view.setOnLongClickListener(long_click_delegate);
        tag.header_view.setLongClickable(is_aa);
        
        tag.entry_body_view = (TextView) view.findViewById(R.id.entry_body);
        tag.entry_body_view.setLinkTextColor(style.link_color_);
        tag.entry_body_view.setOnTouchListener(on_touch_listener);
        tag.entry_body_view.setOnLongClickListener(long_click_delegate);
        tag.entry_body_view.setLongClickable(is_aa);
        
        tag.entry_body_view = (TextView) view.findViewById(R.id.entry_body);
        if (!is_aa) {
            tag.entry_body_view.setTextSize(view_config.entry_body_);
            tag.entry_body_view.getPaint().setSubpixelText(false);
        }
        else {
            tag.entry_body_view.setTextSize(view_config.entry_aa_body_);
            Typeface aa_font = view_config.getAAFont();
            if (aa_font != null) tag.entry_body_view.setTypeface(aa_font);
            tag.entry_body_view.getPaint().setSubpixelText(true);
        }
        tag.rev_anchor_box_view = (LinearLayout) view.findViewById(R.id.entry_rev_anchor_box);
        
        tag.rev_anchor_view = (TextView) view.findViewById(R.id.entry_rev_anchor);
        if (view_config.use_back_anchor_) {
            tag.rev_anchor_view.setTextSize(view_config.entry_body_);
            tag.rev_anchor_view.setLinkTextColor(style.link_color_);
            tag.rev_anchor_view.setOnTouchListener(on_touch_listener);
            tag.rev_anchor_box_view.setVisibility(View.VISIBLE);
        }
        else {
            tag.rev_anchor_box_view.setVisibility(View.GONE);
        }
        
        tag.thumbnail_box = (TableLayout) view.findViewById(R.id.thread_list_thumbnail_box);
        tag.thumbnail_box.setOnLongClickListener(long_click_delegate);
        tag.thumbnail_box.setLongClickable(true);
        
        return view;
    }
    
    public interface ImageViewerLauncher {
        void onRequired(final ThreadData thread_data, final String image_local_filename, final String image_uri);
    }
    
    public View setView(final TuboroidAgent agent, final ThreadData thread_data, final View view,
            final ViewGroup parent, int read_count, final TuboroidApplication.ViewConfig view_config,
            final ViewStyle style, final boolean is_quick_show, int indent) {
        ViewTag tag = (ViewTag) view.getTag();
        
        if (tag.view_config != view_config) {
        	initView(view, view_config, style, entry_is_aa_);
        	tag = (ViewTag)view.getTag();
        }
        
        // インデント
        indent *= style.entry_tree_indent;
        if (view.getPaddingLeft() != indent) {
        	view.setPadding(indent, view.getPaddingTop(), view.getPaddingRight(), view.getPaddingBottom());
        }
        
        // 透明あぼーん判定
        if (isGone()) {
            setVisibilityIfChanged(view, View.GONE);
            return view;
        }
        else {
            setVisibilityIfChanged(view, View.VISIBLE);
        }
        
        // ////////////////////////////////////////////////////////////
        // ヘッダ部分
        if (entry_id_ <= read_count) {
            tag.setHeaderBackgroundColor(style.style_header_color_default_);
        }
        else {
            tag.setHeaderBackgroundColor(style.style_header_color_emphasis_);
        }
        
        Spannable spanned = getSpannableEntryHeader(view_config, style);
        tag.header_view.setText(spanned, BufferType.NORMAL);
        
        // ////////////////////////////////////////////////////////////
        // ボディ
        if (isNG()) {
            // あぼーん
            SpannableString ignored_token = new SpannableString(view.getResources().getString(
                    R.string.text_ignored_entry));
            ignored_token.setSpan(style.ignored_span_, 0, ignored_token.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            tag.entry_body_view.setText(ignored_token, BufferType.NORMAL);
        }
        else {
            tag.entry_body_view.setText(getSpannableEntryBody(view_config, style), BufferType.SPANNABLE);
        }
        
        // ////////////////////////////////////////////////////////////
        // 逆リンク
        if (view_config.use_back_anchor_) {
            if (view_config.use_back_anchor_ && back_anchor_list_.size() > 0) {
                tag.rev_anchor_view.setText(getSpannableRevLink(view_config, style), BufferType.SPANNABLE);
                tag.rev_anchor_box_view.setVisibility(View.VISIBLE);
            }
            else {
                tag.rev_anchor_box_view.setVisibility(View.GONE);
            }
        }
        
        // ////////////////////////////////////////////////////////////
        // サムネ
        int parent_width = parent.getWidth();
        int thumbnail_size = view_config.thumbnail_size_;
        final int thumbnail_cols = (thumbnail_size > 0 && parent_width > thumbnail_size) ? parent_width
                / thumbnail_size : 1;
        
        if (is_quick_show || img_uri_list_.size() == 0) {
            // サムネ無し
            if (tag.entry_body_view.isLongClickable() && !entry_is_aa_) tag.entry_body_view.setLongClickable(false);
            
            if (tag.thumbnail_box.getChildCount() > 0) tag.thumbnail_box.removeAllViews();
            setVisibilityIfChanged(tag.thumbnail_box, View.GONE);
        }
        else {
            // サムネ有り
            if (!tag.entry_body_view.isLongClickable()) tag.entry_body_view.setLongClickable(true);
            
            final int thumbnail_rows = (img_uri_list_.size() - 1) / thumbnail_cols + 1;
            
            initThumbnailsBox(tag.thumbnail_box, thumbnail_cols, thumbnail_rows, view_config);
            rebuildThumbnails(tag.thumbnail_box, thumbnail_cols, thumbnail_rows, agent, thread_data, view_config, style);
            setVisibilityIfChanged(tag.thumbnail_box, View.VISIBLE);
        }
        
        return view;
    }
    
    private void initThumbnailsBox(final TableLayout thumbnail_box, final int thumbnail_cols, final int thumbnail_rows,
            final TuboroidApplication.ViewConfig view_config) {
        if (thumbnail_box.getChildCount() > 0) thumbnail_box.removeAllViews();
        
        ListIterator<String> it = img_uri_list_.listIterator();
        for (int i = 0; i < thumbnail_rows; i++) {
            TableRow table_row = new TableRow(thumbnail_box.getContext());
            for (int j = 0; j < thumbnail_cols; j++) {
                if (it.hasNext()) {
                    final ImageButton image_button = new ImageButton(thumbnail_box.getContext());
                    
                    image_button.setBackgroundResource(android.R.color.transparent);
                    image_button.setPadding(0, 5, 0, 5);
                    image_button.setMinimumWidth(view_config.thumbnail_size_);
                    image_button.setScaleType(ScaleType.CENTER);
                    image_button.setOnClickListener(null);
                    image_button.setLongClickable(false);
                    table_row.addView(image_button);
                    it.next();
                }
            }
            thumbnail_box.addView(table_row);
        }
    }
    
    public void deleteThumbnails(final Context context, final TuboroidAgent agent, final ThreadData thread_data) {
        for (int i = 0; i < img_uri_list_.size(); i++) {
            if (img_uri_enabled_list_.get(i)) {
                img_uri_enabled_list_.set(i, false);
                final File local_image_file = getImageLocalFile(context, thread_data, i);
                try {
                    local_image_file.delete();
                }
                catch (SecurityException e) {
                }
                agent.deleteImage(local_image_file);
            }
        }
    }
    
    private void rebuildThumbnails(final TableLayout thumbnail_box, final int thumbnail_cols, final int thumbnail_rows,
            final TuboroidAgent agent, final ThreadData thread_data, final TuboroidApplication.ViewConfig view_config,
            final ViewStyle style) {
        int image_index = 0;
        for (int i = 0; i < thumbnail_rows; i++) {
            final TableRow table_row = (TableRow) thumbnail_box.getChildAt(i);
            for (int j = 0; j < thumbnail_cols; j++) {
                if (image_index < img_uri_list_.size()) {
                    final ImageButton image_button = (ImageButton) table_row.getChildAt(j);
                    final boolean enabled = getImageEnabled(agent, thread_data, image_index);
                    
                    if (enabled) {
                        showThumbnail(image_button, agent, thread_data, image_index, view_config, style);
                    }
                    else {
                        image_button.setImageResource(R.drawable.ic_btn_show_thumbnail);
                        setShowThumbnailButton(image_button, agent, thread_data, image_index, view_config, style);
                    }
                    
                    image_index++;
                }
            }
        }
    }
    
    private void showThumbnail(final ImageButton image_button, final TuboroidAgent agent, final ThreadData thread_data,
            final int image_index, final TuboroidApplication.ViewConfig view_config, final ViewStyle style) {
        if (thread_data == null) return;
        
        img_uri_enabled_list_.set(image_index, true);
        
        final Context context = image_button.getContext();
        final String image_uri = img_uri_list_.get(image_index);
        final File local_image_file = getImageLocalFile(context, thread_data, image_index);
        
        final WeakReference<ImageButton> image_button_ref = new WeakReference<ImageButton>(image_button);
        
        final ImageFetchAgent.BitmapFetchedCallback callback = new ImageFetchAgent.BitmapFetchedCallback() {
            
            @Override
            public void onBegeinNoCache() {
                final ImageButton image_button_tmp = image_button_ref.get();
                if (image_button_tmp == null) return;
                image_button_tmp.setImageResource(R.drawable.ic_btn_load_image);
            }
            
            @Override
            public void onCacheFetched(Bitmap bitmap) {
                final ImageButton image_button_tmp = image_button_ref.get();
                if (image_button_tmp == null) return;
                image_button_tmp.setImageBitmap(bitmap);
                image_button_tmp.setOnClickListener(createThumbnailOnClickListener(thread_data, style, image_uri,
                        local_image_file));
            }
            
            @Override
            public void onFetched(final Bitmap bitmap) {
                final ImageButton image_button_tmp = image_button_ref.get();
                if (image_button_tmp == null) return;
                image_button_tmp.post(new Runnable() {
                    @Override
                    public void run() {
                        image_button_tmp.setImageBitmap(bitmap);
                        image_button_tmp.setOnClickListener(createThumbnailOnClickListener(thread_data, style,
                                image_uri, local_image_file));
                    }
                });
            }
            
            @Override
            public void onFailed() {
                final ImageButton image_button_tmp = image_button_ref.get();
                if (image_button_tmp == null) return;
                image_button_tmp.post(new Runnable() {
                    @Override
                    public void run() {
                        image_button_tmp.setImageResource(R.drawable.ic_btn_load_image_failed);
                        setShowThumbnailButton(image_button, agent, thread_data, image_index, view_config, style);
                    }
                });
            }
        };
        
        agent.fetchImage(callback, local_image_file, img_uri_list_.get(image_index), view_config.real_thumbnail_size_,
                view_config.real_thumbnail_size_, true);
    }
    
    private void setShowThumbnailButton(final ImageButton image_button, final TuboroidAgent agent,
            final ThreadData thread_data, final int image_index, final TuboroidApplication.ViewConfig view_config,
            final ViewStyle style) {
        image_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                v.post(new Runnable() {
                    @Override
                    public void run() {
                        image_button.setClickable(false);
                        showThumbnail((ImageButton) v, agent, thread_data, image_index, view_config, style);
                    }
                });
            }
        });
    }
    
    private View.OnClickListener createThumbnailOnClickListener(final ThreadData thread_data, final ViewStyle style,
            final String image_uri, final File local_image_file) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.post(new Runnable() {
                    @Override
                    public void run() {
                        if (style.image_viewer_launcher_ != null) {
                            style.image_viewer_launcher_.onRequired(thread_data, local_image_file.getAbsolutePath(),
                                    image_uri);
                        }
                    }
                });
            }
        };
    }
    
    private void setVisibilityIfChanged(View view, int visibility) {
        if (view.getVisibility() == visibility) return;
        view.setVisibility(visibility);
    }
    
    private synchronized Spannable getSpannableEntryHeader(final TuboroidApplication.ViewConfig view_config,
            final ViewStyle style) {
        if (entry_header_cache_ == null || !entry_header_cache_.isValid(style, view_config)) {
            createSpannableEntryHeader(view_config, style);
        }
        return entry_header_cache_.get();
    }
    
    private synchronized void createSpannableEntryHeader(final TuboroidApplication.ViewConfig view_config,
            final ViewStyle style) {
        StringBuilder buf = new StringBuilder();
        
        // ////////////////////////////////////////////////////////////
        // 文字列組み立て
        // ////////////////////////////////////////////////////////////
        // レス番号
        final String entry_id_string = String.valueOf(entry_id_);
        buf.append(entry_id_string);
        buf.append(' ');
        
        final int entry_id_length = entry_id_string.length();
        TextAppearanceSpan entry_id_span = null;
        
        final int back_anchor_list_size = back_anchor_list_.size(); 
        if (back_anchor_list_size == 0) {
            entry_id_span = style.entry_id_style_span_1_;
        }
        else if (back_anchor_list_size < 3) {
            entry_id_span = style.entry_id_style_span_2_;
        }
        else {
            entry_id_span = style.entry_id_style_span_3_;
        }
        
        // //////////////////////////////
        // 名前とメール
        final int author_name_begin = buf.length();
        int author_name_length = 0;
        TextAppearanceSpan author_name_span = null;
        int author_mail_begin = 0;
        int author_mail_length = 0;
        TextAppearanceSpan author_mail_span = null;
        if (!isNG()) {
            // 名前
            author_name_length = author_name_.length();
            buf.append(author_name_);
            buf.append(' ');
            author_name_span = style.author_name_style_span_;
            
            // メール
            author_mail_length = author_mail_.length();
            if (author_mail_length > 0) {
                author_mail_begin = buf.length();
                author_mail_length += 2;
                //buf.append('[');
                buf.append(author_mail_);
                buf.append(' ');
                //buf.append("] ");
                author_mail_span = style.author_mail_style_span_;
            }
        }
        
        // //////////////////////////////
        // 時間
        final int entry_time_begin = buf.length();
        final int entry_time_length;
        // 0000/00/00(/) 00:00:00 の形式で1ヶ月以内のときだけ年を省略
        if (entry_time_.length() == 12 && dateBorder.compareTo(entry_time_) < 0) {
        	// 年を省略
	        entry_time_length = entry_time_.length()-5;
	        buf.append(entry_time_.substring(5));
        }
        else {
	        entry_time_length = entry_time_.length();
	        buf.append(entry_time_);
        }
        buf.append(' ');
        
        // //////////////////////////////
        // 書き込みID
        final int author_id_begin = buf.length();
        final int author_id_real_length = author_id_.length();
        int author_id_length = 0;
        int author_id_count = 0;
        
        //buf.append("ID:");
        //final int author_id_prefix_length = 3;
        final int author_id_prefix_length = 0;
        
        if (author_id_real_length > 0) {
            author_id_length = author_id_real_length;
            buf.append(author_id_);
            if (author_id_count_ > 1 && author_id_.indexOf('?') == -1) {
                author_id_count = author_id_count_;
            }
        }
        else {
            buf.append("????");
            author_id_length = 4; // <= "????".length();
        }
        author_id_length += author_id_prefix_length;
        
        // 必死度を数える
        TextAppearanceSpan author_id_span = null;
        if (author_id_count >= 5) {
            author_id_span = style.author_id_style_span_3_;
        }
        else if (author_id_count >= 2) {
            author_id_span = style.author_id_style_span_2_;
        }
        else {
            author_id_span = style.author_id_style_span_1_;
        }
        
        // //////////////////////////////
        // レス数
        final int author_id_count_begin = buf.length();
        int author_id_count_length = 0;
        if (author_id_count > 1) {
            buf.append(" (");
            buf.append(author_id_count);
            buf.append(") ");
            author_id_count_length = buf.length() - author_id_count_begin;
        }
        
        // //////////////////////////////
        // Be
        int author_be_begin = buf.length();
        int author_be_length = author_be_.length();
        if (author_be_length > 0) {
            buf.append(' ');
            buf.append(author_be_);
        }
        
        // ////////////////////////////////////////////////////////////
        // span適用
        // ////////////////////////////////////////////////////////////
        SpannableString spanned = new SpannableString(buf);
        
        
        // //////////////////////////////
        // レス番号
        spanned.setSpan(entry_id_span, 0, entry_id_length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        
        // //////////////////////////////
        // 名前
        if (author_name_span != null) {
            spanned.setSpan(author_name_span, author_name_begin, author_name_begin + author_name_length,
                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            if (author_mail_span != null) {
                spanned.setSpan(author_mail_span, author_mail_begin, author_mail_begin + author_mail_length,
                        Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            }
        }
        
        // //////////////////////////////
        // 時間
        spanned.setSpan(style.entry_time_style_span_, entry_time_begin, entry_time_begin + entry_time_length,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        
        // //////////////////////////////
        // 書き込みID
        spanned.setSpan(style.author_id_prefix_style_span_, author_id_begin, author_id_begin + author_id_prefix_length,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        // 必死度を色付け
        spanned.setSpan(author_id_span, author_id_begin + author_id_prefix_length, author_id_begin + author_id_length,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        
        // //////////////////////////////
        // レス数
        if (author_id_count > 1) {
            spanned.setSpan(style.author_id_suffix_style_span_, author_id_count_begin, author_id_count_begin
                    + author_id_count_length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        }
        
        // ////////////////////////////////////////////////////////////
        // Be
        if (author_be_length > 0) {
            spanned.setSpan(style.author_be_style_span_, author_be_begin, author_be_begin + author_be_length,
                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        }
        
        entry_header_cache_ = new SpannableCache(spanned, style, view_config);
    }
    
    private synchronized Spannable getSpannableEntryBody(final TuboroidApplication.ViewConfig view_config,
            final ViewStyle style) {
        if (entry_body_cache_ == null || !entry_body_cache_.isValid(style, view_config)) {
            createSpannableEntryBody(view_config, style);
        }
        return entry_body_cache_.get();
    }
    
    private synchronized void createSpannableEntryBody(final TuboroidApplication.ViewConfig view_config,
            final ViewStyle style) {
    	String body = entry_body_;
    	if (body.length() > 0) {
    		body = body.substring(body.charAt(0) == ' ' ? 1 : 0).replace("\n ", "\n");
    	}
        Spannable spannable = style.spanify_.apply(
        		body, 
        		new Long(entry_id_));
        entry_body_cache_ = new SpannableCache(spannable, style, view_config);
    }
    
    private synchronized Spannable getSpannableRevLink(TuboroidApplication.ViewConfig view_config, ViewStyle style) {
        if (entry_rev_cache_ == null || !entry_rev_cache_.isValid(style, view_config)) {
            createSpannableRevLink(view_config, style);
        }
        return entry_rev_cache_.get();
    }
    
    private synchronized void createSpannableRevLink(TuboroidApplication.ViewConfig view_config, ViewStyle style) {
        class JumpAnchorSpan extends ClickableSpan {
            private long num_;
            ViewStyle style_;
            
            public JumpAnchorSpan(ViewStyle style, long num) {
                num_ = num;
                style_ = style;
            }
            
            @Override
            public void onClick(View widget) {
                if (num_ > 0) style_.callback_.onNumberAnchorClicked((int) entry_id_, (int) num_);
            }
        }
        
        SpannableStringBuilder spanned = new SpannableStringBuilder();
        int back_links = 0;
        synchronized (ThreadEntryData.class) {
            for (Long num : back_anchor_list_) {
                spanned.append(" [");
                String num_str = "<<" + String.valueOf(num);
                int start_index = spanned.length();
                spanned.append(num_str);
                spanned.setSpan(new JumpAnchorSpan(style, num), start_index, start_index + num_str.length(),
                        Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                spanned.append("]");
                back_links++;
                if (back_links >= MAX_BACK_LINKS) break;
            }
        }
        entry_rev_cache_ = new SpannableCache(spanned, style, view_config);
    }

    private static String getNewDateBorder() {
    	return (String) DateFormat.format("yyyy/MM/dd", 
    			System.currentTimeMillis() - (long)31*24*60*60*1000);
    }
    
    static public class ViewStyle {
        public int style_header_color_default_;
        public int style_header_color_emphasis_;
        
        public TextAppearanceSpan entry_id_style_span_1_;
        public TextAppearanceSpan entry_id_style_span_2_;
        public TextAppearanceSpan entry_id_style_span_3_;
        public TextAppearanceSpan author_name_style_span_;
        public TextAppearanceSpan author_mail_style_span_;
        public TextAppearanceSpan entry_time_style_span_;
        public TextAppearanceSpan author_id_prefix_style_span_;
        public TextAppearanceSpan author_id_style_span_1_;
        public TextAppearanceSpan author_id_style_span_2_;
        public TextAppearanceSpan author_id_style_span_3_;
        public TextAppearanceSpan author_id_suffix_style_span_;
        public TextAppearanceSpan author_be_style_span_;
        public TextAppearanceSpan ignored_span_;
        
        public int link_color_;
        public int on_clicked_bgcolor_;
        public int entry_tree_indent;
        
        public SpanifyAdapter spanify_;
        public OnAnchorClickedCallback callback_;
        public ImageViewerLauncher image_viewer_launcher_;
        
        public ViewStyle(Activity activity, ImageViewerLauncher image_viewer_launcher, OnAnchorClickedCallback callback) {
        	final TypedArray theme = activity.obtainStyledAttributes(R.styleable.Theme);
        	
            style_header_color_default_ = theme.getColor(
            		R.styleable.Theme_headerColorDefault, 0);
            style_header_color_emphasis_ = theme.getColor(
                    R.styleable.Theme_headerColorEmphasis, 0);
            
            link_color_ = theme.getColor(R.styleable.Theme_entryLinkColor,
                    0);
            
            entry_tree_indent = (int)theme.getDimension(R.styleable.Theme_entryTreeIndent, 10);
            
            on_clicked_bgcolor_ = theme.getColor(
                    R.styleable.Theme_entryLinkClickedBgColor, 0);
            
            entry_id_style_span_1_ = new TextAppearanceSpan(activity, R.style.EntryListEntryID1);
            entry_id_style_span_2_ = new TextAppearanceSpan(activity, R.style.EntryListEntryID2);
            entry_id_style_span_3_ = new TextAppearanceSpan(activity, R.style.EntryListEntryID3);
            author_name_style_span_ = new TextAppearanceSpan(activity, R.style.EntryListAuthorName);
            author_mail_style_span_ = new TextAppearanceSpan(activity, R.style.EntryListAuthorMail);
            entry_time_style_span_ = new TextAppearanceSpan(activity, R.style.EntryListEntryTime);
            author_id_prefix_style_span_ = new TextAppearanceSpan(activity, R.style.EntryListAuthorIDPrefix);
            author_id_style_span_1_ = new TextAppearanceSpan(activity, R.style.EntryListAuthorID1);
            author_id_style_span_2_ = new TextAppearanceSpan(activity, R.style.EntryListAuthorID2);
            author_id_style_span_3_ = new TextAppearanceSpan(activity, R.style.EntryListAuthorID3);
            author_id_suffix_style_span_ = new TextAppearanceSpan(activity, R.style.EntryListAuthorIDSuffix);
            author_be_style_span_ = new TextAppearanceSpan(activity, R.style.EntryListAuthorBE);
            ignored_span_ = new TextAppearanceSpan(activity, R.style.EntryListIgnored);
            
            image_viewer_launcher_ = image_viewer_launcher;
            callback_ = callback;
            
            spanify_ = new SpanifyAdapter();
            spanify_.addFilter(new ThreadWebUriFilter());
            spanify_.addFilter(new EntryAnchorFilter());
        }
        
        private class ThreadWebUriFilter extends SpanifyAdapter.PatternFilter {
            private class ThreadURLSpan extends URLSpan {
                public ThreadURLSpan(String uri) {
                    super(uri);
                }
                
                @Override
                public void onClick(View widget) {
                    Uri uri = Uri.parse(getURL());
                    callback_.onThreadLinkClicked(uri);
                }
            }
            
            private class BoardURLSpan extends URLSpan {
                public BoardURLSpan(String uri) {
                    super(uri);
                }
                
                @Override
                public void onClick(View widget) {
                    Uri uri = Uri.parse(getURL());
                    callback_.onBoardLinkClicked(uri);
                }
            }
            
            @Override
            protected Pattern getPattern() {
                return URL_PATTERN;
            }
            
            @Override
            public Object getSpan(String text, Object arg) {
                if (text.startsWith("ttp")) {
                    text = "h" + text;
                }
                if (ThreadData.isThreadUri(text)) {
                    return new ThreadURLSpan(text);
                }
                else if (BoardData.isBoardUri(text)) {
                    return new BoardURLSpan(text);
                }
                return new URLSpan(text);
            }
            
        }
        
        private class EntryAnchorFilter extends PatternFilter {
            
            private class EntryAnchorSpan extends ClickableSpan {
                int current_entry_id_;
                int entry_id_;
                
                public EntryAnchorSpan(long current_entry_id, String text) {
                    current_entry_id_ = new Long(current_entry_id).intValue();
                    try {
                        entry_id_ = Integer.parseInt(text.substring(text.lastIndexOf('>') + 1));
                    }
                    catch (NumberFormatException e) {
                        entry_id_ = 0;
                    }
                }
                
                @Override
                public void onClick(View widget) {
                    if (entry_id_ > 0) {
                        callback_.onNumberAnchorClicked(current_entry_id_, entry_id_);
                    }
                }
            }
            
            @Override
            protected Pattern getPattern() {
                return BODY_ANCHOR_PATTERN;
            }
            
            @Override
            public Object getSpan(String text, Object arg) {
                Long current_entry_id_ = (arg != null && arg instanceof Long) ? (Long) arg : 0;
                EntryAnchorSpan span = new EntryAnchorSpan(current_entry_id_, text);
                return span;
            }
        }
    }
    
    public boolean canAddNGID() {
        return author_id_.length() > 0 && author_id_.indexOf('?') == -1;
    }
}
