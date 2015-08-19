package com.alexlabs.trackmovement;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity {
	
	private static final String SELECTED_MINUTE_KEY = "selectedMinute";
	private static final String MILLISECNODS_UNTIL_FINISHED_KEY = "millisecondsUntilFinished";
	private static final String IS_TIMER_STARTED_KEY = "isTimerStarted";
	private static final String TIMER_MODE_KEY = "timerMode";
	

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
	private int _selectedMinute;

	private ScreenReceiver _screenReceiver;

	private enum TimerMode {
		BASE,
		START_STOP,
		EDIT_TIME
	}

	private TimerMode _timerMode;
	private long _millisUntilFinished;

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
		
		initEditTimeButtonGroup();

		if(savedInstanceState != null) {
			restoreState(savedInstanceState);
		} else {		
			setTimerMode(TimerMode.BASE);
		}
		
		_content.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View view, MotionEvent event) {
				if (!TimerMode.START_STOP.equals(_timerMode)) {
					int action = event.getActionMasked();
					_motionEvent = event;

					switch (action) {
					case MotionEvent.ACTION_DOWN:
						view.performClick();
						break;
						
					case MotionEvent.ACTION_MOVE:
						onActionMove(getBaseContext(), _content);
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
				onActionMove(getBaseContext(), _content);
			}
		});

		_startStopStateButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// Make sure that if the timer is being set for the 1st time 
				// the milliseconds are set.
				if (TimerMode.BASE == _timerMode) {
					_millisUntilFinished = TimerUtils.getMillisFromMinutes(_selectedMinute);
				}

				if (_millisUntilFinished == 0) {
					Toast.makeText(getBaseContext(), "Please, pick a time.",
							Toast.LENGTH_SHORT).show();

					_secondsTextView.setVisibility(View.GONE);
					return;
				}

				toggleCountDownTimerState();
				setTimerMode(TimerMode.START_STOP);
			}

		});
		
		// Register a broadcast receiver that saves the previous state of the
		// screen - whether it was on or off.
		registerScreenReciver();
	}
	
	private void initEditTimeButtonGroup() {
		initEditTimeButton();
		initEditTimeAcceptChangeButton();
		initEditTimeCancelChangeButton();
	}

	private void initEditTimeCancelChangeButton() {
		_editTimeCancelChangeButton = (Button) findViewById(R.id.edit_time_cancel_change_button);
		_editTimeCancelChangeButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				exitEditTimeMode(_isTimerStarted);
			}
		});
	}

	private void initEditTimeButton() {
		_editTimeButton = (Button) findViewById(R.id.edit_time_button);
		_editTimeButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				setTimerMode(TimerMode.EDIT_TIME);
			}
		});
	}

	private void initEditTimeAcceptChangeButton() {
		_editTimeAcceptChangeButton = (Button) findViewById(R.id.edit_time_accept_change_button);
		_editTimeAcceptChangeButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				_millisUntilFinished = TimerUtils.getMillisFromMinutes(_selectedMinute);
				exitEditTimeMode(true);
			}
		});
	}

	private void exitEditTimeMode(boolean shouldRestartCountdown) {
		if (shouldRestartCountdown) {
			stopCountDown();
			startCountDown();
		}
		setTimerMode(TimerMode.START_STOP);
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


	private void onActionMove(Context context, View content) {
		if (_motionEvent == null)
			throw new IllegalArgumentException("Motion event is null.");

		_selectedMinute = TimerUtils.generateMinute(_motionEvent, context, content);
		updateCurrentTime(TimerUtils.getMillisFromMinutes(_selectedMinute));
		renderArc((float) TimerUtils.generateAngleFromMinute(_selectedMinute));
	}

	private void toggleStartStopButtonState() {
		if(TimerMode.EDIT_TIME != _timerMode) {
			_startStopStateButton.setText(_isTimerStarted ? "Stop" : "Start");
		}
	}

	private void renderUIEditMode() {
		////////////////////////// VISIBLE //////////////////////////////
		_editTimeAcceptChangeButton.setVisibility(View.VISIBLE);
		_editTimeCancelChangeButton.setVisibility(View.VISIBLE);
		
		////////////////////////// INVISIBLE ////////////////////////////
		_secondsTextView.setVisibility(View.GONE);
		_editTimeButton.setVisibility(View.GONE);		
		_startStopStateButton.setVisibility(View.GONE);
	}

	private void renderUIStartStopMode() {
		////////////////////////// VISIBLE //////////////////////////////
		_startStopStateButton.setVisibility(View.VISIBLE);
		_editTimeButton.setVisibility(View.VISIBLE);
		_secondsTextView.setVisibility(View.VISIBLE);
		
		////////////////////////// INVISIBLE ////////////////////////////
		_editTimeAcceptChangeButton.setVisibility(View.GONE);
		_editTimeCancelChangeButton.setVisibility(View.GONE);
	}

	private void renderUIBaseMode() {
		////////////////////////// VISIBLE //////////////////////////////
		_startStopStateButton.setVisibility(View.VISIBLE);
		
		////////////////////////// INVISIBLE ////////////////////////////
		_editTimeButton.setVisibility(View.GONE);			
		_secondsTextView.setVisibility(View.GONE);
		_editTimeAcceptChangeButton.setVisibility(View.GONE);
		_editTimeCancelChangeButton.setVisibility(View.GONE);
	}
	
	private AnimatorSet _animator = new AnimatorSet();
	private void toggleTimerSignalAnimation() {

		View pulsatingCircle = findViewById(R.id.pulsatingCicrle);
		View pulsatingCircleBackground = findViewById(R.id.pulsatingCicrleBackground);
		if(_isTimerStarted && _timerMode == TimerMode.START_STOP) {
			pulsatingCircleBackground.setVisibility(View.VISIBLE);
			pulsatingCircleBackground.setScaleX(0.85f);
			pulsatingCircleBackground.setScaleY(0.85f);
			
			pulsatingCircle.setVisibility(View.VISIBLE);
			ObjectAnimator animatorScaleXInc = ObjectAnimator.ofFloat(pulsatingCircle, "ScaleX", 0.8f, 1.2f);//.setDuration(500); 
			ObjectAnimator animatorScaleYInc = ObjectAnimator.ofFloat(pulsatingCircle, "ScaleY", 0.8f, 1.2f);//.setDuration(500); 
			ObjectAnimator animatorScaleXDec = ObjectAnimator.ofFloat(pulsatingCircle, "ScaleX", 1.2f, 0.8f);//.setDuration(500); 
			ObjectAnimator animatorScaleYDec = ObjectAnimator.ofFloat(pulsatingCircle, "ScaleY", 1.2f, 0.8f);//.setDuration(500);
			ValueAnimator fadeAnim = ObjectAnimator.ofFloat(pulsatingCircle, "alpha", 1f, 0.7f);
			_animator.play(fadeAnim).with(animatorScaleXInc);
			_animator.play(animatorScaleXInc).with(animatorScaleYInc);
			_animator.play(animatorScaleXDec).with(animatorScaleYDec);
			_animator.play(animatorScaleXInc).before(animatorScaleXDec);
			_animator.setInterpolator(new AccelerateDecelerateInterpolator());
			_animator.setDuration(500);
			_animator.addListener(new AnimatorListener() {
				
				@Override
				public void onAnimationStart(Animator animation) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void onAnimationRepeat(Animator animation) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void onAnimationEnd(Animator animation) {
					animation.start();
				}
				
				@Override
				public void onAnimationCancel(Animator animation) {
					// TODO Auto-generated method stub
					
				}
			});
			_animator.start();
		} else {
			if(_animator != null) {
				_animator.cancel();
			}
			
			pulsatingCircle.setVisibility(View.INVISIBLE);
			pulsatingCircleBackground.setVisibility(View.INVISIBLE);
		}
	}

	/////////////////////////////////////////////////
	/////////// Controller
	/////////////////////////////////////////////////

	private void setCountDownTimer(long millis) {
		_countDownTimer = new CountDownTimer(millis, 500) {

			public void onTick(long millisUntilFinished) {
				Log.d("ALEX_LABS", "Tick "
						+ ((Long) (millisUntilFinished / 1000)).toString());
				_millisUntilFinished = millisUntilFinished;				
				
				if (TimerMode.START_STOP == _timerMode) {
					updateCurrentTime(_millisUntilFinished);
					if(TimerUtils.getSecondsFromMillisecnods(_millisUntilFinished) == 59
							|| !_screenReceiver.getWasScreenOn()) {
						int minute = TimerUtils.getMinuteFromMillisecnods(millisUntilFinished);
						renderArc(TimerUtils.generateAngleFromMinute(minute + 1));
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
				resetTimer();

				localWakeLock.release();
			}
		};
	}
	
	private void resetTimer() {
		stopCountDown();
		_selectedMinute = 0;
		_millisUntilFinished = 0;
		setTimerMode(TimerMode.BASE);
	}
	
	private void updateCurrentTime(long millisUntilFinished) {
		int minute = TimerUtils.getMinuteFromMillisecnods(millisUntilFinished);
		int sec = TimerUtils.getSecondsFromMillisecnods(millisUntilFinished);

		_minutesTextView.setText(((Integer)minute).toString());
		_secondsTextView.setText(((Integer)sec).toString());
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
			_countDownTimer = null;
			
			_isTimerStarted = false;
		}
	}

	public void registerScreenReciver() {
		final IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		_screenReceiver = new ScreenReceiver();
		registerReceiver(_screenReceiver, filter);
	}
	
	public void setTimerMode(TimerMode mode) {
		_timerMode = mode;
		if(_timerMode == TimerMode.BASE) {
			renderUIBaseMode();
			
			renderArc(TimerUtils.generateAngleFromMinute(_selectedMinute));			
			updateCurrentTime(TimerUtils.getMillisFromMinutes(_selectedMinute));
		
		} else if(_timerMode == TimerMode.START_STOP) {
			renderUIStartStopMode();
			
			_selectedMinute = -1;			
			int minute = TimerUtils.getMinuteFromMillisecnods(_millisUntilFinished);
			renderArc(TimerUtils.generateAngleFromMinute(minute + 1));
			updateCurrentTime(_millisUntilFinished);
		
		} else if(_timerMode == TimerMode.EDIT_TIME) {
			renderUIEditMode();

			int minute;
			if(_selectedMinute >= 0) {
				minute = _selectedMinute;
			} else {				
				minute = TimerUtils.getMinuteFromMillisecnods(_millisUntilFinished);
			}
			
			renderArc(TimerUtils.generateAngleFromMinute(minute));
			updateCurrentTime(TimerUtils.getMillisFromMinutes(minute));
		} else {
			throw new IllegalArgumentException();
		}
		
		toggleStartStopButtonState();
		toggleTimerSignalAnimation();
	}


	private void restoreState(Bundle inState) {
		_selectedMinute = inState.getInt(SELECTED_MINUTE_KEY);
		_millisUntilFinished = inState.getLong(MILLISECNODS_UNTIL_FINISHED_KEY);
		_isTimerStarted = inState.getBoolean(IS_TIMER_STARTED_KEY);
		TimerMode timerMode = (TimerMode) inState.getSerializable(TIMER_MODE_KEY);
		
		if(_timerMode != TimerMode.BASE && _isTimerStarted) {
			startCountDown();
		}
		
		setTimerMode(timerMode);
	}

	private void toggleCountDownTimerState() {
		if (_countDownTimer != null) {
			if (_isTimerStarted) {
				stopCountDown();
			} else {
				startCountDown();
			}
		} else {
			startCountDown();
		}
	}
	
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
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
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putInt(SELECTED_MINUTE_KEY, _selectedMinute);
		outState.putLong(MILLISECNODS_UNTIL_FINISHED_KEY, _millisUntilFinished);
		outState.putBoolean(IS_TIMER_STARTED_KEY, _isTimerStarted);
		outState.putSerializable(TIMER_MODE_KEY, _timerMode);
		super.onSaveInstanceState(outState);
	}
}
