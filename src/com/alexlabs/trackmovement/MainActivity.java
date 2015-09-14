package com.alexlabs.trackmovement;

import java.util.Locale;

import com.alexlabs.trackmovement.dialogs.ConfirmScheduledAramDialog;

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
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MainActivity extends ActionBarActivity {	

	/////////////////////////////////////////////////
	/////////// Model
	/////////////////////////////////////////////////

	private final static int ARC_ACTIVE_ID_KEY = 0;
	private final static int ARC_EDIT_TIME_ID_KEY = 1;
	private final static int ARC_SUPPORT_EDIT_TIME_ID_KEY = 2;

	// dial view
	private RelativeLayout _content;
	private TextView _minutesTextView;
	private TextView _secondsTextView;
	private TextView _currentModeTextView;
	private TextView _messageTextView;
	
	// button bar
	private View _buttonBar;
	private ImageButton _startStopStateButton;
	private ImageButton _editTimeButton;
	private ImageButton _editTimeAcceptChangeButton;
	private ImageButton _editTimeCancelChangeButton;
	private ImageButton _settingsButton;

	// events
	private MotionEvent _motionEvent;

	// flags
	private boolean _isTimerStarted;
	private boolean _isWaitingForConfirmation;
	
	// time
	private int _selectedMinute;
	private int _currentMinute;
	private int _currentSeconds;
	private int _UIMode = CountDownTimerService.MODE_BASE;
	
	// broadcast receivers
	private ScreenReceiver _screenReceiver;
	
	// interprocess communication
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
    			
    			if (_isTimerStarted && _currentSeconds % 10 == 0) {
					renderAll(_UIMode);
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
			// timer state related
			int timerState = info.getInt(CountDownTimerService.TIMER_STATE);
			_isTimerStarted = timerState == CountDownTimerService.TIMER_STATE_STARTED;
			_isWaitingForConfirmation = timerState == CountDownTimerService.TIMER_STATE_FINISHED;
			
			// time related
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
		_currentModeTextView = (TextView) findViewById(R.id.modeView);
		_messageTextView = (TextView) findViewById(R.id.message_text_view);
		
		_buttonBar = findViewById(R.id.buttonArea);
		_startStopStateButton = (ImageButton) findViewById(R.id.start_stop_state_button);
		_settingsButton = (ImageButton) findViewById(R.id.settings_button);
		
		// FIXME remove after testing
		_settingsButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				try {
					_countDownService.send(Message.obtain(null, CountDownTimerService.MSG_SET_SELECTED_MINUTE, 0, 0));
				} catch (RemoteException e) {
					e.printStackTrace();
				}
				
				setTimerState(CountDownTimerService.MSG_START_TIMER);
			}
		});
		
		initEditTimeButtonGroup();
		
		_content.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View view, MotionEvent event) {
				int action = event.getActionMasked();
				_motionEvent = event;

				switch (action) {
				case MotionEvent.ACTION_DOWN:
					view.performClick();
					break;
					
				case MotionEvent.ACTION_MOVE:
					if (_UIMode != CountDownTimerService.MODE_ACTIVE) {
						onActionMove(getBaseContext(), _content);
					}
					break;
					
				case MotionEvent.ACTION_UP:
					break;
					
				case MotionEvent.ACTION_CANCEL:
					break;
				}
				
				return true;
			}
		});

		_content.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (_UIMode != CountDownTimerService.MODE_ACTIVE) {
					onActionMove(getBaseContext(), _content);
				} else {
					UIUtils.showSetNewTimeToast(MainActivity.this);				
				}
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
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		  
        doBindToCountDownService();
	}
	
	private void initEditTimeButtonGroup() {
		initEditTimeButton();
		initEditTimeAcceptChangeButton();
		initEditTimeCancelChangeButton();
	}

	private void initEditTimeCancelChangeButton() {
		_editTimeCancelChangeButton = (ImageButton) findViewById(R.id.edit_time_cancel_change_button);
		_editTimeCancelChangeButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				updateUIMode(CountDownTimerService.MODE_ACTIVE);
			}
		});
	}

	private void initEditTimeButton() {
		_editTimeButton = (ImageButton) findViewById(R.id.edit_time_button);
		_editTimeButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				updateUIMode(CountDownTimerService.MODE_EDIT_TIME);
			}
		});
	}

	private void initEditTimeAcceptChangeButton() {
		_editTimeAcceptChangeButton = (ImageButton) findViewById(R.id.edit_time_accept_change_button);
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
			_startStopStateButton.setImageResource(_isTimerStarted ? R.drawable.ic_pause_circle_outline_white_48dp : R.drawable.ic_play_circle_outline_white_48dp);
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
		_minutesTextView.setText(String.format("%02d", minute));
		_secondsTextView.setText(String.format(":%02d", seconds));
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
		
		AnimationUtils.toggleTimerSignalAnimation(this, _isTimerStarted);
		
		// If the mode changed - animate the button bar transition.
		if(_UIMode != priviousMode) {
			AnimationUtils.slideButtonBar(_buttonBar, this);
		} else {
			updateButtonBar();
		}
		
		if(_isWaitingForConfirmation){
			final FragmentManager manager = getSupportFragmentManager();
			if(retrieveConfirmSchedulingAlarmDialog(manager) == null) {
				DialogFragment d = ConfirmScheduledAramDialog.newInstance(_countDownService, R.string.timer_finished);
				d.setCancelable(false);
				d.show(manager, ConfirmScheduledAramDialog.TAG);
			}
		}
	}
	
	private DialogFragment retrieveConfirmSchedulingAlarmDialog(FragmentManager manager){
		DialogFragment d = (DialogFragment) manager.findFragmentByTag(ConfirmScheduledAramDialog.TAG);
		return d;
	}

	private void renderUIEditMode() {
		int minute;
		
		setMessageViewText(R.string.set_new_time);
		
		_minutesTextView.setVisibility(View.VISIBLE);
		_secondsTextView.setVisibility(View.GONE);		
		_currentModeTextView.setVisibility(View.GONE);
		
		if(_selectedMinute >= 0) {
			minute = _selectedMinute;
		} else {				
			minute = _currentMinute;
		}

		// NOTE: at the lowest level, a pure red arc is created. Above it is a green arc that shows the current time. Lastly,
		// at the top, an arc that is opaque is displayed that shows selected time over the green arc.
		
		renderArc(TimerUtils.generateAngleFromMinute(minute), ARC_SUPPORT_EDIT_TIME_ID_KEY, 1, R.color.red, 255);
		renderArc(TimerUtils.generateAngleFromTime(_currentMinute, _currentSeconds), ARC_ACTIVE_ID_KEY, 2, R.color.timer_active_color, 255);
		renderArc(TimerUtils.generateAngleFromMinute(minute), ARC_EDIT_TIME_ID_KEY, 3, R.color.timer_select_time_color, 120);
		
		updateCurrentTime(minute, 0);
	}

	/**
	 * Message is converted to upper case.
	 * @param messageResId
	 */
	private void setMessageViewText(int messageResId) {
		_messageTextView.setText(new String(getString(messageResId)).toUpperCase(Locale.ENGLISH));
	}

	private void renderUIActiveMode() {
		setMessageViewText(_isTimerStarted ? R.string.timer_state_active : R.string.timer_state_paused);
		
		_selectedMinute = -1;		
		
		removeEditModeArcs();
		
		renderArc(TimerUtils.generateAngleFromTime(_currentMinute, _currentSeconds), ARC_ACTIVE_ID_KEY, 1, R.color.timer_active_color, 255);
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

	/**
	 * If the user is coming from edit mode - remove any arcs specific to the edit mode.
	 */
	private void removeEditModeArcs() {
		if(findViewById(ARC_EDIT_TIME_ID_KEY) != null) {
			_content.removeView(_content.findViewById(ARC_EDIT_TIME_ID_KEY));
		}
		
		if(findViewById(ARC_SUPPORT_EDIT_TIME_ID_KEY) != null){
			_content.removeView(_content.findViewById(ARC_SUPPORT_EDIT_TIME_ID_KEY));
		}
	}

	private void renderUIBaseMode() {
		// If the timer finishes while the current mode is EDIT_MODE, remove all edit mode
		// specific arcs.
		removeEditModeArcs();
		
		renderArc(TimerUtils.generateAngleFromMinute(_selectedMinute), ARC_ACTIVE_ID_KEY, 1, R.color.timer_select_time_color, 255);		
		updateCurrentTime(_selectedMinute, 0);

		_secondsTextView.setVisibility(View.GONE);
		_buttonBar.setVisibility(View.VISIBLE);
		
		if(_selectedMinute == 0){			
			_minutesTextView.setVisibility(View.GONE);
			
			_currentModeTextView.setVisibility(View.VISIBLE);
			_currentModeTextView.setText(R.string.set);
		} else {
			_minutesTextView.setVisibility(View.VISIBLE);
			
			_currentModeTextView.setVisibility(View.GONE);
		}
		
		setMessageViewText(R.string.set_time);
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
	
	public Messenger getCountDownTimerService(){
		return _countDownService;
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
	
	// NOTE: onDestory() is not called when the screen is turned off, when the home button is pressed
	// and when the user navigates to the overview screen. However, onStop() is called in all scenarios
	// mentioned above including the scenario when the back button is pressed.
	@Override
	protected void onStop() {
		super.onStop();	
		
		// NOTE: when exiting the application, the UI mode is set to active.
		if(_UIMode == CountDownTimerService.MODE_EDIT_TIME) {
			updateUIMode(CountDownTimerService.MODE_ACTIVE);
		}

		// If the timer is running and the user has exited the application itself,
		// show toast that the timer is active.
		if(_isTimerStarted){
			UIUtils.showToast(this, R.string.timer_still_running);
		}
		
		doUnbindFromCountDownService();		
	}
	
	@Override
	protected void onUserLeaveHint() {
		super.onUserLeaveHint();
		
		if(_isWaitingForConfirmation) {
			AlarmBell.sendStopAlarmNoiseAndVibrationMessage(_countDownService);
		}
	}
}
