package com.alexlabs.trackmovement;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/**
 * A class for creating and managing the characteristics of an Arc.
 */
public class Arc extends View {
	private View _content;
	private Context _context;
	private int _alpha;
	
	private int _colorResId;
	
	// The angle corresponding to the selected minute. 
	private float _angle;
	
	private Paint _paint;
	private RectF _oval;
	
	
	public Arc(Context context) {
		super(context);
		_context = context;
		_paint = new Paint();
		_oval = new RectF();
	}
	
	public Arc(Context context, View parentView, float angle, int colorResId, int alpha) {
		this(context);
		_content = parentView;
		_angle = angle;
		_colorResId = colorResId;
		_alpha = alpha;
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		// Set the radius for the circle.
		float radius = _context.getResources().getDimension(R.dimen.dail_pad_size)/2f;
		float centerX = ((float)_content.getWidth())/2f;
		float centerY = ((float)_content.getHeight())/2f;
		_paint.setColor(getContext().getResources().getColor(_colorResId));
		_paint.setStrokeWidth(5);	

		// The paint object will fill its inside area.
		_paint.setStyle(Paint.Style.FILL);
		// Set the opacity of the arc's paint
		_paint.setAlpha(_alpha);
		
		// Set coordinates to the specified values.
		_oval.set(centerX - radius, 
				centerY - radius, 
				centerX + radius, 
				centerY + radius);
		// draw the arc
		canvas.drawArc(_oval, 270, _angle, true, _paint);
	}
}