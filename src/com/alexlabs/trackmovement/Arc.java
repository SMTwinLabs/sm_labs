package com.alexlabs.trackmovement;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

public class Arc extends View {
	// The side length of the clock's dial view. The host is 
	// the view that contains the clock's user interface.
	private float _hostEdgeLength;
	
	// The side length of the clock's parent view. This view
	// contains the clock's dial view.
	private float _parentEdgeLength;
	
	// The angle corresponding to the selected minute. 
	private float _angle;
	
	private Paint _paint;
	private RectF _oval;
	
	
	public Arc(Context context) {
		super(context);
		_paint = new Paint();
		_oval = new RectF();
	}
	
	public Arc(Context context, float diameter, float angle) {
		this(context);
		_hostEdgeLength = diameter;
		_angle = angle;
	}
	
	public Arc(Context context, float diameter, float angle, float parentHeight) {
		this(context, diameter, angle);
		_parentEdgeLength = parentHeight;
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		// Set the radius for the circle.
		float radius = _hostEdgeLength/2;
		float center = _parentEdgeLength/2f;
		_paint.setColor(getContext().getResources().getColor(R.color.red));
		_paint.setStrokeWidth(5);	

		// The paint object will fill its inside area.
		_paint.setStyle(Paint.Style.FILL);

		// Set coordinates to the specified values.
		_oval.set(center - radius, 
				center - radius, 
				center + radius, 
				center + radius);

		// draw the arc
		canvas.drawArc(_oval, 270, _angle, true, _paint);
	}
}