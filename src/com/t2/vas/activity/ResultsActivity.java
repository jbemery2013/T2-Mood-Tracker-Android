package com.t2.vas.activity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;

import com.t2.vas.Global;
import com.t2.vas.GroupResultsSeriesDataAdapter;
import com.t2.vas.NotesSeriesDataAdapter;
import com.t2.vas.R;
import com.t2.vas.ScaleKeyAdapter;
import com.t2.vas.ScaleResultsSeriesDataAdapter;
import com.t2.vas.db.DBAdapter;
import com.t2.vas.db.tables.Group;
import com.t2.vas.db.tables.Result;
import com.t2.vas.db.tables.Scale;
import com.t2.vas.db.tables.Scale.ResultValues;
import com.t2.vas.view.ChartLayout;
import com.t2.vas.view.chart.Chart;
import com.t2.vas.view.chart.Label;
import com.t2.vas.view.chart.LineSeries;
import com.t2.vas.view.chart.NotesSeries;
import com.t2.vas.view.chart.Series;
import com.t2.vas.view.chart.SeriesAdapterData;
import com.t2.vas.view.chart.Value;
import com.t2.vas.view.chart.Chart.ChartEventListener;
import com.t2.vas.view.chart.Series.SeriesDataAdapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.BounceInterpolator;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ViewSwitcher;
import android.widget.AdapterView.OnItemClickListener;

public class ResultsActivity extends BaseActivity implements OnItemClickListener, OnClickListener, ChartEventListener {
	private static final String TAG = "ResultsActivity";
	private static final int NOTES_MANAGE = 234;
	private static final int SHARE_RESULTS = 452;
	
	private LinkedHashMap<Long, ChartLayout> chartLayouts = new LinkedHashMap<Long, ChartLayout>();
	
	private long activeGroupId;
	private LayoutInflater layoutInflater;
	
	private ListView keyListView;
	private ScaleKeyAdapter keyListAdapter;
	
	private int resultsGroupBy = ScaleResultsSeriesDataAdapter.GROUPBY_DAY;
	private ChartLayout currentChartLayout;
	private FrameLayout chartsContainer;
	private Animation chartInAnimation;
	private AnimationSet flashAnimation;
	private ChartLayout groupChartLayout;
	private Group activeGroup;
	
	
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        this.setContentView(R.layout.results_activity);
        
        activeGroupId = this.getIntent().getLongExtra("group_id", -1);
        layoutInflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        keyListView = (ListView)this.findViewById(R.id.list);
        chartsContainer = (FrameLayout)this.findViewById(R.id.charts);
        chartInAnimation = AnimationUtils.loadAnimation(this, R.anim.push_bottom_in);
        
        // Build the animation that let the user know they already have selected
        // that chart.
        flashAnimation = new AnimationSet(true);
		AlphaAnimation alphaAnim;
		alphaAnim = new AlphaAnimation(1.0f, 0.5f);
		alphaAnim.setDuration(250);
		flashAnimation.addAnimation(alphaAnim);
		alphaAnim = new AlphaAnimation(0.5f, 1.0f);
		alphaAnim.setDuration(250);
		alphaAnim.setStartOffset(250);
		flashAnimation.addAnimation(alphaAnim);
        
		
        DBAdapter db = new DBAdapter(this, Global.Database.name, Global.Database.version);
        db.open();
        
