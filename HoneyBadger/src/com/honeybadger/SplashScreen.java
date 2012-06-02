package com.honeybadger;

/*--------------------------------------------------------------------------------------------------------------------------------
 * Author(s): Alex Harris
 * Version: 1.1
 * Date of last modification: 4 MAR 2012
 * Source Info:    
 |Information regarding the creation of a splash screen was obtained and adapted from the following resource created by Igor Kushnarev:
 |http://www.codeproject.com/Articles/113831/An-Advanced-Splash-Screen-for-Android-App
 |
 --------------------------------------------------------------------------------------------------------------------------------
 */

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;

public class SplashScreen extends Activity
{

	/**
	 * The thread to process splash screen events
	 * 
	 */
	private Thread mSplashThread;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// Splash screen view
		setContentView(R.layout.splash);

		final SplashScreen sPlashScreen = this;

		// The thread to wait for splash screen events
		mSplashThread = new Thread()
		{
			@Override
			public void run()
			{
				try
				{
					synchronized (this)
					{
						// Wait given period of time or exit on touch
						wait(5000);
					}
				}
				catch (InterruptedException ex)
				{
				}

				finish();

				// Run next activity
				Intent intent = new Intent();
				intent.setClass(sPlashScreen, HoneyBadgerActivity.class);
				startActivity(intent);
				stop();
			}
		};

		mSplashThread.start();
	}

	/**
	 * Processes splash screen touch events
	 */
	@Override
	public boolean onTouchEvent(MotionEvent evt)
	{
		if (evt.getAction() == MotionEvent.ACTION_DOWN)
		{
			synchronized (mSplashThread)
			{
				mSplashThread.notifyAll();
			}
		}
		return true;
	}

}