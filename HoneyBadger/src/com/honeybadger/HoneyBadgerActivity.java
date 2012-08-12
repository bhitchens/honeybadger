package com.honeybadger;

/*--------------------------------------------------------------------------------------------------------------------------------
 * Author(s): Todd Berry Ann, Alex Harris, Brad Hitchens
 * Version: 2.1
 * Date of last modification: 19 June 2012
 * Source Info:    
 * The majority of the form code used in this activity is the adaptation of tutorials from the Android Developers Resource page  
 * located at the following link: http://developer.android.com/resources/tutorials/views/hello-formstuff.html
 *
 * Edit 2.1: Added method call to load apps; conformed to change from StartUp to SharedMethods
 *--------------------------------------------------------------------------------------------------------------------------------
 */

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.honeybadger.api.databases.LogDBAdapter;
import com.honeybadger.api.scripts.RequirementsScript;
import com.honeybadger.views.EditPreferencesActivity;
import com.honeybadger.views.EditRulesActivity;
import com.honeybadger.views.ViewLogActivity;
import com.honeybadger.views.ViewRulesActivity;

public class HoneyBadgerActivity extends Activity
{

	Menu optionsMenu = null;
	public static Context stcCtx;

	/**
	 * Called when the activity is first created; it ensures that the IPTables
	 * is installed, generates and launches a script string based on settings,
	 * sets the view, and creates buttons for launching
	 * {@link ViewRulesActivity}, {@link EditRulesActivity}, and
	 * {@link ViewLogActivity}.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// Set the view for the activity
		setContentView(R.layout.home);

		AppRater.app_launched(this);

		IntentFilter filter = new IntentFilter("com.honeybadger.ERROR");
		registerReceiver(new Receiver(), filter);

		Intent checkRequirements = new Intent(this, RequirementsScript.class);
		checkRequirements.putExtra("script", "iptables -L FORWARD\nbusybox wget -O - http://www.google.com");
		startService(checkRequirements);		
	}

	/**
	 * Launches an options menu when the device options button is selected. The
	 * basic structure of this method is from the book <i>Pro Android 2</i>.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	/**
	 * Used to handle selection of items in options menu. Starts activity for
	 * selected item. If "Settings" is selected, then
	 * {@link EditPreferencesActivity} is started. If "Clear Log" is selected,
	 * then {@link LogDBAdapter} is used to clear the log. If "Add Rule" is
	 * selected, then {@link EditRulesActivity} is started.
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{

		// Handle item selection
		switch (item.getItemId())
		{
			case R.id.settings:
				Intent prefIntent = new Intent(this, EditPreferencesActivity.class);
				startActivity(prefIntent);
				return true;
			case R.id.clearLog:
				LogDBAdapter logDB = new LogDBAdapter(this);
				logDB.open();
				logDB.clearLog();
				logDB.close();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private class Receiver extends BroadcastReceiver
	{

		@Override
		public void onReceive(Context context, Intent intent)
		{
			String error = intent.getExtras().getString("error");

			if (error.contains("iptables"))
			{
				AlertDialog.Builder builder = new AlertDialog.Builder(HoneyBadgerActivity.this);

				builder.setMessage(
						"Iptables is not compatible with your phone's kernel. The firewall will not work. You may be able to fix this by installing a new ROM.")
						.setCancelable(true)
						.setNeutralButton("OK", new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialog, int id)
							{
								dialog.cancel();
							}
						});

				AlertDialog alert = builder.create();
				alert.show();
			}
			else if (error.contains("busybox"))
			{
				AlertDialog.Builder builder = new AlertDialog.Builder(HoneyBadgerActivity.this);

				builder.setMessage(
						"Busybox was not found on your device. Logging and the option to automatically generate rules from an online database will not function. Please ensure that you have busybox properly installed.")
						.setCancelable(true)
						.setNeutralButton("OK", new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialog, int id)
							{
								dialog.cancel();
							}
						});

				AlertDialog alert = builder.create();
				alert.show();
			}
			else if (error.contains("wget"))
			{
				AlertDialog.Builder builder = new AlertDialog.Builder(HoneyBadgerActivity.this);

				builder.setMessage(
						"Wget exists but is not functioning properly. The option to automatically generate rules from an online database will not function. This occures when you either lack network connectivity or busybox was not configured to allow wget to properly use DNS.")
						.setCancelable(true)
						.setNeutralButton("OK", new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialog, int id)
							{
								dialog.cancel();
							}
						});

				AlertDialog alert = builder.create();
				alert.show();
			}

		}
	}

}