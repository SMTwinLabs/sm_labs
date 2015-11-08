package com.alexlabs.trackmovement;

import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;

public class SettingsFragment extends PreferenceFragment {
	private VibrationManager _vibrationManager = new VibrationManager();
	private Handler _vibrationDemoHandler = new Handler();
	private Runnable _stopVibrationDemoRunnable = new Runnable() {
		
		@Override
		public void run() {
			_vibrationManager.stop(getActivity());
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		getPreferenceManager().setSharedPreferencesName(Preferences.PREF_NAME);
		addPreferencesFromResource(R.xml.settings_preferences);
		
		// Monitor the 'Vibration' preference
		findPreference(getActivity().getString(R.string.vibration_toggle_pref)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				// Remove any existing runnables
				_vibrationDemoHandler.removeCallbacks(_stopVibrationDemoRunnable);

				// Start the device vibration only if the checkbox is selected
				if((Boolean) newValue) {
					// Start the device vibration.
					_vibrationManager.start(getActivity());
				}			

				// Stop the alarm vibration after a short delay
				_vibrationDemoHandler.postDelayed(_stopVibrationDemoRunnable, 1000);
				
				return true;
			}
		});
	}
}
