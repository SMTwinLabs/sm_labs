package com.alexlabs.trackmovement;

public class Clock {
	public static final int MAX_MINUTES_ON_CLOCK = 60;
	public static final int DEGRESS_IN_CIRCLE = 360;
	public static final int DEGRESS_PER_MINUTE = 6;
	
	private double _minute;
	
	public Clock() {
		// Do nothing.
	}
	
	/**
	 * Set the clock's minute from the X and Y coordinates of the selected point on the screen. 
	 * @param parentEdgeLength - the length of the paren's edge. It is paramount that the parent is
	 * quadratic in shape.
	 * @param selectedX - the X coordinate of the selected point on the screen.
	 * @param selectedY - the Y coordinate of the selected point on the screen.
	 */
	public void wind(int parentEdgeLength, float selectedX, float selectedY) {
		// Sides of the triangle.
		double sideA, sideB, sideC;
		sideA = parentEdgeLength/2;
		sideB = Clock.calculateSideLength(parentEdgeLength/2, parentEdgeLength/2, selectedX, selectedY);
		sideC = Clock.calculateSideLength(parentEdgeLength/2, 0, selectedX, selectedY);
		
		// Get the angle.
		double angle = Math.ceil(Clock.calculatePivotAngle(sideA, sideB, sideC, parentEdgeLength, selectedX));
		_minute = Math.ceil(angle/DEGRESS_PER_MINUTE);
	}
	
	/**
	 * Calculates the side of a triangle based on the start and end X/Y coordinates.
	 * @param startX
	 * @param startY
	 * @param endX
	 * @param endY
	 * @return the calculated side's length.
	 */
	private static double calculateSideLength(float startX, float startY, float endX, float endY) {
		double sideLenght = Math.sqrt(Math.pow(startX - endX, 2) + Math.pow(startY - endY, 2));
		return sideLenght;
	}
	
	private static double calculatePivotAngle(double sideA, double sideB, double sideC, int diameter, float selectedX) {
		double angle = Math.acos((Math.pow(sideA, 2) + Math.pow(sideB, 2) - Math.pow(sideC, 2))/(2*sideA*sideB));
		angle = Math.toDegrees(angle);
		if(selectedX <= diameter/2) {
			angle = DEGRESS_IN_CIRCLE - angle;
		}
		return angle;
	}
	
	public int getMinute() {
		return (int)_minute;
	}
	
	public int generateAngleFromMinute() {
		return (int)_minute*DEGRESS_PER_MINUTE;
	}	
}
