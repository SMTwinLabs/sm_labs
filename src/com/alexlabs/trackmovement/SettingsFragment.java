package com.alexlabs.trackmovement;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

public class SettingsFragment extends PreferenceFragment {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		getPreferenceManager().setSharedPreferencesName(Preferences.PREF_NAME);
		addPreferencesFromResource(R.xml.settings_preferences);
		findPreference(getResources().getString(R.string.sound_toggle_pref)).setOnPreferenceClickListener(
				new Preference.OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference preference) {
						return false;
					}
				});
				
	}
}
