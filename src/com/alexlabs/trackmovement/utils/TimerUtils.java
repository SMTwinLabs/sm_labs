package com.alexlabs.trackmovement.utils;

import com.alexlabs.trackmovement.R;
import com.alexlabs.trackmovement.R.dimen;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;

/**
 * A utility class for generating minutes and seconds from the user specified coordinates. The idea behind this class is to imagine a circle in
 * a coordinate system. The X/Y coordinates are used to derive the radius of the imagined circle.
 * <br><br>
 * IMPORTANT: The start of the coordinate system is in the upper left corner.
 */
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
	
	/**
	 * This method calculates the so called pivot angle. By using this angle, the minutes can later be derived.
	 * The term pivot is used to point towards the fact that we can imagine a circumference and use its center as 
	 * a pivot around which the user operates.
	 * <br><br>
	 * This angle is derived on the following basis: to calculate an angle, the sides of a triangle will be necessary. 
	 * The length of the three sides are calculated as follows: calculating the so called "vertical line" - the line drawn from the center 
	 * of the circumference and perpendicular to the X-axis (this line does not depend on the selected coordinates); the line between the center 
	 * of the circumference and the user selected point; the line drawn from the selected point on the screen and the point 
	 * where the "vertical line" and the X-axis converge.
	 * @param selectedX
	 * @param selectedY
	 * @param context
	 * @param content
	 * @return
	 */
	private static double calculatePivotAngle(double selectedX, double selectedY, Context context, View content) {
		// Sides of the triangle.
		double sideA, sideB, sideC;
		
		// The vertical line
		sideA = content.getHeight()/2;
		sideB = TimerUtils.calculateLineLengthBetweenTwoPoints(content.getWidth()/2, content.getHeight()/2, selectedX, selectedY);
		sideC = TimerUtils.calculateLineLengthBetweenTwoPoints(content.getWidth()/2, 0, selectedX, selectedY);	
		
		// A mathematical theorem for deriving the cosine of a triangle where the length of all tree sides is known. 
		double angle = Math.acos((Math.pow(sideA, 2) + Math.pow(sideB, 2) - Math.pow(sideC, 2))/(2*sideA*sideB));
		angle = Math.toDegrees(angle);
		
		// The methodology used will be able to ascertain the minutes only on the one side (from 180 to -180 degrees). To expand 
		// this to a full 360 degrees, we establish weather the selected minute is in the first vertical half (from the start of the coordinate
		// system till coordinates of the pivot) or the second half.
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
