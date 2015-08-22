package com.alexlabs.trackmovement;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.Log;

public class CountDownTimerService extends Service{
	
	// common
	static final int MSG_REGISTER_CLIENT = 0;
	static final int MSG_UNREGISTER_CLIENT = 1;
	static final int MSG_SET_MODE = 2;
	static final int MSG_START_TIMER = 3;
	static final int MSG_PAUSE_TIMER = 4;
	static final int MSG_UNPAUSE_TIMER = 5;
	static final int MSG_SET_SELECTED_MINUTE = 6;
	
	static final int MSG_GET_TIMER_INFO = 7;
	
	
	// modes
	static final int MODE_BASE = 10;
	static final int MODE_ACTIVE = 11; 
	static final int MODE_EDIT_TIME = 12;
	
	// states
	static final int TIMER_STATE_NONE = 20;
	static final int TIMER_STATE_STARTED = 21;
	static final int TIMER_STATE_STOPPED = 22;
	static final int TIMER_STATE_FINISHED = 23;
	
	static final int TIMER_CURRENT_MILLIS_UNTIL_FINISHED = 30;
	
	private int _timerState = TIMER_STATE_NONE;
	private int _mode = MODE_BASE;
	
	private int _selectedMinute;
	private long _millisUntilFinished;
	
	private Messenger _remoteClientMessenger;
	private CountDownTimer _countDownTimer;
	
	/**
    * Target we publish for clients to send messages to IncomingHandler.
    */
	final Messenger _serviceMessenger = new Messenger(new IncomingHandler());
	
	@SuppressLint("HandlerLeak") 
	class IncomingHandler extends Handler {
		
		@Override
		public void handleMessage(Message msg) {
			if(android.os.Debug.isDebuggerConnected())
				android.os.Debug.waitForDebugger();  // this line is key
			
			switch(msg.what) {		

			case MSG_REGISTER_CLIENT:
				_remoteClientMessenger = msg.replyTo;
				break;
				
			case MSG_UNREGISTER_CLIENT:
				_remoteClientMessenger = null;
				break;
				
			case MSG_SET_MODE:
				_mode = msg.arg1;
				break;

			case MSG_SET_SELECTED_MINUTE:
				_selectedMinute = msg.arg1;
				Log.d("ALEX_LABS", "" + _selectedMinute);
				break;
				
			case MSG_START_TIMER:
				_millisUntilFinished = _selectedMinute*1000*60;
				Log.d("ALEX_LABS", "" + _millisUntilFinished);
				startCountDown();	
				break;
			
			case MSG_PAUSE_TIMER:
				stopCountDown();
				break;
				
			case MSG_UNPAUSE_TIMER:
				startCountDown();
				break;
				
			case MSG_GET_TIMER_INFO:
				if(_remoteClientMessenger != null){
					try {
						// get everything
						Bundle data = new Bundle();
						data.putInt("mode", _mode);
						data.putBoolean("isTimerStarted", _timerState == TIMER_STATE_STARTED);
						data.putInt("selectedMinute", _selectedMinute);
						data.putInt("currnetMinute", TimerUtils.getMinuteFromMillisecnods(_millisUntilFinished));
						data.putInt("currnetSeconds", TimerUtils.getSecondsFromMillisecnods(_millisUntilFinished));
						Message infoMsg = Message.obtain(null, MSG_GET_TIMER_INFO);
						infoMsg.setData(data);
						_remoteClientMessenger.send(infoMsg);
					} catch (RemoteException e) {
						// TODO: handle exception
					}
				}
				break;
				
			default:
				super.handleMessage(msg);
			}
		}
	}	
	
	public void startCountDown() {
		if (_countDownTimer != null) {
			stopCountDown();
		} 
		
		initCountDownTimer();

		_countDownTimer.start();
		_timerState = TIMER_STATE_STARTED;
	}
	
	public void stopCountDown() {
		if (_countDownTimer != null) {
			_countDownTimer.cancel();
			_countDownTimer = null;

			_timerState = TIMER_STATE_STOPPED;	
		}
	}

	private void initCountDownTimer() {
		_countDownTimer = new CountDownTimer(_millisUntilFinished, 100) {
			
			@Override
			public void onTick(long millisUntilFinished) {
				_millisUntilFinished = millisUntilFinished;
				Log.d("ALEX_LABS", "" + _millisUntilFinished);
				if(_remoteClientMessenger != null)  {
					try {
						_remoteClientMessenger.send(Message.obtain(null, TIMER_CURRENT_MILLIS_UNTIL_FINISHED,
								TimerUtils.getMinuteFromMillisecnods(_millisUntilFinished),
								TimerUtils.getSecondsFromMillisecnods(_millisUntilFinished)));
					} catch (RemoteException e) {
						// NOTE: there is no registered client.
						_remoteClientMessenger = null;
					}
				}
			}
			
			@Override
			public void onFinish() {
				// TODO _timerState = TIMER_STATE_FINISHED;
				_timerState = TIMER_STATE_NONE;
				_mode = MODE_BASE;
				// Wake the device and sound the ring tone.
				WakeLocker localWakeLock = new WakeLocker();
				localWakeLock.acquire(getBaseContext());

				Ringtone ringtone  = RingtoneManager.getRingtone(getApplicationContext(),
							RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));	
				
				if(ringtone != null)
					ringtone.play();

				// Cancel the current count down.
				_millisUntilFinished = _selectedMinute = 0;
				
				try {
					_serviceMessenger.send(Message.obtain(null, MSG_GET_TIMER_INFO));
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
				}

				localWakeLock.release();
			}
		};
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return _serviceMessenger.getBinder();
	}

}
