package com.alexlabs.trackmovement;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.Log;

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
	
	private static final String DEFAULT_RINGTONE_KEY = App.instance().getResources().getString(R.string.old_clock_ringing_short);
	private static final Map<String, Integer> _ringtones = new HashMap<String, Integer>(){{
		put(App.instance().getResources().getString(R.string.electronic_chime), R.raw.electronic_chime);
		put(App.instance().getResources().getString(R.string.old_clock_ringing_short), R.raw.old_clock_ringing_short);
		
	}};
	
	public Preferences(){
		_context = App.instance();
		_resources = _context.getResources();
		_preferences = _context.getSharedPreferences(PREF_NAME, Context.MODE_MULTI_PROCESS);
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
	
	public void onSaveVolumePreferences(int volumeProgess) {
		checkEditMode();
		_preferenceEditor.putInt(_resources.getString(R.string.volume_pref), volumeProgess);
		
		_isDirty = true;
	}
	
	public boolean isSoundOn() {
		return _preferences.getBoolean(_resources.getString(R.string.sound_toggle_pref), true);
	}
	
	public boolean isVibrationOn() {
		return _preferences.getBoolean(_resources.getString(R.string.vibration_toggle_pref), true);
	}
	
	public int getRingtone() {
		String key = _preferences.getString(_resources.getString(R.string.alarm_ringtone_pref), null);
		if(key == null || !_ringtones.containsKey(key)) {
			key = DEFAULT_RINGTONE_KEY;
		}
		return _ringtones.get(key);
	}
	
	public int getVolumeProgess(){
		return _preferences.getInt(_resources.getString(R.string.volume_pref), DEFAULT_VOLUME_PROGRESS);
	}
	
	public int getAlarmNoiseDuration() {
		return Integer.parseInt(_preferences.getString(_resources.getString(R.string.alarm_noise_duration_pref), _resources.getString(R.string.alarm_noise_duration_value)));
	}
	
	public boolean getShouldBeep() {
		return _preferences.getBoolean(_resources.getString(R.string.alarm_beep_pref), true);
	}
}
