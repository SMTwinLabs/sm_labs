package com.alexlabs.trackmovement.dialogs;

import android.content.Context;
import android.media.AudioManager;
import android.os.Handler;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.alexlabs.trackmovement.AlarmBell;
import com.alexlabs.trackmovement.Preferences;
import com.alexlabs.trackmovement.R;

public class AdjustVolumeDialog extends DialogPreference {
	
	private int _progress;
	private SeekBar _seekBar;	
	private TextView _volumeProgressTextView;
	
	private Handler alaramBellDemoHandler = new Handler();
	private Runnable stopAlarmBellDemoRunnable = new Runnable() {
		
		@Override
		public void run() {
			AlarmBell.instance().stopAlarmBell(getContext());
		}
	};

	public AdjustVolumeDialog(Context context, AttributeSet attr) {
		super(context, attr);
		
		setDialogLayoutResource(R.layout.adjust_volume_dialog_layout);
		setPositiveButtonText(R.string.OK);
		setNegativeButtonText(R.string.cancel);
	}
	
	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);
		
		if(positiveResult) {
			Preferences prefs = new Preferences();
			prefs.startEdit();
			prefs.onSaveVolumePreferences(_progress);
			prefs.finishEdit();
		}
			
	}
	
	@Override
    protected void onBindDialogView(View view) {
	    super.onBindDialogView(view);
	   
	    _volumeProgressTextView = (TextView) view.findViewById(R.id.volumeProgressTextView);
	    _seekBar = (SeekBar) view.findViewById(R.id.volumeControl);
	    
	    final Preferences prefs = new Preferences();
		_progress = prefs.getVolumeProgess();
		
		_volumeProgressTextView.setText("" + _progress);
		_seekBar.setProgress(_progress);
		
		
		
		_seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
				int volume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)*_progress/100;
				audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.ADJUST_SAME);
				
				AlarmBell.instance().startAlarmBell(getContext(), false, audioManager);
				alaramBellDemoHandler.postDelayed(stopAlarmBellDemoRunnable, 1000);
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				alaramBellDemoHandler.removeCallbacks(stopAlarmBellDemoRunnable);
			}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				_progress = progress;
				_volumeProgressTextView.setText("" + _progress);
			}
		});		
    }
}
