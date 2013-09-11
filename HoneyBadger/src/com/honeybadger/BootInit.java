package com.honeybadger;

/*--------------------------------------------------------------------------------------------------------------------------------
 * Version: 4.0
 * Date of last modification: 12FEB13
 *
 * Edit 2.1: Loads app rules
 * Edit 4.0: Clean up
 *--------------------------------------------------------------------------------------------------------------------------------
 */

import com.honeybadger.api.AppBlocker;
import com.honeybadger.api.Blocker;
import com.honeybadger.api.SharedMethods;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.widget.Toast;

public class BootInit extends BroadcastReceiver
{
	SharedPreferences settings;

	@Override
	public void onReceive(Context context, Intent intent)
	{
		Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show();
		Log.d("test", "started");
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
				.setSmallIcon(R.drawable.icon).setContentTitle("New App Detected")
				.setContentText("Honeybadger is currently ");

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

		String startScript = "";

		settings = context.getSharedPreferences("main", 1);

		startScript = SharedMethods.initialString(startScript, context);

		startScript = SharedMethods.setLogging(startScript, settings, context);

		startScript = SharedMethods.setBlock(startScript, settings, context);

		// Launch Script
		SharedMethods.execScript(startScript);

		// reload rules
		Intent reload = new Intent(context, Blocker.class);
		reload.putExtra("reload", "true");
		context.startService(reload);

		// reload app rules
		Intent reloadApps = new Intent(context, AppBlocker.class);
		context.startService(reloadApps);

		// reload auto-generated rules if specified
		if (settings.getBoolean("generate", false) | settings.getBoolean("autoUpdate", false))
		{
			SharedMethods.fetch(context);
		}

		SharedMethods.loadApps(context, settings);// , appAdapter);
	}

}
