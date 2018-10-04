package com.alexlabs.trackmovement;

import android.app.Application;

public class App extends Application{
	private static App _instance;
	
	public App() {
		_instance = this;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
	}
	
	public static App instance(){		
		return _instance;
	}
}
