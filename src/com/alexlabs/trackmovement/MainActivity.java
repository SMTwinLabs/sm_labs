package com.alexlabs.trackmovement;

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
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.Window;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MainActivity extends ActionBarActivity {	

	/////////////////////////////////////////////////
	/////////// Model
	/////////////////////////////////////////////////

	private final static int ARC_ACTIVE_ID_KEY = 0;
	private final static int ARC_EDIT_TIME_ID_KEY = 1;

	private RelativeLayout _content;
	private TextView _minutesTextView;
	private TextView _secondsTextView;
	private TextView _currentModeTextView;
	
	private View _buttonBar;
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
	private int _UIMode = CountDownTimerService.MODE_BASE;
	
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
    			
    			if(_UIMode == CountDownTimerService.MODE_ACTIVE){
					updateCurrentTime(_currentMinute, _currentSeconds);
    			}
    			
    			if (_isTimerStarted && _currentSeconds == 59) {
						renderArc(TimerUtils.generateAngleFromMinute(_currentMinute + 1), ARC_ACTIVE_ID_KEY, 1, R.color.timer_active_color, 255);
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
			renderAll(info.getInt(CountDownTimerService.MODE));
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
            
            requestTimerInfoFromCountDownTimerService();
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
		_buttonBar = findViewById(R.id.buttonArea);
		_currentModeTextView = (TextView) findViewById(R.id.modeView);
		
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
					UIUtils.showToast(getBaseContext(), R.string.pick_time_prompt);

					_secondsTextView.setVisibility(View.GONE);
					return;
				}

				if (_UIMode == CountDownTimerService.MODE_BASE){
					setTimerState(CountDownTimerService.MSG_START_TIMER);
				
				} else if (_UIMode == CountDownTimerService.MODE_ACTIVE) {
					if (_isTimerStarted) {
						setTimerState(CountDownTimerService.MSG_PAUSE_TIMER);
						
					} else {
						setTimerState(CountDownTimerService.MSG_UNPAUSE_TIMER);					
					}					
				}
				
				updateUIMode(CountDownTimerService.MODE_ACTIVE);
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
				updateUIMode(CountDownTimerService.MODE_ACTIVE);
			}
		});
	}

	private void initEditTimeButton() {
		_editTimeButton = (Button) findViewById(R.id.edit_time_button);
		_editTimeButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				updateUIMode(CountDownTimerService.MODE_EDIT_TIME);
			}
		});
	}

	private void initEditTimeAcceptChangeButton() {
		_editTimeAcceptChangeButton = (Button) findViewById(R.id.edit_time_accept_change_button);
		_editTimeAcceptChangeButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if(_selectedMinute > 0) {
					// apply the new time
					setTimerState(CountDownTimerService.MSG_START_TIMER);
					updateUIMode(CountDownTimerService.MODE_ACTIVE);
				} else if(_selectedMinute < 0){
					// simply return to active mode without doing any work.
					updateUIMode(CountDownTimerService.MODE_ACTIVE);
				} else if(_selectedMinute == 0){
					// when the timer is 0, the timer will finish immediately, setting everything to 
					// BASE mode.
					setTimerState(CountDownTimerService.MSG_START_TIMER);
				}
			}
			
		});
	}
	
	private void requestTimerInfoFromCountDownTimerService() {
		Message msg = Message.obtain(null,
                    CountDownTimerService.MSG_GET_TIMER_INFO);
        msg.replyTo = _clientMessenger;
        try {
			_countDownService.send(msg);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void renderArc(float angle, int arcId, int index, int color, int alpha) {

		// Remove the current arc. This is done so that a new arc will be
		// generated with the newly selected angle.
		_content.removeView(_content.findViewById(arcId));

		// Create the new arc from the new angle that has been selected.
		Arc arc = new Arc(getBaseContext(), _content, angle, color, alpha);

		// Set the arc view's id.
		arc.setId(arcId);

		// Add the arc to the content view of the clock.
		_content.addView(arc, index);
	}
	
	private void onActionMove(Context context, View content) {
		if (_motionEvent == null)
			throw new IllegalArgumentException("Motion event is null.");

		_selectedMinute = TimerUtils.generateMinute(_motionEvent, context, content);
		
		try {
			_countDownService.send(Message.obtain(null, CountDownTimerService.MSG_SET_SELECTED_MINUTE, _selectedMinute, 0));
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		renderAll(_UIMode);
	}

	private void toggleStartStopButtonState() {
		if(CountDownTimerService.MODE_EDIT_TIME != _UIMode) {
			_startStopStateButton.setText(_isTimerStarted ? "Stop" : "Start");
		}
	}

	private void renderButtonBarEditMode() {
		////////////////////////// VISIBLE //////////////////////////////
		_editTimeAcceptChangeButton.setVisibility(View.VISIBLE);
		_editTimeCancelChangeButton.setVisibility(View.VISIBLE);
		
		////////////////////////// INVISIBLE ////////////////////////////
		_editTimeButton.setVisibility(View.GONE);		
		_startStopStateButton.setVisibility(View.GONE);
	}

	private void renderButtonBarActiveMode() {
		toggleStartStopButtonState();
		
		////////////////////////// VISIBLE //////////////////////////////
		_startStopStateButton.setVisibility(View.VISIBLE);
		_editTimeButton.setVisibility(View.VISIBLE);
		
		////////////////////////// INVISIBLE ////////////////////////////
		_editTimeAcceptChangeButton.setVisibility(View.GONE);
		_editTimeCancelChangeButton.setVisibility(View.GONE);
	}

	private void renderButtonBarBaseMode() {		
		toggleStartStopButtonState();
		////////////////////////// VISIBLE //////////////////////////////
		_startStopStateButton.setVisibility(View.VISIBLE);
		
		////////////////////////// INVISIBLE ////////////////////////////
		_editTimeButton.setVisibility(View.GONE);
		_editTimeAcceptChangeButton.setVisibility(View.GONE);
		_editTimeCancelChangeButton.setVisibility(View.GONE);
	}
	
	private void updateCurrentTime(int minute, int seconds) {
		_minutesTextView.setText(((Integer)minute).toString());
		_secondsTextView.setText(((Integer)seconds).toString());
	}

	/////////////////////////////////////////////////
	/////////// Controller
	/////////////////////////////////////////////////

	void doBindToCountDownService() {
        Log.d("ALEX_LABS", "binding");

        // Establish a connection with the service. We use an explicit
        // class name because there is no reason to be able to let other
        // applications replace our component.
        bindService(new Intent(MainActivity.this, CountDownTimerService.class),
                        _serviceConnetcion, Context.BIND_AUTO_CREATE);
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
	
	/**
	 * This method only renders the UI. To set the mode use {@link#updateUIMode}
	 * @param newMode
	 */
	public void renderAll(int newMode) {
		int priviousMode = _UIMode;
		_UIMode = newMode;
		if(_UIMode == CountDownTimerService.MODE_BASE) {			
			renderUIBaseMode();
		
		} else if(_UIMode == CountDownTimerService.MODE_ACTIVE) {			
			renderUIActiveMode();
		
		} else if(_UIMode == CountDownTimerService.MODE_EDIT_TIME) {
			renderUIEditMode();
			
		} else {
			throw new IllegalArgumentException();
		}
		
		// TODO: fix anmitaion problems
		AnimationUtils.toggleTimerSignalAnimation(this, _isTimerStarted);
		
		// If the mode changed - animate the button bar transition.
		if(_UIMode != priviousMode) {
			AnimationUtils.slideButtonBar(_buttonBar, this);
		} else {
			updateButtonBar();
		}		
	}

	private void renderUIEditMode() {
		int minute;		
		
		_minutesTextView.setVisibility(View.VISIBLE);
		_secondsTextView.setVisibility(View.GONE);		
		_currentModeTextView.setVisibility(View.GONE);
		
		if(_selectedMinute >= 0) {
			minute = _selectedMinute;
		} else {				
			minute = _currentMinute;
		}

		renderArc(TimerUtils.generateAngleFromMinute(_currentMinute + 1), ARC_ACTIVE_ID_KEY, 1, R.color.timer_active_color, 255);
		renderArc(TimerUtils.generateAngleFromMinute(minute), ARC_EDIT_TIME_ID_KEY, 2, R.color.timer_select_time_color, 150);
		
		updateCurrentTime(minute, 0);			

	}

	private void renderUIActiveMode() {
		_selectedMinute = -1;
		
		// If the user is coming from edit mode - remove the edit mode arc.
		if(findViewById(ARC_EDIT_TIME_ID_KEY) != null) {
			_content.removeView(_content.findViewById(ARC_EDIT_TIME_ID_KEY));
		}
		
		renderArc(TimerUtils.generateAngleFromMinute(_currentMinute + 1), ARC_ACTIVE_ID_KEY, 1, R.color.timer_active_color, 255);
		updateCurrentTime(_currentMinute, _currentSeconds);

		_minutesTextView.setVisibility(View.VISIBLE);
		_secondsTextView.setVisibility(View.VISIBLE);			
		_currentModeTextView.setVisibility(View.GONE);
		
		try {
			_countDownService.send(Message.obtain(null, CountDownTimerService.MSG_SET_SELECTED_MINUTE, _selectedMinute, 0));
		} catch (RemoteException e) {
			// TODO: handle exception
		}
	}

	private void renderUIBaseMode() {
		renderArc(TimerUtils.generateAngleFromMinute(_selectedMinute), ARC_ACTIVE_ID_KEY, 1, R.color.timer_active_color, 255);		
		updateCurrentTime(_selectedMinute, 0);

		_secondsTextView.setVisibility(View.GONE);
		
		if(_selectedMinute == 0){
			Animation anim = AnimationUtils.slideHide(_buttonBar, this);
			anim.setAnimationListener(new AnimationListener() {
				
				@Override
				public void onAnimationStart(Animation animation) {}
				
				@Override
				public void onAnimationRepeat(Animation animation) {}
				
				@Override
				public void onAnimationEnd(Animation animation) {
					_buttonBar.setVisibility(View.INVISIBLE);
				}
			});
			_buttonBar.setAnimation(anim);
			
			_minutesTextView.setVisibility(View.GONE);
			
			_currentModeTextView.setVisibility(View.VISIBLE);
			_currentModeTextView.setText(R.string.set_time);
		} else {
			if(_buttonBar.getVisibility() == View.INVISIBLE) {
				_buttonBar.setVisibility(View.VISIBLE);
				_buttonBar.setAnimation(AnimationUtils.slideShow(_buttonBar, this));
			}
			
			_minutesTextView.setVisibility(View.VISIBLE);
			
			_currentModeTextView.setVisibility(View.GONE);
		}
	}

	void updateButtonBar() {
		if(_UIMode == CountDownTimerService.MODE_BASE) {
			renderButtonBarBaseMode();
		
		} else if(_UIMode == CountDownTimerService.MODE_ACTIVE) {
			renderButtonBarActiveMode();
		
		} else if(_UIMode == CountDownTimerService.MODE_EDIT_TIME) {
			renderButtonBarEditMode();
		};
	}
	
	/**
	 * Send a message to the timer service to change to UI Mode to the provided <b>mode</b>. The updating also 
	 * sends a message to request the timer info from the service hence updating the UI.
	 * @param mode
	 */
	private void updateUIMode(int mode) {
		try {
			_countDownService.send(Message.obtain(null, CountDownTimerService.MSG_SET_MODE, mode, 0));
		} catch (RemoteException e) {
			// TODO: handle exception
		}
		
		requestTimerInfoFromCountDownTimerService();
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
	protected void onUserLeaveHint()
	{
	    super.onUserLeaveHint();
		// If the timer is running and the user has exited the application themself,
		// show toast that the timer is active.
		if(_isTimerStarted){
			UIUtils.showToast(this, R.string.timer_still_running);
		}
	}
	
	@Override
	protected void onDestroy() {	
		
		// NOTE: when exiting the application, the UI mode is set to active.
		if(isFinishing()  && _UIMode == CountDownTimerService.MODE_EDIT_TIME) {
			updateUIMode(CountDownTimerService.MODE_ACTIVE);
		}

		// If the timer is running and the user has exited the application themself,
		// show toast that the timer is active.
		if(isFinishing() && _isTimerStarted){
			UIUtils.showToast(this, R.string.timer_still_running);
		}
		
		doUnbindFromCountDownService();
		
		super.onDestroy();
	}
}
