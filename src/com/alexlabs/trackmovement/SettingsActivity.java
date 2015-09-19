package com.alexlabs.trackmovement;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;

public class SettingsActivity extends Activity{

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Display the fragment as the main content.
		Fragment existingFragment = getFragmentManager().findFragmentById(android.R.id.content);
        if (existingFragment == null || !existingFragment.getClass().equals(SettingsFragment.class)){
	        getFragmentManager().beginTransaction()
	                .replace(android.R.id.content, new SettingsFragment())
	                .commit();
        }
	}
}
