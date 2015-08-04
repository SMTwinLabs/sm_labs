package com.alexlabs.trackmovement;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

public class Arc extends View {
	private View _dialView;
	private View _parentView;
	
	// The angle corresponding to the selected minute. 
	private float _angle;
	
	private Paint _paint;
	private RectF _oval;
	
	
	public Arc(Context context) {
		super(context);
		_paint = new Paint();
		_oval = new RectF();
	}
	
	public Arc(Context context, View dialView, View parentView, float angle) {
		this(context);
		_dialView = dialView;
		_parentView = parentView;
		_angle = angle;
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		// Set the radius for the circle.
		float radius = ((float)_dialView.getHeight())/2f;
		float center = ((float)_parentView.getHeight())/2f;
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