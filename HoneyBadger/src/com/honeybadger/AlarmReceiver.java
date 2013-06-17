package com.honeybadger;

/*--------------------------------------------------------------------------------------------------------------------------------
 * Version: 4.4
 * Date of last modification: 17JUN13
 * 
 * Edit 4.4: Brought notification into standard compliance
 * 
 * This Broadcast receiver is fired by the alarm to update known malicious IPs.  
 --------------------------------------------------------------------------------------------------------------------------------
 */

import java.util.Calendar;

import com.honeybadger.api.SharedMethods;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
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

				SharedMethods.fetch(context);

				sendNotification(context);
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
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
				.setSmallIcon(R.drawable.icon)
				.setContentTitle("Malicious IPs Updated")
				.setContentText(
						"Honeybadger has updated the malicious IPs from which you are protected");

		// Creates an explicit intent for an Activity
		Intent resultIntent = new Intent(context, HBTabActivity.class);

		// The stack builder object will contain an artificial back stack for
		// the started Activity.
		// This ensures that navigating backward from the Activity leads out of
		// the application to the Home screen.
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
		
		// Adds the back stack for the Intent (but not the Intent itself)
		stackBuilder.addParentStack(HBTabActivity.class);
		
		// Adds the Intent that starts the Activity to the top of the stack
		stackBuilder.addNextIntent(resultIntent);
		PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,
				PendingIntent.FLAG_UPDATE_CURRENT);
		mBuilder.setContentIntent(resultPendingIntent);
		NotificationManager mNotificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		
		// mId allows you to update the notification later on.
		mNotificationManager.notify(0, mBuilder.build());
	}
}
