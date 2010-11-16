package com.t2.vas;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.t2.vas.db.DBAdapter;
import com.t2.vas.db.tables.Scale;
import com.t2.vas.db.tables.Scale.ResultValues;

public class GroupNotesSeriesDataAdapter extends AbsResultsSeriesDataAdapter {
	private static final String TAG = GroupNotesSeriesDataAdapter.class.getName();

	public GroupNotesSeriesDataAdapter(DBAdapter dbAdapter, long startTime, long endTime, int groupBy, String labelFormat) {
		super(dbAdapter, startTime, endTime, groupBy, labelFormat);
	}

	@Override
	protected Cursor getCursor(long startTime, long endTime, String db_date_format) {
		super.debugMode = 1;
		Cursor c = dbAdapter.getDatabase().query(
				"note r ",
				new String[]{
					"strftime('"+db_date_format+"', datetime(MIN(r.timestamp) / 1000, 'unixepoch')) label_value",
					"MIN(r.timestamp) timestamp",
					"100 value",
				},
				"timestamp >= ? AND timestamp < ?",
				new String[] {
						startTime+"",
						endTime+""
				},
				"strftime('"+db_date_format+"', datetime(r.timestamp / 1000, 'unixepoch'))",
				null,
				"label_value ASC",
				null
		);

		return c;
	}
}
