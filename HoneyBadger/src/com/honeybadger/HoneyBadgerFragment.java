package com.honeybadger;

/*--------------------------------------------------------------------------------------------------------------------------------
 * Author(s): Todd Berry Ann, Alex Harris, Brad Hitchens
 * Version: 2.1
 * Date of last modification: 19 June 2012
 *
 * Edit 2.1: Added method call to load apps; conformed to change from StartUp to SharedMethods
 *--------------------------------------------------------------------------------------------------------------------------------
 */

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.honeybadger.R.id;
import com.honeybadger.api.AppBlocker;
import com.honeybadger.api.Blocker;
import com.honeybadger.api.SharedMethods;
import com.honeybadger.api.databases.AppsDBAdapter;
import com.honeybadger.api.databases.LogDBAdapter;
import com.honeybadger.api.scripts.RequirementsScript;
import com.honeybadger.views.EditPreferencesActivity;
import com.honeybadger.views.AddRulesFragment;

public class HoneyBadgerFragment extends SherlockFragment
{
	Menu optionsMenu;
	MenuItem fwEnabledItem;

	Intent rec;

	SharedPreferences settings;
	SharedPreferences.Editor editor;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		settings = getActivity().getSharedPreferences("main", 1);
		editor = settings.edit();

		if (!settings.getBoolean("suppressWarn", false))
		{
			IntentFilter filter = new IntentFilter("com.honeybadger.ERROR");
			rec = getActivity().registerReceiver(new Receiver(), filter);

			Intent checkRequirements = new Intent(getActivity(), RequirementsScript.class);
			checkRequirements.putExtra("script",
					"iptables -L FORWARD\nbusybox wget -O - http://www.google.com");
			getActivity().startService(checkRequirements);
		}

		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		final View v = inflater.inflate(R.layout.home, container, false);

		AppRater.app_launched(getActivity());

		return v;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		inflater.inflate(R.menu.menu, menu);

		optionsMenu = menu;
		fwEnabledItem = optionsMenu.findItem(id.fw_enabled);

		if (settings.getBoolean("fwEnabled", true))
		{
			fwEnabledItem.setTitle("Disable HB");
		}
		else
		{
			fwEnabledItem.setTitle("Enable HB");
		}
	}

	/**
	 * Used to handle selection of items in options menu. Starts activity for
	 * selected item. If "Settings" is selected, then
	 * {@link EditPreferencesActivity} is started. If "Clear Log" is selected,
	 * then {@link LogDBAdapter} is used to clear the log. If "Add Rule" is
	 * selected, then {@link AddRulesFragment} is started.
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
					String disableScript = getActivity().getDir("bin", 0) + "/iptables -F\n"
							+ getActivity().getDir("bin", 0) + "/iptables -X\n";
					SharedMethods.execScript(disableScript);

					editor.putBoolean("fwEnabled", false);
					this.fwEnabledItem.setTitle("Enable HB");

					Toast.makeText(getActivity(), "HoneyBadger Firewall Disabled",
							Toast.LENGTH_SHORT).show();
				}
				else
				{
					String startScript = "";

					settings = getActivity().getSharedPreferences("main", 1);

					startScript = SharedMethods.initialString(startScript, getActivity());

					startScript = SharedMethods.setLogging(startScript, settings, getActivity());

					startScript = SharedMethods.setBlock(startScript, settings, getActivity());

					// Launch Script
					SharedMethods.execScript(startScript);

					// reload rules
					Intent reload = new Intent(getActivity(), Blocker.class);
					reload.putExtra("reload", "true");
					getActivity().startService(reload);

					// reload app rules
					Intent reloadApps = new Intent(getActivity(), AppBlocker.class);
					getActivity().startService(reloadApps);

					// reload auto-generated rules if specified
					if (settings.getBoolean("generate", false)
							| settings.getBoolean("autoUpdate", false))
					{
						SharedMethods.fetch(getActivity());
					}

					AppsDBAdapter appAdapter = new AppsDBAdapter(getActivity());
					;
					SharedMethods.loadApps(getActivity(), settings, appAdapter);

					editor.putBoolean("fwEnabled", true);
					this.fwEnabledItem.setTitle("Disable HB");
				}
				editor.commit();
				editor.clear();

				return true;
			case R.id.settings:
				Intent prefIntent = new Intent(getActivity(), EditPreferencesActivity.class);
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
				createDialog(0);
			}
			else if (error.contains("busybox"))
			{
				createDialog(1);
			}
			else if (error.contains("wget"))
			{
				createDialog(2);
			}
		}
	}

	protected Dialog createDialog(int error)
	{
		Dialog d;
		AlertDialog.Builder builder;
		switch (error)
		{
			case 0:
				builder = new AlertDialog.Builder(getActivity());
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
				builder = new AlertDialog.Builder(getActivity());

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
				builder = new AlertDialog.Builder(getActivity());

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