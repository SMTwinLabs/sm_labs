package com.alexlabs.trackmovement;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

public class AnimationUtils {

	public static void toggleTimerSignalAnimation(Activity activity, int _UIMode, boolean _isTimerStarted) {
	
		AnimatorSet animator = new AnimatorSet();
	
		View pulsatingCircle = activity.findViewById(R.id.pulsatingCicrle);
		View pulsatingCircleBackground = activity.findViewById(R.id.pulsatingCicrleBackground);
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
			
			animator.play(fadeAnim).with(animatorScaleXInc);
			animator.play(animatorScaleXInc).with(animatorScaleYInc);
			animator.play(animatorScaleXDec).with(animatorScaleYDec);
			animator.play(animatorScaleXInc).before(animatorScaleXDec);
			animator.setInterpolator(new AccelerateDecelerateInterpolator());
			animator.setDuration(500);
			animator.addListener(new AnimatorListener() {
				
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
			animator.start();
		} else {
			if(animator != null) {
				animator.cancel();
			}
			
			pulsatingCircle.setVisibility(View.INVISIBLE);
			pulsatingCircleBackground.setVisibility(View.INVISIBLE);
		}
	}

}
