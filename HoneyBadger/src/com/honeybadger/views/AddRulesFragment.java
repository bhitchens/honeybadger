package com.honeybadger.views;

/*--------------------------------------------------------------------------------------------------------------------------------
 * Author(s): Alex Harris, Brad Hitchens, Todd Berry Ann
 * Version: 1.3
 * Date of last modification: 14 JUNE 2012
 * Source Info:    
 *The majority of form code is the adaptation of tutorials from the Android Developers Resource page  
 *located at the following link: http://developer.android.com/resources/tutorials/views/hello-formstuff.html
 *Information regarding the creation of an Alert Dialog was obtained and adapted from the following two resources:
 *http://stackoverflow.com/questions/4850493/open-a-dialog-when-i-click-a-button
 *http://developer.android.com/guide/topics/ui/dialogs.html#AlertDialog
 *
 * Edit 1.3: Effected by move of database adapter.
 --------------------------------------------------------------------------------------------------------------------------------
 */

import com.actionbarsherlock.app.SherlockFragment;
import com.honeybadger.HoneyBadgerNotify;
import com.honeybadger.R;
import com.honeybadger.api.Blocker;
import com.honeybadger.api.databases.RulesDBAdapter;
import com.honeybadger.api.scripts.Fetcher;
import com.honeybadger.api.scripts.Scripts;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

public class AddRulesFragment extends SherlockFragment
{

	Button CommitButton;
	Button FetchButton;
	Button ClearButton;

	Button CheckIn;
	Button CheckOut;
	Button CheckWifi;
	Button CheckCell;

	Button BlockAllow;

	Boolean in = false;
	Boolean out = false;
	Boolean wifi = false;
	Boolean cell = false;

	String allow = "allow";
	String ipAddress = "null";
	String urlAddress = "null";
	String port = "null";
	String source = "";

	EditText ipEdit;
	EditText urlEdit;

	SharedPreferences settings;
	SharedPreferences.Editor editor;

	RulesDBAdapter rulesDB;

	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        rulesDB = new RulesDBAdapter(activity);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		setHasOptionsMenu(true);

		final View v = inflater.inflate(R.layout.editrule_viewer, container, false);
		
		settings = getActivity().getSharedPreferences("main", 0);

		CommitButton = (Button) v.findViewById(R.id.buttonCommit);
		if (settings.getBoolean("block", false))
		{
			CommitButton.setText("Allow");
		}
		else
		{
			CommitButton.setText("Block");
		}

		FetchButton = (Button) v.findViewById(R.id.buttonDownload);
		ClearButton = (Button) v.findViewById(R.id.button_clear_download);

		urlEdit = (EditText) v.findViewById(R.id.urlEntry);
		ipEdit = (EditText) v.findViewById(R.id.ipEntry);

		CheckIn = (CheckBox) v.findViewById(R.id.checkIn);
		CheckOut = (CheckBox) v.findViewById(R.id.checkOut);
		CheckWifi = (CheckBox) v.findViewById(R.id.checkWifi);
		CheckCell = (CheckBox) v.findViewById(R.id.checkCell);

		createListeners();
		
