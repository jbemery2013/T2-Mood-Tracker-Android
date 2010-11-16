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

public class ScaleResultsSeriesDataAdapter extends AbsResultsSeriesDataAdapter {
	private static final String TAG = ScaleResultsSeriesDataAdapter.class.getName();

	protected long scaleId;

	public ScaleResultsSeriesDataAdapter(DBAdapter dbAdapter, long startTime, long endTime, long scaleId, int groupBy, String labelFormat) {
		super(dbAdapter, startTime, endTime, groupBy, labelFormat);
		this.scaleId = scaleId;
	}

	@Override
	protected Cursor getCursor(long startTime, long endTime, String db_date_format) {
		Cursor c = dbAdapter.getDatabase().query(
				"result r ",
				new String[]{
					"strftime('"+db_date_format+"', datetime(r.timestamp / 1000, 'unixepoch', 'localtime')) label_value",
					"MIN(r.timestamp) timestamp",
					"AVG(r.value) value",
				},
				"scale_id=? AND timestamp >= ? AND timestamp < ?",
				new String[]{
					this.scaleId+"",
					startTime+"",
					endTime+"",
				},
				"strftime('"+db_date_format+"', datetime(r.timestamp / 1000, 'unixepoch', 'localtime'))",
				null,
				"label_value ASC",
				null
		);

		return c;
	}
}
