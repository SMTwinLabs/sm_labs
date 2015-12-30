package com.alexlabs.trackmovement;

import android.content.Context;
import android.os.PowerManager;

public class PartialWakeLocker {
	private PowerManager.WakeLock _wakeLock;
	private static final String APP_TAG = "com.alexlabs.trackmovement.Main";

	public PartialWakeLocker() {
		// empty
	}

	public void acquire(Context ctx) {
		if (_wakeLock != null)
			_wakeLock.release();

		PowerManager pm = (PowerManager) ctx
				.getSystemService(Context.POWER_SERVICE);
		_wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, APP_TAG);
		_wakeLock.acquire();
	}

	public void release() {
		if (_wakeLock != null)
			_wakeLock.release();

		_wakeLock = null;
	}
}