		return v;
	}


	/**
	 * Creates listeners for the various buttons
	 */
	private void createListeners()
	{
		CheckIn.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				// Perform action on clicks, depending on whether it's now
				// checked
				if (((CheckBox) v).isChecked())
				{
					in = true;
				}
				else
				{
					in = false;
				}
			}
		});

		CheckOut.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				// Perform action on clicks, depending on whether it's now
				// checked
				if (((CheckBox) v).isChecked())
				{
					out = true;
				}
				else
				{
					out = false;
				}
			}
		});

		CheckWifi.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				// Perform action on clicks, depending on whether it's now
				// checked
				if (((CheckBox) v).isChecked())
				{
					wifi = true;
				}
				else
				{
					wifi = false;
				}
			}
		});

		CheckCell.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				// Perform action on clicks, depending on whether it's now
				// checked
				if (((CheckBox) v).isChecked())
				{
					cell = true;
				}
				else
				{
					cell = false;
				}
			}
		});

		CommitButton.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				ipAddress = AddRulesFragment.this.ipEdit.getText().toString();
				urlAddress = AddRulesFragment.this.urlEdit.getText().toString();

				commitRule();
			}
		});

		FetchButton.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				fetchIPs();
				sendUpdateNotification();
			}
		});

		ClearButton.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				Intent clearScript = new Intent(getActivity(), Scripts.class);
				clearScript.putExtra("script", getActivity().getDir("bin", 0) + "/iptables -F FETCH \n");
				getActivity().startService(clearScript);
				Toast.makeText(
						getActivity(),
						"Downloaded IPs have been cleared.",
						Toast.LENGTH_LONG).show();
			}
		});

	}

	/**
	 * Empties FETCH Chain and then repopulates it from
	 * www.malwaredomainlist.com. Also adds entry to shared preferences
	 * specifying this has been done.
	 */
	public void fetchIPs()
	{
		editor = settings.edit();
		editor.putBoolean("generate", true);
		editor.commit();

		Intent start = new Intent(getActivity(), Fetcher.class);
		start.putExtra(
				"script",
				getActivity().getDir("bin", 0)
						+ "/iptables -F FETCH"
						+ "\n"
						+ "busybox wget http://www.malwaredomainlist.com/mdlcsv.php -O - | "
						+ "busybox egrep -o '[[:digit:]]{1,3}\\.[[:digit:]]{1,3}\\.[[:digit:]]{1,3}\\.[[:digit:]]{1,3}'");
		getActivity().startService(start);
	}

	/**
	 * Sends Android notification to user that the malicious IP address database
	 * has been updated.
	 */
	@SuppressWarnings("deprecation")
	public void sendUpdateNotification()
	{
		NotificationManager manager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);

		Notification notification = new Notification(R.drawable.icon,
				"Malicious Domains have been successfully updated.", System.currentTimeMillis());

		PendingIntent contentI = PendingIntent.getActivity(getActivity(), 1, new Intent(getActivity(),
				HoneyBadgerNotify.class), 0);

		notification.setLatestEventInfo(getActivity(), "Malicious Domains",
				"Known malicious domains are blocked.", contentI);

		manager.notify(2, notification);
	}

	/**
	 * Commits all uncommitted rules in the database.
	 */
	private void commitRule()
	{
		String domain = "";

		rulesDB.open();
		if ((!(ipAddress == "null" & urlAddress == "null") & (in | out)) & (wifi | cell))
		{

			if (settings.getBoolean("block", false))
			{
				allow = "allow";
			}
			else
			{
				allow = "block";
			}

			String direction;
			String netInt;

			if ((ipAddress.length() < 3) & (urlAddress.length() < 3))
			{

			}

			if (ipAddress.length() > 3)
			{
				source = ipAddress;
				domain = "ip";
			}
			else
			{
				source = urlAddress;
				domain = "domain";
			}

			if (in & out)
			{
				direction = "both";
			}
			else if (in)
			{
				direction = "in";
			}
			else
			{
				direction = "out";
			}

			if (wifi & cell)
			{
				netInt = "both";
			}
			else if (wifi)
			{
				netInt = "wifi";
			}
			else
			{
				netInt = "cell";
			}

			if (direction == "both")
			{
				if (netInt == "both")
				{
					rulesDB.createEntry(source, port, "in", allow, domain, "wifi");
					rulesDB.createEntry(source, port, "out", allow, domain, "wifi");
					rulesDB.createEntry(source, port, "in", allow, domain, "cell");
					rulesDB.createEntry(source, port, "out", allow, domain, "cell");
				}
				else
				{
					rulesDB.createEntry(source, port, "in", allow, domain, netInt);
					rulesDB.createEntry(source, port, "out", allow, domain, netInt);
				}
			}
			else
			{
				if (netInt == "both")
				{
					rulesDB.createEntry(source, port, direction, allow, domain, "wifi");
					rulesDB.createEntry(source, port, direction, allow, domain, "cell");
				}
				else
				{
					rulesDB.createEntry(source, port, direction, allow, domain, netInt);
				}
			}

			launchCommitDialog();
		}
		else
		{
			Toast.makeText(
					getActivity(),
					"You must enter either an IP Address or Domain name, and specify direction and interface of traffic.",
					Toast.LENGTH_LONG).show();
		}
		rulesDB.close();
		ipAddress = "null";
	}

	/**
	 * Launches dialog box informing user that the rule has been added to the
	 * database, but not yet applied and provides option to do or not do so.
	 */
	private void launchCommitDialog()
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setMessage("The rule has been applied.").setCancelable(false)
				.setNeutralButton("OK", new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int id)
					{
						Intent myIntent = new Intent(getActivity(), Blocker.class);
						myIntent.putExtra("reload", "false");
						getActivity().startService(myIntent);
						clear();
					}
				});

		AlertDialog alert = builder.create();
		alert.show();
	}

	private void clear()
	{
		urlEdit = (EditText) getActivity().findViewById(R.id.urlEntry);
		ipEdit = (EditText) getActivity().findViewById(R.id.ipEntry);

		CheckIn = (CheckBox) getActivity().findViewById(R.id.checkIn);
		CheckOut = (CheckBox) getActivity().findViewById(R.id.checkOut);
		CheckWifi = (CheckBox) getActivity().findViewById(R.id.checkWifi);
		CheckCell = (CheckBox) getActivity().findViewById(R.id.checkCell);

		urlEdit.setText("");
		ipEdit.setText("");
		((CompoundButton) CheckIn).setChecked(false);
		in = false;
		((CompoundButton) CheckOut).setChecked(false);
		out = false;
		((CompoundButton) CheckWifi).setChecked(false);
		wifi = false;
		((CompoundButton) CheckCell).setChecked(false);
		cell = false;
	}

}
