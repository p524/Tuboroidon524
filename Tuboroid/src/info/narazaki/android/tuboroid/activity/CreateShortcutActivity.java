package info.narazaki.android.tuboroid.activity;

import info.narazaki.android.tuboroid.R;

import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class CreateShortcutActivity extends ListActivity {
	private int menus [] = new int [] { R.string.title_favorite, R.string.title_recents };
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.create_shortcut_dialog);

		ArrayList<String> values = new ArrayList<String>();
		for (int j=0; j<menus.length; j++) {
			values.add(getString(menus[j]));
		}
		
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, 
				android.R.layout.simple_list_item_1, values);
		setListAdapter(adapter);
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		createShortcut(menus[position]);
		finish();
	}
	
	private void createShortcut(int id) {
		
        Intent shortcutIntent = new Intent(Intent.ACTION_MAIN);
        
        switch (id) {
        case R.string.title_favorite:
            shortcutIntent.setClassName(this, FavoriteListActivity.class.getName());
        	break;
        case R.string.title_recents:
    	default:
            shortcutIntent.setClassName(this, RecentListActivity.class.getName());
        	break;	
        }
        
        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(id));
        Parcelable iconResource = Intent.ShortcutIconResource.fromContext(
                this,  R.drawable.icon);
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);
		setResult(RESULT_OK, intent);
	}
}
