package com.alexlabs.trackmovement;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class ScreenReceiver extends BroadcastReceiver {

    final String SYSTEM_DIALOG_REASON_KEY = "reason";
    
	private static boolean _isScreenOn = true;

	@Override
	public void onReceive(final Context context, final Intent intent) {	
		if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
			// screen is turned on
			_isScreenOn = false;
		
		} else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {			
			// screen is turned off
			_isScreenOn = true;
		}

		Log.d("ALEX_LABS", "Screen past state was on?: " + _isScreenOn);
		
		IBinder binder = peekService(context, new Intent(context, CountDownTimerService.class));
		if(binder != null && !_isScreenOn) {
			Messenger countDownTimerSerivce = new Messenger(binder);
			try {
				countDownTimerSerivce.send(Message.obtain(null, CountDownTimerService.MSG_CHECK_MODE_ON_SCREEN_TOGGLE));
			} catch (RemoteException e) {
				// TODO: handle exception
			}
		}
	}

	public boolean getWasScreenOn() {
		return _isScreenOn;
	}
}