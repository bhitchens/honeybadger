package com.honeybadger.views;

/*--------------------------------------------------------------------------------------------------------------------------------
 * Author(s): Alex Harris, Brad Hitchens, Todd Berry Ann
 * Version: 1.1
 * Date of last modification: 14 APRIL 2012
 * Source Info: 
 | 
 --------------------------------------------------------------------------------------------------------------------------------
 */

import java.util.Calendar;

import com.honeybadger.AlarmReceiver;
import com.honeybadger.R;
import com.honeybadger.api.scripts.Scripts;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.RadioButton;

import android.widget.Toast;

public class EditPreferencesActivity extends Activity
{

	RadioButton RadioLogOn;
	RadioButton RadioLogOff;

	CheckBox CheckAutoUpdate;

	RadioButton RadioBlock;
	RadioButton RadioAllow;

	SharedPreferences settings;
	SharedPreferences.Editor editor;

	Boolean logChange = false;
	Boolean blockChange = false;

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
		setContentView(R.layout.edit_preferences_viewer);

		// declare various buttons
		RadioLogOn = (RadioButton) findViewById(R.id.radioLogOn);
		RadioLogOff = (RadioButton) findViewById(R.id.radioLogOff);

		RadioBlock = (RadioButton) findViewById(R.id.radioBlock);
		RadioAllow = (RadioButton) findViewById(R.id.radioAllow);

		CheckAutoUpdate = (CheckBox) findViewById(R.id.checkAutoUpdate);

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
			Intent updateScript = new Intent();
			updateScript.setClass(this, Scripts.class);
			updateScript.putExtra("script", script);
			startService(updateScript);
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
						+ "\n"
						+ this.getDir("bin", 0) + "/iptables -A INPUT -j ACCEPTIN" + "\n"
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
		Intent intent = new Intent();
		intent.setClass(this, AlarmReceiver.class);

		PendingIntent sender = PendingIntent.getBroadcast(this, 2, intent,
				PendingIntent.FLAG_UPDATE_CURRENT);

		AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
		am.set(AlarmManager.RTC, cal.getTimeInMillis(), sender);
	}

	/**
	 * Ensures that correct boxes are checked based on current preferences.
	 */
	public void checkBoxes()
	{
		// get right thing checked for blocking
		if (settings.getBoolean("block", false))
		{
			RadioBlock.setChecked(true);
			RadioAllow.setChecked(false);
		}
		else
		{
			RadioBlock.setChecked(false);
			RadioAllow.setChecked(true);
		}

		// get right thing checked for logging
		if (settings.getBoolean("log", true))
		{
			RadioLogOn.setChecked(true);
			RadioLogOff.setChecked(false);
		}
		else
		{
			RadioLogOn.setChecked(false);
			RadioLogOff.setChecked(true);
		}

		if (settings.getBoolean("autoUpdate", false))
		{
			CheckAutoUpdate.setChecked(true);
		}
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
	}

}
