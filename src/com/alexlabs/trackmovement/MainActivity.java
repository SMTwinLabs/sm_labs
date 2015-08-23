package com.alexlabs.trackmovement;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
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

	private boolean _isTimerStarted;
	
	private int _selectedMinute;
	private int _currentMinute;
	private int _currentSeconds;
	
	private ScreenReceiver _screenReceiver;
	private int _UIMode;
	
	 /** Messenger for communicating with service. */
    Messenger _countDownService;


    @SuppressLint("HandlerLeak") 
    class IncomingHandler extends Handler {
    	@Override
    	public void handleMessage(Message msg){
    		switch(msg.what) {
    			
    		case CountDownTimerService.SEND_CURRENT_MILLIS_UNTIL_FINISHED:
    			_currentMinute = msg.arg1;
    			_currentSeconds = msg.arg2;
    			if (CountDownTimerService.MODE_ACTIVE == _UIMode) {
					updateCurrentTime(_currentMinute, _currentSeconds);
					if(_currentSeconds == 59 || !_screenReceiver.getWasScreenOn()) {
						renderArc(TimerUtils.generateAngleFromMinute(_currentMinute + 1));
					}
				}
    			break;

    		case CountDownTimerService.MSG_GET_TIMER_INFO:
    			if(msg.getData() != null) {
	    			extractDataFromBundle(msg.getData());
    			}
    			break;
    			
    		default:
    			super.handleMessage(msg);
    		}
    	}

		private void extractDataFromBundle(Bundle info) {
			_isTimerStarted = info.getBoolean(CountDownTimerService.IS_TIMER_STARTED);
			_selectedMinute = info.getInt(CountDownTimerService.SELECTED_MINUTE);
			_currentMinute = info.getInt(CountDownTimerService.CURRENT_MINUTE);
			_currentSeconds = info.getInt(CountDownTimerService.CURRENT_SECONDS);
			renderUIMode(info.getInt(CountDownTimerService.MODE));
		}
    }
    
    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    private final Messenger _clientMessenger = new Messenger(new IncomingHandler());
    
    private ServiceConnection _serviceConnetcion = new ServiceConnection() {
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			_countDownService = null;
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			_countDownService = new Messenger(service);
			
            // We want to monitor the service for as long as we are
            // connected to it.
			Message msg = null;
            try {
                msg = Message.obtain(null,
                                CountDownTimerService.MSG_REGISTER_CLIENT);
                msg.replyTo = _clientMessenger;
                _countDownService.send(msg);
            } catch (RemoteException e) {
                // In this case the service has crashed before we could even
                // do anything with it; we can count on soon being
                // disconnected (and then reconnected if it can be restarted)
                // so there is no need to do anything here.
            }
            
            try {
                msg = Message.obtain(null,
                                CountDownTimerService.MSG_GET_TIMER_INFO);
                msg.replyTo = _clientMessenger;
                _countDownService.send(msg);
            } catch (RemoteException e) {
                // TODO
            }            
		}
	};

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
		
		initEditTimeButtonGroup();
		
		_content.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View view, MotionEvent event) {
				if (_UIMode != CountDownTimerService.MODE_ACTIVE) {
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

				if (_selectedMinute == 0) {
					Toast.makeText(getBaseContext(), "Please, pick a time.",
							Toast.LENGTH_SHORT).show();

					_secondsTextView.setVisibility(View.GONE);
					return;
				}

				if (_UIMode == CountDownTimerService.MODE_BASE){
					setTimerState(CountDownTimerService.MSG_START_TIMER);
					_isTimerStarted = true;
				
				} else if (_UIMode == CountDownTimerService.MODE_ACTIVE) {
					if (_isTimerStarted) {
						setTimerState(CountDownTimerService.MSG_PAUSE_TIMER);
						
					} else {
						setTimerState(CountDownTimerService.MSG_UNPAUSE_TIMER);					
					}
					_isTimerStarted = !_isTimerStarted;
					
				}
				
				renderUIMode(CountDownTimerService.MODE_ACTIVE);
			}

		});
		
		// Register a broadcast receiver that saves the previous state of the
		// screen - whether it was on or off.
		registerScreenReciver();

		startService(new Intent(this, CountDownTimerService.class));        
        doBindToCountDownService();
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
				renderUIMode(CountDownTimerService.MODE_ACTIVE);
			}
		});
	}

	private void initEditTimeButton() {
		_editTimeButton = (Button) findViewById(R.id.edit_time_button);
		_editTimeButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				renderUIMode(CountDownTimerService.MODE_EDIT_TIME);
			}
		});
	}

	private void initEditTimeAcceptChangeButton() {
		_editTimeAcceptChangeButton = (Button) findViewById(R.id.edit_time_accept_change_button);
		_editTimeAcceptChangeButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				setTimerState(CountDownTimerService.MSG_START_TIMER);
				_isTimerStarted = true;
				renderUIMode(CountDownTimerService.MODE_ACTIVE);
			}
		});
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
		updateCurrentTime(_selectedMinute, 0);
		renderArc((float) TimerUtils.generateAngleFromMinute(_selectedMinute));
		
		try {
			_countDownService.send(Message.obtain(null, CountDownTimerService.MSG_SET_SELECTED_MINUTE, _selectedMinute, 0));
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void toggleStartStopButtonState() {
		if(CountDownTimerService.MODE_EDIT_TIME != _UIMode) {
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
	
	// TODO - send to a new class specific to animations
	private AnimatorSet _animator = new AnimatorSet();
	private void toggleTimerSignalAnimation() {

		View pulsatingCircle = findViewById(R.id.pulsatingCicrle);
		View pulsatingCircleBackground = findViewById(R.id.pulsatingCicrleBackground);
		if(_isTimerStarted && _UIMode == CountDownTimerService.MODE_ACTIVE) {
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
	
	private void updateCurrentTime(int minute, int seconds) {
		_minutesTextView.setText(((Integer)minute).toString());
		_secondsTextView.setText(((Integer)seconds).toString());
	}

	/////////////////////////////////////////////////
	/////////// Controller
	/////////////////////////////////////////////////
	
	void doBindToCountDownService() {
        // Establish a connection with the service. We use an explicit
        // class name because there is no reason to be able to let other
        // applications replace our component.
        bindService(new Intent(MainActivity.this, CountDownTimerService.class),
                        _serviceConnetcion, Context.BIND_AUTO_CREATE);
        Log.d("ALEX_LABS", "binding");
    }
	
	void doUnbindFromCountDownService() {

        Log.d("ALEX_LABS", "unbinding");
        
        // If we have received the service, and hence registered with
        // it, then now is the time to unregister.
        if (_countDownService != null) {
               try {
                    Message msg = Message.obtain(null,
                                    CountDownTimerService.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = _clientMessenger;
                    _countDownService.send(msg);
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service
                    // has crashed.
                }
        }

        // Detach our existing connection.
        unbindService(_serviceConnetcion);
    }

	public void registerScreenReciver() {
		final IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		_screenReceiver = new ScreenReceiver();
		registerReceiver(_screenReceiver, filter);
	}
	
	public void renderUIMode(int mode) {
		_UIMode = mode;
		if(_UIMode == CountDownTimerService.MODE_BASE) {
			renderUIBaseMode();
			
			renderArc(TimerUtils.generateAngleFromMinute(_selectedMinute));			
			updateCurrentTime(_selectedMinute, 0);
		
		} else if(_UIMode == CountDownTimerService.MODE_ACTIVE) {
			renderUIStartStopMode();
			
			_selectedMinute = -1;			
			renderArc(TimerUtils.generateAngleFromMinute(_currentMinute + 1));
			updateCurrentTime(_currentMinute, _currentSeconds);
			
			try {
				_countDownService.send(Message.obtain(null, CountDownTimerService.MSG_SET_SELECTED_MINUTE, _selectedMinute, 0));
			} catch (RemoteException e) {
				// TODO: handle exception
			}
		
		} else if(_UIMode == CountDownTimerService.MODE_EDIT_TIME) {
			renderUIEditMode();

			int minute;
			if(_selectedMinute >= 0) {
				minute = _selectedMinute;
			} else {				
				minute = _currentMinute;
			}
			
			renderArc(TimerUtils.generateAngleFromMinute(minute));
			updateCurrentTime(minute, 0);
		} else {
			throw new IllegalArgumentException();
		}
		
		toggleStartStopButtonState();
		toggleTimerSignalAnimation();
		
		try {
			_countDownService.send(Message.obtain(null, CountDownTimerService.MSG_SET_MODE, mode, 0));
		} catch (RemoteException e) {
			// TODO: handle exception
		}
	}
	
	private void setTimerState(int state) {		
		try {
			_countDownService.send(Message.obtain(null, state));
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
		}
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
	protected void onDestroy() {
		doUnbindFromCountDownService();
		super.onDestroy();
	}
}
