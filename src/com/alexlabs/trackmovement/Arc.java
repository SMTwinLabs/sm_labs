package com.alexlabs.trackmovement;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

public class Arc extends View {
	private float _diameter;
	private float _angle;
	private Paint _paint;
	private RectF _oval;
	private View _parent;
	
	public Arc(Context context) {
		super(context);
		_diameter = 0.0f;
		_angle = 0.0f;
		_paint = new Paint();
		_oval = new RectF();
		_parent = null;
	}
	
	public Arc(Context context, float diameter, float angle) {
		this(context);
		_diameter = diameter;
		_angle = angle;
	}
	
	public Arc(Context context, float diameter, float angle, View parent) {
		this(context, diameter, angle);
		_parent = parent;
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		// set the radius for the circle
		float radius = _diameter/2;
		_paint.setColor(getContext().getResources().getColor(R.color.red));
		_paint.setStrokeWidth(5);	

		// the paint object will fill its inside area
		_paint.setStyle(Paint.Style.FILL);

		// set coordinates to the specified values
		_oval.set(getCenterX() - radius, 
				getCenterY() - radius, 
				getCenterX() + radius, 
				getCenterY() + radius);

		// draw the arc
		canvas.drawArc(_oval, 270, _angle, true, _paint);
	}		  
	
	private float getCenterX() {
		return ((float)_parent.getHeight())/2f;
	}
	
	private float getCenterY() {
		return ((float)_parent.getWidth())/2f;
	}
}