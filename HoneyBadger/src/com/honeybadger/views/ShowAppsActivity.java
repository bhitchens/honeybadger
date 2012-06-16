package com.honeybadger.views;

/*--------------------------------------------------------------------------------------------------------------------------------
 * Author(s): Brad Hitchens
 * Version: 1.3
 * Date of last modification: 14 JUNE 2012
 * Source Info: n/a
 * 
 * Edit 1.3: Created
 *--------------------------------------------------------------------------------------------------------------------------------
 */

import java.util.ArrayList;
import java.util.List;
import com.honeybadger.R;
import com.honeybadger.api.Scripts;
import com.honeybadger.api.databases.AppsDBAdapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;

public class ShowAppsActivity extends Activity
{
	private ListView lv;
	private AppsDBAdapter appAdapter = new AppsDBAdapter(this);

	Button CheckAllButton;
	Button ClearAllButton;
	Button ApplyButton;

	SharedPreferences settings;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		display();

		CheckAllButton = (Button) findViewById(R.id.check_all);
		ClearAllButton = (Button) findViewById(R.id.clear_all);
		ApplyButton = (Button) findViewById(R.id.apply);
		createListeners(this);

	}

	private void createListeners(Context ctx)
	{
		CheckAllButton.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				// Perform action on clicks
				appAdapter.open();
				appAdapter.checkAll(true);
				appAdapter.close();
				setLV();
			}
		});

		ClearAllButton.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				// Perform action on clicks
				appAdapter.open();
				appAdapter.checkAll(false);
				appAdapter.close();
				setLV();
			}
		});

		ApplyButton.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				createRules();
				// put toast here
			}
		});

	}

	public void createRules()
	{
		settings = getSharedPreferences("main", 0);
		String script = "";
		// Perform action on clicks
		appAdapter.open();
		Cursor c = appAdapter.fetchAllEntries();
		if (!settings.getBoolean("block", false))
		{
			script += this.getDir("bin", 0) + "/iptables -D OUTPUT -j ACCEPTOUT" + "\n";
			while (c.getPosition() < c.getCount() - 1)
			{
				c.moveToNext();
				if (c.getString(2).contains("block"))
				{
					script += this.getDir("bin", 0) + "/iptables -D OUTPUT -m owner --uid-owner "
							+ c.getInt(0) + " -j ACCEPTOUT" + "\n";
					script += this.getDir("bin", 0) + "/iptables -D OUTPUT -m owner --uid-owner "
							+ c.getInt(0) + " -j DROPOUT" + "\n";
					script += this.getDir("bin", 0) + "/iptables -A OUTPUT -m owner --uid-owner "
							+ c.getInt(0) + " -j DROPOUT" + "\n";
				}
				else
				{
					script += this.getDir("bin", 0) + "/iptables -D OUTPUT -m owner --uid-owner "
							+ c.getInt(0) + " -j DROPOUT" + "\n";
				}
			}
			script += this.getDir("bin", 0) + "/iptables -A OUTPUT -j ACCEPTOUT" + "\n";
		}
		else
		{
			script += this.getDir("bin", 0) + "/iptables -D OUTPUT -j DROPOUT" + "\n";
			while (c.getPosition() < c.getCount() - 1)
			{
				c.moveToNext();
				if (!c.getString(2).contains("block"))
				{
					script += this.getDir("bin", 0) + "/iptables -D OUTPUT -m owner --uid-owner "
							+ c.getInt(0) + " -j DROPOUT" + "\n";
					script += this.getDir("bin", 0) + "/iptables -D OUTPUT -m owner --uid-owner "
							+ c.getInt(0) + " -j ACCEPTOUT" + "\n";
					script += this.getDir("bin", 0) + "/iptables -A OUTPUT -m owner --uid-owner "
							+ c.getInt(0) + " -j ACCEPTOUT" + "\n";
				}
				else
				{
					script += this.getDir("bin", 0) + "/iptables -D OUTPUT -m owner --uid-owner "
							+ c.getInt(0) + " -j ACCEPTOUT" + "\n";
				}
			}
			script += this.getDir("bin", 0) + "/iptables -A OUTPUT -j DROPOUT" + "\n";			
		}
		appAdapter.close();

		Intent scriptIntent = new Intent();
		scriptIntent.setClass(this, Scripts.class);
		scriptIntent.putExtra("script", script);
		startService(scriptIntent);
	}

	public void display()
	{
		appAdapter = new AppsDBAdapter(this);
		appAdapter.open();

		setContentView(R.layout.show_apps);
		setLV();
	}

	public void setLV()
	{
		ArrayList<App> appData = new ArrayList<App>();
		ArrayList<AppInfo> list = getPackages();
		int i;
		for (i = 0; i < list.size(); i++)
		{
			appData.add(new App(list.get(i).icon, list.get(i).appname, list.get(i).uid));
		}

		AppAdapter adapter = new AppAdapter(this, R.layout.app_item_row, appData);
		lv = (ListView) this.findViewById(R.id.listView1);

		lv.setAdapter(adapter);
		lv.setItemsCanFocus(false);
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

	private ArrayList<AppInfo> getPackages()
	{
		ArrayList<AppInfo> apps = getInstalledApps(false);
		return apps;
	}

	class AppInfo
	{
		private String appname = "";
		private Drawable icon;
		private int uid = 0;
	}

	private ArrayList<AppInfo> getInstalledApps(boolean getSysPackages)
	{
		ArrayList<AppInfo> res = new ArrayList<AppInfo>();
		List<PackageInfo> packs = getPackageManager().getInstalledPackages(0);
		for (int i = 0; i < packs.size(); i++)
		{
			PackageInfo p = packs.get(i);
			if ((packs.get(i).applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 1)
			{
				continue;
			}

			AppInfo newApp = new AppInfo();
			newApp.appname = p.applicationInfo.loadLabel(getPackageManager()).toString();
			newApp.icon = p.applicationInfo.loadIcon(getPackageManager());
			newApp.uid = p.applicationInfo.uid;
			res.add(newApp);
		}
		return res;
	}

}
