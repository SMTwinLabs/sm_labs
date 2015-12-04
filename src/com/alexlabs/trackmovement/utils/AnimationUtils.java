package com.alexlabs.trackmovement.utils;

import com.alexlabs.trackmovement.MainActivity;
import com.alexlabs.trackmovement.R;
import com.alexlabs.trackmovement.R.anim;
import com.alexlabs.trackmovement.R.id;

import android.animation.AnimatorSet;
import android.app.Activity;
import android.graphics.drawable.AnimationDrawable;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.ImageView;

public class AnimationUtils {

	
	static AnimatorSet _animator = new AnimatorSet();
	public static void toggleTimerSignalAnimation(Activity activity, boolean isTimerStarted) {		
		ImageView pulsatingCircle = (ImageView) activity.findViewById(R.id.pulsatingCircleAnimation);
		
		View pulsatingCircleBackground = activity.findViewById(R.id.pulsatingCicrleBackground);
		
		AnimationDrawable pulsationAnimation = (AnimationDrawable) pulsatingCircle.getDrawable();
		
		if(isTimerStarted){
			pulsatingCircle.setVisibility(View.VISIBLE);
			pulsatingCircle.setLayerType(View.LAYER_TYPE_HARDWARE, null);
			
			pulsatingCircleBackground.setVisibility(View.VISIBLE);
			pulsatingCircleBackground.setLayerType(View.LAYER_TYPE_HARDWARE, null);
			
			pulsationAnimation.start();
			pulsationAnimation.setAlpha(180);

		} else {			
			pulsationAnimation.stop();
			
			pulsatingCircle.setVisibility(View.INVISIBLE);
			pulsatingCircle.setLayerType(View.LAYER_TYPE_NONE, null);
			
			pulsatingCircleBackground.setVisibility(View.INVISIBLE);
			pulsatingCircleBackground.setLayerType(View.LAYER_TYPE_NONE, null);
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
