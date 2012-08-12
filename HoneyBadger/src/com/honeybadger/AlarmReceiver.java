package com.honeybadger;

/*--------------------------------------------------------------------------------------------------------------------------------
 * Author(s): Todd Berry Ann, Brad Hitchens
 * Version: 1.1
 * Date of last modification: 22 April 2012
 * Source Info:    
 |This Broadcast receiver is fired by the alarm to update known malicious IPs.  The use of the Alarm Manager and AlarmReceiver were heavily influenced by
 | the blog at http://www.androidguys.com/2009/04/02/wake-up-with-the-alarm/.  Documentation for Alarm Manager was referenced from http://developer.android.com/reference/android/app/AlarmManager.html.
 |
 --------------------------------------------------------------------------------------------------------------------------------
 */

import java.util.Calendar;

import com.honeybadger.api.scripts.Fetcher;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

public class AlarmReceiver extends BroadcastReceiver
{

	SharedPreferences settings;

	@Override
	/**
	 * Checks if user still has auto update enabled. If so, it sets an alarm for the next day and updates the IPs now.
	 * 
	 */
	public void onReceive(Context context, Intent intent)
	{
		try
		{
			// retrieves shared preferences from honeybadger
			settings = context.getSharedPreferences("main", 1);

			if (settings.getBoolean("autoUpdate", false))
			{

				// gets time for 24 hours from now.
				Calendar cal = Calendar.getInstance();
				cal.add(Calendar.HOUR, 24);
				scheduleUpdate(cal, context);

				// readies the intent to launch Fetcher service that updates
				// IPs.
				Intent start = new Intent(context, Fetcher.class);

				start.putExtra(
						"script",
						context.getDir("bin", 0)
								+ "/iptables -F FETCH"
								+ "\n"
								+ "busybox wget http://www.malwaredomainlist.com/mdlcsv.php -O - | "
								+ "busybox egrep -o '[[:digit:]]{1,3}\\.[[:digit:]]{1,3}\\.[[:digit:]]{1,3}\\.[[:digit:]]{1,3}'");

				sendNotification(context);

				// launches the Fetcher service intent
				context.startService(start);
			}
		}
		catch (Exception e)
		{
			Toast.makeText(
					context,
					"Error Updating Block List. Please check data connection.  If failure continues, manually update.",
					Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}
	}

	/**
	 * Schedules AlarmManager to fire AlarmReceiver 24 hours from now.
	 * 
	 * @param cal
	 * @param context
	 */
	public void scheduleUpdate(Calendar cal, Context context)
	{
		Intent intent = new Intent();
		intent.setClass(context, AlarmReceiver.class);

		PendingIntent sender = PendingIntent.getBroadcast(context, 2, intent,
				PendingIntent.FLAG_UPDATE_CURRENT);

		AlarmManager am = (AlarmManager) context.getSystemService("alarm");
		am.set(AlarmManager.RTC, cal.getTimeInMillis(), sender);
	}

	/**
	 * Sends a notification of success to user.
	 * 
	 * @param context
	 */
	public void sendNotification(Context context)
	{
		NotificationManager manager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);

		Notification notification = new Notification(R.drawable.icon,

		"Malicious IPs have been updated.", System.currentTimeMillis());

		PendingIntent contentI = PendingIntent.getActivity(context, 1,
				new Intent(context, HoneyBadgerNotify.class), 0);

		notification.setLatestEventInfo(context, "HoneyBadger",
				"Malicious IPs have been updated.", contentI);

		manager.notify(2, notification);
	}
}
