package com.honeybadger.views;

/*--------------------------------------------------------------------------------------------------------------------------------
 * Version: 4.21
 * Date of last modification: 25MAY13
 * 4.21 (25MAY13): Fixed bug from using wrong radio button, see checkBoxes()
 *
 --------------------------------------------------------------------------------------------------------------------------------
 */

import java.util.Calendar;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.honeybadger.AlarmReceiver;
import com.honeybadger.R;
import com.honeybadger.api.AppBlocker;
import com.honeybadger.api.SharedMethods;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;

import android.widget.Toast;

public class EditPreferencesActivity extends SherlockFragmentActivity
{

	RadioButton RadioLogOn;
	RadioButton RadioLogOff;

	CheckBox CheckAutoUpdate;
	CheckBox CheckSuppressWarn;

	RadioButton RadioBlock;
	RadioButton RadioAllow;

	Button ButtonViewRules;

	SharedPreferences settings;
	SharedPreferences.Editor editor;

	Boolean logChange = false;
	Boolean blockChange = false;

	RadioButton RadioHome;
	RadioButton RadioApps;
	RadioButton RadioAdd;
	RadioButton RadioView;
	RadioButton RadioLog;

	/**
	 * Called when the activity is first created; allows for modification of
	 * settings.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// get settings and editor
		settings = getSharedPreferences("main", 1);
		editor = settings.edit();

		// set view
		setContentView(R.layout.view_edit_preferences);

		// declare various buttons
		RadioLogOn = (RadioButton) findViewById(R.id.radioLogOn);
		RadioLogOff = (RadioButton) findViewById(R.id.radioLogOff);

		RadioBlock = (RadioButton) findViewById(R.id.radioBlock);
		RadioAllow = (RadioButton) findViewById(R.id.radioAllow);

		CheckAutoUpdate = (CheckBox) findViewById(R.id.checkAutoUpdate);

		CheckSuppressWarn = (CheckBox) findViewById(R.id.checkSuppressWarnings);

		RadioHome = (RadioButton) findViewById(R.id.radioHome);
		RadioApps = (RadioButton) findViewById(R.id.radioApps);
		RadioAdd = (RadioButton) findViewById(R.id.radioAddRules);
		RadioView = (RadioButton) findViewById(R.id.radioViewRules);
		RadioLog = (RadioButton) findViewById(R.id.radioLog);

		ButtonViewRules = (Button) findViewById(R.id.pref_raw_rule_button);

		// make sure proper buttons are checked
		checkBoxes();

		// create listeners for buttons
		createListeners();
	}

	/**
	 * Commits preference edits, checks to see if there have been changes to
	 * logging or blocking preferences, and creates service to implement script
	 * if so.
	 */
	@Override
	public void onStop()
	{
		String script = "";

		super.onStop();
		editor.commit();

		// check to see if change has been made to log settings
		// if so, apply it
		script = checkLogging(logChange, script);

		// check to see if change has been made to block settings
		// if so, apply it
		script = checkBlocking(blockChange, script);

		// send scripts to apply update
		if (logChange | blockChange)
		{
			SharedMethods.execScript(script);
		}
	}

	/**
	 * Check to see if blocking preferences have been changed and return
	 * appropriate script if they have.
	 * 
	 * @param blockChange
	 *            True if changes have been made, false otherwise.
	 * @param script
	 *            Current script is passed into the method.
	 * @return Updated script is returned.
	 */
	private String checkBlocking(Boolean blockChange, String script)
	{
		if (blockChange)
		{
			if (settings.getBoolean("block", false))
			{
				script += this.getDir("bin", 0) + "/iptables -D INPUT -j ACCEPTIN" + "\n"
						+ this.getDir("bin", 0)
						+ "/iptables -D OUTPUT -j ACCEPTOUT"
						+ "\n"
						+ this.getDir("bin", 0)
						+ "/iptables -A INPUT -j DROPIN"
						+ "\n"
						+ this.getDir("bin", 0)
						+ "/iptables -A OUTPUT -m state --state NEW,RELATED,ESTABLISHED -j DROPOUT"
						+ "\n"
						// make sure dns rule isn't duplicated outbound
						+ this.getDir("bin", 0)
						+ "/iptables -D OUTPUT -p udp --dport 53 -j RETURN"
						+ "\n"
						// make sure dns rule isn't duplicated inbound
						+ this.getDir("bin", 0)
						+ "/iptables -D INPUT -p udp --sport 53 -j RETURN"
						+ "\n"
						// add dns rule out
						+ this.getDir("bin", 0) + "/iptables -I OUTPUT -p udp --dport 53 -j RETURN"
						+ "\n"
						// add dns rule in
						+ this.getDir("bin", 0) + "/iptables -I INPUT -p udp --sport 53 -j RETURN"
						+ "\n";
			}
			else
			{
				script += this.getDir("bin", 0) + "/iptables -D INPUT -j DROPIN" + "\n"
						+ this.getDir("bin", 0)
						+ "/iptables -D OUTPUT -m state --state NEW,RELATED,ESTABLISHED -j DROPOUT"
						+ "\n" + this.getDir("bin", 0) + "/iptables -A INPUT -j ACCEPTIN" + "\n"
						+ this.getDir("bin", 0) + "/iptables -A OUTPUT -j ACCEPTOUT"
						+ "\n"
						// delete dns rule outbound
						+ this.getDir("bin", 0) + "/iptables -D OUTPUT -p udp --dport 53 -j RETURN"
						+ "\n"
						// delete dns rule inbound
						+ this.getDir("bin", 0) + "/iptables -D INPUT -p udp --sport 53 -j RETURN"
						+ "\n";
			}
		}
		Intent loadRules = new Intent(this, AppBlocker.class);
		this.startService(loadRules);

		return script;
	}

