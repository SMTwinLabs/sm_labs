package com.alexlabs.trackmovement.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.alexlabs.trackmovement.CountDownTimerService;
import com.alexlabs.trackmovement.MainActivity;
import com.alexlabs.trackmovement.R;

public class ConfirmScheduledAramDialog extends DialogFragment{
	
	public static final String TAG = "confirmAlarm";
	
	private static final String MESSAGE_KEY = "messageResId";
	
	public ConfirmScheduledAramDialog() {
		// empty
	}
	
	public static ConfirmScheduledAramDialog newInstance(int messageResId){
		ConfirmScheduledAramDialog d = new ConfirmScheduledAramDialog();
		Bundle b = new Bundle();
		b.putInt(MESSAGE_KEY, messageResId);
		d.setArguments(b);
		return d;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		return initDialog();
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
				if(getActivity() instanceof MainActivity) {
					Messenger countDownMessenger = ((MainActivity)getActivity()).getCountDownTimerService();
					
					// Notify the count down service that the alarm has been confirmed. 
					Message msg = Message.obtain(null, CountDownTimerService.MSG_DONE_USING_TIMER);
			        try {
			        	countDownMessenger.send(msg);
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		});
		return builder.create();
	}

}
