package com.alexlabs.trackmovement.utils;

import android.app.Activity;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.ImageView;

import com.alexlabs.trackmovement.MainActivity;
import com.alexlabs.trackmovement.R;

public class AnimationUtils {
	
	public static class PulsationAnimation {
		
		private static class IntervalIterator {
			int index;
			int direction;
			int count;
			
			public IntervalIterator (ViewGroup anim){
				index = 0;
				direction = 0;
				count = anim.getChildCount();
			}
			
			public int getPrevIndex(){
				return index - direction;
			}
			
			public void next() {
				if(index == count - 1) {
					direction = -1;
				} else if(index == 0) {
					direction = 1;
				}
				
				index += direction;
			}
		}
		
		private boolean _isStarted;
		private Handler h = new Handler();
		private Runnable r;
		
		private IntervalIterator w;
		
		public void toggleTimerSignalAnimation(Activity activity, boolean isTimerStarted) {
			final ViewGroup anim = (ViewGroup) activity.findViewById(R.id.pulsatingCircleView);
			View pulsatingCircleBackground = activity.findViewById(R.id.pulsatingCicrleBackground);
			
			if(_isStarted == isTimerStarted) {
				return;
			} else if(_isStarted == true && isTimerStarted == false) {
				h.removeCallbacks(r);
				_isStarted = isTimerStarted;
				pulsatingCircleBackground.setVisibility(View.INVISIBLE);
				// We need to hide the last visible circle from the animation
				((ImageView) anim.getChildAt(w.getPrevIndex())).setVisibility(View.INVISIBLE);
				return;
			}
			
			_isStarted = true;
			w = new IntervalIterator(anim);
			pulsatingCircleBackground.setVisibility(View.VISIBLE);
			
			r = new Runnable() {
			
				@Override
				public void run() {
					int prevIndex = w.getPrevIndex();
					((ImageView) anim.getChildAt(prevIndex)).setVisibility(View.INVISIBLE);
					((ImageView) anim.getChildAt(w.index)).setVisibility(View.VISIBLE);
					w.next();
					h.postDelayed(this, 50);				
				}
			};
			activity.runOnUiThread(r);
		}
	}
		
	public static void slideButtonBar(final View buttonBarLayout, final Activity activity){
		Animation slideHide = slideHide(buttonBarLayout, activity);
		slideHide.setAnimationListener(new AnimationListener() {
			
			@Override
			public void onAnimationStart(Animation animation) {}
			
			@Override
			public void onAnimationRepeat(Animation animation) {}
			
			@Override
			public void onAnimationEnd(Animation animation) {
				((MainActivity)activity).updateButtonBar();
				buttonBarLayout.startAnimation(slideShow(buttonBarLayout, activity));
			}
		});
		buttonBarLayout.startAnimation(slideHide);
	}
	
	public static Animation slideShow(final View buttonBarLayout, Activity activity){
		int anim;
		if(UIUtils.isLandscape(activity)) {
			anim = R.anim.anim_slide_right;
		} else {
			anim = R.anim.anim_slide_up;
		}
		
		return android.view.animation.AnimationUtils.loadAnimation(activity, anim);
	}
	
	public static Animation slideHide(final View buttonBarLayout, Activity activity){
		int anim;
		if(UIUtils.isLandscape(activity)) {
			anim = R.anim.anim_slide_left;
		} else {
			anim = R.anim.anim_slide_down;
		}
		
		return android.view.animation.AnimationUtils.loadAnimation(activity, anim);
	}
}
