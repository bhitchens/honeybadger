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
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

public class ShowAppsActivity extends Activity
{
	private ListView lv;
	private AppsDBAdapter appAdapter = new AppsDBAdapter(this);

	Button CheckAllButton;
	Button ClearAllButton;
	Button ApplyButton;

	ImageView wifi;
	ImageView cell;

	SharedPreferences settings;

	ProgressDialog dialog;

	ArrayList<AppInfo> list;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		display();

		CheckAllButton = (Button) findViewById(R.id.check_all);
		ClearAllButton = (Button) findViewById(R.id.clear_all);
		ApplyButton = (Button) findViewById(R.id.apply);

		wifi = (ImageView) findViewById(R.id.imageWifi);
		cell = (ImageView) findViewById(R.id.imageCell);

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

		wifi.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				AlertDialog.Builder builder = new AlertDialog.Builder(ShowAppsActivity.this);
				builder.setMessage("Check or clear all wifi rules.").setCancelable(true)
						.setPositiveButton("Check All", new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialog, int id)
							{
								appAdapter.open();
								appAdapter.checkWifi(true);
								appAdapter.close();
								setLV();
							}
						}).setNegativeButton("Clear All", new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialog, int id)
							{
								appAdapter.open();
								appAdapter.checkWifi(false);
								appAdapter.close();
								setLV();
							}
						});
				AlertDialog alert = builder.create();
				alert.show();
			}
		});

		cell.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				AlertDialog.Builder builder = new AlertDialog.Builder(ShowAppsActivity.this);
				builder.setMessage("Check or clear all cell rules.").setCancelable(true)
						.setPositiveButton("Check All", new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialog, int id)
							{
								appAdapter.open();
								appAdapter.checkCell(true);
								appAdapter.close();
								setLV();
							}
						}).setNegativeButton("Clear All", new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialog, int id)
							{
								appAdapter.open();
								appAdapter.checkCell(false);
								appAdapter.close();
								setLV();
							}
						});
				AlertDialog alert = builder.create();
				alert.show();
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
		class GetLV extends AsyncTask<Integer, Integer, Integer>
		{
			protected Integer doInBackground(Integer... integers)
			{
				/* ArrayList<AppInfo> */list = new ArrayList<AppInfo>();
				appAdapter.open();
				Cursor c = appAdapter.fetchAllEntries();
				while (c.getPosition() < c.getCount() - 1)
				{
					c.moveToNext();
					AppInfo app = (new SharedMethods()).new AppInfo();
					app.uid = c.getInt(0);
					app.appname = c.getString(1);
					app.icon = new BitmapDrawable(BitmapFactory.decodeByteArray(c.getBlob(2), 0,
							c.getBlob(2).length));
					list.add(app);
				}
				c.close();
				appAdapter.close();

				ShowAppsActivity.this.runOnUiThread(new Runnable()
				{
					public void run()
					{
						new Handler().postDelayed(new Runnable()
						{
							public void run()
							{

								AppAdapter adapter = new AppAdapter(ShowAppsActivity.this,
										R.layout.app_item_row, list);

								lv = (ListView) ShowAppsActivity.this.findViewById(R.id.listView1);
								lv.setAdapter(adapter);
								lv.setItemsCanFocus(false);

							}
						}, 100);
					}

				});

				return 0;
			}

			protected void onPreExecute()
			{
				dialog = ProgressDialog.show(ShowAppsActivity.this, "", "Loading");
			}

			protected void onPostExecute(Integer result)
			{
				dialog.dismiss();
			}

		}

		new GetLV().execute();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.app_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle item selection
		switch (item.getItemId())
		{

			case R.id.refresh_apps:
				settings = getSharedPreferences("main", 1);
				SharedPreferences.Editor editor = settings.edit();
				editor.putBoolean("loaded", false);
				editor.commit();
				SharedMethods.loadApps(this, settings, appAdapter);
				display();
				return true;
			case R.id.settings_from_apps:
				Intent prefIntent = new Intent(this, EditPreferencesActivity.class);
				startActivity(prefIntent);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

}
