package com.alexlabs.trackmovement.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnKeyListener;
import android.os.Bundle;
import android.os.IBinder;
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
import com.alexlabs.trackmovement.IDialogDismissListener;
import com.alexlabs.trackmovement.MainActivity;
import com.alexlabs.trackmovement.R;

public class ConfirmScheduledAramDialog extends DialogFragment{
	
	public static final String TAG = "confirmAlarm";
	
	private static final String MESSAGE_KEY = "messageResId";
	
	private Messenger _countDownService;
	
	private ServiceConnection _serviceConnetcion = new ServiceConnection() {
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			_countDownService = null;
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			_countDownService = new Messenger(service);
		}
	};
	
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
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		getActivity().bindService(new Intent(getActivity(), CountDownTimerService.class),
                _serviceConnetcion, Context.BIND_AUTO_CREATE);
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Dialog d = initDialog();
		d.setOnKeyListener(new OnKeyListener() {
			
			@Override
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				if(KeyEvent.KEYCODE_BACK == keyCode) {
					AlarmBell.sendStopAlarmNoiseAndVibrationMessage(_countDownService);
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
				// Notify the count down service that the alarm has been confirmed. 
				Message msg = Message.obtain(null, CountDownTimerService.MSG_DONE_USING_TIMER);
				if(_countDownService != null) {
			        try {
			        	_countDownService.send(msg);
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				if(getActivity() instanceof IDialogDismissListener) {
					((IDialogDismissListener)getActivity()).notifyDialogClosed();
				}
			}
		});
		return builder.create();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		getActivity().unbindService(_serviceConnetcion);
	}
}
