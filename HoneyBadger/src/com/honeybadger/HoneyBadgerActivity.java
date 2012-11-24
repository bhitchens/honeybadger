package com.honeybadger;

/*--------------------------------------------------------------------------------------------------------------------------------
 * Author(s): Todd Berry Ann, Alex Harris, Brad Hitchens
 * Version: 2.1
 * Date of last modification: 19 June 2012
 *
 * Edit 2.1: Added method call to load apps; conformed to change from StartUp to SharedMethods
 *--------------------------------------------------------------------------------------------------------------------------------
 */

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.honeybadger.R.id;
import com.honeybadger.api.AppBlocker;
import com.honeybadger.api.Blocker;
import com.honeybadger.api.SharedMethods;
import com.honeybadger.api.databases.AppsDBAdapter;
import com.honeybadger.api.databases.LogDBAdapter;
import com.honeybadger.api.scripts.Fetcher;
import com.honeybadger.api.scripts.RequirementsScript;
import com.honeybadger.api.scripts.Scripts;
import com.honeybadger.views.EditPreferencesActivity;
import com.honeybadger.views.EditRulesActivity;
import com.honeybadger.views.ViewLogActivity;
import com.honeybadger.views.ViewRulesActivity;

public class HoneyBadgerActivity extends Activity
{
	Menu optionsMenu;
	MenuItem fwEnabledItem;
	
	Intent rec;
	
	SharedPreferences settings;
	SharedPreferences.Editor editor;

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

		settings = getSharedPreferences("main", 1);
		editor = settings.edit();

		if (!settings.getBoolean("suppressWarn", false))
		{
			IntentFilter filter = new IntentFilter("com.honeybadger.ERROR");
			rec = registerReceiver(new Receiver(), filter);

			Intent checkRequirements = new Intent(this, RequirementsScript.class);
			checkRequirements.putExtra("script",
					"iptables -L FORWARD\nbusybox wget -O - http://www.google.com");
			startService(checkRequirements);
		}
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
		
		this.optionsMenu = menu;
		fwEnabledItem = optionsMenu.findItem(id.fw_enabled);
		
		if (settings.getBoolean("fwEnabled", true))
		{
			fwEnabledItem.setTitle("Disable HB");
		}
		else
		{
			fwEnabledItem.setTitle("Enable HB");
		}
		
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
			case R.id.fw_enabled:
				if (settings.getBoolean("fwEnabled", true))
				{
					String disableScript = this.getDir("bin", 0) + "/iptables -F\n" + this.getDir("bin", 0) + "/iptables -X\n"; 
					Intent scriptIntent = new Intent(this, Scripts.class);
					scriptIntent.putExtra("script", disableScript);
					this.startService(scriptIntent);
					
					editor.putBoolean("fwEnabled", false);
					this.fwEnabledItem.setTitle("Enable HB");
					
					Toast.makeText(this, "HoneyBadger Firewall Disabled", Toast.LENGTH_SHORT).show();
				}
				else
				{
					String startScript = "";

					settings = this.getSharedPreferences("main", 1);

					startScript = SharedMethods.initialString(startScript, this);

					startScript = SharedMethods.setLogging(startScript, settings, this);

					startScript = SharedMethods.setBlock(startScript, settings, this);

					// Launch Script
					Intent script = new Intent(this, Scripts.class);
					//script.setClass();
					script.putExtra("script", startScript);
					this.startService(script);

					// reload rules
					Intent reload = new Intent(this, Blocker.class);
					//reload.setClass(this, Blocker.class);
					reload.putExtra("reload", "true");
					this.startService(reload);

					// reload app rules
					Intent reloadApps = new Intent(this, AppBlocker.class);
					//reloadApps.setClass(this, AppBlocker.class);
					this.startService(reloadApps);

					// reload auto-generated rules if specified
					if (settings.getBoolean("generate", false) | settings.getBoolean("autoUpdate", false))
					{
						Intent generate = new Intent(this, Fetcher.class);
						//generate.setClass(this, Fetcher.class);
						generate.putExtra(
								"script",
								this.getDir("bin", 0)
										+ "/iptables -F FETCH"
										+ "\n"
										+ "busybox wget http://www.malwaredomainlist.com/mdlcsv.php -O - | "
										+ "busybox egrep -o '[[:digit:]]{1,3}\\.[[:digit:]]{1,3}\\.[[:digit:]]{1,3}\\.[[:digit:]]{1,3}'");
						this.startService(generate);
					}

					AppsDBAdapter appAdapter = new AppsDBAdapter(this);;
					SharedMethods.loadApps(this, settings, appAdapter);				
					
					editor.putBoolean("fwEnabled", true);
					this.fwEnabledItem.setTitle("Disable HB");
				}
				editor.commit();
				editor.clear();				
				
				return true;
			case R.id.settings:
				Intent prefIntent = new Intent(this, EditPreferencesActivity.class);
				startActivity(prefIntent);
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
			final String error = intent.getExtras().getString("error");

			if (error.contains("iptables"))
			{
				showDialog(0);
			}
			else if (error.contains("busybox"))
			{
				showDialog(1);
			}
			else if (error.contains("wget"))
			{
				showDialog(2);
			}
			unregisterReceiver(this);
		}
	}

	@Override
	protected Dialog onCreateDialog(int error)
	{
		Dialog d;
		AlertDialog.Builder builder;
		switch (error)
		{
			case 0:
				builder = new AlertDialog.Builder(HoneyBadgerActivity.this);
				builder.setMessage(
						"Iptables is not compatible with your phone's kernel. The firewall will not work. You may be able to fix this by installing a new ROM.")
						.setCancelable(true)
						.setNeutralButton("OK", new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialog, int id)
							{
								dialog.dismiss();
							}
						});
				d = builder.create();
				break;
			case 1:
				builder = new AlertDialog.Builder(HoneyBadgerActivity.this);

				builder.setMessage(
						"Busybox was not found on your device. Logging and the option to automatically generate rules from an online database will not function. Please ensure that you have busybox properly installed.\n\nThis warning may be suppressed in the application settings.")
						.setCancelable(true)
						.setNeutralButton("OK", new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialog, int id)
							{
								dialog.dismiss();
							}
						});
				d = builder.create();
				break;
			case 2:
				builder = new AlertDialog.Builder(HoneyBadgerActivity.this);

				builder.setMessage(
						"Wget exists but is not functioning properly. The option to automatically generate rules from an online database will not function. This occures when you either lack network connectivity or busybox was not configured to allow wget to properly use DNS.\n\nThis warning may be suppressed in the application settings.")
						.setCancelable(true)
						.setNeutralButton("OK", new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialog, int id)
							{
								dialog.dismiss();
							}
						});
				d = builder.create();
				break;
			default:
				d = null;
		}
		return d;
	}
}