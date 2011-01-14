package info.narazaki.android.tuboroid.view;

import info.narazaki.android.tuboroid.R;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ZoomButton;
import android.widget.ZoomControls;

public class ImageViewerFooter extends RelativeLayout {

	public ImageViewerFooter(Context context, AttributeSet attrs) {
		super(context, attrs);

	}

	private TextView text_view;
	private ZoomControls zoom;
	private String image_local_file_;
	private String image_uri_;
	private float scale;
	private int LINE_COUNT = 3;
	private float ZOOM_CONTROL_WIDTH_RATIO = 0.33f;
	private ScrollImageView image_view_;

	public void init(String image_local_file, String image_uri,
			ScrollImageView image_view, int width) {
		image_local_file_ = image_local_file;
		image_uri_ = image_uri;
		image_view_ = image_view;
		
		text_view = new TextView(getContext());
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
		zoom.setLayoutParams(zoom_layout_params);

		for(int i = 0; i < 2; i++){
			((ZoomButton) zoom.getChildAt(i)).getLayoutParams().width = zoom_controls_width / 2;
		}
		
		RelativeLayout.LayoutParams text_layout_params = new RelativeLayout.LayoutParams(
				width, text_view.getLineHeight() * LINE_COUNT);
		text_layout_params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		text_layout_params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		text_view.setLayoutParams(text_layout_params);
		
		text_view.setBackgroundColor(Color.argb(0x70, 0x00, 0x00, 0x00));
		text_view.setTextColor(Color.argb(0xff, 0xff, 0xff, 0xff));
		
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

		addView(text_view);
		addView(zoom);

	}

	public void setScale(float scale) {
		this.scale = scale;
		setTextViewText();
	}

	private void setTextViewText() {
		StringBuilder string_builder = new StringBuilder();
		string_builder.append(image_uri_);
		string_builder.append("\n");
		string_builder.append(image_local_file_);
		string_builder.append("\n");
		if(scale <= 0.1){
			string_builder.append(String.format("%1.2f", scale * 100));
		}else{
			string_builder.append((int) (scale * 100));
		}
		string_builder.append("%\n");
		text_view.setText(string_builder.toString());
	}
}
