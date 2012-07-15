package com.honeybadger.api;

/*--------------------------------------------------------------------------------------------------------------------------------
 * Author(s): Brad Hitchens
 * Version: 2.1
 * Date of last modification: 19 June 2012
 * Source Info:    
 *
 * Edit 2.1 (Initial): Uses database of applications and status of those applications to create rules.
 * 
 *--------------------------------------------------------------------------------------------------------------------------------
 */

import com.honeybadger.api.databases.AppsDBAdapter;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class AppBlocker extends IntentService
{
	SharedPreferences settings;
	private AppsDBAdapter appAdapter;
	private final IBinder mBinder = new MyBinder();
	
	public AppBlocker()
	{
		super("AppBlocker");
	}

	protected void onHandleIntent(Intent intent)
	{
		settings = getSharedPreferences("main", 0);
		createAppRules(this);
	}

	/**
	 * Uses database of applications and status of those applications to create
	 * rules.
	 * 
	 * @param context
	 */
	public void createAppRules(Context context)
	{
		String script = "";
		int uid;

		// Perform action on clicks
		appAdapter = new AppsDBAdapter(context);
		appAdapter.open();
		Cursor c = appAdapter.fetchAllEntries();
		if (!settings.getBoolean("block", false))
		{
			// script += context.getDir("bin", 0) +
			// "/iptables -D OUTPUT -j ACCEPTOUT" + "\n";
			while (c.getPosition() < c.getCount() - 1)
			{
				c.moveToNext();
				uid = c.getInt(0);
				if (c.getString(3).contains("block") || c.getString(4).contains("block"))
				{
					script = SharedMethods.ruleBuilder(this, script, "App", Integer.toString(uid),
							false, "ACCEPT", "OUT", c.getString(3).contains("block"), c
									.getString(4).contains("block"));
					script = SharedMethods.ruleBuilder(this, script, "App", Integer.toString(uid),
							false, "DROP", "OUT", c.getString(3).contains("block"), c.getString(4)
									.contains("block"));
					script = SharedMethods.ruleBuilder(this, script, "App", Integer.toString(uid),
							true, "DROP", "OUT", c.getString(3).contains("block"), c.getString(4)
									.contains("block"));
					/*
					 * Log.d("test", Integer.toString(uid) + ", " +
					 * appAdapter.checkBlockW(uid) + ", " +
					 * appAdapter.checkBlockC(uid));
					 */
				}
				else
				{
					script = SharedMethods.ruleBuilder(this, script, "App", Integer.toString(uid),
							false, "DROP", "OUT", !c.getString(3).contains("block"), !c
									.getString(4).contains("block"));
				}

				//Log.d("test", "============" + script);
				Intent scriptIntent = new Intent();
				scriptIntent.setClass(context, Scripts.class);
				scriptIntent.putExtra("script", script);
				context.startService(scriptIntent);
				script = "";
			}
			// script += context.getDir("bin", 0) +
			// "/iptables -A OUTPUT -j ACCEPTOUT" + "\n";
		}
		else
		{
			// script += context.getDir("bin", 0) +
			// "/iptables -D OUTPUT -j DROPOUT" + "\n";
			while (c.getPosition() < c.getCount() - 1)
			{
				c.moveToNext();
				uid = c.getInt(0);
				if (!c.getString(3).contains("block") || !c.getString(4).contains("block"))
				{
					script = SharedMethods.ruleBuilder(this, script, "App", Integer.toString(uid),
							false, "DROP", "OUT", appAdapter.checkBlockW(uid),
							appAdapter.checkBlockC(uid));
					script = SharedMethods.ruleBuilder(this, script, "App", Integer.toString(uid),
							false, "ACCEPT", "OUT", appAdapter.checkBlockW(uid),
							appAdapter.checkBlockC(uid));
					script = SharedMethods.ruleBuilder(this, script, "App", Integer.toString(uid),
							true, "ACCEPT", "OUT", appAdapter.checkBlockW(uid),
							appAdapter.checkBlockC(uid));
				}
				else
				{
					script = SharedMethods.ruleBuilder(this, script, "App", Integer.toString(uid),
							false, "ACCEPT", "OUT", !appAdapter.checkBlockW(uid),
							!appAdapter.checkBlockC(uid));
				}

				Log.d("test", "============" + script);
				Intent scriptIntent = new Intent();
				scriptIntent.setClass(context, Scripts.class);
				scriptIntent.putExtra("script", script);
				context.startService(scriptIntent);
				script = "";
			}
			// script += context.getDir("bin", 0) +
			// "/iptables -A OUTPUT -j DROPOUT" + "\n";
		}
		appAdapter.close();
		//Log.d("test", "============" + script);
		Intent scriptIntent = new Intent();
		scriptIntent.setClass(context, Scripts.class);
		scriptIntent.putExtra("script", script);
		context.startService(scriptIntent);

		c.close();
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		return mBinder;
	}

	public class MyBinder extends Binder
	{
		AppBlocker getService()
		{
			return AppBlocker.this;
		}
	}

}
