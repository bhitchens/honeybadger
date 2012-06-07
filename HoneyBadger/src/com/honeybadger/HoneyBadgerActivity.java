package com.honeybadger;

/*--------------------------------------------------------------------------------------------------------------------------------
 * Author(s): Todd Berry Ann, Alex Harris, Brad Hitchens
 * Version: 1.1
 * Date of last modification: 22 April 2012
 * Source Info:    
 |The majority of the form code used in this activity is the adaptation of tutorials from the Android Developers Resource page  
 |located at the following link: http://developer.android.com/resources/tutorials/views/hello-formstuff.html
 |
 --------------------------------------------------------------------------------------------------------------------------------
 */

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.honeybadger.api.LogDBAdapter;
import com.honeybadger.api.Scripts;
import com.honeybadger.api.StartUp;
import com.honeybadger.views.EditPreferencesActivity;
import com.honeybadger.views.EditRulesActivity;
import com.honeybadger.views.ViewLogActivity;
import com.honeybadger.views.ViewRulesActivity;

public class HoneyBadgerActivity extends Activity
{

	Menu optionsMenu = null;
	String startScript = "";

	SharedPreferences settings;

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
		boolean check = StartUp.installIPTables(this);
		if (check == true)
		{
			sendNotification();
		}

		settings = getSharedPreferences("main", 1);

		startScript = initialString(startScript);

		startScript = setLogging(startScript);

		startScript = setBlock(startScript);

		// Launch Script
		Intent script = new Intent();
		script.setClass(this, Scripts.class);
		script.putExtra("script", startScript);
		startService(script);

		// Set the view for the activity
		setContentView(R.layout.main);

		// Create ViewLog Button
		Button ViewLog = (Button) findViewById(R.id.button3);

