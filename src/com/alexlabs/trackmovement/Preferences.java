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
	private SharedPreferences _sharedPreferences;
	private SharedPreferences.Editor _preferenceEditor;
	private boolean _isDirty;
	
	// Preference Settings
	private boolean _isSoundOn;
	private boolean _isVibrationOn;
	
	public Preferences(){
		_context = App.instance();
		_resources = _context.getResources();
		_sharedPreferences = _context.getSharedPreferences(PREF_NAME, 0);
		
		init();
	}
	
	public static Preferences instance(){
		if(_instance == null) {
			_instance = new Preferences();
		}
		
		return _instance;
	}

	public SharedPreferences getSharedPreferences() {
		return _sharedPreferences;
	}
	
	public synchronized void startEdit() {
		if (_preferenceEditor != null)
			throw new IllegalStateException();

		_preferenceEditor = _sharedPreferences.edit();
	}
	
	public synchronized void finishEdit() {
		if (_isDirty) {
			_preferenceEditor.commit();
			_isDirty = false;
		}
		_preferenceEditor = null;			
	}
	
	private void init() {
		if(_preferenceEditor != null){
			throw new IllegalArgumentException();
		}
		
		_isSoundOn = _sharedPreferences.getBoolean(_resources.getString(R.string.sound_toggle_pref), true);
		_isVibrationOn = _sharedPreferences.getBoolean(_resources.getString(R.string.vibration_toggle_pref), true);
	}
	
	public void updatePreferences(){
		init();
	}
	
	public boolean isSoundOn() {
		return _isSoundOn;
	}
	
	public boolean isVibrationOn() {
		return _isVibrationOn;
	}
}
