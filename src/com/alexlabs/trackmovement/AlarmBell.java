package com.alexlabs.trackmovement;

import java.io.IOException;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.media.MediaPlayer.OnErrorListener;
import android.net.Uri;
import android.os.Vibrator;

public class AlarmBell {
	// Volume suggested by media team for in-call alarms.
    private static final float IN_CALL_VOLUME = 0.125f;
    
    private static MediaPlayer s_MediaPlayer;

    
    public static MediaPlayer getMediaPlayer(){
    	if(s_MediaPlayer == null){
    		s_MediaPlayer = new MediaPlayer();
    	}
    	
    	return s_MediaPlayer;
    }
    
    private static void dispose(){
    	s_MediaPlayer = null;
    }

    public static void stop(Context context) {
        if (getMediaPlayer().isPlaying()) {
            // Stop audio playing
            if (s_MediaPlayer != null) {
            	getMediaPlayer().stop();
                AudioManager audioManager = (AudioManager)
                        context.getSystemService(Context.AUDIO_SERVICE);
                audioManager.abandonAudioFocus(null);
                getMediaPlayer().release();
	            dispose();
            }
        }

        ((Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE)).cancel();
    }

    public static void start(final Context context, boolean inTelephoneCall) {
        // Make sure we are stop before starting
        stop(context);
        
        getMediaPlayer().setOnErrorListener(new OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                AlarmBell.stop(context);
                return true;
            }
        });

        try {
            // Check if we are in a call. If we are, use the in-call alarm
            // resource at a low volume to not disrupt the call.
            if (inTelephoneCall) {
            	getMediaPlayer().setVolume(IN_CALL_VOLUME, IN_CALL_VOLUME);
            } 
            
            setDataSourceFromResource(context, R.raw.old_clock_ringing_short);
            
            startAlarm(context, getMediaPlayer());
        } catch (Exception ex) {
            // The alarmNoise may be on the SD card which could be busy right
            // now. Use the fallback ringtone.
            try {
                // Must reset the media player to clear the error state.
            	getMediaPlayer().reset();
            	getMediaPlayer().setDataSource(context, provideFallBackAlarmNoise());
                startAlarm(context, s_MediaPlayer);
            } catch (Exception ex2) {
            	
            	// No rigtones are available, so the user will not hear anything.
            	getMediaPlayer().stop();
                getMediaPlayer().release();
	            dispose();
	            // TODO - consider adding string vibration
            }
        }       
        
        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(new long[] {500, 500}, 0);
    }

	private static Uri provideFallBackAlarmNoise() {
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
            getMediaPlayer().setDataSource(assetFileDescriptor.getFileDescriptor(), assetFileDescriptor.getStartOffset(), assetFileDescriptor.getLength());
            assetFileDescriptor.close();
        }
    }
}
