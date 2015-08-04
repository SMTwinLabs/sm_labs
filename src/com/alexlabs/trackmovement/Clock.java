package com.alexlabs.trackmovement;

import android.view.MotionEvent;
import android.view.View;

public class Clock {
	public static final int MAX_MINUTES_ON_CLOCK = 60;
	public static final int DEGRESS_IN_CIRCLE = 360;
	public static final int DEGRESS_PER_MINUTE = 6;
	public static final int PROXIMITY_TO_EVERY_FIFTH_MINUTE = 2;
	
	private int _minute;
	private int _angle;
	private View _content;
	private View _dial;
	
	public Clock(View content, View dial) {
		_content = content;
		_dial = dial;
	}
	
	/**
	 * Set the clock's minute from the X and Y coordinates of the selected point on the screen. 
	 * @param parentEdgeLength - the length of the paren's edge. It is paramount that the parent is
	 * quadratic in shape.
	 * @param selectedX - the X coordinate of the selected point on the screen.
	 * @param selectedY - the Y coordinate of the selected point on the screen.
	 */
	public void wind(MotionEvent motionEvent) {		
		// Get the angle.
		int pivotAngle = (int) Math.ceil(calculatePivotAngle(motionEvent.getX(), motionEvent.getY()));
		int selectedMinuteOnDial = (int)Math.ceil(pivotAngle/DEGRESS_PER_MINUTE);
		if(isSelectedPointInDialBounds(motionEvent.getX(), motionEvent.getY())) {			
			_angle = generateDialAreaBoundedAngle(selectedMinuteOnDial);
			_minute = (int)Math.ceil(_angle/DEGRESS_PER_MINUTE);
		} else {
			_angle = selectedMinuteOnDial*DEGRESS_PER_MINUTE;
			_minute = selectedMinuteOnDial;
		}
	}
	
	/**
	 * Calculates the length of a line based on the start and end X/Y coordinates.
	 * @param startX - the X coordinate of the first point
	 * @param startY - the Y coordinate of the first point
	 * @param endX - the X coordinate of the second point
	 * @param endY - the Y coordinate of the second point
	 * @return the calculated line length.
	 */
	private static double calculateLineLengthBetweenTwoPoints(double startX, double startY, double endX, double endY) {
		double sideLenght = Math.sqrt(Math.pow(startX - endX, 2) + Math.pow(startY - endY, 2));
		return sideLenght;
	}
	
	private double calculatePivotAngle(double selectedX, double selectedY) {
		// Sides of the triangle.
		double sideA, sideB, sideC;
		int parentEdgeLength = _content.getHeight();
		
		sideA = parentEdgeLength/2;
		sideB = Clock.calculateLineLengthBetweenTwoPoints(parentEdgeLength/2, parentEdgeLength/2, selectedX, selectedY);
		sideC = Clock.calculateLineLengthBetweenTwoPoints(parentEdgeLength/2, 0, selectedX, selectedY);	
		
		double angle = Math.acos((Math.pow(sideA, 2) + Math.pow(sideB, 2) - Math.pow(sideC, 2))/(2*sideA*sideB));
		angle = Math.toDegrees(angle);
		
		if(selectedX <= parentEdgeLength/2) {
			angle = DEGRESS_IN_CIRCLE - angle;
		}
		
		return angle;
	}
	
	/**
	 * This angle is calculated based on where the point is located - inside or outside the dial's area. If its is inside the dial, then the
	 * the user can only set every fifth minute. If the user selected 16 minutes, then the generated angle will correspond to that of 15 minutes. 
	 * If the location of the point is outside the aforementioned dial area, then the use can set every minute from 1 to 60 (placing no restraints
	 * on the angle).
	 * @param minute
	 * @return
	 */
	private int generateDialAreaBoundedAngle(int minute) {
		int angle = 0;
		if(minute % 5 > PROXIMITY_TO_EVERY_FIFTH_MINUTE) {
			angle = ((minute / 5) * 5 + 5) * DEGRESS_PER_MINUTE;
		} else {
			if(minute <= PROXIMITY_TO_EVERY_FIFTH_MINUTE) {
				angle = DEGRESS_IN_CIRCLE;
			} else {
				angle = ((minute / 5) * 5) * DEGRESS_PER_MINUTE;
			}
		}		
		return angle;
	}
	
	private boolean isSelectedPointInDialBounds(double selectedX, double selectedY) {
		double parentCenterCoordinate = ((double)_content.getHeight())/2;
		double distanceFromCentre = calculateLineLengthBetweenTwoPoints(parentCenterCoordinate, parentCenterCoordinate, selectedX, selectedY);
		double clockRadius = ((double)_dial.getHeight())/2;
		return clockRadius >= distanceFromCentre;
	}
	
	public int getMinute() {
		return _minute;
	}
	
	public int getAngle() {
		return _angle;
	}
}
