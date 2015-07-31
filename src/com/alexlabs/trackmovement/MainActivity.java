package com.alexlabs.trackmovement;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
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
	private TextView _coordinates;
	private TextView _minutes;
	private TextView _time;
	private View _dial;
	
	private final static int ARC_ID_KEY = 0;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {        
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);        
       
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_main);
        
        getSupportActionBar().hide();
        
        _content = (RelativeLayout) findViewById(R.id.content);
        _coordinates = (TextView) findViewById(R.id.coordinates);
        _minutes = (TextView) findViewById(R.id.minutes);
        _dial = findViewById(R.id.dialView);

        _time = (TextView) findViewById(R.id.minutesView);
        
        final Clock clock = new Clock();
        
        _content.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				int index = event.getActionIndex();
		        int action = event.getActionMasked();
		        int pointerId = event.getPointerId(index);
		        
		        switch(action) {
		            case MotionEvent.ACTION_DOWN:
		                break;
		            case MotionEvent.ACTION_MOVE:
		            	float x, y, diameter;
		            	String XCoord = null, YCoord = null;
		            	
		            	x = event.getX();
		            	y =  event.getY();
		            	diameter = (float)_dial.getHeight();
		            	
		            	if(x < 0 || x > diameter || y < 0 || y > diameter) {
		            		XCoord = "Out of bounds";
		            		YCoord = "Out of bounds";
		            	} else {
		            		XCoord = "" + x;	
		            		YCoord = "" + y;
		            	}
		            	
		            	clock.wind(_content.getHeight(), x, y);
		                _coordinates.setText("X:" + XCoord + " Y: " + YCoord);
		                _minutes.setText("Minutes:" + clock.getMinute() + "  Degrees: " + (float)clock.getAngle());
		                _time.setText(((Integer)clock.getMinute()).toString());
		                
		                // Remove the current arc. This is done so that a new arc will be generated with the 
		                // newly selected angle.
		                _content.removeView(_content.findViewById(ARC_ID_KEY));
		                
		                // Create the new arc from the new angle that has been selected.
		                Arc arc = new Arc(getBaseContext(), diameter,
		                		(float)(clock.getMinute()*(Clock.DEGRESS_IN_CIRCLE/Clock.MAX_MINUTES_ON_CLOCK)),
		                		_content);

		                // Set the arc view's id.
		                arc.setId(ARC_ID_KEY);
		                
		                // Add the arc to the content view of the clock.
		                _content.addView(arc, 0);
		                
		                break;
		            case MotionEvent.ACTION_UP:
		                v.performClick();
		                break;
		            case MotionEvent.ACTION_CANCEL:		                
		                break;
		        }
		        return true;
			}
		});
        
        Button button = (Button) findViewById(R.id.button);
        button.setVisibility(View.GONE);
        button.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Toast.makeText(getBaseContext(), "Button pressed.", Toast.LENGTH_SHORT).show();
			}
		});
        
        
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
