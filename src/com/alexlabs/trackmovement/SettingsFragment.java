package com.alexlabs.trackmovement;

import com.alexlabs.trackmovement.dialogs.SelectRingtonePreferenceDialog;
import com.alexlabs.trackmovement.utils.RingtoneUtils;

import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.util.DisplayMetrics;
import android.widget.TextView;

public class SettingsFragment extends PreferenceFragment {
	private static final boolean SHOULD_DISPLAY_TEST_INFO = false;
	
	
	private VibrationManager _vibrationManager = new VibrationManager();
	private Handler _vibrationDemoHandler = new Handler();
	private OnSharedPreferenceChangeListener _preferenceChangeListener = new OnSharedPreferenceChangeListener() {
		
		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
				String key) {
			if(key.equals(getActivity().getResources().getString(R.string.volume_pref))
					|| key.equals(getActivity().getResources().getString(R.string.sound_toggle_pref))) {
				updateVolumePref();
			} else if(key.equals(getActivity().getResources().getString(R.string.alarm_noise_duration_pref))) {
				updateAlarmDurationPref();
			} else if(key.equals(getActivity().getResources().getString(R.string.alarm_ringtone_pref))) {
				updateRingtonePreferenceSummary();
			} else {
				// something is wrong
			}
		}
	};
	
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
		initVibrationPreferences();
		initRingtonePreference();
		
		// Set the summary text for the 'Volume' preference.
		updateVolumePref();
		updateAlarmDurationPref();
		updateRingtonePref();
		updateRingtonePreferenceSummary();
		initTestInfoPreference();
		
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(_preferenceChangeListener);
	}

	private void initTestInfoPreference() {
		Preference testInfoPreference = findPreference(getActivity().getString(R.string.test_info_pref));
		testInfoPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			
			@Override
			public boolean onPreferenceClick(Preference preference) {
				AlertDialog.Builder ad = new AlertDialog.Builder(getActivity());
				TextView tv  = new TextView(getActivity());
				DisplayMetrics displayMetrics = getActivity().getResources().getDisplayMetrics();

		        float dpHeight = displayMetrics.heightPixels / displayMetrics.density;
		        float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
		        
				tv.setText(String.format("density = %f\nheight = %f\nwidth = %f", displayMetrics.density, dpHeight, dpWidth));
				
				ad.setView(tv);
				ad.create().show();
				return true;
			}
		});
		
		// TODO: remove the test info for production
		PreferenceScreen ps = (PreferenceScreen) findPreference(getResources().getString(R.string.settings_key));
		if(!SHOULD_DISPLAY_TEST_INFO) {
			ps.removePreference(testInfoPreference);
		}
	}

	private void initRingtonePreference() {
		Preference alarmRingtonePreference = findPreference(getActivity().getString(R.string.alarm_ringtone_pref));
		alarmRingtonePreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			
			@Override
			public boolean onPreferenceClick(Preference preference) {
				DialogFragment ringtonesDialogFragment = new SelectRingtonePreferenceDialog();
				ringtonesDialogFragment.show(getFragmentManager(), SelectRingtonePreferenceDialog.TAG);
				return true;
			}
		});
	}
	
	private void updateRingtonePreferenceSummary() {
		Preference alarmRingtonePreference = findPreference(getActivity().getString(R.string.alarm_ringtone_pref));
		Preferences prefs = new Preferences();
		
		alarmRingtonePreference.setSummary(String.format("Current: %s", RingtoneUtils.getRingtoneName(prefs.getRingtoneResId())));
	}

	private void initVibrationPreferences() {
		Preference vibrationPreference = findPreference(getActivity().getString(R.string.vibration_toggle_pref));
		vibrationPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				// Remove any existing runnables
				_vibrationDemoHandler.removeCallbacks(_stopVibrationDemoRunnable);
				// Start the device vibration only if the checkbox is selected
				if((Boolean) newValue) {
					// Start the device vibration.
					_vibrationManager.stop(getActivity());
					_vibrationManager.start(getActivity());
				}			

				// Stop the alarm vibration after a short delay
				_vibrationDemoHandler.postDelayed(_stopVibrationDemoRunnable, 1000);
				
				return true;
			}
		});
		
		// NOTE: Not all devices have vibrators. Therefore a check for a vibrator needs to be done. 
		Vibrator mVibrator = (Vibrator)getActivity().getSystemService(Context.VIBRATOR_SERVICE);
		vibrationPreference.setEnabled(mVibrator.hasVibrator());
	}
	
	/**
	 * Shows the user the current volume's level.
	 */
	private void updateVolumePref() {
		Preferences p = new Preferences();
		Preference volumePreference = findPreference(getActivity().getString(R.string.volume_pref));
		
		// Update summary.
		volumePreference.setSummary(String.format("Volume: %d%%", p.getVolumeProgess()));
		
		// Enabled only if the user has turned on the 'Sound' option.
		volumePreference.setEnabled(p.isSoundOn());
	}
	
	public void updateAlarmDurationPref() {
		Preferences p = new Preferences();
		Preference alarmNoiseDurationPreference = findPreference(getActivity().getString(R.string.alarm_noise_duration_pref));
		alarmNoiseDurationPreference.setSummary(String.format("Duration: %d seconds", p.getAlarmNoiseDuration()));
	}
	
	public void updateRingtonePref() {
		Preferences p = new Preferences();
		Preference alarmRingtonePreference = findPreference(getActivity().getString(R.string.alarm_ringtone_pref));
		alarmRingtonePreference.setEnabled(p.isSoundOn());
	}
	
	@Override
	public void onDestroy() {
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(_preferenceChangeListener);
		super.onDestroy();
	}
}
