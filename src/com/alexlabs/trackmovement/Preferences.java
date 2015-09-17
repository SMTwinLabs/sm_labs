package com.alexlabs.trackmovement;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;

public class Preferences {
	public static final String PREF_NAME = "timerPref";
	
	// Preference objects
	private static Preferences _instance;
	private Context _context;
	private Resources _resources;
	private SharedPreferences _preferences;
	private SharedPreferences.Editor _preferenceEditor;
	private boolean _isDirty;
	
	// Preference Settings
	private boolean _isSoundOn;
	private boolean _isVibrationOn;
	
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
	
	private void init() {		
		_isSoundOn = _preferences.getBoolean(_resources.getString(R.string.sound_toggle_pref), true);
		_isVibrationOn = _preferences.getBoolean(_resources.getString(R.string.vibration_toggle_pref), true);
	}
	
	public boolean isSoundOn() {
		return _isSoundOn;
	}
	
	public boolean isVibrationOn() {
		return _isVibrationOn;
	}
}
