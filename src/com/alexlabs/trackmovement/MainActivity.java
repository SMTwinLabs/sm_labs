package com.alexlabs.trackmovement;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends ActionBarActivity {

	private RelativeLayout _content;
	private TextView _coordinatesTextView;
	private TextView _minutesTextView;
	private TextView _time;
	private View _dial;
	
	private MotionEvent _motionEvent;
	
	private final static int ARC_ID_KEY = 0;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {        
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);        
       
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_main);
        
        getSupportActionBar().hide();
        
        _content = (RelativeLayout) findViewById(R.id.content);
        _coordinatesTextView = (TextView) findViewById(R.id.coordinates);
        _minutesTextView = (TextView) findViewById(R.id.minutes);
        _dial = findViewById(R.id.dialView);

        _time = (TextView) findViewById(R.id.minutesView);
        
        final Clock clock = new Clock(_content, _dial);
        
        _content.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
		        int action = event.getActionMasked();
		        _motionEvent = event;
		        
		        switch(action) {
		            case MotionEvent.ACTION_DOWN: 
		                v.performClick();
		                break;
		            case MotionEvent.ACTION_MOVE:
		            	onActionMove(clock);
		                
		                break;
		            case MotionEvent.ACTION_UP:
		                break;
		            case MotionEvent.ACTION_CANCEL:		                
		                break;
		        }
		        return true;
			}
			
		});
        
        _content.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				onActionMove(clock);
			}
		});
    }

	private void renderArc(final Clock clock) {
		// Remove the current arc. This is done so that a new arc will be generated with the 
		// newly selected angle.
		_content.removeView(_content.findViewById(ARC_ID_KEY));
		
		// Create the new arc from the new angle that has been selected.
		Arc arc = new Arc(getBaseContext(), _dial, _content, (float)clock.getAngle());

		// Set the arc view's id.
		arc.setId(ARC_ID_KEY);
		
		// Add the arc to the content view of the clock.
		_content.addView(arc, 0);
	}

	private void displayDebugInformation(final Clock clock,
			String XCoord, String YCoord) {
		_coordinatesTextView.setText("X:" + XCoord + " Y: " + YCoord);
		_minutesTextView.setText("Minutes:" + clock.getMinute() + "  Degrees: " + (float)clock.getAngle());
		_time.setText(((Integer)clock.getMinute()).toString());
	}
	
	private void onActionMove(final Clock clock) {
		if(_motionEvent == null)
			throw new IllegalArgumentException("Motion event is null.");
    	
		float selectedPointX = _motionEvent.getX();
    	float selectedPointY = _motionEvent.getY();
		String XCoord = ((Float)selectedPointX).toString();	
		String YCoord = ((Float)selectedPointY).toString();

		clock.wind(_motionEvent);
		
		displayDebugInformation(clock, XCoord, YCoord);
		
		renderArc(clock);
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
