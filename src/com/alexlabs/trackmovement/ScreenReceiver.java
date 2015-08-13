package com.alexlabs.trackmovement;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ScreenReceiver extends BroadcastReceiver {

	private static boolean _wasScreenOn = true;

	@Override
	public void onReceive(final Context context, final Intent intent) {
		if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
			// do whatever you need to do here
			_wasScreenOn = false;
		} else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
			// and do whatever you need to do here
			_wasScreenOn = true;
		}

		Log.d("alex_labs", "Screen past state was on?: " + _wasScreenOn);
	}

	public boolean getWasScreenOn() {
		return _wasScreenOn;
	}
}