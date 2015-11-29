package com.alexlabs.trackmovement;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.telephony.TelephonyManager;
import android.widget.Toast;

public class CallBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
    	AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    	Toast.makeText(context, audioManager.getMode(), Toast.LENGTH_SHORT).show();
//        if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_RINGING)) {
//            // This code will execute when the phone has an incoming call
//
//            // get the phone number 
//            Toast.makeText(context, "Call received", Toast.LENGTH_SHORT).show();
//
//        } else if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(
//                TelephonyManager.EXTRA_STATE_IDLE)
//                || intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(
//                        TelephonyManager.EXTRA_STATE_OFFHOOK)) {
//            // This code will execute when the call is disconnected
//            Toast.makeText(context, "Detected call hangup event", Toast.LENGTH_LONG).show();
//
//        }
    }
}
