package com.honeybadger;

/*--------------------------------------------------------------------------------------------------------------------------------
 * Version: 1.1
 * Date of last modification: 4 MAR 2012
 --------------------------------------------------------------------------------------------------------------------------------
 */

import com.honeybadger.api.SharedMethods;
import com.honeybadger.api.databases.AppsDBAdapter;
import com.honeybadger.api.databases.LogDBAdapter;
import com.honeybadger.api.scripts.Scripts;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

import java.lang.Runnable;

public class SplashScreen extends Activity
{
	String startScript = "";

	SharedPreferences settings;
	SharedPreferences.Editor editor;

	private AppsDBAdapter appAdapter;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// Splash screen view
		setContentView(R.layout.splash);

		new Handler().postDelayed(new Runnable()
		{
			public void run()
			{
				settings = getSharedPreferences("main", 1);
				editor = settings.edit();

				boolean check = SharedMethods.installIPTables(SplashScreen.this, settings, editor);
				if (check == true)
				{
					sendNotification();
				}

				if (settings.getBoolean("fwEnabled", true))
				{
					startScript = SharedMethods.initialString(startScript, SplashScreen.this);

					startScript = SharedMethods
							.setLogging(startScript, settings, SplashScreen.this);

					startScript = SharedMethods.setBlock(startScript, settings, SplashScreen.this);
				}

				// Launch Script
				Intent script = new Intent(SplashScreen.this, Scripts.class);
				script.putExtra("script", startScript);
				startService(script);

				if (!settings.getBoolean("4_0", false))
				{
					upgrade();
				}

				// Load apps if not already added
				if (!settings.getBoolean("loaded", false))
				{
					SharedMethods.loadApps(SplashScreen.this, settings, appAdapter);
					SharedPreferences.Editor editor = settings.edit();
					editor.putBoolean("loaded", true);
					editor.commit();
				}

				Intent intent = new Intent(SplashScreen.this, HBTabActivity.class);
				startActivity(intent);
			}
		}, 100);

	}

	/**
	 * Used to send system notification that the IPTables binary has been
	 * installed. This is done by starting the {@link HoneyBadgerNotify}
	 * activity.
	 */
	@SuppressWarnings("deprecation")
	public void sendNotification()
	{
		NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		Notification notification = new Notification(R.drawable.icon,
				"IPTables has been installed.", System.currentTimeMillis());

		PendingIntent contentI = PendingIntent.getActivity(this, 1, new Intent(this,
				HoneyBadgerNotify.class), 0);

		notification.setLatestEventInfo(this, "HoneyBadger", "IPTables has been installed.",
				contentI);

		manager.notify(2, notification);
	}

	public void upgrade()
	{
		LogDBAdapter db = new LogDBAdapter(this);
		db.open();
		db.clearLog();
		db.close();
		
		editor.putBoolean("4_0", true);
	}
}