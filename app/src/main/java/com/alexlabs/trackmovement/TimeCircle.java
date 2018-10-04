package com.alexlabs.trackmovement;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.view.View;

public class TimeCircle extends View {
	private View _content;
	private Context _context;
	private int _alpha;
	
	private int _colorResId;
	private Paint _paint;
	
	// The angle corresponding to the selected minute. 
	private float _angle;
	
	
	public TimeCircle(Context context) {
		super(context);
		_context = context;
		_paint = new Paint();
	}
	
	public TimeCircle(Context context, View parentView, float angle, int colorResId, int alpha) {
		this(context);
		_content = parentView;
		_angle = angle;
		_colorResId = colorResId;
		_alpha = alpha;
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		// Calculate the radius of the dial.
		
		_paint.setColor(getContext().getResources().getColor(_colorResId));
		_paint.setStrokeWidth(5);	

		// The paint object will fill its inside area.
		_paint.setStyle(Paint.Style.FILL);
		// Set the opacity of the arc's paint
		_paint.setAlpha(_alpha);
		
		Point coordinates = calculateCircleCoordinates();
		
		// draw the arc
		canvas.drawCircle(coordinates.x, coordinates.y, (int)getResources().getDimension(R.dimen.time_circle), _paint);
	}
	
	private Point calculateCircleCoordinates() {
		float radius = _context.getResources().getDimension(R.dimen.dail_pad_size)/2f;

		// Center values
		float halfWidth = ((float)_content.getWidth())/2f;
		float halfHeight = ((float)_content.getHeight())/2f;
		
		int quadrant = 1;
		if(_angle < 360) {
			quadrant += (int)(_angle/90);
		}
		
		double angle = _angle % 90;
		
		angle = Math.toRadians(angle);
		
		Point coordinates = new Point();
		switch(quadrant) {
			case 1:
				coordinates.x = (int) (halfWidth + radius * Math.sin(angle));
				coordinates.y = (int) (halfHeight - radius * Math.cos(angle));
				break;
			case 2:
				coordinates.x = (int) (halfWidth + radius * Math.cos(angle));
				coordinates.y = (int) (halfHeight + radius * Math.sin(angle));
				break;
			case 3:
				coordinates.x = (int) (halfWidth - radius * Math.sin(angle));
				coordinates.y = (int) (halfHeight + radius * Math.cos(angle));
				break;
			case 4:
				coordinates.x = (int) (halfWidth - radius * Math.cos(angle));
				coordinates.y = (int) (halfHeight - radius * Math.sin(angle));
				break;
		}
		
		return coordinates;
	}
}
