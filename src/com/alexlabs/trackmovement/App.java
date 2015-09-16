package com.alexlabs.trackmovement;

import android.app.Application;

public class App extends Application{
	private static App _instance;
	
	public App() {
		_instance = this;
	}
	
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
	}
	
	public static App instance(){
		if(_instance == null){
			_instance = new App();
		}
		
		return _instance;
	}
}