        // ensure the group provided exists.
        activeGroup = (Group)db.getTable("group").newInstance();
        activeGroup._id = activeGroupId;
        if(!activeGroup.load()) {
        	this.finish();
        	return;
        }
        
        
        // Create the chart for each scale.
        ArrayList<Scale> scales = activeGroup.getScales();
        int scaleCount = scales.size();
        for(int i = 0; i < scaleCount ; i++) {
        	Scale s = scales.get(i);
        	LineSeries lineSeries = new LineSeries(s.max_label +" - "+ s.min_label);
        	
        	lineSeries.setSeriesDataAdapter(new ScaleResultsSeriesDataAdapter(
        			db,
        			s._id,
        			resultsGroupBy,
        			Global.CHART_LABEL_DATE_FORMAT
        	));
        	
        	ChartLayout chartLayout = (ChartLayout)layoutInflater.inflate(R.layout.chart_layout, null);
        	chartLayout.setYMaxLabel(s.max_label);
        	chartLayout.setYMinLabel(s.min_label);
        	chartLayout.setTag((Long)s._id);
        	
        	// Style the colors of the lines and points.
        	setLineSeriesColor(lineSeries, i+1, scaleCount+1);
        	
        	// Add the series and add the chart to the list of charts.
        	chartLayout.getChart().setChartEventListener(this);
        	chartLayout.getChart().addSeries("main", lineSeries);
        	chartLayouts.put(s._id, chartLayout);
        }
        
        
        // Create the chart for the group in general
        NotesSeries notesSeries = new NotesSeries("Notes");
        notesSeries.setSeriesDataAdapter(new GroupResultsSeriesDataAdapter(
        		db,
        		activeGroup._id,
        		resultsGroupBy,
        		Global.CHART_LABEL_DATE_FORMAT
        ));
        LineSeries lineSeries = new LineSeries("");
        lineSeries.setSeriesDataAdapter(new GroupResultsSeriesDataAdapter(
        		db,
        		activeGroup._id,
        		resultsGroupBy,
        		Global.CHART_LABEL_DATE_FORMAT
        ));
        setLineSeriesColor(lineSeries, 0, scaleCount+1);
        groupChartLayout = (ChartLayout)this.findViewById(R.id.groupChart);
        groupChartLayout.setYMaxLabel(activeGroup.title);
        groupChartLayout.getChart().setChartEventListener(this);
        groupChartLayout.getChart().addSeries("notes", notesSeries);
        groupChartLayout.getChart().addSeries("main", lineSeries);
        this.currentChartLayout = groupChartLayout;
        
        
        // Add the charts to the key list
        ArrayList<ChartLayout> chartLayoutsList = new ArrayList<ChartLayout>();
        chartLayoutsList.addAll(chartLayouts.values());
        
        keyListAdapter = new ScaleKeyAdapter(this, R.layout.key_box_adapter_list_label_right, chartLayoutsList);
        keyListView.setAdapter(keyListAdapter);
        keyListView.setOnItemClickListener(this);
        
        this.findViewById(R.id.notesButton).setOnClickListener(this);
        this.findViewById(R.id.addNoteButton).setOnClickListener(this);
        
        db.close();
        
