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

		Log.d("ALEX_LABS", "Screen past state was on?: " + _wasScreenOn);
		
		IBinder binder = peekService(context, new Intent(context, CountDownTimerService.class));
		if(binder != null && !_wasScreenOn) {
			Messenger countDownTimerSerivce = new Messenger(binder);
			try {
				countDownTimerSerivce.send(Message.obtain(null, CountDownTimerService.MSG_CHECK_MODE_ON_SCREEN_TOGGLE));
			} catch (RemoteException e) {
				// TODO: handle exception
			}
		}
	}

	public boolean getWasScreenOn() {
		return _wasScreenOn;
	}
}