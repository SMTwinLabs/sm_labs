package com.alexlabs.trackmovement;

import java.io.IOException;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.Vibrator;

public class AlarmBell {
	// Volume suggested by media team for in-call alarms.
    private static final float IN_CALL_VOLUME = 0.125f;
    
    private MediaPlayer _mediaPlayer;
	private boolean _isMediaPlayerStarted;
	private boolean _isAlarmStarted;
	private volatile static AlarmBell _bell;
	
	private AlarmBell(){
	}
	
	// Double-checked locking
	public static AlarmBell instance(){
		if(_bell == null) {
			synchronized(AlarmBell.class){
				if(_bell == null) {
					_bell = new AlarmBell();
				}
			}
		}
		
		return _bell;
	}

	public boolean isAlarmStarted() {
		return _isAlarmStarted;
	}
	
    /**
     * If the media player is playing, then Stops both the media player and vibration
     * @param context
     */
    public void stop(Context context) {
        if (_isMediaPlayerStarted) {
            // Stop audio playing
            stopMediaPlayer(context);
        }

        stopVibration(context);
        _isAlarmStarted = false;
    }

	private void stopVibration(Context context) {
		((Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE)).cancel();
	}

    /**
     * Stop the media player from producing any sound and dispose the media player.
     * @param context
     */
	private void stopMediaPlayer(Context context) {
		_mediaPlayer.stop();
	    AudioManager audioManager = (AudioManager)
	            context.getSystemService(Context.AUDIO_SERVICE);
	    audioManager.abandonAudioFocus(null);
	    _mediaPlayer.release();
	    _mediaPlayer = null;	
	    
	    _isMediaPlayerStarted = false;
	}

	/**
	 * Start both the media player and vibration. If the user is taking a phone call,
	 * the strength of the audio is greatly diminished.
	 * @param context
	 * @param inTelephoneCall
	 */
    public void start(final Context context, boolean inTelephoneCall) {
        // NOTE: because we are using a single instance of the media player, we need
        // to reset the media player, so that it goes in its uninitialized state. After
        // initialize the player again.
    	Preferences preferences = new Preferences();
    	if(preferences.isSoundOn()) {
    		startMediaPlayer(context, inTelephoneCall);
    	}
        
    	if(preferences.isVibrationOn()) {
    		startVibration(context);
    	}
    	
        _isAlarmStarted = true;
    }

	private void startVibration(final Context context) {
		Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(new long[] {500, 500}, 0);
	}

    /**
     * Start the media player.If the user is taking a phone call,
	 * the strength of the audio is greatly diminished.
     * @param context
     * @param inTelephoneCall
     */
	private void startMediaPlayer(final Context context,
			boolean inTelephoneCall) {
		
		// Make sure we are stop before starting
    	if(_mediaPlayer != null){
    		stopMediaPlayer(context);  
    	}

		AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), AudioManager.FLAG_PLAY_SOUND);
    	
		_mediaPlayer = new MediaPlayer();
        
        _mediaPlayer.setOnErrorListener(new OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                AlarmBell.instance().stop(context);
                return true;
            }
        });

        try {
            // Check if we are in a call. If we are, use the in-call alarm
            // resource at a low volume to not disrupt the call.
            if (inTelephoneCall) {
            	_mediaPlayer.setVolume(IN_CALL_VOLUME, IN_CALL_VOLUME);
            } 
            
            setDataSourceFromResource(context, R.raw.old_clock_ringing_short);
            
            startAlarm(context, _mediaPlayer);
        } catch (Exception ex) {
            // The alarmNoise may be on the SD card which could be busy right
            // now. Use the fallback ringtone.
            try {
                // Must reset the media player to clear the error state.
            	_mediaPlayer.reset();
            	_mediaPlayer.setDataSource(context, provideFallbackAlarmNoise());
                startAlarm(context, _mediaPlayer);
            } catch (Exception ex2) {
            	
            	// No rigtones are available, so the user will not hear anything.
            	_mediaPlayer.stop();
                _mediaPlayer.release();
                _mediaPlayer = null;
	            // TODO - consider adding string vibration
            }
        }
        
        _isMediaPlayerStarted = true;
	}

	private Uri provideFallbackAlarmNoise() {
		Uri fallBackAlarmNoiseUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        // Fall back on the default alarm if the database does not have an
        // alarm stored.
        if (fallBackAlarmNoiseUri == null) {
            fallBackAlarmNoiseUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        
		return fallBackAlarmNoiseUri;
	}
    
    private void startAlarm(Context context, MediaPlayer player) throws IOException {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setLooping(true);
        player.prepare();
        audioManager.requestAudioFocus(null,
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        player.start();
    }
    
    private void setDataSourceFromResource(Context context, int res)
            throws IOException {
        AssetFileDescriptor assetFileDescriptor = context.getResources().openRawResourceFd(res);
        if (assetFileDescriptor != null) {
            _mediaPlayer.setDataSource(assetFileDescriptor.getFileDescriptor(), assetFileDescriptor.getStartOffset(), assetFileDescriptor.getLength());
            assetFileDescriptor.close();
        }
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
