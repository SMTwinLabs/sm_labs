package com.alexlabs.trackmovement;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;

public class TimerUtils {
	public static final int MAX_MINUTES_ON_CLOCK = 60;
	public static final int DEGRESS_IN_CIRCLE = 360;
	public static final int DEGRESS_PER_MINUTE = 6;
	public static final float DEGRESS_PER_SECOND = 0.1f;
	public static final int PROXIMITY_TO_EVERY_FIFTH_MINUTE = 2;
	
	/**
	 * Calculate the clock's minute from the X and Y coordinates of the selected point on the screen from
	 * the motion event. 
	 * @param motionEvent
	 * @param context
	 * @param content
	 * @return the calculated minute
	 */
	public static int generateMinute(MotionEvent motionEvent, Context context, View content) {		
		// Get the angle.
		int pivotAngle = (int) Math.ceil(calculatePivotAngle(motionEvent.getX(), motionEvent.getY(), context, content));
		int selectedMinuteOnDial = (int)Math.ceil(pivotAngle/DEGRESS_PER_MINUTE);
		int minute = 0;
		if(isSelectedPointInDialBounds(motionEvent.getX(), motionEvent.getY(), context, content)) {			
			int angle = generateDialAreaBoundedAngle(selectedMinuteOnDial);
			minute = (int)Math.ceil(angle/DEGRESS_PER_MINUTE);
		} else {
			minute = selectedMinuteOnDial;
		}
		
		return minute;
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
	
	private static double calculatePivotAngle(double selectedX, double selectedY, Context context, View content) {
		// Sides of the triangle.
		double sideA, sideB, sideC;
		
		sideA = content.getHeight()/2;
		sideB = TimerUtils.calculateLineLengthBetweenTwoPoints(content.getWidth()/2, content.getHeight()/2, selectedX, selectedY);
		sideC = TimerUtils.calculateLineLengthBetweenTwoPoints(content.getWidth()/2, 0, selectedX, selectedY);	
		
		double angle = Math.acos((Math.pow(sideA, 2) + Math.pow(sideB, 2) - Math.pow(sideC, 2))/(2*sideA*sideB));
		angle = Math.toDegrees(angle);
		
		if(selectedX <= content.getWidth()/2) {
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
	private static int generateDialAreaBoundedAngle(int minute) {
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
	
	private static boolean isSelectedPointInDialBounds(double selectedX, double selectedY, Context context, View content) {
		double parentCenterCoordinateY = ((double)content.getHeight())/2;
		double parentCenterCoordinateX = ((double)content.getWidth())/2;
		double distanceFromCentre = calculateLineLengthBetweenTwoPoints(parentCenterCoordinateX, parentCenterCoordinateY, selectedX, selectedY);
		double clockRadius = context.getResources().getDimension(R.dimen.clock_every_fifth_minute_area_diameter)/2f;
		return clockRadius >= distanceFromCentre;
	}
	
	public static float generateAngleFromTime(int minute, int seconds){
		return generateAngleFromMinute(minute) + generateAngleFromSeconds(seconds);
	}
	
	public static int generateAngleFromMinute(int minute) {
		return minute*DEGRESS_PER_MINUTE;
	}
	
	public static float generateAngleFromSeconds(int seconds) {
		return seconds*DEGRESS_PER_SECOND;
	}
	
	public static long convertMinuteToMillis(int minute){
		return minute * 1000 * 60;
	}

	public static int getSecondsFromMillisecnods(long millisUntilFinished) {
		return (int) millisUntilFinished / 1000 % 60;
	}

	public static int getMinuteFromMillisecnods(long millisUntilFinished) {
		return (int) millisUntilFinished / 1000 / 60;
	}

	public static long getMillisFromMinutes(int selectedMinute) {
		return selectedMinute * 1000 * 60;
	}
}
