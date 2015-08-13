package com.alexlabs.trackmovement;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.ReceiverCallNotAllowedException;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.PowerManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity {

	/////////////////////////////////////////////////
	/////////// Model
	/////////////////////////////////////////////////

	private final static int ARC_ID_KEY = 0;

	private RelativeLayout _content;
	private TextView _minutesTextView;
	private TextView _secondsTextView;
	private Button _startStopStateButton;
	private Button _editTimeButton;
	private Button _editTimeAcceptChangeButton;
	private Button _editTimeCancelChangeButton;

	private MotionEvent _motionEvent;

	private CountDownTimer _countDownTimer;
	private Ringtone _ringtone;
	private boolean _isTimerStarted;

	private ScreenReceiver _screenReceiver;

	private enum TimerMode {
		DEFAULT,
		START_STOP,
		EDIT_TIME
	}

	private TimerMode _timerMode = TimerMode.DEFAULT;

	/////////////////////////////////////////////////
	/////////// View
	/////////////////////////////////////////////////

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		getWindow().requestFeature(Window.FEATURE_ACTION_BAR);

		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);

		getSupportActionBar().hide();

		_content = (RelativeLayout) findViewById(R.id.content);
		_minutesTextView = (TextView) findViewById(R.id.minutesView);
		_secondsTextView = (TextView) findViewById(R.id.secondsTextView);
		_startStopStateButton = (Button) findViewById(R.id.start_stop_state_button);

		_ringtone = RingtoneManager.getRingtone(getApplicationContext(),
				RingtoneManager
						.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));

		// NOTE: The seconds' text field is invisible when the timer is not
		// running or when the time is being edited. So, when the app is 
		// started, the aforementioned field is hidden.
		hideSecondsTextView();

		final Clock clock = new Clock(this, _content);

		initEditTimeButtonGroup(clock);

		_content.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (!TimerMode.START_STOP.equals(_timerMode)) {
					int action = event.getActionMasked();
					_motionEvent = event;

					switch (action) {
					case MotionEvent.ACTION_DOWN:
						v.performClick();
						break;
						
					case MotionEvent.ACTION_MOVE:
						onActionMove(clock);
						break;
						
					case MotionEvent.ACTION_UP:
						break;
						
					case MotionEvent.ACTION_CANCEL:
						break;
					}
				}
				return true;
			}
		});

		_content.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				onActionMove(clock);
			}
		});

		_startStopStateButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// Make sure that if the timer is being set for the 1st time 
				// the milliseconds are set.
				if (TimerMode.DEFAULT == _timerMode) {
					_millisUntilFinished = clock.getMinuteInMillis();
				}

				if (_millisUntilFinished == 0) {
					Toast.makeText(getBaseContext(), "Please, pick a time.",
							Toast.LENGTH_SHORT).show();
					hideSecondsTextView();
					return;
				}

				_timerMode = TimerMode.START_STOP;

				if (_secondsTextView.getVisibility() == View.GONE) {
					showSecondsTextView();
				}

				if (_editTimeButton.getVisibility() == View.GONE) {
					_editTimeButton.setVisibility(View.VISIBLE);
				}

				ensureCountDownTimerUI();

			}

		});
		
		// Register a broadcast receiver that saves the previous state of the
		// screen - whether it was on or off.
		registerScreenReciver();
	}

	private void ensureCountDownTimerUI() {
		if (_countDownTimer != null) {
			if (_isTimerStarted) {
				stopCountDown();
			} else {
				startCountDown();
			}
		} else {
			startCountDown();
		}

		_startStopStateButton.setText(_isTimerStarted ? "Stop" : "Start");
	}

	private void initEditTimeButtonGroup(final Clock clock) {
		initEditTimeButton();
		initEditTimeAcceptChangeButton(clock);
		initEditTimeCancelChangeButton();
	}

	private void initEditTimeCancelChangeButton() {
		_editTimeCancelChangeButton = (Button) findViewById(R.id.edit_time_cancel_change_button);
		_editTimeCancelChangeButton.setVisibility(View.GONE);
		_editTimeCancelChangeButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				updateCurrentTimeDisplayInfo();
				updateArcFromCurrentMillis();
				exitEditTimeMode(_isTimerStarted);
			}
		});
	}

	private void initEditTimeButton() {
		_editTimeButton = (Button) findViewById(R.id.edit_time_button);
		_editTimeButton.setVisibility(View.GONE);
		_editTimeButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				beginEditTimeMode();
			}
		});
	}

	private void initEditTimeAcceptChangeButton(final Clock clock) {
		_editTimeAcceptChangeButton = (Button) findViewById(R.id.edit_time_accept_change_button);
		_editTimeAcceptChangeButton.setVisibility(View.GONE);
		_editTimeAcceptChangeButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				_millisUntilFinished = clock.getMinuteInMillis();
				exitEditTimeMode(true);
			}
		});
	}

	private void beginEditTimeMode() {
		_timerMode = TimerMode.EDIT_TIME;

		hideSecondsTextView();

		_startStopStateButton.setVisibility(View.GONE);
		_editTimeButton.setVisibility(View.GONE);

		_editTimeAcceptChangeButton.setVisibility(View.VISIBLE);
		_editTimeCancelChangeButton.setVisibility(View.VISIBLE);
	}

	private void exitEditTimeMode(boolean shouldRestartCountdown) {
		_timerMode = TimerMode.START_STOP;

		if (shouldRestartCountdown) {
			stopCountDown();
			startCountDown();

			_startStopStateButton.setText("Stop");
		}

		showSecondsTextView();

		_startStopStateButton.setVisibility(View.VISIBLE);
		_editTimeButton.setVisibility(View.VISIBLE);

		_editTimeAcceptChangeButton.setVisibility(View.GONE);
		_editTimeCancelChangeButton.setVisibility(View.GONE);
	}

	private void renderArc(float angle) {

		// Remove the current arc. This is done so that a new arc will be
		// generated with the newly selected angle.
		_content.removeView(_content.findViewById(ARC_ID_KEY));

		// Create the new arc from the new angle that has been selected.
		Arc arc = new Arc(getBaseContext(), _content, angle);

		// Set the arc view's id.
		arc.setId(ARC_ID_KEY);

		// Add the arc to the content view of the clock.
		_content.addView(arc, 1);
	}

	private void displayCurrentTime(final Clock clock) {
		_minutesTextView.setText(((Integer) clock.getMinute()).toString());
	}

	private void displayCurrentTime(Integer minutes, Integer seconds) {
		_minutesTextView.setText(minutes.toString());
		_secondsTextView.setText(seconds.toString());
	}

	private void onActionMove(final Clock clock) {
		if (_motionEvent == null)
			throw new IllegalArgumentException("Motion event is null.");

		clock.wind(_motionEvent);

		displayCurrentTime(clock);

		renderArc((float) clock.getAngle());
	}

	private void hideSecondsTextView() {
		_secondsTextView.setVisibility(View.GONE);
	}

	private void showSecondsTextView() {
		_secondsTextView.setVisibility(View.VISIBLE);
	}
	
	public void renderUI(){
		// implement
	}

	/////////////////////////////////////////////////
	/////////// Controller
	/////////////////////////////////////////////////

	private long _millisUntilFinished;

	private void setCountDownTimer(long millis) {
		_countDownTimer = new CountDownTimer(millis, 500) {

			public void onTick(long millisUntilFinished) {
				Log.d("ALEX_LABS", "Tick "
						+ ((Long) (millisUntilFinished / 1000)).toString());
				_millisUntilFinished = millisUntilFinished;				
				
				if (TimerMode.START_STOP == _timerMode) {
					updateCurrentTimeDisplayInfo();
					if(Clock.getSeconsFromMillisecnods(_millisUntilFinished) == 59
							|| !_screenReceiver.getWasScreenOn()) {
						updateArcFromCurrentMillis();
					}
				}
			}

			public void onFinish() {
				// Wake the device and sound the ring tone.
				WakeLocker localWakeLock = new WakeLocker();
				localWakeLock.acquire(getBaseContext());

				if (_ringtone != null) {
					_ringtone.play();
				}

				// Cancel the current count down.
				stopCountDown();
				
				_startStopStateButton.setText("Start");
				
				_millisUntilFinished = 0;
				
				hideSecondsTextView();
				updateArcFromCurrentMillis();
				_editTimeButton.setVisibility(View.GONE);
				
				_timerMode = TimerMode.DEFAULT;

				localWakeLock.release();
			}

		};
	}



	private void updateArcFromCurrentMillis() {
		// NOTE: when the timer is started for the 1st time, the seconds become
		// 59 and the minutes are decrease by 1, but the arc remains the same as 
		//when the timer was set.
		// It should be noted that the timer was set at minutes + 1. This means
		// the arc always shows minutes + 1.
		// To keep that logic, when the screen is turned on (after being turned
		// off), the arc is rendered with minutes + 1.
		if(_millisUntilFinished != 0) {
			int minute = Clock.getMinuteFromMillisecnods(_millisUntilFinished);
			renderArc(Clock.generateAngleFromMinute(minute + 1));
		} else {
			renderArc(0);
		}
	}
	
	private void updateCurrentTimeDisplayInfo() {
		int minute = Clock.getMinuteFromMillisecnods(_millisUntilFinished);
		int sec = Clock.getSeconsFromMillisecnods(_millisUntilFinished);

		displayCurrentTime(minute, sec);
	}

	public void startCountDown() {
		if (_countDownTimer == null) {
			setCountDownTimer(_millisUntilFinished);
		}

		_countDownTimer.start();
		_isTimerStarted = true;

	}

	public void stopCountDown() {
		if (_countDownTimer != null) {
			_countDownTimer.cancel();
			disposeCountDownTimer();
			_isTimerStarted = false;
		}
	}

	public void disposeCountDownTimer() {
		_countDownTimer = null;
	}

	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public void registerScreenReciver() {
		final IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		_screenReceiver = new ScreenReceiver();
		registerReceiver(_screenReceiver, filter);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}
}
