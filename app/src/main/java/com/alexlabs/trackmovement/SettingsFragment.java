package com.alexlabs.trackmovement;

import java.util.Timer;
import java.util.TimerTask;

import com.alexlabs.trackmovement.dialogs.SelectRingtonePreferenceDialog;
import com.alexlabs.trackmovement.utils.RingtoneUtils;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
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
import android.view.View;
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
			} else if(key.equals(getActivity().getResources().getString(R.string.keep_screen_awake_pref))) {
				updateKeepScreenAwake();
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
		
		// If the device has no vibrator, the preference is loaded with the disabled layout resource.
		if(!mVibrator.hasVibrator()) {
			vibrationPreference.setLayoutResource(R.layout.preference_disabled);
		}
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
	
	public void updateKeepScreenAwake() {
		// A new MainActivity activity is created that will clear all existing activities.
		Intent mStartActivity = new Intent(getActivity(), MainActivity.class);
		mStartActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		// After the preference has been changed the app needs to be restarted so that
		// the change can take effect. However, the restart cannot happen immediately
		// because otherwise the change to the preference will not be reflected. Therefore,
		// a task is designated to do the restarting of the app and it is started with a
		// certain delay.
		Timer timer = new Timer();
        timer.schedule(new RestartAppTask(getActivity().getBaseContext(), mStartActivity), 100);
	}
	
	/**
	 * RestartAppTask is responsible for closing the entire application
	 * and starting a new activity.
	 * @author Alex
	 *
	 */
	class RestartAppTask extends TimerTask {
		private Intent _intent;
		private Context _ctx;
		
		public RestartAppTask() {}
		
		public RestartAppTask(Context ctx, Intent i) {
			_intent = i;
			_ctx = ctx;
		}
		
        public void run() {
            System.exit(0);
            _ctx.startActivity(_intent);
            cancel();
        }
    }  
	
	@Override
	public void onDestroy() {
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(_preferenceChangeListener);
		super.onDestroy();
	}
}
