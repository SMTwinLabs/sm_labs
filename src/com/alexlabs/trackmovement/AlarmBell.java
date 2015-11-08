package com.alexlabs.trackmovement;


import com.alexlabs.trackmovement.MediaPlayerManager.Volumes;

import android.content.Context;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

/**
 * This class provides a facility for managing the alarm noise and vibration, which are grouped
 * under the common term "alarm bell".
 */
public class AlarmBell {
	private static AlarmBell _bell;

	private Preferences _preferences = new Preferences();
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
        	_mediaPlayerManager.stopMediaPlayer(context);
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
	 * @param inTelephoneCall
	 */
    public void start(final Context context, boolean inTelephoneCall) {
    	
    	if(_preferences.isSoundOn()) {
    		_mediaPlayerManager.start(context, inTelephoneCall ? Volumes.getInCallLevel() : Volumes.getPreferencesLevel());
    	}
		
		if(_preferences.isVibrationOn()) {
			_vibrationManager.start(context);
		}
    	
        _isAlarmStarted = true;
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
