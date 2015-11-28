package com.alexlabs.trackmovement.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.Handler;

import com.alexlabs.trackmovement.MediaPlayerManager;
import com.alexlabs.trackmovement.Preferences;
import com.alexlabs.trackmovement.R;

public class SelectRingtonePreferenceDialog extends DialogFragment{
	public static final String TAG = "SelectRingtonePreferenceDialog";

	private MediaPlayerManager _mediaPlayerManager = new MediaPlayerManager();
	
	private Runnable _stopRingtoneDemoRunnable = new Runnable() {
		
		@Override
		public void run() {
			_mediaPlayerManager.stop(getActivity());
		}
	};
	
	private Handler _ringtoneDemoHandler = new Handler();
	
	public SelectRingtonePreferenceDialog() {
		// NOTE: This empty constructor is a must for recreating the dialog
		// on life cycle events.
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.choose_ringtone_dialog_title);
		builder.setSingleChoiceItems(RingtoneUtils.getRingtonePreferenceNames(), RingtoneUtils.findSavedRingtonePos(new Preferences()), new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				_ringtoneDemoHandler.removeCallbacks(_stopRingtoneDemoRunnable);
				_mediaPlayerManager.start(getActivity(), 1.0, RingtoneUtils.gerResId(which), true);
				_ringtoneDemoHandler.postDelayed(_stopRingtoneDemoRunnable, 2000);
			}
		})
		.setPositiveButton(android.R.string.ok, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				int pos = ((AlertDialog)dialog).getListView().getCheckedItemPosition();
				Preferences prefs = new Preferences();
				try {
					prefs.startEdit();
					prefs.onSaveRingtonePreferences(RingtoneUtils.gerResId(pos));
				} finally {
					prefs.finishEdit();
				}
			}
		})
		.setNegativeButton(android.R.string.cancel, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// Do nothing.
			}
		});
		
		return builder.create();
	}
}
