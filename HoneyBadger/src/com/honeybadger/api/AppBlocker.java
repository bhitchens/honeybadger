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

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Binder;
import android.os.IBinder;

public class AppBlocker extends Service
{
	SharedPreferences settings;
	private AppsDBAdapter appAdapter;
	private final IBinder mBinder = new MyBinder();

	public int onStartCommand(Intent intent, int flags, int startId)
	{
		settings = getSharedPreferences("main", 0);
		createAppRules(this);
		return START_NOT_STICKY;
	}

	/**
	 * Uses database of applications and status of those applications to create rules.
	 * 
	 * @param context
	 */
	public void createAppRules(Context context)
	{
		String script = "";

		// Perform action on clicks
		appAdapter = new AppsDBAdapter(context);
		appAdapter.open();
		Cursor c = appAdapter.fetchAllEntries();
		if (!settings.getBoolean("block", false))
		{
			script += context.getDir("bin", 0) + "/iptables -D OUTPUT -j ACCEPTOUT" + "\n";
			while (c.getPosition() < c.getCount() - 1)
			{
				c.moveToNext();
				if (c.getString(3).contains("block"))
				{
					script += context.getDir("bin", 0)
							+ "/iptables -D OUTPUT -m owner --uid-owner " + c.getInt(0)
							+ " -j ACCEPTOUT" + "\n";
					script += context.getDir("bin", 0)
							+ "/iptables -D OUTPUT -m owner --uid-owner " + c.getInt(0)
							+ " -j DROPOUT" + "\n";
					script += context.getDir("bin", 0)
							+ "/iptables -A OUTPUT -m owner --uid-owner " + c.getInt(0)
							+ " -j DROPOUT" + "\n";
				}
				else
				{
					script += context.getDir("bin", 0)
							+ "/iptables -D OUTPUT -m owner --uid-owner " + c.getInt(0)
							+ " -j DROPOUT" + "\n";
				}
			}
			script += context.getDir("bin", 0) + "/iptables -A OUTPUT -j ACCEPTOUT" + "\n";
		}
		else
		{
			script += context.getDir("bin", 0) + "/iptables -D OUTPUT -j DROPOUT" + "\n";
			while (c.getPosition() < c.getCount() - 1)
			{
				c.moveToNext();
				if (!c.getString(2).contains("block"))
				{
					script += context.getDir("bin", 0)
							+ "/iptables -D OUTPUT -m owner --uid-owner " + c.getInt(0)
							+ " -j DROPOUT" + "\n";
					script += context.getDir("bin", 0)
							+ "/iptables -D OUTPUT -m owner --uid-owner " + c.getInt(0)
							+ " -j ACCEPTOUT" + "\n";
					script += context.getDir("bin", 0)
							+ "/iptables -A OUTPUT -m owner --uid-owner " + c.getInt(0)
							+ " -j ACCEPTOUT" + "\n";
				}
				else
				{
					script += context.getDir("bin", 0)
							+ "/iptables -D OUTPUT -m owner --uid-owner " + c.getInt(0)
							+ " -j ACCEPTOUT" + "\n";
				}
			}
			script += context.getDir("bin", 0) + "/iptables -A OUTPUT -j DROPOUT" + "\n";
		}
		appAdapter.close();

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
