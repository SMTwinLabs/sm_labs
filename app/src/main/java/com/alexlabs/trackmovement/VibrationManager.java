package com.alexlabs.trackmovement;

import android.content.Context;
import android.os.Vibrator;

public class VibrationManager {
	
	private boolean _isVibrationStarted;
	
	public boolean isVibrationStarted() {
		return _isVibrationStarted;
	}
	
	/**
	 * Start only the vibration of the alarm bell.
	 * @param context
	 */
	public void start(Context context) {
		Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
	    v.vibrate(new long[] {500, 500}, 0);
	    
	    _isVibrationStarted = true;
	}

	/**
	 * Stop the vibration of the alarm bell.
	 */
	public void stop(Context context) {
		((Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE)).cancel();
		_isVibrationStarted = false;
	}
}
