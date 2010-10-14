package info.narazaki.android.tuboroid.activity;

import info.narazaki.android.lib.system.MigrationSDK5;
import info.narazaki.android.tuboroid.activity.base.TuboroidActivity;
import info.narazaki.android.tuboroid.data.ThreadData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class IntentReceiverActivity extends TuboroidActivity {
    private Uri uri_;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        uri_ = getIntent().getData();
        if (uri_ == null) {
            if (getIntent().hasExtra(Intent.EXTRA_TEXT)) {
                uri_ = Uri.parse(getIntent().getStringExtra(Intent.EXTRA_TEXT));
            }
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        if (uri_ == null) {
            finish();
            return;
        }
        forwardView();
        finish();
    }
    
    private boolean forwardView() {
        if (!is_active_) return false;
        
        if (ThreadData.isThreadUri(uri_.toString())) {
            Intent intent = new Intent(this, ThreadEntryListActivity.class);
            intent.setData(uri_);
            MigrationSDK5.Intent_addFlagNoAnimation(intent);
            startActivity(intent);
            return true;
        }
        Intent intent = new Intent(this, ThreadListActivity.class);
        intent.setData(uri_);
        MigrationSDK5.Intent_addFlagNoAnimation(intent);
        startActivity(intent);
        return true;
    }
}
