package com.t2.vas.activity.editor;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.t2.vas.Global;
import com.t2.vas.R;
import com.t2.vas.VASAnalytics;
import com.t2.vas.activity.ABSActivity;
import com.t2.vas.db.DBAdapter;
import com.t2.vas.db.tables.Group;

public class ClearDataActivity extends ABSActivity {

	private long groupId;
	private Group group;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		VASAnalytics.onEvent(VASAnalytics.EVENT_CLEAR_DATA_ACTIVITY);

		this.groupId = this.getIntent().getLongExtra("group_id", 0);
		if(this.groupId <= 0) {
			this.finish();
		}

		this.group = ((Group)dbAdapter.getTable("group")).newInstance();
        this.group._id = this.groupId;

        if(!this.group.load()) {
        	this.finish();
        }


		this.setContentView(R.layout.clear_data_activity);

		this.findViewById(R.id.yesButton).setOnClickListener(this);
		this.findViewById(R.id.noButton).setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.yesButton:
			this.group.clearResults();

			Toast t = Toast.makeText(this, R.string.clear_data_cleared_text, Toast.LENGTH_LONG);
			t.show();
			this.finish();
			return;

		case R.id.noButton:
			this.finish();
			return;
		}
		super.onClick(v);
	}
}