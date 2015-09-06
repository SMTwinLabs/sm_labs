package com.alexlabs.trackmovement;

import android.app.Activity;
import android.content.Context;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

public class UIUtils {

	public static boolean isLandscape(Activity activity) {
		Display display = ((WindowManager)activity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		int rotation = display.getRotation();
		if(rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270)
			return true;
		
		return false;
	}
	
	public static void showToast(Context context, int textResId) {
		Toast.makeText(context, context.getString(textResId), Toast.LENGTH_SHORT).show();
	}

	public static void showSetNewTimeToast(Activity activity) {
		LayoutInflater inflater = activity.getLayoutInflater();
		View toastView = inflater.inflate(R.layout.edit_mode_toast_promt_layout,
		                               (ViewGroup) activity.findViewById(R.id.relativeLayout1));
	
		Toast toast = new Toast(activity.getBaseContext());
		toast.setView(toastView);
		toast.setDuration(Toast.LENGTH_LONG);
		toast.show();
	}
}
