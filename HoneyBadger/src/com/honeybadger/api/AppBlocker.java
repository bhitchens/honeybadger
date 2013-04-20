package com.honeybadger.api;

/*--------------------------------------------------------------------------------------------------------------------------------
 * Version: 4.2
 * Date of last modification: 20APR13
 *
 * Edit 2.1 (Initial): Uses database of applications and status of those applications to create rules.
 * Edit 4.2: See method createAppRules
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
	 * Update 4.2 (20APR13): Moved execution of script outside of loops to
	 * reduce number of script executions.
	 * 
	 * @param context
	 */
	public void createAppRules(Context context)
	{
		String script = this.getDir("bin", 0) + "/iptables -F APPS\n";
		int uid;

		// Perform action on clicks
		appAdapter = new AppsDBAdapter(context);
		appAdapter.open();
		Cursor c = appAdapter.fetchAllEntries();
		if (!settings.getBoolean("block", false))
		{
			while (c.getPosition() < c.getCount() - 1)
			{
				c.moveToNext();
				uid = c.getInt(0);
				if (c.getString(3).contains("block") || c.getString(4).contains("block"))
				{
					script = SharedMethods.ruleBuilder(this, script, "App", Integer.toString(uid),
							true, "DROP", "OUT", c.getString(3).contains("block"), c.getString(4)
									.contains("block"));
				}
			}
			SharedMethods.execScript(script);
			script = "";
		}
		else
		{
			while (c.getPosition() < c.getCount() - 1)
			{
				c.moveToNext();
				uid = c.getInt(0);
				if (!c.getString(3).contains("block") || !c.getString(4).contains("block"))
				{
					script = SharedMethods.ruleBuilder(this, script, "App", Integer.toString(uid),
							true, "ACCEPT", "OUT", !appAdapter.checkBlockW(uid),
							!appAdapter.checkBlockC(uid));
				}
			}
			SharedMethods.execScript(script);
			script = "";
		}

		SharedMethods.execScript(script);

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
