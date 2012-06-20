package com.honeybadger.views;

/*--------------------------------------------------------------------------------------------------------------------------------
 * Author(s): Brad Hitchens
 * Version: 2.1
 * Date of last modification: 14 JUNE 2012
 * Source Info: n/a
 * 
 * Edit 2.1: Affected by refactoring.
 *--------------------------------------------------------------------------------------------------------------------------------
 */

import java.util.ArrayList;
import com.honeybadger.R;
import com.honeybadger.api.AppBlocker;
import com.honeybadger.api.SharedMethods;
import com.honeybadger.api.SharedMethods.AppInfo;
import com.honeybadger.api.databases.AppsDBAdapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

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

	/**
	 * Declares click listeners for all of the buttons.
	 * 
	 * @param ctx
	 *            Passed in Context.
	 */
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
				Toast.makeText(ShowAppsActivity.this, "Rules have been applied.", Toast.LENGTH_LONG)
						.show();
			}
		});

	}

	/**
	 * Launches AppBlocker class to create rules for applications.
	 */
	public void createRules()
	{
		Intent loadRules = new Intent();
		loadRules.setClass(this, AppBlocker.class);
		this.startService(loadRules);
	}

	/**
	 * Sets overall content view then calls method to set the list view.
	 */
	public void display()
	{
		setContentView(R.layout.show_apps);
		setLV();
	}

	/**
	 * Creates ArrayList of AppInfo objects (which contain application data) and
	 * uses it to create and set a list view.
	 */
	public void setLV()
	{
		ArrayList<AppInfo> list = SharedMethods.getPackages(this);

		AppAdapter adapter = new AppAdapter(this, R.layout.app_item_row, list);

		lv = (ListView) this.findViewById(R.id.listView1);
		lv.setAdapter(adapter);
		lv.setItemsCanFocus(false);
	}

}