	/**
	 * Check to see if blocking preferences have been changed and return
	 * appropriate script if they have.
	 * 
	 * @param logChange
	 *            True if changes have been made, false otherwise.
	 * @param script
	 *            Current script is passed into the method.
	 * @return Updated script is returned.
	 */
	private String checkLogging(Boolean logChange, String script)
	{
		if (logChange)
		{
			script += this.getDir("bin", 0)
					+ "/iptables -D OUTPUT -m limit --limit 1/second -j LOG --log-level 7 --log-prefix \"[HoneyBadger - OUTPUT]\" --log-uid"
					+ "\n"
					+ this.getDir("bin", 0)
					+ "/iptables -D INPUT -m limit --limit 1/second -j LOG --log-level 7 --log-prefix \"[HoneyBadger - INPUT]\" --log-uid"
					+ "\n";

			if (settings.getBoolean("log", true))
			{
				script += this.getDir("bin", 0)
						+ "/iptables -A OUTPUT -m limit --limit 1/second -j LOG --log-level 7 --log-prefix \"[HoneyBadger - OUTPUT]\" --log-uid"
						+ "\n"
						+ this.getDir("bin", 0)
						+ "/iptables -A INPUT -m limit --limit 1/second -j LOG --log-level 7 --log-prefix \"[HoneyBadger - INPUT]\" --log-uid"
						+ "\n";
			}
		}
		return script;
	}

	/**
	 * Uses alarm API to schedule daily updates of malicious IP database.
	 * 
	 * @param cal
	 *            Time passed into method.
	 */
	public void scheduleUpdate(Calendar cal)
	{
		Intent intent = new Intent(this, AlarmReceiver.class);

		PendingIntent sender = PendingIntent.getBroadcast(this, 2, intent,
				PendingIntent.FLAG_UPDATE_CURRENT);

		AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
		am.set(AlarmManager.RTC, cal.getTimeInMillis(), sender);
	}

	/**
	 * Ensures that correct boxes are checked based on current preferences.
	 * 
	 * Version 4.21 (25MAY13): Reduced code used.
	 */
	public void checkBoxes()
	{
		// get right thing checked for blocking
		RadioBlock.setChecked(settings.getBoolean("block", false));
		RadioAllow.setChecked(!settings.getBoolean("block", false));
		Log.d("test", "1");

		// get right thing checked for logging
		RadioLogOn.setChecked(settings.getBoolean("log", true));
		RadioLogOff.setChecked(!settings.getBoolean("log", true));
		Log.d("test", "2");
		// check auto-update if necessary
		CheckAutoUpdate.setChecked(settings.getBoolean("autoUpdate", false));
		Log.d("test", "3");
		//check suppress-warning is necessary
		CheckSuppressWarn.setChecked(settings.getBoolean("suppressWarn", false));
		Log.d("test", "4");
		//get right thing checked for start tab
		RadioHome.setChecked(false);
		RadioApps.setChecked(false);
		RadioAdd.setChecked(false);
		RadioView.setChecked(false);
		RadioLog.setChecked(false);
		switch (settings.getInt("selectedTab", 0))
		{
			case 0:
				RadioHome.setChecked(true);
				Log.d("test", "s");
				break;
			case 1:
				RadioApps.setChecked(true);
				break;
			case 2:
				RadioAdd.setChecked(true);
				break;
			case 3:
				RadioView.setChecked(true);
				break;
			case 4:
				RadioLog.setChecked(true);Log.d("test", "log");
				break;
		}
		Log.d("test", "5");
		/*
		 * if (settings.getBoolean("showIcons", true)) {
		 * CheckShowIcons.setChecked(true); }
		 */
	}

