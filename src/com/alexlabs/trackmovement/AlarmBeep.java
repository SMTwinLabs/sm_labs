package com.alexlabs.trackmovement;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.media.MediaPlayer;

public class AlarmBeep {
	private static final double MINIMUM_PROGRESS = 0.65;

	private MediaPlayerManager _mediaPlayerManager;

	public static final int BEEP_DELAY_INTERVAL = 20;//FIXME: make 20 for production
	private static AlarmBeep _alarmBeep;
	
	private AlarmBeep() {
		_mediaPlayerManager = new MediaPlayerManager();
	}
	
	public static AlarmBeep instance() {
		if(_alarmBeep == null) {
			_alarmBeep = new AlarmBeep();
		}
		
		return _alarmBeep;
	}
	
	public void beep(final Context context) {
		Preferences preferences = new Preferences();
		// The level is between 0 and 1.
		double level = preferences.getVolumeProgess()/100;
		
		// The minimum sound progress is set only when the selected sound is lower than the minimum audible
		// threshold or when the sound option is turned off.
		if(level < MINIMUM_PROGRESS || !preferences.isSoundOn()) {
			level = MINIMUM_PROGRESS;
		}
		
		_mediaPlayerManager.start(context, level, R.raw.electronic_chime);
		ScheduledExecutorService execService = Executors.newScheduledThreadPool(1);
		execService.schedule(new Runnable() {
			
			@Override
			public void run() {
				_mediaPlayerManager.stop(context);
			}
		}, getBeepDuration(context), TimeUnit.MILLISECONDS);
	}
	
	public int getBeepDuration(Context context) {		
		MediaPlayer mp = MediaPlayer.create(context, R.raw.electronic_chime);
		return mp.getDuration();
	}

}
