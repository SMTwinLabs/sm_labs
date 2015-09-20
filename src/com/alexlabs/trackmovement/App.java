package com.alexlabs.trackmovement;

import java.util.ArrayList;

import android.app.Activity;
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
