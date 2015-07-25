package com.alexlabs.trackmovement;

public class Clock {
	public static final float MAX_MINUTES_ON_CLOCK = 60;
	public static final float DEGRESS_IN_CIRCLE = 360;
	
	private double _angle;
	private double _minute;
	
	public Clock() {
		// Do nothing.
	}
	
	public void wind(int diameter, float selectedX, float selectedY) {
		// Sides of the triangle.
		double sideA, sideB, sideC;
		sideA = diameter/2;
		sideB = Clock.calculateEdgeLength(diameter/2, diameter/2, selectedX, selectedY);
		sideC = Clock.calculateEdgeLength(diameter/2, 0, selectedX, selectedY);
		
		// Get the angle.
		_angle = Math.ceil(Clock.calculateAngle(sideA, sideB, sideC, diameter, selectedX));
		_minute = Math.ceil(_angle/(DEGRESS_IN_CIRCLE/MAX_MINUTES_ON_CLOCK));
	}
	
	
	private static double calculateEdgeLength(float startX, float startY, float endX, float endY) {
		double edgeLenght = Math.sqrt(Math.pow(startX - endX, 2) + Math.pow(startY - endY, 2));
		return edgeLenght;
	}
	
	private static double calculateAngle(double sideA, double sideB, double sideC, int diameter, float selectedX) {
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
	
	public double getAngle() {
		return _angle;
	}	
}
