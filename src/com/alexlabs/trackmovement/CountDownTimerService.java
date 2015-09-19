package com.alexlabs.trackmovement;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class CountDownTimerService extends Service{
	
	private static final boolean SHOULD_WAIT_FOR_DEBUGGER = false; // FIXME - set false for production
	
	private static final int ONGOING_NOTIFICATION_ID = 1;
	
	// common
	public static final int MSG_REGISTER_CLIENT = 0;
	public static final int MSG_UNREGISTER_CLIENT = 1;
	public static final int MSG_SET_MODE = 2;
	public static final int MSG_START_TIMER = 3;
	public static final int MSG_PAUSE_TIMER = 4;
	public static final int MSG_UNPAUSE_TIMER = 5;
	public static final int MSG_SET_SELECTED_MINUTE = 6;	
	public static final int MSG_GET_TIMER_INFO = 7;
	public static final int MSG_CHECK_MODE_ON_SCREEN_TOGGLE = 8;
	public static final int MSG_DONE_USING_TIMER = 9;
	public static final int MSG_STOP_ALARM_NOISE_AND_VIBRATION = 10;
	
	
	// modes
	static final int MODE_BASE = 11;
	static final int MODE_ACTIVE = 12; 
	static final int MODE_EDIT_TIME = 13;
	
	// states
	static final int TIMER_STATE_NONE = 20;
	static final int TIMER_STATE_STARTED = 21;
	static final int TIMER_STATE_STOPPED = 22;
	static final int TIMER_STATE_FINISHED = 23;
	
	static final int SEND_CURRENT_MILLIS_UNTIL_FINISHED = 30;
	
	// Bundle constants
	static final String MODE = "mode";
	static final String CURRENT_SECONDS = "currentSeconds";
	static final String CURRENT_MINUTE = "currentMinute";
	static final String SELECTED_MINUTE = "selectedMinute";
	static final String TIMER_STATE = "isTimerStarted";
	
	// Timer related
	private int _timerState = TIMER_STATE_NONE;
	private int _mode = MODE_BASE;
	
	private int _selectedMinute;
	private long _millisUntilFinished;
	private ScheduledExecutorService _scheduler;// = Executors.newScheduledThreadPool(1);

	
	// Service related
	private Messenger _remoteClientMessenger;
	private CountDownTimer _countDownTimer;
	
	/**
    * Target we publish for clients to send messages to IncomingHandler.
    */
	final Messenger _serviceMessenger = new Messenger(new IncomingHandler());

	// For debugging
	static {
		// NOTE: the code below starts the app in debug mode every time, causing the
		// application the start very slowly, and animations become glitchy. 
		// Set SHOULD_WAIT_FOR_DEBUGGER = false to avoid this behavior.
		if(SHOULD_WAIT_FOR_DEBUGGER) {
			android.os.Debug.waitForDebugger();
		}	
	}
	
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
				_millisUntilFinished = TimerUtils.convertMinuteToMillis(_selectedMinute);
				Log.d("ALEX_LABS", "" + _millisUntilFinished);
				startCountDown();	
				break;
			
			case MSG_PAUSE_TIMER:
				stopCountDown();
				break;
				
			case MSG_UNPAUSE_TIMER:
				startCountDown();
				break;
				
			case MSG_DONE_USING_TIMER:
				_timerState = TIMER_STATE_NONE;
				
				AlarmBell.instance().stop(getBaseContext());
				Log.d("ALEX_LABS", AlarmBell.instance().toString());
				
				if(_scheduler != null && !_scheduler.isShutdown()) {
					_scheduler.shutdown();
				}
				
				try {
					_serviceMessenger.send(Message.obtain(null, MSG_GET_TIMER_INFO));
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
				}
				
				break;
			case MSG_STOP_ALARM_NOISE_AND_VIBRATION:
				AlarmBell.instance().stop(getBaseContext());
				break;
				
			case MSG_GET_TIMER_INFO:
				if(_remoteClientMessenger != null){
					try {
						// get everything
						Message infoMsg = Message.obtain(null, MSG_GET_TIMER_INFO);
						infoMsg.setData(getDataInBundle());
						_remoteClientMessenger.send(infoMsg);
					} catch (RemoteException e) {
						// TODO: handle exception
					}
				}
				break;
			
			case MSG_CHECK_MODE_ON_SCREEN_TOGGLE:
				// NOTE: the application is not destroyed when the screen is turned off. Therefore,
				// it is safe to send a message to update the UI of the app.
				if(_mode == MODE_EDIT_TIME) {
					_mode = MODE_ACTIVE;
					
					try {
						_serviceMessenger.send(Message.obtain(null, MSG_GET_TIMER_INFO));
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
					}
				}
				break;
				
			default:
				super.handleMessage(msg);
			}
		}

		private Bundle getDataInBundle() {
			Bundle data = new Bundle();
			data.putInt(MODE, _mode);
			data.putInt(TIMER_STATE, _timerState);			
			data.putInt(SELECTED_MINUTE, _selectedMinute);
			data.putInt(CURRENT_MINUTE, TimerUtils.getMinuteFromMillisecnods(_millisUntilFinished));
			data.putInt(CURRENT_SECONDS, TimerUtils.getSecondsFromMillisecnods(_millisUntilFinished));			
			return data;
		}
	}	
	
	public void startCountDown() {
		if (_countDownTimer != null) {
			_countDownTimer.cancel();
			_countDownTimer = null;
		}
		
		_timerState = TIMER_STATE_STARTED;
		
		initCountDownTimer();
		
		UIUtils.sendNotification(getBaseContext(), this, ONGOING_NOTIFICATION_ID, getApplicationContext().getString(R.string.timer_started));
		
		_countDownTimer.start();
	}
	
	public void stopCountDown() {
		if (_countDownTimer != null) {
			_countDownTimer.cancel();
			_countDownTimer = null;

			_timerState = TIMER_STATE_STOPPED;	
			
			UIUtils.sendNotification(getBaseContext(), this, ONGOING_NOTIFICATION_ID, getApplicationContext().getString(R.string.timer_paused));
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
						_remoteClientMessenger.send(Message.obtain(null, SEND_CURRENT_MILLIS_UNTIL_FINISHED,
								TimerUtils.getMinuteFromMillisecnods(_millisUntilFinished),
								TimerUtils.getSecondsFromMillisecnods(_millisUntilFinished)));
					} catch (RemoteException e) {
						// NOTE: there is no registered client.
						_remoteClientMessenger = null;
					}
				}
				
				if(_millisUntilFinished < 200) {					
					// Wake the device and sound the ring tone.
					beginRepeatingAlarm();
				}
			}
			
			@Override
			public void onFinish() {
				_timerState = TIMER_STATE_FINISHED;
				
				_mode = MODE_BASE;
				
				_millisUntilFinished = _selectedMinute = 0;				
				UIUtils.sendNotification(getBaseContext(), CountDownTimerService.this, ONGOING_NOTIFICATION_ID, getApplicationContext().getString(R.string.timer_finished));
				
				try {
					_serviceMessenger.send(Message.obtain(null, MSG_GET_TIMER_INFO));
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
				}
			}
		};
	}
	
	private void showMainActivity() {
		Intent i = new Intent();
		i.setAction(Intent.ACTION_MAIN);
		i.addCategory(Intent.CATEGORY_LAUNCHER);
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);
		i.setComponent(new ComponentName(getApplicationContext().getPackageName(), MainActivity.class.getName()));
		startActivity(i);
	}
	
	private void beginRepeatingAlarm() {
		if(_scheduler == null || _scheduler.isShutdown())
			_scheduler = Executors.newScheduledThreadPool(1);
		
//		_scheduler.scheduleWithFixedDelay(new Runnable() {
//			
//			@Override
//			public void run() {
//				WakeLocker localWakeLock = new WakeLocker();
//				localWakeLock.acquire(getBaseContext());
//				
//				AlarmBell.start(getBaseContext(), false);
//				
//				localWakeLock.release();
//				//TODO: shut down scheduler when the user does not want a repeating alarm.
//			}
//		}, 0, 10, TimeUnit.SECONDS);
		
		WakeLocker localWakeLock = new WakeLocker();
		localWakeLock.acquire(getBaseContext());
		
		showMainActivity();
		
		AlarmBell.instance().start(getBaseContext(), false);
		
		localWakeLock.release();
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return _serviceMessenger.getBinder();
	}
}
