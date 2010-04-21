package com.t2.vas.activity;

import com.t2.vas.R;
import com.t2.vas.activity.editor.GroupListActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class BaseActivity extends Activity {
	private static final String TAG = BaseActivity.class.getName();
	public static final int GROUP_EDITOR = 4325;
	
	public void onCreate(Bundle savedInstanceState) {
		//this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		//this.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
		super.onCreate(savedInstanceState);
	}
	
	/*@Override
	protected void onPause() {
		this.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
		super.onPause();
	}*/

	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		
		if(this.getHelp() == null) {
			menu.removeItem(R.id.help);
		}
		
		return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent i;
		switch(item.getItemId()){
			case R.id.settings:
				return true;
				
			case R.id.groupEditor:
				i = new Intent(this, GroupListActivity.class);
				this.startActivityForResult(i, GROUP_EDITOR);
				return true;
				
			case R.id.help:
				i = new Intent(this, HelpActivity.class);
				i.putExtra("message", this.getHelp());
				this.startActivity(i);
				return true;
		}
		
		return super.onContextItemSelected(item);
	}
	
	public String getHelp() {
		return null;
	}
}
