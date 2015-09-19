package com.alexlabs.trackmovement.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnKeyListener;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.DialogFragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.alexlabs.trackmovement.AlarmBell;
import com.alexlabs.trackmovement.CountDownTimerService;
import com.alexlabs.trackmovement.MainActivity;
import com.alexlabs.trackmovement.R;

public class ConfirmScheduledAramDialog extends DialogFragment{
	
	public static final String TAG = "confirmAlarm";
	
	private static final String MESSAGE_KEY = "messageResId";
	private static final String MESSENGER_KEY = "messenger";
	
	public ConfirmScheduledAramDialog() {
		// empty
	}
	
	public static ConfirmScheduledAramDialog newInstance(Messenger messenger, int messageResId){
		ConfirmScheduledAramDialog d = new ConfirmScheduledAramDialog();
		Bundle b = new Bundle();
		b.putInt(MESSAGE_KEY, messageResId);
		b.putParcelable(MESSENGER_KEY, messenger);
		d.setArguments(b);
		return d;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Dialog d = initDialog();
		d.setOnKeyListener(new OnKeyListener() {
			
			@Override
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				if(KeyEvent.KEYCODE_BACK == keyCode) {
					AlarmBell.sendStopAlarmNoiseAndVibrationMessage((Messenger) getArguments().getParcelable(MESSENGER_KEY));
					getActivity().finish();
					return true;
				}
				
				return false;
			}
		});
		return d;
	}
	
	private AlertDialog initDialog(){
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		
		// message
		LayoutInflater inflater = getActivity().getLayoutInflater();
		View v = inflater.inflate(R.layout.confirmation_dialog_view, null);
		TextView tv = (TextView) v.findViewById(R.id.confirmationTextView);
		tv.setText(getArguments().getInt(MESSAGE_KEY));
		builder.setView(v);
		
		builder.setPositiveButton(getActivity().getString(R.string.OK), new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Messenger countDownMessenger = getArguments().getParcelable(MESSENGER_KEY);
				
				// Notify the count down service that the alarm has been confirmed. 
				Message msg = Message.obtain(null, CountDownTimerService.MSG_DONE_USING_TIMER);
		        try {
		        	countDownMessenger.send(msg);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		return builder.create();
	}
}