		// Create listener to launch activity to view log when ViewLog button is
		// pressed.
		ViewLog.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				Intent myIntent = new Intent(view.getContext(), ViewLogActivity.class);
				startActivity(myIntent);
			}
		});

		// Create ViewRules button.
		Button ViewRules = (Button) findViewById(R.id.button1);

		// Create listener to launch activity to view rules when ViewRules
		// button is pressed.
		ViewRules.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				Intent ruleIntent = new Intent(view.getContext(), ViewRulesActivity.class);
				startActivity(ruleIntent);
			}
		});

		// Create EditRules button.
		Button EditRules = (Button) findViewById(R.id.button2);

		// Create listener to launch activity to edit rules when EditRules
		// button is pressed.
		EditRules.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				Intent myIntent = new Intent(view.getContext(), EditRulesActivity.class);
				startActivity(myIntent);
			}
		});

	}

	/**
	 * Returns the initial string for the script. The script does the following:
	 * <p>
	 * 1. Removes any existing log rules to prevent duplications and allow for
	 * there to be no logging.
	 * <p>
	 * 2. Removes and creates rules which allow DNS traffic in order to ensure
	 * their existence without duplication.
	 * <p>
	 * 3. Creates a chain named FETCH. If the chain already exists, nothing will
	 * happen.
	 * <p>
	 * 4. Removes and creates rules to jump to the FETCH chain in order to
	 * ensure their existence without duplication.
	 * 
	 * @param input
	 *            string containing previously generated script. Should be empty
	 *            at this point.
	 * @return string consisting of input + initial string rules.
	 */
	private String initialString(String input)
	{
		return input
				+ "\n" + this.getDir("bin", 0) + "/iptables -D OUTPUT -p udp --dport 53 -j ACCEPT"
				+ "\n" + this.getDir("bin", 0) + "/iptables -D INPUT -p udp --sport 53 -j ACCEPT"
				+ "\n" + this.getDir("bin", 0) + "/iptables -I OUTPUT -p udp --dport 53 -j ACCEPT"
				+ "\n" + this.getDir("bin", 0) + "/iptables -I INPUT -p udp --sport 53 -j ACCEPT"
				+ "\n" 
				
				+ this.getDir("bin", 0) + "/iptables -N FETCH" + "\n"
				+ this.getDir("bin", 0)	+ "/iptables -N ACCEPTIN" + "\n" 
				+ this.getDir("bin", 0)	+ "/iptables -N ACCEPTOUT" + "\n" 
				+ this.getDir("bin", 0) + "/iptables -N DROPIN" + "\n"
				+ this.getDir("bin", 0) + "/iptables -N DROPOUT" + "\n"
				
				+ this.getDir("bin", 0) + "/iptables -D INPUT -j FETCH" + "\n"
				+ this.getDir("bin", 0) + "/iptables -D OUTPUT -j FETCH" + "\n"
				+ this.getDir("bin", 0) + "/iptables -I OUTPUT -j FETCH" + "\n"
				+ this.getDir("bin", 0) + "/iptables -I INPUT -j FETCH" + "\n"

				+ this.getDir("bin", 0) + "/iptables -D ACCEPTIN -j ACCEPT" + "\n"
				+ this.getDir("bin", 0) + "/iptables -D ACCEPTOUT -j ACCEPT" + "\n"
				+ this.getDir("bin", 0) + "/iptables -A ACCEPTOUT -j ACCEPT" + "\n"
				+ this.getDir("bin", 0) + "/iptables -A ACCEPTIN -j ACCEPT" + "\n"

				+ this.getDir("bin", 0) + "/iptables -D DROPIN -j DROP" + "\n"
				+ this.getDir("bin", 0) + "/iptables -D DROPOUT -j DROP" + "\n"
				+ this.getDir("bin", 0) + "/iptables -A DROPOUT -j DROP" + "\n"
				+ this.getDir("bin", 0) + "/iptables -A DROPIN -j DROP" + "\n"
				
				+ this.getDir("bin", 0)
				+ "/iptables -D ACCEPTOUT -m limit --limit 100/second -j LOG --log-level 7 --log-prefix \"[HoneyBadger - ACCEPTOUT]\" --log-uid"
				+ "\n"
				+ this.getDir("bin", 0)
				+ "/iptables -D ACCEPTIN -m limit --limit 100/second -j LOG --log-level 7 --log-prefix \"[HoneyBadger - ACCEPTIN]\" --log-uid"
				+ "\n"
				+ this.getDir("bin", 0)
				+ "/iptables -D DROPOUT -m limit --limit 100/second -j LOG --log-level 7 --log-prefix \"[HoneyBadger - DROPOUT]\" --log-uid"
				+ "\n"
				+ this.getDir("bin", 0)
				+ "/iptables -D DROPIN -m limit --limit 100/second -j LOG --log-level 7 --log-prefix \"[HoneyBadger - DROPIN]\" --log-uid"
				+ "\n"
				
				+ this.getDir("bin", 0) + "/iptables -D INPUT -j ACCEPTIN" + "\n"
				+ this.getDir("bin", 0) + "/iptables -D INPUT -j DROPIN" + "\n"
				+ this.getDir("bin", 0) + "/iptables -D OUTPUT -j ACCEPTOUT" + "\n"
				+ this.getDir("bin", 0) + "/iptables -D OUTPUT -j DROPOUT" + "\n";				
	}

	/**
	 * Returns string consisting of the input string plus either nothing or
	 * logging rules. If logging is selected in the settings, then rules
	 * implementing logging are added to the string. Otherwise, nothing is added
	 * to the string.
	 * 
	 * @param input
	 *            string containing previously generated script.
	 * @return string consisting of input + either logging rules or nothing.
	 */
	private String setLogging(String input)
	{
		if (settings.getBoolean("log", true))
		{
			return input
			+ this.getDir("bin", 0)
			+ "/iptables -I ACCEPTOUT -m limit --limit 100/second -j LOG --log-level 7 --log-prefix \"[HoneyBadger - ACCEPTOUT]\" --log-uid"
			+ "\n"
			+ this.getDir("bin", 0)
			+ "/iptables -I ACCEPTIN -m limit --limit 100/second -j LOG --log-level 7 --log-prefix \"[HoneyBadger - ACCEPTIN]\" --log-uid"
			+ "\n"
			+ this.getDir("bin", 0)
			+ "/iptables -I DROPOUT -m limit --limit 100/second -j LOG --log-level 7 --log-prefix \"[HoneyBadger - DROPOUT]\" --log-uid"
			+ "\n"
			+ this.getDir("bin", 0)
			+ "/iptables -I DROPIN -m limit --limit 100/second -j LOG --log-level 7 --log-prefix \"[HoneyBadger - DROPIN]\" --log-uid"
			+ "\n";
		}
		else
		{
			return input;
		}
	}

	/**
	 * Returns string consisting of the input string plus commands to set the
	 * standard policy for input and output based on the settings. If blocking
	 * is set in the settings, the default policy will be to drop all traffic;
	 * otherwise the default policy will be to accept all traffic.
	 * 
	 * @param input
	 *            string containing previously generated script.
	 * @return string consisting of input + commands for policy
	 */
	private String setBlock(String input)
	{
		if (settings.getBoolean("block", false))
		{
			return input + this.getDir("bin", 0) + "/iptables -P INPUT DROP" + "\n"
					+ this.getDir("bin", 0) + "/iptables -P OUTPUT DROP" + "\n"
					+ this.getDir("bin", 0) + "/iptables -A INPUT -j DROPIN" + "\n"
					+ this.getDir("bin", 0) + "/iptables -A OUTPUT -j DROPOUT" + "\n";
		}
		else
		{
			return input + this.getDir("bin", 0) + "/iptables -P INPUT ACCEPT" + "\n"
					+ this.getDir("bin", 0) + "/iptables -P OUTPUT ACCEPT" + "\n"
					+ this.getDir("bin", 0) + "/iptables -A INPUT -j ACCEPTIN" + "\n"
					+ this.getDir("bin", 0) + "/iptables -A OUTPUT -j ACCEPTOUT" + "\n";
		}
	}

	/**
	 * Used to send system notification that the IPTables binary has been
	 * installed. This is done by starting the {@link HoneyBadgerNotify}
	 * activity.
	 */
	public void sendNotification()
	{
		NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		Notification notification = new Notification(R.drawable.icon,
				"IPTables has been installed.", System.currentTimeMillis());

		PendingIntent contentI = PendingIntent.getActivity(this, 1, new Intent(this,
				HoneyBadgerNotify.class), 0);

		notification.setLatestEventInfo(this, "HoneyBadger", "IPTables has been installed.",
				contentI);

		manager.notify(2, notification);
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
			case R.id.addRule:
				Intent myIntent = new Intent(this, EditRulesActivity.class);
				startActivity(myIntent);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

}