        // Restore some of the data
        if(savedInstanceState != null) {
        	// Remember which charts are visible
        	long[] chartScaleIds = savedInstanceState.getLongArray("chartScaleIds");
        	for(int i = 0; i < chartScaleIds.length; i++) {
        		ChartLayout tmpChartLayout = this.chartLayouts.get(chartScaleIds[i]);
        		if(tmpChartLayout != null) {
        			this.showChart(tmpChartLayout, false);
        		}
        	}
        	
        	// Remember the scroll position
	        int listFirstVisible = savedInstanceState.getInt("listFirstVisible");
	        keyListView.setSelection(listFirstVisible);
        }
	}
	
	private void setLineSeriesColor(LineSeries lineSeries, int currentIndex, int totalCount) {
		// Style the colors of the lines and points.
    	float hue = currentIndex / (1.00f * totalCount) * 360.00f;
    	lineSeries.setFillColor(Color.HSVToColor(
    			255, 
    			new float[]{
    				hue,
    				1.0f,
    				1.0f
    			}
    	));
    	lineSeries.setStrokeColor(Color.HSVToColor(
    			255, 
    			new float[]{
    				hue,
    				1.0f,
    				0.25f
    			}
    	));
    	
    	lineSeries.setLineFillColor(Color.HSVToColor(
    			255, 
    			new float[]{
    				hue,
    				0.65f,
    				1.0f
    			}
    	));
    	lineSeries.setLineStrokeColor(Color.HSVToColor(
    			255, 
    			new float[]{
    				hue,
    				0.5f,
    				0.5f
    			}
    	));
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// Determine which charts are visible.
		ArrayList<Long> list = new ArrayList<Long>();
		for(int i = 0; i < this.chartsContainer.getChildCount(); i++) {
			View childView = this.chartsContainer.getChildAt(i);
			try {
				ChartLayout childChart = (ChartLayout)childView;
				long scaleId = (Long)childChart.getTag();
				list.add(scaleId);
			} catch (ClassCastException cce) {}
		}
		
		long[] scaleIds = new long[list.size()];
		for(int i = 0; i < scaleIds.length; i++) {
			scaleIds[i] = list.get(i);
		}
		outState.putLongArray("chartScaleIds", scaleIds);
		
		
		//store the scoll position of the key list
		outState.putInt("listFirstVisible", keyListView.getFirstVisiblePosition());
		
		
		super.onSaveInstanceState(outState);
	}
	
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		// Refresh current chart to show notes.
		if(requestCode == NOTES_MANAGE) {
			this.groupChartLayout.getChart().updateChart();
			this.groupChartLayout.invalidate();
		}
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		this.showChart((ChartLayout)arg1.getTag(), true);
	}
	
	public void showChart(ChartLayout chartLayout, boolean animate) {
		int chartBackgroundCount = 1;
		
		// Flash the chart if it already selected
		if(currentChartLayout != null && currentChartLayout.equals(chartLayout)) {
			if(animate) {
				currentChartLayout.startAnimation(flashAnimation);
			}
			return;
		}
		
		currentChartLayout = chartLayout;
		currentChartLayout.setShowLabels(true);
		currentChartLayout.setLabelsColor(Color.WHITE);
		currentChartLayout.getChart().setShowYHilight(true);
		
		// Have the parent remove the child view. This prevents an issue in the next code block.
		ViewGroup parent = (ViewGroup)currentChartLayout.getParent();
		if(parent != null) {
			parent.removeView(currentChartLayout);
		}
		
		// Remove any child views that are in excess
		this.chartsContainer.addView(currentChartLayout);
		int removeCount = this.chartsContainer.getChildCount() - chartBackgroundCount - 1;
		for(int i = 0; i < removeCount; i++) {
			this.chartsContainer.removeViewAt(0);
		}
		
		
		// Fade the group chart down
		AlphaAnimation aa = new AlphaAnimation(1.0f, 0.5f);
		aa.setFillBefore(true);
		aa.setFillAfter(true);
		groupChartLayout.startAnimation(aa);
		groupChartLayout.setShowLabels(false);
		groupChartLayout.getChart().setDropShadowEnabled(false);
		groupChartLayout.getChart().setShowYHilight(false);
		
		
		// Progressivly fade out each view behind the current one
		int childCount = chartsContainer.getChildCount();
		float startAlpha = 0.50f;
		float endAlpha = 0.25f;
		float alphaStep = (startAlpha - endAlpha) / chartBackgroundCount;
		for(int i = childCount-2; i >= 0; i--) {
			int multiplier = childCount - i - 1;
			float alphaEnd = 1 - (alphaStep * multiplier) - startAlpha;
			float alphaStart = alphaEnd + alphaStep;
			View childView = chartsContainer.getChildAt(i);
			
			AlphaAnimation alphaAnimation = new AlphaAnimation(
				alphaStart,
				alphaEnd
			);
			if(animate) {
				alphaAnimation.setDuration(1000);
			}
			alphaAnimation.setFillAfter(true);
			
			chartsContainer.getChildAt(i).startAnimation(alphaAnimation);
			
			// If the child view is a chartlayout, remove some of the extra drawables.
			try {
				ChartLayout childChart = (ChartLayout)childView;
				childChart.setShowLabels(false);
				childChart.getChart().setDropShadowEnabled(false);
				childChart.getChart().setShowYHilight(false);
			} catch (ClassCastException cce) {}
		}
		
		// Slide the new chart in.
		if(animate) {
			this.chartsContainer.getChildAt(chartsContainer.getChildCount()-1).startAnimation(chartInAnimation);
		}
		
		// Since a chart is now selected, show the notes buttons.
		this.findViewById(R.id.addNoteButton).setVisibility(View.VISIBLE);
		this.findViewById(R.id.notesButton).setVisibility(View.VISIBLE);
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
			case R.id.notesButton:
				this.viewNotesPressed();
				break;
				
			case R.id.addNoteButton:
				this.addNotesPressed();
				break;
		}
	}

	private long[] getActiveChartTimeRange() {
		ChartLayout chartLayout = this.currentChartLayout;
		
		if(chartLayout == null) {
			return null;
		}
		
		Calendar cal = Calendar.getInstance();
		Date tmpDate;
		long startTimestamp = -1;
		long endTimestamp = -1;
		
		int seriesIndex = chartLayout.getChart().getYHilightSeriesIndex();
		if(seriesIndex >= 0) {
			tmpDate = (Date)chartLayout.getChart().getSeriesAt(0).getLabels().get(seriesIndex).getLabelValue();
			cal.setTime(tmpDate);
			startTimestamp = cal.getTimeInMillis();
			
			if(seriesIndex+1 < chartLayout.getChart().getSeriesAt(0).getLabels().size()) {
				tmpDate = (Date)chartLayout.getChart().getSeriesAt(0).getLabels().get(seriesIndex+1).getLabelValue();
				cal.setTime(tmpDate);
				endTimestamp = cal.getTimeInMillis();
			}
		}
		
		return new long[]{
			startTimestamp,
			endTimestamp
		};
	}
	
	private void addNotesPressed() {
		long[] range = getActiveChartTimeRange();
		Intent i = new Intent(this, NoteActivity.class);
		i.putExtra("timestamp", range[0]);
		
		this.startActivityForResult(i, NOTES_MANAGE);
	}
	
	private void viewNotesPressed() {
		long[] range = getActiveChartTimeRange();
		Intent i = new Intent(this, NotesDialogActivity.class);
		i.putExtra("start_timestamp", range[0]);
		i.putExtra("end_timestamp", range[1]);
		
		this.startActivityForResult(i, NOTES_MANAGE);
	}
	
	@Override
	public boolean onChartClick(Chart c, MotionEvent event) {
		return false;
	}

	@Override
	public boolean onChartLongClick(Chart c, MotionEvent event) {
		this.addNotesPressed();
		return false;
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		MenuItem item = menu.add(Menu.NONE, SHARE_RESULTS, 0, R.string.activity_results_share_menu);
		item.setIcon(android.R.drawable.ic_menu_share);
		
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
			case SHARE_RESULTS:
				shareResultsClicked();
				return true;
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	
	
	private void shareResultsClicked() {
		Uri outFileUri = Uri.parse("file://"+ Environment.getExternalStorageDirectory() +"/vasdata.csv");
		File outFile = new File(outFileUri.getPath());
		
		// Remove the output file before we begin writing.
		if(outFile.exists()) {
			outFile.delete();
		}
		
		// Write the file
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(outFile));
			
			int rowCount = 0;
			for(long id: this.chartLayouts.keySet()) {
				ChartLayout cl = this.chartLayouts.get(id);
				Series series = cl.getChart().getSeries("main");
				SeriesAdapterData sad = series.getSeriesDataAdapter().getData();
				ArrayList<Label> labels = sad.getLabels();
				ArrayList<Value> values = sad.getValues();
				
				// Write the header
				if(rowCount == 0) {
					bw.write(",");
					for(int j = 0; j < labels.size(); j++) {
						bw.write(labels.get(j).getLabelString()+",");
					}
					bw.write("\n");
				}
				
				for(int j = 0; j < labels.size(); j++) {
					// Write the left header for this row
					if(j == 0) {
						bw.write(series.getName()+",");
					}
					
					// Write the value
					bw.write(values.get(j).getValue()+",");
				}
				
				bw.write("\n");
				rowCount++;
			}
			
			bw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		// Start the new email intent
		Intent i = new Intent(Intent.ACTION_SEND);
		i.putExtra(
				Intent.EXTRA_SUBJECT, 
				this.getString(
						R.string.activity_results_share_subject).replace("{0}", this.activeGroup.title)
				);
		i.putExtra(
				Intent.EXTRA_STREAM, 
				outFileUri
		);
		i.setType("text/csv");
		//i.setType("message/rfc882"); 
		
		this.startActivity(Intent.createChooser(i, "Send to someone"));
	}
}
