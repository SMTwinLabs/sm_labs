package com.alexlabs.trackmovement;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

public class Circle extends View {
	private View _content;
	private Context _context;
	private int _alpha;
	
	private int _colorResId;
	
	// The angle corresponding to the selected minute. 
	private float _angle;
	
	private Paint _paint;
	
	
	public Circle(Context context) {
		super(context);
		_context = context;
		_paint = new Paint();
	}
	
	public Circle(Context context, View parentView, float angle, int colorResId, int alpha) {
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
		float radius = _context.getResources().getDimension(R.dimen.dail_pad_size)/2f;
		
		// Center values
		float centerX = ((float)_content.getWidth())/2f;
		float centerY = ((float)_content.getHeight())/2f;
		
		// Calculating coordinates
		float coordX = (float) (centerX + Math.cos((double)_angle)*radius);
		float coordY = (float) (centerY + Math.sin((double)_angle)*radius);
		
		_paint.setColor(getContext().getResources().getColor(_colorResId));
		_paint.setStrokeWidth(5);	

		// The paint object will fill its inside area.
		_paint.setStyle(Paint.Style.FILL);
		// Set the opacity of the arc's paint
		_paint.setAlpha(_alpha);
		
		// draw the arc
		canvas.drawCircle(coordX, coordY, 10, _paint);
	}
}
