package info.narazaki.android.tuboroid.view;

import info.narazaki.android.tuboroid.R;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ZoomButton;
import android.widget.ZoomControls;

public class ImageViewerFooter extends RelativeLayout {

	public ImageViewerFooter(Context context, AttributeSet attrs) {
		super(context, attrs);
	
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b){
		// TODO Auto-generated method stub
		super.onLayout(changed, l, t, r, b);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
		// TODO Auto-generated method stub
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		if(width == 0) {
			width = getMeasuredWidth();
			
			image_path_text_view = new TextView(getContext());
			text_view = new TextView(getContext());
			error_text_view = new TextView(getContext());
			zoom = new ZoomControls(getContext());

			image_view_.setFooter(this);

			int btn_zoom_down_id = getResources().getIdentifier("btn_zoom_down",
					"drawable", "android");
			Drawable btn_zoom_down_drawable = (Drawable) getResources()
					.getDrawable(btn_zoom_down_id);

			// ズームボタンを右のほうに設置
			int zoom_controls_width = (int) (width * ZOOM_CONTROL_WIDTH_RATIO);
			RelativeLayout.LayoutParams zoom_layout_params = new RelativeLayout.LayoutParams(
					zoom_controls_width,
					btn_zoom_down_drawable.getIntrinsicHeight());
			zoom_layout_params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
			zoom_layout_params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
			zoom.setId(1);
			zoom.setLayoutParams(zoom_layout_params);

			for(int i = 0; i < 2; i++){
				((ZoomButton) zoom.getChildAt(i)).getLayoutParams().width = zoom_controls_width / 2;
			}
			
			//画像の情報を表示するTextViewを左に配置
			
			RelativeLayout.LayoutParams text_layout_params = new RelativeLayout.LayoutParams(
					width / 3 * 2, text_view.getLineHeight() * LINE_COUNT);
			text_layout_params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
			text_layout_params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
			text_view.setLayoutParams(text_layout_params);
			text_view.setId(3);
			
			text_view.setBackgroundColor(Color.argb(0x70, 0x00, 0x00, 0x00));
			text_view.setTextColor(Color.argb(0xff, 0xff, 0xff, 0xff));
			
			RelativeLayout.LayoutParams image_path_text_layout_params = new RelativeLayout.LayoutParams(
					width / 3 * 2, LayoutParams.WRAP_CONTENT);
			image_path_text_layout_params.addRule(RelativeLayout.ABOVE, 3);
			image_path_text_view.setLayoutParams(image_path_text_layout_params);
			image_path_text_view.setId(2);
			
			image_path_text_view.setEllipsize(TruncateAt.MARQUEE);
			image_path_text_view.setMarqueeRepeatLimit(-1);
			image_path_text_view.setSingleLine();
			image_path_text_view.setFocusable(true);
			image_path_text_view.setFocusableInTouchMode(true);
			
			image_path_text_view.setBackgroundColor(Color.argb(0x70, 0x00, 0x00, 0x00));
			image_path_text_view.setTextColor(Color.argb(0xff, 0xff, 0xff, 0xff));
			
			//エラーメッセージ表示用TextView
			RelativeLayout.LayoutParams error_text_layout_params = new RelativeLayout.LayoutParams(
					width, error_text_view.getLineHeight() * LINE_COUNT);
			error_text_layout_params.addRule(RelativeLayout.CENTER_IN_PARENT);
			
			error_text_view.setLayoutParams(error_text_layout_params);
			error_text_view.setVisibility(View.INVISIBLE);
			
			error_text_view.setTextSize(13);
			
			//text_view.setHorizontallyScrolling(true);
			
			//setBackgroundColor(Color.argb(0x70, 0xff, 0x00, 0x00));
			
			zoom.setOnZoomInClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					image_view_.onZoomIn();
				}
			});
	        zoom.setOnZoomOutClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					image_view_.onZoomOut();
				}
			});

			setTextViewText();

			removeAllViews();
			addView(image_path_text_view);
			addView(text_view);
			addView(zoom);
			addView(error_text_view);
		}
	}

	private int width = 0;
	private TextView image_path_text_view;
	private TextView text_view;
	private TextView error_text_view;
	private ZoomControls zoom;
	private String image_local_file_;
	private String image_uri_;
	private long entry_id_;
	private int image_index_;
	private int image_count_;
	
	private float scale;
	private int LINE_COUNT = 2;
	private float ZOOM_CONTROL_WIDTH_RATIO = 0.33f;
	private ScrollImageView image_view_;

	public void setImageInfo(String image_local_file, String image_uri,
			ScrollImageView image_view, long entry_id, int image_index, int image_count, int width) {
		image_local_file_ = image_local_file;
		image_uri_ = image_uri;
		image_view_ = image_view;
		entry_id_ = entry_id;
		image_index_ = image_index;
		image_count_ = image_count;

		setErrorMessage(null);
		setTextViewText();
	}

	public void setScale(float scale) {
		this.scale = scale;
		setTextViewText();
	}

	private void setTextViewText() {
		if(image_path_text_view == null) {
			return;
		}
		
		if(image_uri_ != null) {
			image_path_text_view.setText(image_uri_);
		}
		/*if(image_uri_.length() > 23){
			string_builder.append("...");
			string_builder.append(image_uri_.substring(image_uri_.length() - 20, image_uri_.length()));
		}else{
			string_builder.append(image_uri_);
		}
		string_builder.append("\n");*/
		
		StringBuilder string_builder = new StringBuilder();
		string_builder.append(entry_id_);
		string_builder.append(" ");
		string_builder.append(image_index_);
		string_builder.append("/");
		string_builder.append(image_count_);
		string_builder.append("\n");
		if(scale <= 0.1){
			string_builder.append(String.format("%1.2f", scale * 100));
		}else{
			string_builder.append((int) (scale * 100));
		}
		string_builder.append("%\n");
		text_view.setText(string_builder.toString());
	}
	
	public void setErrorMessage(String s) {
		if(error_text_view == null) {
			return;
		}
		if(s == null) {
			error_text_view.setVisibility(View.INVISIBLE);
		}else {
			image_uri_ = null;
			error_text_view.setText(s);
			error_text_view.setVisibility(View.VISIBLE);
		}
	}
	
	public boolean hasErrorMessage() {
		Log.v("test", "error_text_view.getVisibility() = " + error_text_view.getVisibility() + "  visi=" + View.VISIBLE + " inv=" + View.INVISIBLE);
		return error_text_view.getVisibility() == View.VISIBLE; 
	}
}