	/**
	 * Creates listeners for the various buttons.
	 */
	public void createListeners()
	{
		// Create listeners for the Block/Allow options
		RadioBlock.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				editor.putBoolean("block", true);
				Toast.makeText(EditPreferencesActivity.this, "Traffic Blocked", Toast.LENGTH_SHORT)
						.show();
				blockChange = true;
			}
		});

		RadioAllow.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				editor.putBoolean("block", false);
				Toast.makeText(EditPreferencesActivity.this, "Traffic Allowed", Toast.LENGTH_SHORT)
						.show();
				blockChange = true;
			}
		});

		// Create listeners for the logging options
		RadioLogOn.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				editor.putBoolean("log", true);
				Toast.makeText(EditPreferencesActivity.this, "Logging Enabled", Toast.LENGTH_SHORT)
						.show();
				logChange = true;
			}
		});

		RadioLogOff.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				editor.putBoolean("log", false);
				Toast.makeText(EditPreferencesActivity.this, "Logging Disabled", Toast.LENGTH_SHORT)
						.show();
				logChange = true;
			}
		});

		// Create listener for the auto-update option
		CheckAutoUpdate.setOnClickListener(new OnClickListener()
		{
			/**
			 * if auto update is selected, call schedule update with time for 24
			 * hours from now.
			 */
			public void onClick(View v)
			{
				// Perform action on clicks, depending on whether it's now
				// checked
				// if checked, schedule update
				if (((CheckBox) v).isChecked())
				{
					editor.putBoolean("autoUpdate", true);
					Toast.makeText(EditPreferencesActivity.this, "Auto-update Enabled",
							Toast.LENGTH_SHORT).show();
					Calendar cal = Calendar.getInstance();
					cal.add(Calendar.HOUR, 24);
					scheduleUpdate(cal);
				}
				else
				{
					editor.putBoolean("autoUpdate", false);
					Toast.makeText(EditPreferencesActivity.this, "Auto-update Disabled",
							Toast.LENGTH_SHORT).show();
				}
			}
		});

		// Listener for suppressing warnings
		CheckSuppressWarn.setOnClickListener(new OnClickListener()
		{
			/**
			 * if auto update is selected, call schedule update with time for 24
			 * hours from now.
			 */
			public void onClick(View v)
			{
				// Perform action on clicks, depending on whether it's now
				// checked
				// if checked, schedule update
				if (((CheckBox) v).isChecked())
				{
					editor.putBoolean("suppressWarn", true);
					Toast.makeText(EditPreferencesActivity.this, "Warnings suppressed.",
							Toast.LENGTH_SHORT).show();
				}
				else
				{
					editor.putBoolean("suppressWarn", false);
					Toast.makeText(EditPreferencesActivity.this, "Warnings not suppressed.",
							Toast.LENGTH_SHORT).show();
				}
			}
		});

		RadioHome.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				editor.putInt("selectedTab", 0);
			}
		});

		RadioApps.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				editor.putInt("selectedTab", 1);
			}
		});

		RadioAdd.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				editor.putInt("selectedTab", 2);
			}
		});

		RadioView.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				editor.putInt("selectedTab", 3);
			}
		});

		RadioLog.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				editor.putInt("selectedTab", 4);
			}
		});

		// Listener for showing icons
		/*
		 * CheckShowIcons.setOnClickListener(new OnClickListener() { public void
		 * onClick(View v) { // Perform action on clicks, depending on whether
		 * it's now // checked // if checked, schedule update if (((CheckBox)
		 * v).isChecked()) { editor.putBoolean("showIcons", true);
		 * Toast.makeText(EditPreferencesActivity.this,
		 * "Icons will be diplayed.", Toast.LENGTH_SHORT).show(); } else {
		 * editor.putBoolean("showIcons", false);
		 * Toast.makeText(EditPreferencesActivity.this,
		 * "Icons will not be displayed.", Toast.LENGTH_SHORT).show(); } } });
		 */

		ButtonViewRules.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				Intent rawRulesIntent = new Intent(EditPreferencesActivity.this,
						ViewRawRulesActivity.class);
				startActivity(rawRulesIntent);
			}
		});
	}

}
