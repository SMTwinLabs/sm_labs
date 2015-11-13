package com.alexlabs.trackmovement;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;

/**
 * Does not save an instance. This class only access the preference file every time the constructor is called.
 * This is so, because multiple processes can access the preference file, while a singleton is not unique when
 * multiple processes are concerned. 
 * @author Alex
 *
 */
public class Preferences {
	public static final String PREF_NAME = "timerPref";
	private static final int DEFAULT_VOLUME_PROGRESS = 50;
	
	// Preference objects
	private Context _context;
	private Resources _resources;
	private SharedPreferences _preferences;
	private SharedPreferences.Editor _preferenceEditor;
	private boolean _isDirty;
	
	// Preference Settings
	private boolean _isSoundOn;
	private boolean _isVibrationOn;
	private int _ringtoneResId;
	private int _volumeProgress;
	
	public Preferences(){
		_context = App.instance();
		_resources = _context.getResources();
		_preferences = _context.getSharedPreferences(PREF_NAME, Context.MODE_MULTI_PROCESS);
		
		init();
	}

	public SharedPreferences getPreferences() {
		return _preferences;
	}
	
	public synchronized void startEdit() {
		if (_preferenceEditor != null)
			throw new IllegalStateException();

		_preferenceEditor = _preferences.edit();
	}
	
	public synchronized void finishEdit() {
		if (_isDirty) {
			_preferenceEditor.commit();
			_isDirty = false;
		}
		_preferenceEditor = null;			
	}
	
	private void checkEditMode() {
		if (_preferenceEditor == null)
			throw new IllegalStateException();
	}
	
	private void init() {		
		_isSoundOn = _preferences.getBoolean(_resources.getString(R.string.sound_toggle_pref), true);
		_isVibrationOn = _preferences.getBoolean(_resources.getString(R.string.vibration_toggle_pref), true);
		_ringtoneResId = R.raw.old_clock_ringing_short;
		_volumeProgress = _preferences.getInt(_resources.getString(R.string.volume_pref), DEFAULT_VOLUME_PROGRESS);
	}
	
	public void onSaveVolumePreferences(int volumeProgess) {
		checkEditMode();
		_volumeProgress = volumeProgess;
		_preferenceEditor.putInt(_resources.getString(R.string.volume_pref), _volumeProgress);
		
		_isDirty = true;
	}
	
	public boolean isSoundOn() {
		return _isSoundOn;
	}
	
	public boolean isVibrationOn() {
		return _isVibrationOn;
	}
	
	public int getRingtone() {
		return _ringtoneResId;
	}
	
	public int getVolumeProgess(){
		return _volumeProgress;
	}
	
	public int getAlarmNoiseDuration() {
		return Integer.parseInt(_preferences.getString(_resources.getString(R.string.alarm_noise_duration_pref), _resources.getString(R.string.alarm_noise_default_duration_label)));
	}
}
