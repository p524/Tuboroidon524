package info.narazaki.android.tuboroid.activity;

import info.narazaki.android.tuboroid.R;
import info.narazaki.android.tuboroid.activity.base.TuboroidActivity;
import android.os.Bundle;
import android.webkit.WebView;

public class HelpActivity extends TuboroidActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.help_view);
        setTitle(getString(R.string.title_help, getString(R.string.app_name)));
        
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        WebView web_view = (WebView) findViewById(R.id.help_main_box);
        web_view.loadUrl(getString(R.string.const_help_url));
        
    }
    
}
