package com.alexlabs.trackmovement;

import java.io.IOException;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.media.RingtoneManager;
import android.net.Uri;

public class MediaPlayerManager {
	/**
     * Holds a reference to the current instance of the media player. Whenever the media player
     * has been stopped, create a new instance of the media player.
     */
    private MediaPlayer _mediaPlayer;
	private boolean _isMediaPlayerStarted;
	
	public boolean isMediaPlayerStarted() {
		return _isMediaPlayerStarted;
	}
	
	public void start(Context context, double level) {
		// NOTE: because we are using a single instance of the media player, we need
        // to reset the media player, so that it goes in its uninitialized state. After
        // initialize the player again.
    	if(level > 0) {
    		AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    		audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (int)(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) * level), AudioManager.ADJUST_SAME);
    		
    		initMediaPlayer(context, audioManager);
    	}
	}

	/**
	 * Start the media player.If the user is taking a phone call,
	 * the strength of the audio is greatly diminished.
	 * @param context
	 * @param inTelephoneCall
	 */
	private void initMediaPlayer(final Context context, final AudioManager audioManager) {
		
		// Make sure we are stop before starting
		if(_mediaPlayer != null){
			stopMediaPlayer(context);  
		}
		
		_mediaPlayer = new MediaPlayer();
	    _mediaPlayer.setOnErrorListener(new OnErrorListener() {
	        @Override
	        public boolean onError(MediaPlayer mp, int what, int extra) {
	            AlarmBell.instance().stop(context);
	            return true;
	        }
	    });
	
	
	    Preferences preferences = new Preferences();
	    try {
	        setDataSourceFromResource(context, preferences.getRingtone());
	        
	        startMediaPlayer(context, audioManager);
	    } catch (Exception ex) {
	        // The alarmNoise may be on the SD card which could be busy right
	        // now. Use the fallback ringtone.
	        try {
	            // Must reset the media player to clear the error state.
	        	_mediaPlayer.reset();
	        	_mediaPlayer.setDataSource(context, provideFallbackAlarmNoise());
	            
	        	startMediaPlayer(context, audioManager);
	        } catch (Exception ex2) {
	        	
	        	// No ringtones are available, so the user will not hear anything.
	        	_mediaPlayer.stop();
	            _mediaPlayer.release();
	            _mediaPlayer = null;
	            // TODO - consider adding string vibration
	        }
	    }
	    
	    _isMediaPlayerStarted = true;
	}

	/**
	 * Start the media player. The method will do nothing unless an instance of the media player has already been created.
	 * @param context
	 * @param audioManager
	 * @throws IOException
	 */
    private void startMediaPlayer(Context context, AudioManager audioManager) throws IOException {
    	if(_mediaPlayer == null) {
    		return;
    	}
    	
    	_mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
//        player.setLooping(true);
    	_mediaPlayer.prepare();
        audioManager.requestAudioFocus(null,
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        _mediaPlayer.start();
    }

	/**
	 * Stop the media player from producing any sound and dispose of the media player.
	 * @param context
	 */
	public void stopMediaPlayer(Context context) {
		if(_mediaPlayer != null) {
			_mediaPlayer.stop();
		    AudioManager audioManager = (AudioManager)
		            context.getSystemService(Context.AUDIO_SERVICE);
		    audioManager.abandonAudioFocus(null);
		    _mediaPlayer.release();
		    _mediaPlayer = null;	
		}
	    
	    _isMediaPlayerStarted = false;
	}

	/**
	 * Provide a fallback ringtone in case the originally intended ringtone cannot be loaded.
	 */
	private Uri provideFallbackAlarmNoise() {
		Uri fallBackAlarmNoiseUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
	    // Fall back on the default alarm if the database does not have an
	    // alarm stored.
	    if (fallBackAlarmNoiseUri == null) {
	        fallBackAlarmNoiseUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
	    }
	    
		return fallBackAlarmNoiseUri;
	}

	private void setDataSourceFromResource(Context context, int res) throws IOException {
	    AssetFileDescriptor assetFileDescriptor = context.getResources().openRawResourceFd(res);
	    if (assetFileDescriptor != null) {
	        _mediaPlayer.setDataSource(assetFileDescriptor.getFileDescriptor(), assetFileDescriptor.getStartOffset(), assetFileDescriptor.getLength());
	        assetFileDescriptor.close();
	    }
	}
	
	public static class Volumes {
		// Volume suggested by media team for in-call alarms.
	    private static final double IN_CALL_VOLUME = 0.125;
		
	    private static Preferences _preferences = new Preferences();
		
		public static double getPreferencesLevel() {
			if(_preferences.isSoundOn()) {
	    		return (double)_preferences.getVolumeProgess() / 100;
	    	}
			
			return 0;
		}
		
		public static double getInCallLevel() {
			return IN_CALL_VOLUME;
		}
	}
}
