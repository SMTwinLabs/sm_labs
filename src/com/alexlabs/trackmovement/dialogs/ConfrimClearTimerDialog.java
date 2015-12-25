package com.alexlabs.trackmovement.dialogs;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.alexlabs.trackmovement.CountDownTimerService;
import com.alexlabs.trackmovement.R;

public class ConfrimClearTimerDialog extends DialogFragment {
	public static final String TAG = "confirmClear";	
	
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

	public ConfrimClearTimerDialog() {
		// empty
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		getActivity().bindService(new Intent(getActivity(), CountDownTimerService.class),
                _serviceConnetcion, Context.BIND_AUTO_CREATE);
	}
	
	@SuppressLint("InflateParams") @Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
	
		// message
		LayoutInflater inflater = getActivity().getLayoutInflater();
		View v = inflater.inflate(R.layout.confirmation_dialog_view, null);
		TextView tv = (TextView) v.findViewById(R.id.confirmationTextView);
		tv.setText(getResources().getString(R.string.confirm_timer_clear));
		builder.setView(v);
		
		builder.setPositiveButton(R.string.yes, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Message msg = Message.obtain(null, CountDownTimerService.MSG_CLEAR_TIMER);
				if(_countDownService != null) {
			        try {
			        	_countDownService.send(msg);
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		});
		
		builder.setNegativeButton(R.string.cancel, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();			
			}
		});
		
		return builder.create();
	}
}
