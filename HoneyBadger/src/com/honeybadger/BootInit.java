package com.honeybadger;

/*--------------------------------------------------------------------------------------------------------------------------------
 * Author(s): Brad Hitchens
 * Version: 1.2
 * Date of last modification: 12 June 2012
 |
 |Edit 1.2 (Initial): Recreates IPTables rules on boot.
 |
 --------------------------------------------------------------------------------------------------------------------------------
 */

import java.util.ArrayList;
import java.util.List;

import com.honeybadger.api.Blocker;
import com.honeybadger.api.Fetcher;
import com.honeybadger.api.ScriptCreation;
import com.honeybadger.api.Scripts;
import com.honeybadger.api.databases.AppsDBAdapter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.graphics.drawable.Drawable;

public class BootInit extends BroadcastReceiver
{
	SharedPreferences settings;
	private AppsDBAdapter appAdapter;

	@Override
	public void onReceive(Context context, Intent intent)
	{
		String startScript = "";

		settings = context.getSharedPreferences("main", 1);

		startScript = ScriptCreation.initialString(startScript, context);

		startScript = ScriptCreation.setLogging(startScript, settings, context);

		startScript = ScriptCreation.setBlock(startScript, settings, context);

		// Launch Script
		Intent script = new Intent();
		script.setClass(context, Scripts.class);
		script.putExtra("script", startScript);
		context.startService(script);

		// reload rules
		Intent reload = new Intent();
		reload.setClass(context, Blocker.class);
		reload.putExtra("reload", "true");
		context.startService(reload);

		// reload auto-generated rules if specified
		if (settings.getBoolean("generate", false) | settings.getBoolean("autoUpdate", false))
		{
			Intent generate = new Intent();
			generate.setClass(context, Fetcher.class);
			generate.putExtra(
					"script",
					context.getDir("bin", 0)
							+ "/iptables -F FETCH"
							+ "\n"
							+ "busybox wget http://www.malwaredomainlist.com/mdlcsv.php -O - | "
							+ "busybox egrep -o '[[:digit:]]{1,3}\\.[[:digit:]]{1,3}\\.[[:digit:]]{1,3}\\.[[:digit:]]{1,3}'");
			context.startService(generate);
		}

		loadApps(context);
	}

	public void loadApps(Context ctx)
	{
		if (!settings.getBoolean("loaded", false))
		{
			String block = "";
			appAdapter = new AppsDBAdapter(ctx);
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
			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean("loaded", true);
		}
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
