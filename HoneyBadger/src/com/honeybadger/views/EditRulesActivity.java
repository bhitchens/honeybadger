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

import com.honeybadger.HoneyBadgerNotify;
import com.honeybadger.R;
import com.honeybadger.api.Blocker;
import com.honeybadger.api.Fetcher;
import com.honeybadger.api.databases.RulesDBAdapter;

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
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

public class EditRulesActivity extends Activity
{

	Button CommitButton;
	Button FetchButton;

	Button CheckIn;
	Button CheckOut;

	Button BlockAllow;

	String in = "null";
	String out = "null";
	String allow = "allow";
	String ipAddress = "null";
	String urlAddress = "null";
	String port = "null";
	String source = "";

	EditText ipEdit;
	EditText urlEdit;

	SharedPreferences settings;
	SharedPreferences.Editor editor;

	RulesDBAdapter rulesDB = new RulesDBAdapter(this);

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.editrule_viewer);

		settings = getSharedPreferences("main", 0);

		CommitButton = (Button) findViewById(R.id.buttonCommit);
		FetchButton = (Button) findViewById(R.id.buttonDownload);

		urlEdit = (EditText) findViewById(R.id.urlEntry);
		ipEdit = (EditText) findViewById(R.id.ipEntry);

		CheckIn = (CheckBox) findViewById(R.id.checkIn);
		CheckOut = (CheckBox) findViewById(R.id.checkOut);

		createListeners();
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
					in = "true";
				}
				else
				{
					in = "false";
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
					out = "true";
				}
				else
				{
					out = "false";
				}
			}
		});

		CommitButton.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				ipAddress = EditRulesActivity.this.ipEdit.getText().toString();
				urlAddress = EditRulesActivity.this.urlEdit.getText().toString();

				commitRule();

				// http://stackoverflow.com/questions/4850493/open-a-dialog-when-i-click-a-button
				// http://developer.android.com/guide/topics/ui/dialogs.html#AlertDialog
			}
		});

		FetchButton.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				fetchIPs();
				sendUpdateNotification();

				// http://stackoverflow.com/questions/4850493/open-a-dialog-when-i-click-a-button
				// http://developer.android.com/guide/topics/ui/dialogs.html#AlertDialog
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

		Intent start = new Intent();
		start.setClass(this, Fetcher.class);
		start.putExtra(
				"script",
				getDir("bin", 0)
						+ "/iptables -F FETCH"
						+ "\n"
						+ "busybox wget http://www.malwaredomainlist.com/mdlcsv.php -O - | "
						+ "busybox egrep -o '[[:digit:]]{1,3}\\.[[:digit:]]{1,3}\\.[[:digit:]]{1,3}\\.[[:digit:]]{1,3}'");
		startService(start);
	}

	/**
	 * Sends Android notification to user that the malicious IP address database
	 * has been updated.
	 */
	public void sendUpdateNotification()
	{
		NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		Notification notification = new Notification(R.drawable.icon,
				"Malicious Domains have been successfully updated.", System.currentTimeMillis());

		PendingIntent contentI = PendingIntent.getActivity(this, 1, new Intent(this,
				HoneyBadgerNotify.class), 0);

		notification.setLatestEventInfo(this, "Malicious Domains",
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
		if (!(ipAddress == "null" & urlAddress == "null") & !(in == "null" & out == "null"))
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

			if (in == "true" & out == "true")
			{
				direction = "both";
			}
			else if (in == "true")
			{
				direction = "in";
			}
			else
			{
				direction = "out";
			}

			if (direction == "both")
			{
				rulesDB.createEntry(source, port, "in", allow, domain);
				rulesDB.createEntry(source, port, "out", allow, domain);
			}
			else
			{
				rulesDB.createEntry(source, port, direction, allow, domain);
			}

			launchCommitDialog();
		}
		else
		{
			Toast.makeText(
					EditRulesActivity.this,
					"You must enter either an IP Address or port number, and specify direction of traffic.",
					Toast.LENGTH_LONG).show();
		}
		rulesDB.close();
		ipAddress = "null";
		in = "null";
	}

	/**
	 * Launches dialog box informing user that the rule has been added to the
	 * database, but not yet applied and provides option to do or not do so.
	 */
	private void launchCommitDialog()
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(EditRulesActivity.this);
		builder.setMessage(
				"The rule has been saved but has not been applied to the firewall.  Would you like to apply it now?")
				.setCancelable(false)
				.setPositiveButton("Yes", new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int id)
					{
						Intent myIntent = new Intent(EditRulesActivity.this, Blocker.class);
						myIntent.putExtra("reload", "false");
						startService(myIntent);
						clear();
					}
				}).setNegativeButton("No", new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int id)
					{
						dialog.cancel();
					}
				});
		
		AlertDialog alert = builder.create();
		alert.show();
	}
	
	private void clear()
	{
		urlEdit = (EditText) findViewById(R.id.urlEntry);
		ipEdit = (EditText) findViewById(R.id.ipEntry);

		CheckIn = (CheckBox) findViewById(R.id.checkIn);
		CheckOut = (CheckBox) findViewById(R.id.checkOut);
		
		urlEdit.setText("");
		ipEdit.setText("");
		((CompoundButton) CheckIn).setChecked(false);
		((CompoundButton) CheckOut).setChecked(false);
	}

}
