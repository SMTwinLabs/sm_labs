package com.alexlabs.trackmovement;


import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.alexlabs.trackmovement.MediaPlayerManager.Volumes;

import android.content.Context;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.telephony.TelephonyManager;

/**
 * This class provides a facility for managing the alarm noise and vibration, which are grouped
 * under the common term "alarm bell".
 */
public class AlarmBell {
	private static AlarmBell _bell;
	
	private MediaPlayerManager _mediaPlayerManager;
	private VibrationManager _vibrationManager;
	
	private boolean _isAlarmStarted;
	
	
	private AlarmBell(){
		_mediaPlayerManager = new MediaPlayerManager();
		_vibrationManager = new VibrationManager();
	}
	
	// Double-checked locking
	public static AlarmBell instance(){
		if(_bell == null) {
			_bell = new AlarmBell();
		}
		
		return _bell;
	}
	
	/**
	 * Returns weather the alarm bell has been started. 
	 * @return Weather the alarm bell has been started. 
	 */
	public boolean isAlarmStarted() {
		return _isAlarmStarted;
	}
	
    /**
     * If the media player is playing, then stop both the media player and the vibration. This method
     * should also be invoked in the case when an error occurs.
     * @param context
     */
    public void stop(Context context) {
        if (_mediaPlayerManager.isMediaPlayerStarted()) {
            // Stop audio playing
        	_mediaPlayerManager.stop(context);
        }

        if(_vibrationManager.isVibrationStarted()) {
        	_vibrationManager.stop(context);
        }
        
        _isAlarmStarted = false;
    }
    
    /**
	 * Start both the media player and vibration. If the user is taking a phone call,
	 * the strength of the audio is greatly diminished.
	 * @param context
	 */
    public void start(final Context context) {    	
    	Preferences preferences = new Preferences();
    	
    	// The alarm bell's audio file is set to loop indefinitely. It will be stopped
    	// below, taking into consideration the user preferences for the loop length.
    	if(preferences.isSoundOn()) {
    		TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    		boolean inTelephoneCall = telephonyManager.getCallState() == TelephonyManager.CALL_STATE_OFFHOOK;
    		_mediaPlayerManager.start(context, inTelephoneCall ? Volumes.getInCallLevel() : Volumes.getPreferencesLevel(), preferences.getRingtone(), true);
    	}
		
		if(preferences.isVibrationOn()) {
			_vibrationManager.start(context);
		}
    	
        _isAlarmStarted = true;
        
        // After the alarm bell is started it is stopped after a period of time
        // (this time period is chosen from the preferences).
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.schedule(new Runnable() {
			
			@Override
			public void run() {
				if(_isAlarmStarted) {
					stop(context);
				}
			}
		}, preferences.getAlarmNoiseDuration(), TimeUnit.SECONDS);
        
        scheduler.shutdown();
    }
    
    // FIXME send to another class
	public static void sendStopAlarmNoiseAndVibrationMessage(Messenger service) {
		Message msg = Message.obtain(null,
		        CountDownTimerService.MSG_STOP_ALARM_NOISE_AND_VIBRATION);
		try {
			service.send(msg);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
