package com.alexlabs.trackmovement;

import java.io.IOException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.media.MediaPlayer.OnErrorListener;
import android.net.Uri;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.Vibrator;

public class AlarmBell {
	// Volume suggested by media team for in-call alarms.
    private static final float IN_CALL_VOLUME = 0.125f;
    
    private static MediaPlayer s_MediaPlayer;
	private static RingerModeBroadcastRecever _ringerModeBroadcastReceiver;
	private boolean mediaPlayerStarted;
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

    /**
     * If the media player is playing, then Stops both the media player and vibration
     * @param context
     */
    public void stop(Context context) {
        if (mediaPlayerStarted) {
            // Stop audio playing
            stopMediaPlayer(context);
    	    unregisterRingerModeBroadcastReciver(context);
        }

        stopVibration(context);
	    
    }

	private void stopVibration(Context context) {
		((Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE)).cancel();
	}

    /**
     * Stop the media player from producing any sound and dispose the media player.
     * @param context
     */
	private void stopMediaPlayer(Context context) {
		s_MediaPlayer.stop();
	    AudioManager audioManager = (AudioManager)
	            context.getSystemService(Context.AUDIO_SERVICE);
	    audioManager.abandonAudioFocus(null);
	    s_MediaPlayer.release();
	    s_MediaPlayer = null;	
	    
	    mediaPlayerStarted = false;
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
        startMediaPlayer(context, inTelephoneCall);        
        registerRingerModeBroadcastReciver(context);
        
        startVibration(context);
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
    	if(s_MediaPlayer != null){
    		stopMediaPlayer(context);  
    	}
    	
		s_MediaPlayer = new MediaPlayer();
        
        s_MediaPlayer.setOnErrorListener(new OnErrorListener() {
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
            	s_MediaPlayer.setVolume(IN_CALL_VOLUME, IN_CALL_VOLUME);
            } 
            
            setDataSourceFromResource(context, R.raw.old_clock_ringing_short);
            
            startAlarm(context, s_MediaPlayer);
        } catch (Exception ex) {
            // The alarmNoise may be on the SD card which could be busy right
            // now. Use the fallback ringtone.
            try {
                // Must reset the media player to clear the error state.
            	s_MediaPlayer.reset();
            	s_MediaPlayer.setDataSource(context, provideFallbackAlarmNoise());
                startAlarm(context, s_MediaPlayer);
            } catch (Exception ex2) {
            	
            	// No rigtones are available, so the user will not hear anything.
            	s_MediaPlayer.stop();
                s_MediaPlayer.release();
                s_MediaPlayer = null;
	            // TODO - consider adding string vibration
            }
        }
        
        mediaPlayerStarted = true;
	}

	private static Uri provideFallbackAlarmNoise() {
		Uri fallBackAlarmNoiseUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        // Fall back on the default alarm if the database does not have an
        // alarm stored.
        if (fallBackAlarmNoiseUri == null) {
            fallBackAlarmNoiseUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        
		return fallBackAlarmNoiseUri;
	}
    
    private static void startAlarm(Context context, MediaPlayer player) throws IOException {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        // do not play alarms if stream volume is 0 (typically because ringer mode is silent).
        if (audioManager.getStreamVolume(AudioManager.STREAM_RING) != 0) {
            player.setAudioStreamType(AudioManager.STREAM_RING);
            player.setLooping(true);
            player.prepare();
            audioManager.requestAudioFocus(null,
                    AudioManager.STREAM_RING, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            player.start();
        }
    }
    
    private static void setDataSourceFromResource(Context context, int res)
            throws IOException {
        AssetFileDescriptor assetFileDescriptor = context.getResources().openRawResourceFd(res);
        if (assetFileDescriptor != null) {
            s_MediaPlayer.setDataSource(assetFileDescriptor.getFileDescriptor(), assetFileDescriptor.getStartOffset(), assetFileDescriptor.getLength());
            assetFileDescriptor.close();
        }
    }
    

	
	public static class RingerModeBroadcastRecever extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if(AudioManager.RINGER_MODE_CHANGED_ACTION.equals(intent.getAction())){
				AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
				
				// The audio output is on.
				if(AudioManager.RINGER_MODE_NORMAL == audioManager.getRingerMode()){
					instance().startMediaPlayer(context, false);
					
				// The audio output is off.
				} else {
					instance().stopMediaPlayer(context);
				}
			}
		}
		
	}
	
	private static void registerRingerModeBroadcastReciver(Context context) {
		final IntentFilter filter = new IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION);
		_ringerModeBroadcastReceiver = new RingerModeBroadcastRecever();
		context.registerReceiver(_ringerModeBroadcastReceiver, filter);
	}
	
	private static void unregisterRingerModeBroadcastReciver(Context context) {
		context.unregisterReceiver(_ringerModeBroadcastReceiver);
	}
	
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
