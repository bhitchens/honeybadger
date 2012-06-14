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

import android.app.ListActivity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

public class ShowAppsActivity extends ListActivity
{
	private ListView listView1;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		display();
	}

	public void display()
	{
		
		setContentView(R.layout.show_apps);
		
		ArrayList<App> appData = new ArrayList<App>();
		ArrayList<AppInfo> list = getPackages();
		int i;
		for (i = 0; i < list.size(); i++)
		{
			appData.add(new App(list.get(i).icon, list.get(i).appname, list.get(i).uid));
		}
		
		AppAdapter adapter = new AppAdapter(this, R.layout.app_item_row, appData);
		listView1 = getListView();
		
		View header = (View)getLayoutInflater().inflate(R.layout.apps_header_row, null);
        listView1.addHeaderView(header);
        
        listView1.setAdapter(adapter);
				
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
