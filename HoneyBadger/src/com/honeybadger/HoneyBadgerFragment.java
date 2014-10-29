package com.honeybadger;

/*--------------------------------------------------------------------------------------------------------------------------------
 * Version: 4.51
 * Date of last modification: 29OCT14
 *
 * Edit 2.1  (19JUN12): Added method call to load apps; conformed to change from StartUp to SharedMethods
 * Edit 4.5  (11SEP13): Revamp of database interaction
 * Edit 4.51 (29OCT14): Prevented false busybox failure due to "not found" being in the Google.com page
 *--------------------------------------------------------------------------------------------------------------------------------
 */

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
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
import com.honeybadger.api.databases.DBLog;
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
			String check = SharedMethods.execScript("iptables -L FORWARD\nbusybox wget -O - http://www.google.com");
			if (check.contains("can't initialize"))
			{
				createDialog(0);
				return;
			}
			if (check.contains("busybox: not found"))
			{
				createDialog(1);
				return;
			}
			if (check.contains("bad address"))
			{
				createDialog(2);
				return;
			}
			if (check.contains("not allowed") || check == "")
			{
				createDialog(3);
				return;
			}
		}
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		final View v = inflater.inflate(R.layout.view_home, container, false);

		AppRater.app_launched(getActivity());

		return v;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		inflater.inflate(R.menu.menu_home, menu);

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
	 * then {@link DBLog} is used to clear the log. If "Add Rule" is
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
					
					SharedMethods.loadApps(getActivity(), settings);

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

	protected void createDialog(int error)
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
				d.show();
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
				d.show();
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
				d.show();
				break;
			case 3:
				builder = new AlertDialog.Builder(getActivity());

				builder.setMessage(
						"It appears that your phone is not rooted or that you have not given SU permission to the app. This application requires a rooted phone and SU permission.\n\nThis warning may be suppressed in the application settings.")
						.setCancelable(true)
						.setNeutralButton("OK", new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialog, int id)
							{
								dialog.dismiss();
							}
						});
				d = builder.create();
				d.show();
				break;
			default:
				d = null;
		}
	}
}