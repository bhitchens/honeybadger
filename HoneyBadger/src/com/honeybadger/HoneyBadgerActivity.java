package com.honeybadger;

/*--------------------------------------------------------------------------------------------------------------------------------
 * Author(s): Todd Berry Ann, Alex Harris, Brad Hitchens
 * Version: 1.3
 * Date of last modification: 14 June 2012
 * Source Info:    
 *The majority of the form code used in this activity is the adaptation of tutorials from the Android Developers Resource page  
 *located at the following link: http://developer.android.com/resources/tutorials/views/hello-formstuff.html
 *
 *Edit 1.3: Added button for app based rules
 --------------------------------------------------------------------------------------------------------------------------------
 */

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.honeybadger.api.ScriptCreation;
import com.honeybadger.api.Scripts;
import com.honeybadger.api.StartUp;
import com.honeybadger.api.databases.AppsDBAdapter;
import com.honeybadger.api.databases.LogDBAdapter;
import com.honeybadger.views.EditPreferencesActivity;
import com.honeybadger.views.EditRulesActivity;
import com.honeybadger.views.ShowAppsActivity;
import com.honeybadger.views.ViewLogActivity;
import com.honeybadger.views.ViewRulesActivity;

public class HoneyBadgerActivity extends Activity
{

	Menu optionsMenu = null;
	String startScript = "";

	SharedPreferences settings;
	SharedPreferences.Editor editor;
	
	private AppsDBAdapter appAdapter;

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
		
		settings = getSharedPreferences("main", 1);
		editor = settings.edit();
		
		boolean check = StartUp.installIPTables(this, settings, editor);
		if (check == true)
		{
			sendNotification();
		}

		

		startScript = ScriptCreation.initialString(startScript, this);

		startScript = ScriptCreation.setLogging(startScript, settings, this);

		startScript = ScriptCreation.setBlock(startScript, settings, this);

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

		// Create ViewApps button.
		Button ViewApps = (Button) findViewById(R.id.button4);

		// Create listener to launch activity to view rules when ViewApps
		// button is pressed.
		ViewApps.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				Intent ruleIntent = new Intent(view.getContext(), ShowAppsActivity.class);
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
		
		if (!settings.getBoolean("loaded", false))
		{
			loadApps(this);
			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean("loaded", true);
			editor.commit();
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
	
	public void loadApps(Context ctx)
	{
		String block = "";

		appAdapter = new AppsDBAdapter(this);
		appAdapter.open();

		if (settings.getBoolean("block", false))
		{
			block = "block";
		}
		else
		{
			block = "allow";
		}

		ArrayList<AppInfo> list = getPackages(ctx);
		int i;
		for (i = 0; i < list.size(); i++)
		{
			appAdapter.createEntry(list.get(i).uid, list.get(i).appname, block);
		}
		appAdapter.close();
	}

	public class App
	{
		public Drawable icon;
		public String title;
		public int uid;

		public App()
		{
			super();
		}

		public App(Drawable icon, String title, int uid)
		{
			super();
			this.icon = icon;
			this.title = title;
			this.uid = uid;
		}
	}

	private ArrayList<AppInfo> getPackages(Context ctx)
	{
		ArrayList<AppInfo> apps = getInstalledApps(ctx);
		return apps;
	}

	class AppInfo
	{
		private String appname = "";
		private int uid = 0;
	}

	private ArrayList<AppInfo> getInstalledApps(Context ctx)
	{
		ArrayList<AppInfo> res = new ArrayList<AppInfo>();
		List<PackageInfo> packs = ctx.getPackageManager().getInstalledPackages(0);
		for (int i = 0; i < packs.size(); i++)
		{
			PackageInfo p = packs.get(i);
			if ((packs.get(i).applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 1)
			{
				continue;
			}

			AppInfo newApp = new AppInfo();
			newApp.appname = p.applicationInfo.loadLabel(ctx.getPackageManager()).toString();
			newApp.uid = p.applicationInfo.uid;
			res.add(newApp);
		}
		return res;
	}

}
