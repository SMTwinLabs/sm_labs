package com.alexlabs.trackmovement.dialogs;

import android.content.Context;
import android.os.Handler;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.alexlabs.trackmovement.MediaPlayerManager;
import com.alexlabs.trackmovement.Preferences;
import com.alexlabs.trackmovement.R;

/**
 * This class establishes a custom preference dialog for adjusting the sound volume of the alarm bell.
 * When selecting a volume level, a sound is produced, thus allowing the user to decide if the
 * selected volume level is appropriate.
 */
public class AdjustVolumeDialog extends DialogPreference {
	
	private MediaPlayerManager _mediaPlayerManager = new MediaPlayerManager();
	
	/**
	 * The current progress of the seekbar.
	 */
	private int _progress;
	
	/**
	 * The seekbar employed to provide volume level control.
	 */
	private SeekBar _seekBar;
	
	/**
	 * A text view that displays the selected volume level from the seekbar.
	 */
	private TextView _volumeProgressTextView;
	
	/**
	 * Schedules a short sound when the user finishes selecting a volume level. When simply dragging the slider of
	 * the seekbar, no volume is produced until the user releases the slider.
	 */
	private Handler alaramBellDemoHandler = new Handler();
	
	/**
	 * Stop the sound of the demo alarm bell.
	 */
	private Runnable stopAlarmBellDemoRunnable = new Runnable() {
		
		@Override
		public void run() {
			_mediaPlayerManager.stop(getContext());
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
		
		// If the user confirms the changes, the new changes to the preferences are saved.
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
		
		// Display the current volume label.
		_volumeProgressTextView.setText("" + _progress);
		_seekBar.setProgress(_progress);
		
		// When a change on the volume label is performed, produce a short sound. The sound is played once the
		// user releases control of the slider of the seekbar.
		_seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {				
				_mediaPlayerManager.start(getContext(), (double)_progress / 100, prefs.getRingtoneResId());
				// Stop the noise after a short delay.
				alaramBellDemoHandler.postDelayed(stopAlarmBellDemoRunnable, 1000);
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// If the user edits the volume level while the sound from the previous change is still playing,
				// stop the currently playing sound.
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
