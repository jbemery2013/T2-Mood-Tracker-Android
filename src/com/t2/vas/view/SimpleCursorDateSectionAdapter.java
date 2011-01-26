package com.t2.vas.view;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import com.t2.vas.R;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.widget.SectionIndexer;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class SimpleCursorDateSectionAdapter extends SimpleCursorAdapter implements
		SectionIndexer {
	
	private SimpleDateFormat dateFormatter;
	private String[] sections;
	private HashMap<Integer, Integer> sectionToCursorPosMap = new HashMap<Integer,Integer>();
	private HashMap<Integer, Integer> sectionToCursorPosMapRev = new HashMap<Integer,Integer>();
	private ArrayList<Long> sectionTimesList = new ArrayList<Long>();
	
	public SimpleCursorDateSectionAdapter(
			Context context, int layout, Cursor c,
			String[] from, int[] to, 
			int timestampFieldId, SimpleDateFormat dateFormater) {
		super(context, layout, c, from, to);

		this.dateFormatter = dateFormater;
		
		
		int timestampColumnIndex = -1;		
		for(int i = 0; i < to.length; ++i) {
			if(to[i] == timestampFieldId) {
				timestampColumnIndex = c.getColumnIndex(from[i]);
				break;
			}
		}
		
		ArrayList<String> sectionsList = new ArrayList<String>();
		int pos = c.getPosition();
		while(c.moveToNext()) {
			long timestamp = c.getLong(timestampColumnIndex);
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(timestamp);
			String sectionStr = dateFormatter.format(cal.getTime());
			
			if(sectionToCursorPosMap.get(sectionStr) == null) {
				sectionsList.add(sectionStr);
				sectionTimesList.add(cal.getTimeInMillis());
				sectionToCursorPosMap.put(sectionToCursorPosMap.size(), c.getPosition());
				sectionToCursorPosMapRev.put(c.getPosition(), sectionToCursorPosMap.size());
			}
		}
		c.moveToPosition(pos);
		
		this.sections = sectionsList.toArray(new String[sectionsList.size()]);
		
	}

	@Override
	public int getPositionForSection(int section) {
		return sectionToCursorPosMap.get(section);
	}

	@Override
	public int getSectionForPosition(int position) {
		return sectionToCursorPosMapRev.get(position);
	}
	
	public int getPositionForTimestamp(long timestamp) {
		for(int i = 0; i < sectionTimesList.size(); ++i) {
			if(sectionTimesList.get(i) < timestamp) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public Object[] getSections() {
		return sections;
	}
	
	public static SimpleCursorDateSectionAdapter buildNotesAdapter(
			final Context context, 
			Cursor notesCursor, 
			final SimpleDateFormat dateFormatter,
			final SimpleDateFormat sectionDateFormatter
			) {
		
		SimpleCursorDateSectionAdapter notesAdapter = new SimpleCursorDateSectionAdapter(
				context, 
				R.layout.list_item_2,
				notesCursor,
				new String[] {
    				"note",
        			"timestamp"
        		},
        		new int[] {
        			R.id.text1,
        			R.id.text2
        		},
        		R.id.text2,
        		sectionDateFormatter
		);
		notesAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
			@Override
			public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
				if(view.getId() == R.id.text2) {
					long timestamp = cursor.getLong(columnIndex);
					if(timestamp > 0) {
						((TextView)view).setText(
								dateFormatter.format(new Date(timestamp))
						);
					} else {
						((TextView)view).setText(
								context.getString(R.string.never)
						);
					}
					
					return true;
				}
				return false;
			}
		});
		
		return notesAdapter;
	}
}