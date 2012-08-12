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

import com.honeybadger.api.AppBlocker;
import com.honeybadger.api.Blocker;
import com.honeybadger.api.SharedMethods;
import com.honeybadger.api.databases.AppsDBAdapter;
import com.honeybadger.api.databases.RulesDBAdapter;
import com.honeybadger.api.scripts.Scripts;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

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
				SystemClock.sleep(2000);
				settings = getSharedPreferences("main", 1);
				editor = settings.edit();

				boolean check = SharedMethods.installIPTables(SplashScreen.this, settings, editor);
				if (check == true)
				{
					sendNotification();
				}

				startScript = SharedMethods.initialString(startScript, SplashScreen.this);

				startScript = SharedMethods.setLogging(startScript, settings, SplashScreen.this);

				startScript = SharedMethods.setBlock(startScript, settings, SplashScreen.this);

				// Launch Script
				Intent script = new Intent(SplashScreen.this, Scripts.class);
				script.putExtra("script", startScript);
				startService(script);

				if (!settings.getBoolean("2_5", false))
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
	 * Processes splash screen touch events
	 */
	/*
	 * @Override public boolean onTouchEvent(MotionEvent evt) { if
	 * (evt.getAction() == MotionEvent.ACTION_DOWN) { synchronized
	 * (mSplashThread) { mSplashThread.notifyAll(); } } return true; }
	 */

	/**
	 * Used to send system notification that the IPTables binary has been
	 * installed. This is done by starting the {@link HoneyBadgerNotify}
	 * activity.
	 */
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
		RulesDBAdapter rulesAdapter;
		Cursor ruleC;
		Cursor appC;

		editor = settings.edit();
		appAdapter = new AppsDBAdapter(this);
		rulesAdapter = new RulesDBAdapter(this);

		String script;

		try
		{
			// transition app rules
			appAdapter.open();
			appC = appAdapter.fetchAllEntries();
			Log.d("test", "test: " + appC.getCount());// for some reason this
														// makes things work.
			appAdapter.clear();
			while (appC.getPosition() < appC.getCount() - 1)
			{
				appC.moveToNext();
				if (appC.getString(3).contains("block"))
				{
					appAdapter.createEntry(appC.getInt(0), appC.getString(1), appC.getBlob(2),
							"block", "block");
					String rmApp = this.getDir("bin", 0)
							+ "/iptables -D OUTPUT -m owner --uid-owner " + appC.getInt(0)
							+ " -j DROPOUT\n";
					Intent delApp = new Intent(this, Scripts.class);
					delApp.putExtra("script", rmApp);
					startService(delApp);
				}
				else
				{
					appAdapter.createEntry(appC.getInt(0), appC.getString(1), appC.getBlob(2),
							"allow", "allow");
				}
			}
			appC.close();
			appAdapter.close();
			// transition IP rules
			rulesAdapter.open();
			ruleC = rulesAdapter.fetchAllEntriesOld();
			Log.d("test", Integer.toString(ruleC.getCount()));// for some reason
																// this makes
																// things work.
			rulesAdapter.clear();
			while (ruleC.getPosition() < ruleC.getCount() - 1)
			{
				ruleC.moveToNext();
				if (ruleC.getString(4).contains("domain") && ruleC.getString(2).contains("in"))
				{
					// delete rule jumping to chain
					script = this.getDir("bin", 0) + "/iptables -D " + "IN" + "PUT -j "
							+ ruleC.getString(0) + "IN"
							+ "\n"
							// clear chain
							+ this.getDir("bin", 0) + "/iptables -F " + ruleC.getString(0) + "IN"
							+ "\n"
							// delete chain
							+ this.getDir("bin", 0) + "/iptables -X " + ruleC.getString(0) + "IN"
							+ "\n";
				}
				else if (ruleC.getString(4).contains("domain")
						&& ruleC.getString(2).contains("out"))
				{
					script = this.getDir("bin", 0) + "/iptables -D " + "OUT" + "PUT -j "
							+ ruleC.getString(0) + "OUT"
							+ "\n"
							// clear chain
							+ this.getDir("bin", 0) + "/iptables -F " + ruleC.getString(0) + "OUT"
							+ "\n"
							// delete chain
							+ this.getDir("bin", 0) + "/iptables -X " + ruleC.getString(0) + "OUT"
							+ "\n";
				}
				else if (ruleC.getString(2).contains("in"))
				{
					if (ruleC.getString(3).contains("block"))
					{
						script = this.getDir("bin", 0) + "/iptables -D INPUT -s "
								+ ruleC.getString(0) + " -j DROPIN\n";
					}
					else
					{
						script = this.getDir("bin", 0) + "/iptables -D INPUT -s "
								+ ruleC.getString(0) + " -j ALLOWIN\n";
					}
				}
				else
				{
					if (ruleC.getString(3).contains("block"))
					{
						script = this.getDir("bin", 0) + "/iptables -D OUTPUT -d "
								+ ruleC.getString(0)
								+ " -m state --state NEW,RELATED,ESTABLISHED -j DROPOUT\n";
					}
					else
					{
						script = this.getDir("bin", 0) + "/iptables -D OUTPUT -d "
								+ ruleC.getString(0)
								+ " -m state --state NEW,RELATED,ESTABLISHED -j ALLOWOUT\n";
					}
				}

				Log.d("test", script);
				Intent delRule = new Intent(this, Scripts.class);
				delRule.putExtra("script", script);
				startService(delRule);

				rulesAdapter.createEntry(ruleC.getString(0), ruleC.getString(1),
						ruleC.getString(2), ruleC.getString(3), ruleC.getString(4), "wifi");
				rulesAdapter.createEntry(ruleC.getString(0), ruleC.getString(1),
						ruleC.getString(2), ruleC.getString(3), ruleC.getString(4), "cell");

			}
			ruleC.close();
			rulesAdapter.close();
			Intent bApps = new Intent(this, AppBlocker.class);
			startService(bApps);
			Intent bRules = new Intent(this, Blocker.class);
			startService(bRules);

			editor.putBoolean("2_5", true);
			editor.commit();
		}
		catch (Exception e)
		{

		}
	}
}