package com.alexlabs.trackmovement;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.alexlabs.trackmovement.utils.TimerUtils;
import com.alexlabs.trackmovement.utils.UIUtils;

import android.annotation.SuppressLint;
import android.app.Service;
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
	public static final int MSG_CLEAR_TIMER = 6;
	public static final int MSG_SET_SELECTED_MINUTE = 7;	
	public static final int MSG_GET_TIMER_INFO = 8;
	public static final int MSG_DONE_USING_TIMER = 9;
	public static final int MSG_STOP_ALARM_NOISE_AND_VIBRATION = 10;
			
	// modes
	static final int MODE_BASE = 21;
	static final int MODE_ACTIVE = 22; 
	static final int MODE_EDIT_TIME = 23;
	static final int MODE_WAITING_FOR_CONFIRMATION = 24;
	
	// states
	static final int TIMER_STATE_NONE = 30;
	static final int TIMER_STATE_STARTED = 31;
	static final int TIMER_STATE_STOPPED = 32;
	static final int TIMER_STATE_FINISHED = 33;
	
	static final int SEND_CURRENT_MILLIS_UNTIL_FINISHED = 40;
	
	// Bundle constants
	static final String MODE = "mode";
	static final String CURRENT_SECONDS = "currentSeconds";
	static final String CURRENT_MINUTE = "currentMinute";
	static final String SELECTED_MINUTE = "selectedMinute";
	static final String PREVEOUSLY_SET_TIME = "preveouslySetTime";
	static final String TIMER_STATE = "isTimerStarted";
	
	// Timer related
	private int _timerState = TIMER_STATE_NONE;
	private int _mode = MODE_BASE;
	
	private int _selectedMinute;
	private int _preveouslySetTime;
	private long _millisUntilFinished;
	private ScheduledExecutorService _scheduler;

	
	// Service related
	private Messenger _remoteClientMessenger;
	private CountDownTimer _countDownTimer;
	
	// Waker lockers
	private PartialWakeLocker _cpuLocker = new PartialWakeLocker();
	
	/**
    * Target we publish for clients to send messages to IncomingHandler.
    */
	private final Messenger _serviceMessenger = new Messenger(new IncomingHandler());
	
	/////////////////////////////////////////////////
	/////////// Model
	/////////////////////////////////////////////////

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
				Log.d("ALEX_LABS", "set selected minute:" + _selectedMinute);
				break;
				
			case MSG_START_TIMER:
				_preveouslySetTime = _selectedMinute;
				_millisUntilFinished = TimerUtils.convertMinuteToMillis(_selectedMinute);
				Log.d("ALEX_LABS", "started timer: " + _millisUntilFinished);
				startCountDown();	
				break;
			
			case MSG_PAUSE_TIMER:
				pauseCountDown();
				break;
				
			case MSG_UNPAUSE_TIMER:
				startCountDown();
				break;
			
			case MSG_CLEAR_TIMER:
				clearCountDown();
				break;
				
			case MSG_DONE_USING_TIMER:
				doneUsingTimer();
				
				break;
			case MSG_STOP_ALARM_NOISE_AND_VIBRATION:
				AlarmBell.instance().stop(getBaseContext());
				break;
				
			case MSG_GET_TIMER_INFO:
				Log.d("ALEX_LABS", "request made to send time info");
				sendTimerInfoToRemoteClient();
				break;
				
			default:
				super.handleMessage(msg);
			}
		}

	}
	
	private Bundle getDataInBundle() {
		Bundle data = new Bundle();
		data.putInt(MODE, _mode);
		data.putInt(TIMER_STATE, _timerState);			
		data.putInt(SELECTED_MINUTE, _selectedMinute);
		data.putInt(PREVEOUSLY_SET_TIME, _preveouslySetTime);
		data.putInt(CURRENT_MINUTE, TimerUtils.getMinuteFromMillisecnods(_millisUntilFinished));
		data.putInt(CURRENT_SECONDS, TimerUtils.getSecondsFromMillisecnods(_millisUntilFinished));
		return data;
	}
	
	private void startCountDown() {
		if (_countDownTimer != null) {
			_countDownTimer.cancel();
			_countDownTimer = null;
		}
		
		_timerState = TIMER_STATE_STARTED;
		
		initCountDownTimer();
		
		UIUtils.sendNotification(getBaseContext(), this, ONGOING_NOTIFICATION_ID, getApplicationContext().getString(R.string.timer_started));
		
		_countDownTimer.start();
		_cpuLocker.acquire(getBaseContext());
	}
	
	private void pauseCountDown() {
		if (_countDownTimer != null) {
			_countDownTimer.cancel();
			_countDownTimer = null;

			_timerState = TIMER_STATE_STOPPED;	
			
			UIUtils.sendNotification(getBaseContext(), this, ONGOING_NOTIFICATION_ID, getApplicationContext().getString(R.string.timer_paused));
		}
	}
	
	private void doneUsingTimer() {
		_mode = MODE_BASE;
		_timerState = TIMER_STATE_NONE;
		
		AlarmBell.instance().stop(getBaseContext());
		showMainActivity();
		
		Log.d("ALEX_LABS", AlarmBell.instance().toString());
		
		if(_scheduler != null && !_scheduler.isShutdown()) {
			_scheduler.shutdown();
		}
		
		_cpuLocker.release();

		Log.d("ALEX_LABS", "request sent that timer is no longer used");
		sendTimerInfoToRemoteClient();
		
		stopForeground(true);
	}
	
	private void clearCountDown() {
		if (_countDownTimer != null) {
			_countDownTimer.cancel();
			_countDownTimer = null;
		}
		
		_mode = MODE_BASE;
		_timerState = TIMER_STATE_NONE;
		_selectedMinute = 0;
		_millisUntilFinished = 0;
		
		_cpuLocker.release();
		
		sendTimerInfoToRemoteClient();
		
		UIUtils.sendNotification(getBaseContext(), this, ONGOING_NOTIFICATION_ID, getApplicationContext().getString(R.string.timer_cleared));
		
		stopForeground(true);
	}
	
	// FIXME - revert to _millisUntilFinished for production
	private void initCountDownTimer() {
		_countDownTimer = new CountDownTimer(_millisUntilFinished/*5000*/, 100) {
			
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
			}
			
			@Override
			public void onFinish() {
				_timerState = TIMER_STATE_FINISHED;
				
				_mode = MODE_WAITING_FOR_CONFIRMATION;
				
				_millisUntilFinished = _selectedMinute = 0;
				UIUtils.sendNotification(getBaseContext(), CountDownTimerService.this, ONGOING_NOTIFICATION_ID, getApplicationContext().getString(R.string.timer_finished));

				startAlarm();
			}
		};
	}

	private boolean sendTimerInfoToRemoteClient() {

		Log.d("ALEX_LABS", ">>>>remote Client Messenger is: " + _remoteClientMessenger);

		if(_remoteClientMessenger != null){
			try {
				// get everything
				Message infoMsg = Message.obtain(null, MSG_GET_TIMER_INFO);
				infoMsg.setData(getDataInBundle());
				_remoteClientMessenger.send(infoMsg);
				Log.d("ALEX_LABS", ">>>>sent info to MainActivity");
				return true;
			} catch (RemoteException e) {
				// TODO: handle exception
			}
		}
		Log.d("ALEX_LABS", ">>>>have to start MainActivity");
		return false;
	}
	
	private void showMainActivity() {
		Intent i = new Intent(this, MainActivity.class);
		i.setAction(Intent.ACTION_MAIN);
		i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(i);
	}
	
	private void startAlarm() {
		if(_scheduler == null || _scheduler.isShutdown())
			_scheduler = Executors.newScheduledThreadPool(1);
		
		FullWakeLocker localWakeLock = new FullWakeLocker();
		localWakeLock.acquire(getBaseContext());
		
		showMainActivity();
		sendTimerInfoToRemoteClient();
		
		Preferences prefs = new Preferences();
		AlarmBell.instance().start(getBaseContext());
		if(prefs.getShouldBeep()) {
			_scheduler.scheduleWithFixedDelay(new Runnable() {
				
				@Override
				public void run() {
					if(_timerState == TIMER_STATE_FINISHED) {
						AlarmBeep.instance().beep(getBaseContext());
					}
				}
			}, prefs.getAlarmNoiseDuration() + AlarmBeep.BEEP_DELAY_INTERVAL, AlarmBeep.BEEP_DELAY_INTERVAL, TimeUnit.SECONDS);
		}
		
		localWakeLock.release();
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return _serviceMessenger.getBinder();
	}
}
