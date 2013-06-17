package com.honeybadger;

/*--------------------------------------------------------------------------------------------------------------------------------
 * Version: 4.4
 * Date of last modification: 
 --------------------------------------------------------------------------------------------------------------------------------
 */

import java.io.ByteArrayOutputStream;

import com.honeybadger.api.databases.AppsDBAdapter;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

public class AppReceiver extends BroadcastReceiver
{
	ApplicationInfo appInfo;

	SharedPreferences settings;

	AppsDBAdapter appAdapter;

	String block;

	@Override
	/**
	 * Controls actions when intent is received
	 */
	public void onReceive(Context context, Intent intent)
	{
		appAdapter = new AppsDBAdapter(context);
		appAdapter.open();

		settings = context.getSharedPreferences("main", 1);

		final String action = intent.getAction();

		Uri data = intent.getData();
		String pkgName = data.getEncodedSchemeSpecificPart();

		if (settings.getBoolean("block", false))
		{
			block = "block";
		}
		else
		{
			block = "allow";
		}

		if (action == Intent.ACTION_PACKAGE_ADDED)
		{
			try
			{
				appInfo = context.getPackageManager().getApplicationInfo(pkgName, 0);
				if (!settings.getBoolean("hideIcons", false))
				{
					Drawable d = appInfo.loadIcon(context.getPackageManager());

					BitmapDrawable bitIcon;
					Bitmap bm;

					try
					{
						bitIcon = (BitmapDrawable) d;
						bm = bitIcon.getBitmap();
					}
					catch (Exception e)
					{
						bm = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
					}

					ByteArrayOutputStream stream = new ByteArrayOutputStream();
					bm.compress(Bitmap.CompressFormat.JPEG, 100, stream);
					byte[] imageInByte = stream.toByteArray();

					appAdapter.createEntry(appInfo.uid,
							appInfo.loadLabel(context.getPackageManager()).toString(), imageInByte,
							block, block);
				}
				else
				{
					appAdapter.createEntry(appInfo.uid,
							appInfo.loadLabel(context.getPackageManager()).toString(),
							new byte[] {}, block, block);
				}
			}
			catch (NameNotFoundException e)
			{
				appAdapter.close();
			}

			appAdapter.close();

			notify(context, block, appInfo.loadLabel(context.getPackageManager()).toString());
		}
		else if (action == Intent.ACTION_PACKAGE_REMOVED)
		{
			Bundle b = intent.getExtras();
			if (!b.getBoolean(Intent.EXTRA_REPLACING))
			{
				appAdapter.deleteEntry(intent.getExtras().getInt(Intent.EXTRA_UID));
				appAdapter.close();
			}
		}
	}

	/**
	 * Notifies user of action HB is taking on new application
	 * @param context context received
	 * @param block String indicating whether application is blocked or allowed
	 * @param name String giving name of application
	 */
	private void notify(Context context, String block, String name)
	{
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
				.setSmallIcon(R.drawable.icon).setContentTitle("New App Detected")
				.setContentText("Honeybadger is currently " + block + "ing " + name);

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
