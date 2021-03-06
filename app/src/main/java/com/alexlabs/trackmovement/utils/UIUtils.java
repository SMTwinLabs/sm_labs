package com.alexlabs.trackmovement.utils;

import com.alexlabs.trackmovement.MainActivity;
import com.alexlabs.trackmovement.R;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

public class UIUtils {
	
	private static Toast _setNewTimeToast;

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
		try{
			if(_setNewTimeToast == null || !_setNewTimeToast.getView().isShown()) {
				_setNewTimeToast = new Toast(activity.getBaseContext());
				_setNewTimeToast.setView(toastView);
				_setNewTimeToast.setDuration(Toast.LENGTH_LONG);
				_setNewTimeToast.show();
			}
		} catch (Exception e) {
			// invisible if exception
        }
	}

	public  static void sendNotification(Context context, Service service, int notificationId, String text) {
		// Create the Notification.Builder.
		NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
			.setSmallIcon(R.drawable.ic_timelapse_white)
			.setContentText(text)
			.setContentTitle(context.getString(R.string.app_name));
		
		// Create intent.
		// NOTE: to avoid opening a new instance of the MainActivity every time the notification
		// is clicked, set android:launchMode="singleTop" to the activity in the manifest.
		Intent notificationIntent = new Intent(context, MainActivity.class);
		
		// Create pending intent to take us to the app after the notification is clicked.
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
		notificationBuilder.setContentIntent(pendingIntent);
		
		// Set the text in the notification bar too.
		notificationBuilder.setTicker(text);
		
		// Build the notification.
		Notification notification = notificationBuilder.build();		
		
		// Start the notification in the foreground.
		service.startForeground(notificationId, notification);
	}
}
