package com.alexlabs.trackmovement;

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
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends ActionBarActivity {

	private RelativeLayout _content;
	private TextView _coordinates;
	private TextView _minutes;
	
	private final static int ARC_ID_KEY = 0;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        _content = (RelativeLayout) findViewById(R.id.content);
        _coordinates = (TextView) findViewById(R.id.coordinates);
        _minutes = (TextView) findViewById(R.id.minutes);
        
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
		            	float x, y;
		            	String XCoord = null, YCoord = null;
		            	
		            	x = event.getX();
		            	y =  event.getY();
		            	
		            	if(x < 0 || x > _content.getHeight() || y < 0 || y > _content.getWidth()) {
		            		XCoord = "Out of bounds";
		            		YCoord = "Out of bounds";
		            	} else {
		            		XCoord = "" + x;	
		            		YCoord = "" + y;
		            	}
		            	
		            	clock.wind(_content.getHeight(), x, y);
		                _coordinates.setText("X:" + XCoord + " Y: " + YCoord);
		                _minutes.setText("Minutes:" + clock.getMinute() + "  Degrees: " + (float)clock.getAngle());
		                
		                // Remove the current arc. This is done so that a new arc will be generated with the 
		                // newly selected angle.
		                _content.removeView(_content.findViewById(ARC_ID_KEY));
		                
		                // Create the new arc from the new angle that has been selected.
		                Arc arc = new Arc(getBaseContext(), (float)_content.getHeight(),  (float)_content.getWidth(), 
		                		(float)(clock.getMinute()*(Clock.DEGRESS_IN_CIRCLE/Clock.MAX_MINUTES_ON_CLOCK)));

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
    
    // -------------- ARC TEST --------------
    
    public class Arc extends View {
    	private float _height;
    	private float _width;
    	private float _angle;
    	private Paint _paint;
		private RectF _oval;
    	
		public Arc(Context context) {
			super(context);
			_height = 0.0f;
			_width = 0.0f;
			_angle = 0.0f;
			_paint = new Paint();
			_oval = new RectF();
		}
		
		public Arc(Context context, float height, float width, float angle) {
			this(context);
			_height = height;
			_width = width;
			_angle = angle;
		}
		
		@Override
		protected void onDraw(Canvas canvas) {
			super.onDraw(canvas);
			// set the radius for the circle
			float radius = _width/2;
			_paint.setColor(Color.RED);
			_paint.setStrokeWidth(5);	

			// the paint object will fill its inside area
			_paint.setStyle(Paint.Style.FILL);
			
			// calculate the center of the arc
			float center_x, center_y;
			center_x = _width/2;
			center_y = _height/2;

			// set coordinates to the specified values
			_oval.set(center_x - radius, 
					center_y - radius, 
					center_x + radius, 
					center_y + radius);

			// draw the arc
			canvas.drawArc(_oval, 270, _angle, true, _paint);
		}		   	
    }
}
