package com.alexlabs.trackmovement;

import java.util.Locale;

import com.alexlabs.trackmovement.dialogs.ConfirmScheduledAramDialog;
import com.alexlabs.trackmovement.dialogs.ConfrimClearTimerDialog;
import com.alexlabs.trackmovement.utils.AnimationUtils;
import com.alexlabs.trackmovement.utils.TimerUtils;
import com.alexlabs.trackmovement.utils.UIUtils;

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
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

	private static final boolean SHOULD_USE_TESTING_FEATURES = false;
	
	private final static int ARC_ACTIVE_ID_KEY = 0;
	private final static int ARC_EDIT_TIME_ID_KEY = 1;
	private final static int ARC_SUPPORT_EDIT_TIME_ID_KEY = 2;
	private final static int TIME_CIRCLE_ID_KEY = 3;
	private final static int LIGHT_BEAM_ID_KEY = 4;

	// dial view
	private RelativeLayout _content;
	private TextView _minutesTextView;
	private TextView _secondsTextView;
	private TextView _messageTextView;
	
	// button bar
	private View _buttonBar;
	private ImageButton _startStopStateButton;
	private ImageButton _editTimeButton;
	private ImageButton _editTimeAcceptChangeButton;
	private ImageButton _editTimeCancelChangeButton;
	private ImageButton _settingsButton;
	private ImageButton _clearTimerButton;

	// events
	private MotionEvent _motionEvent;

	// flags
	private boolean _isTimerStarted;
	private int _timerState;
	
	// This flag shows weather the user has picked a different time while in the EDIT mode.
	// While in the edit mode, the user may not perform any changes but still click accept.
	// This flag eliminates any ambiguities as to the user's actions.
	private boolean _isTimeEdited;
	
	// time
	private int _selectedMinute;
	private int _preveouslySetTime;
	private int _currentMinute;
	private int _currentSeconds;
	private int _UIMode = CountDownTimerService.MODE_BASE;
	
	// broadcast receivers
	private ScreenReceiver _screenReceiver;
	
	// state
	private boolean _isActivityRunning;
	// Flag indicating when the light beam should be shown on the screen.
	private boolean _shouldShowLightBeam = false;
	
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

		private void extractDataFromBundle(final Bundle info) {
			// timer state related
			int timerState = info.getInt(CountDownTimerService.TIMER_STATE);
			_isTimerStarted = timerState == CountDownTimerService.TIMER_STATE_STARTED;
			_timerState = CountDownTimerService.TIMER_STATE_FINISHED;
			
			// time related
			_selectedMinute = info.getInt(CountDownTimerService.SELECTED_MINUTE);
			_preveouslySetTime = info.getInt(CountDownTimerService.PREVEOUSLY_SET_TIME);
			_currentMinute = info.getInt(CountDownTimerService.CURRENT_MINUTE);
			_currentSeconds = info.getInt(CountDownTimerService.CURRENT_SECONDS);
			
			renderAll(info.getInt(CountDownTimerService.MODE));
		}
    }
    
    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    private Messenger _clientMessenger;
    
    private ServiceConnection _serviceConnetcion;

	/////////////////////////////////////////////////
	/////////// View
	/////////////////////////////////////////////////

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		getWindow().requestFeature(Window.FEATURE_ACTION_BAR);

		super.onCreate(savedInstanceState);
		
		// Unlock only non-secure lock key guards.
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
		
		setContentView(R.layout.activity_main);

		// Hide the action bar.
		getSupportActionBar().hide();

		_content = (RelativeLayout) findViewById(R.id.content);
		_minutesTextView = (TextView) findViewById(R.id.minutesTextView);
		_secondsTextView = (TextView) findViewById(R.id.secondsTextView);
		_messageTextView = (TextView) findViewById(R.id.message_text_view);
		
		_buttonBar = findViewById(R.id.buttonArea);
		_startStopStateButton = (ImageButton) findViewById(R.id.start_stop_state_button);
		_settingsButton = (ImageButton) findViewById(R.id.settings_button);
		
		_settingsButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent i = new Intent(MainActivity.this, SettingsActivity.class);
				i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				getBaseContext().startActivity(i);
			}
		});
		
		initEditTimeButtonGroup();
		initClearTimerButton();
		
		_content.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View view, MotionEvent event) {
				int action = event.getActionMasked();
				_motionEvent = event;

				switch (action) {
				case MotionEvent.ACTION_DOWN:
					// When the user touches the screen the light beam can
					// be displayed.
					_shouldShowLightBeam = true;
					view.performClick();
					break;
					
				case MotionEvent.ACTION_MOVE:
					if (_UIMode != CountDownTimerService.MODE_ACTIVE) {
						_shouldShowLightBeam = true;
						onActionMove(getBaseContext(), _content);
					}
					break;
					
				case MotionEvent.ACTION_UP:
					// When the user's is no longer touching the screen
					// the light beam should not be displayed. 
					_shouldShowLightBeam = false;
					view.performClick();
					break;
					
				case MotionEvent.ACTION_CANCEL:
					_shouldShowLightBeam = false;
					view.performClick();
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
				if (_UIMode == CountDownTimerService.MODE_BASE){					
					if (_selectedMinute == 0) {
						UIUtils.showToast(getBaseContext(), R.string.pick_time_prompt);

						_secondsTextView.setVisibility(View.GONE);
						return;
					}
					
					setTimerState(CountDownTimerService.MSG_START_TIMER);
					
				} else if (_UIMode == CountDownTimerService.MODE_ACTIVE) {
					if (_isTimerStarted) {
						setTimerState(CountDownTimerService.MSG_PAUSE_TIMER);
						
					} else {
						setTimerState(CountDownTimerService.MSG_UNPAUSE_TIMER);					
					}
				}
				
				updateUIMode(CountDownTimerService.MODE_ACTIVE);

				// Change layout of the message text view when the state is altered.
				updateMessageTextViewLayout();
			}

		});
		
		runCountDownTimerService();
        
		// Register a broadcast receiver that saves the previous state of the
		// screen - whether it was on or off.
		registerScreenReciver();
	}

	/**
	 * Start the service if it has not been started and bind to it.
	 */
	private void runCountDownTimerService() {
		_clientMessenger = new Messenger(new IncomingHandler());
	    
	    _serviceConnetcion = new ServiceConnection() {
			
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
		
		startService(new Intent(MainActivity.this, CountDownTimerService.class));
		
		doBindToCountDownService();
	}
	
	@Override
	protected void onRestart() {
		super.onRestart();

		Log.d("ALEX_LABS", "MainActivity is ReStarted");
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		Log.d("ALEX_LABS", "MainActivity is Started");
		_isActivityRunning = true;
		// NOTE: Every time the app is started the 'Keep screen awake' flag needs to be set
		// if the user has specified it. This code is not in onCreate(), because
		// despite what the docs say, when the app is closed the device screen is not
		// put to sleep if the 'Keep screen awake' preference has been turned on.
		// To solve this issue we have to set the flag when the activity is started and
		// clear it in onStop() when the activity is stopped.
		Preferences prefs = new Preferences();
		if (prefs.getShouldKeepScreenAwake()) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
	}

	/**
	 * Shows the confirmation dialog for confirming the alarm.
	 * <br/><br/>
	 * <b>NOTE:</b> When the activity is restarted (either after it was destroyed or simply stopped) by the count down timer service
	 * it quickly alternates its life cycle state in the chain start->stop->start. The dialog's show() method is called when
	 * the activity is in the stopped state as the first change start->stop is almost immediate. This causes an exception that crashes
	 * the app as a dialog(or any other view or fragment) cannot be displayed when the activity is stopped. To overcome this
	 * a worker(background) thread is started which checks the state of the activity every 50 ms and when the activity is in
	 * the started(running) state, the dialog is displayed.
	 */
	private void showConfirmationDialog() {
		final FragmentManager manager = getSupportFragmentManager();
		
		final Handler handler = new Handler();
		
		final Thread th = new Thread() {
			
			@Override
			public void run() {
				while(!_isActivityRunning) {
					try {
						sleep(50);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} 
				
				if(retrieveConfirmSchedulingAlarmDialog(manager) == null) {
					// NOTE: when we are sure the dialog is safe to be shown, the code
					// for displaying it must be put on the UI thread, which is accomplished
					// with the post() method from the Handler class.
					handler.post(new Runnable() {
						
						@Override
						public void run() {
							// Show the dialog at the most convenient time.				
							DialogFragment d = ConfirmScheduledAramDialog.newInstance(R.string.timer_finished);
							d.setCancelable(false);
							d.show(manager, ConfirmScheduledAramDialog.TAG);
						}
					});
					
					interrupt();
				}
				
			}
		};
		
		th.start();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		Log.d("ALEX_LABS", "MainActivity is Resumed");
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
				// When the changes are accepted, we have to take the appropriate action based on
				// weather the user actually edited the time. If he did not, we simply return back to the previous
				// mode without making any changes to the current time.
				if(_isTimeEdited) {
					if(_selectedMinute > 0) {
						// apply the new time
						setTimerState(CountDownTimerService.MSG_START_TIMER);
						updateUIMode(CountDownTimerService.MODE_ACTIVE);
					} else if(_selectedMinute == 0){
						// when the timer is 0, the timer will finish immediately, setting everything to 
						// BASE mode.
						setTimerState(CountDownTimerService.MSG_START_TIMER);
					}
				} else {
					// simply return to active mode without doing any work.
					updateUIMode(CountDownTimerService.MODE_ACTIVE);
				} 
			}
			
		});
	}
	
	private void initClearTimerButton(){
		_clearTimerButton = (ImageButton) findViewById(R.id.clear_timer_button);
		_clearTimerButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				ConfrimClearTimerDialog timerClearConfirmationDialog = new ConfrimClearTimerDialog();
				timerClearConfirmationDialog.show(getFragmentManager(), ConfrimClearTimerDialog.TAG);
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
	
	private void renderCircle(float angle, int circleId, int index, int color, int alpha) {
		// Remove the current time circle. This is done so that a new circle will be
		// generated with the newly selected angle.
		_content.removeView(_content.findViewById(circleId));

		// Create the new circle from the new angle that has been selected.
		TimeCircle circle = new TimeCircle(getBaseContext(), _content, angle, color, alpha);

		// Set the circle view's id.
		circle.setId(circleId);

		// Add the circle to the content view of the clock.
		_content.addView(circle, index);
	}
	
	private void renderLightBeam(float angle, int beamId, int index, int color, int alpha) {
		// Remove the currently drawn light beam, so that it can be redrawn with new
		// coordinates if the user has moved their finger across the screen. 
		_content.removeView(_content.findViewById(beamId));
		
		if (_shouldShowLightBeam) {			
			LightBeam lightBeam = new LightBeam(getBaseContext(), _content, _motionEvent, angle, color, alpha);			
			lightBeam.setId(beamId);			
			_content.addView(lightBeam, index);
		}
	}
	
	private void onActionMove(Context context, View content) {
		if (_motionEvent == null)
			throw new IllegalArgumentException("Motion event is null.");

		_selectedMinute = TimerUtils.generateMinute(_motionEvent, context, content);
		
		// If a minute is selected while in the edit mode, the edit time flag has to be set.
		if(_UIMode == CountDownTimerService.MODE_EDIT_TIME) {
			_isTimeEdited = true;
		}
		
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
        if(_serviceConnetcion != null) {
        	unbindService(_serviceConnetcion);
        }
    }

	public void registerScreenReciver() {
		final IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		_screenReceiver = new ScreenReceiver();
		registerReceiver(_screenReceiver, filter);
	}
	
	/**
	 * This method only renders the UI interface based on the UI mode the user is currently in.
	 * To set the mode use {@link#updateUIMode}
	 * @param newMode
	 */
	AnimationUtils.PulsationAnimation pa = new AnimationUtils.PulsationAnimation();
	public void renderAll(int newMode) {
		int priviousMode = _UIMode;
		_UIMode = newMode;
		
		renderCircle(TimerUtils.generateAngleFromMinute(_preveouslySetTime), TIME_CIRCLE_ID_KEY, 1, R.color.timer_select_time_color, 255);
		
		// The light beam is active and is displayed only when the user is not in Active mode.
		if(_UIMode != CountDownTimerService.MODE_ACTIVE) {			
			renderLightBeam(TimerUtils.generateAngleFromMinute(_selectedMinute), LIGHT_BEAM_ID_KEY, 1, R.color.white, 200);
		}
		
		if(_UIMode == CountDownTimerService.MODE_BASE) {			
			renderUIBaseMode();
		
		} else if(_UIMode == CountDownTimerService.MODE_ACTIVE) {			
			renderUIActiveMode();
		
		} else if(_UIMode == CountDownTimerService.MODE_EDIT_TIME) {
			renderUIEditMode();
		
		} else if(_UIMode == CountDownTimerService.MODE_WAITING_FOR_CONFIRMATION) {
			renderUIBaseMode();
			showConfirmationDialog();
		} else {
			throw new IllegalArgumentException();
		}
		
		pa.toggleTimerSignalAnimation(this, _isTimerStarted);
		//AnimationUtils.toggleTimerSignalAnimation(this, _isTimerStarted);
		
		// If the mode changed - animate the button bar transition.
		if(_UIMode != priviousMode) {
			AnimationUtils.slideButtonBar(_buttonBar, this);
			// Change layout of the message text view when the mode is altered.
			updateMessageTextViewLayout();
		} else {
			updateButtonBar();
		}	
		
	}

	// FIXME is message appropriate
	private void updateMessageTextViewLayout() {
		_messageTextView.setBackgroundDrawable(getResources().getDrawable(R.drawable.rounded_corners_mode_change_background));
		final Handler h = new Handler();
		h.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				_messageTextView.setBackgroundDrawable(getResources().getDrawable(R.drawable.rounded_corners_background));
				h.removeCallbacks(this);
			}
		}, 1000);
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
		
		// The minute that will be displayed depends on weather the user has picked a different
		// time or has just entered edit mode. In the latter case, we just show the current minute
		// of the timer.
		if(_isTimeEdited) {
			minute = _selectedMinute;
		} else {				
			minute = _currentMinute;
		}

		// NOTE: at the lowest level, a pure red arc is created. Above it is a green arc that shows the current time. Lastly,
		// at the top, an arc that is opaque is displayed that shows selected time over the green arc.
		
		renderArc(TimerUtils.generateAngleFromMinute(minute), ARC_SUPPORT_EDIT_TIME_ID_KEY, 2, R.color.red, 255);
		renderArc(TimerUtils.generateAngleFromTime(_currentMinute, _currentSeconds), ARC_ACTIVE_ID_KEY, 3, R.color.timer_active_color, 255);
		renderArc(TimerUtils.generateAngleFromMinute(minute), ARC_EDIT_TIME_ID_KEY, 4, R.color.timer_select_time_color, 120);
		
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
		setMessageViewText(_isTimerStarted ? R.string.timer_state_running : R.string.timer_state_paused);
		
		// The user has no right to edit the time in this mode. Any circumstance in which
		// the user transitions to this state clears the "edited time" flag.
		_isTimeEdited = false;
		// Accordingly, the selected minute has to be cleared as well.
		_selectedMinute = 0;		
		
		removeEditModeArcs();
		
		renderArc(TimerUtils.generateAngleFromTime(_currentMinute, _currentSeconds), ARC_ACTIVE_ID_KEY, 1, R.color.timer_active_color, 255);
		updateCurrentTime(_currentMinute, _currentSeconds);

		_minutesTextView.setVisibility(View.VISIBLE);
		_secondsTextView.setVisibility(View.VISIBLE);
		
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
		
		float angle = TimerUtils.generateAngleFromMinute(_selectedMinute);
		renderArc(angle, ARC_ACTIVE_ID_KEY, 2, R.color.timer_select_time_color, 255);		
		updateCurrentTime(_selectedMinute, 0);
		
		// Make visible the buttons for the base mode.
		_secondsTextView.setVisibility(View.GONE);
		_buttonBar.setVisibility(View.VISIBLE);
		_minutesTextView.setVisibility(View.VISIBLE);
		
		setMessageViewText(R.string.set_time);
	}

	public void updateButtonBar() {
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

		Log.d("ALEX_LABS", "MainActivity is Stopped");
		_isActivityRunning = false;
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		// NOTE: when exiting the application, the UI mode is set to active.
		if(_UIMode == CountDownTimerService.MODE_EDIT_TIME) {
			updateUIMode(CountDownTimerService.MODE_ACTIVE);
		}

		// If the timer is running and the user has exited the application itself,
		// show toast that the timer is running.
		if(_isTimerStarted && _timerState != CountDownTimerService.TIMER_STATE_FINISHED){
			UIUtils.showToast(this, R.string.timer_still_running);
		}
		
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}
	
	@Override
	protected void onUserLeaveHint() {
		super.onUserLeaveHint();
		
		if(_timerState == CountDownTimerService.TIMER_STATE_FINISHED) {
			AlarmBell.sendStopAlarmNoiseAndVibrationMessage(_countDownService);
		}
	}
	
	@Override
	protected void onDestroy() {
		Log.d("ALEX_LABS", "MainActivity is Destoryed");
		super.onDestroy();
		
		unregisterReceiver(_screenReceiver);
		
		doUnbindFromCountDownService();
	}
}
