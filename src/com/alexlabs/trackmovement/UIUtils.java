package com.alexlabs.trackmovement;

import android.app.Activity;
import android.content.Context;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

public class UIUtils {

	public static boolean isLandscape(Activity activity) {
		Display display = ((WindowManager)activity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		int rotation = display.getRotation();
		if(rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270)
			return true;
		
		return false;
	}
}
