package com.alexlabs.trackmovement;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

public class LightBeam extends View{	
	private View _content;	
	private int _colorResId;
	private Paint _paint;
	private int _alpha;
	private float _angle;
	
	// Used to extract the coordinates of the touch.
	private MotionEvent _motionEvent;

	public LightBeam(Context context) {
		super(context);
		_paint = new Paint();
	}
	
	public LightBeam(Context context, View parentView, MotionEvent motionEvent, float angle, int colorResId, int alpha) {
		this(context);
		_content = parentView;
		_motionEvent = motionEvent;
		_angle = angle;
		_colorResId = colorResId;
		_alpha = alpha;
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		// Calculate the center point, or the start points of the light beam;
		float centerX = ((float)_content.getWidth())/2f;
		float centerY = ((float)_content.getHeight())/2f;
		// Acquire the touch coordinates.
		
		// The coordinates of the touch need to be translated to be relative to the
		// layout where the user made the touch. In order to do that the location of
		// of the view on the screen needs to be acquired and then these coordinates
		// need to be subtracted from the raw X and Y physical coordinates.
		int[] viewCoords = new int[2];
		_content.getLocationOnScreen(viewCoords);
		float endX = _motionEvent.getRawX() - viewCoords[0];
		float endY = _motionEvent.getRawY() - viewCoords[1];
		
		// We can imagine that the light beam is an edge in a triangle defined
		// by the center point and the point where the user's finger is.
		// The length of the light beam line can be calculated from the triangle
		// with the edges shown below using the Pythagorean theorem.
		float edgeX = endX - centerX;
		float edgeY = endY - centerY;
		float edgeLightBeam = (float) Math.sqrt(edgeX*edgeX + edgeY*edgeY);
		_paint.setColor(getContext().getResources().getColor(_colorResId));
		_paint.setStrokeWidth(3);
		// The paint object will fill its inside area.
		_paint.setStyle(Paint.Style.FILL);
		// Set the opacity of the arc's paint
		_paint.setAlpha(_alpha);
		
		// The canvas will be rotated. To do that the current state is saved,
		// the canvas is rotated and after that it is restored.
		canvas.save();
		
		canvas.rotate(_angle, centerX, centerY);
		
		// The light-beam effect is achieved by having 2 lines: one that is solid and
		// one that is blurred.
		// The first line the solid line is drawn. 
		canvas.drawLine(centerX, centerY, centerX, centerY - edgeLightBeam, _paint);
		
		// Then the blurred line is drawn.
		_paint.setStrokeWidth(10);
		_paint.setMaskFilter(new BlurMaskFilter(5, BlurMaskFilter.Blur.NORMAL));
		// Set the alpha of the blurred line to be max - 255.
		_paint.setAlpha(255);
		canvas.drawLine(centerX, centerY, centerX, centerY - edgeLightBeam, _paint);
		
		canvas.restore();
	}

}
