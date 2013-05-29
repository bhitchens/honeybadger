package com.honeybadger.views;

/*--------------------------------------------------------------------------------------------------------------------------------
 * Version: 2.1
 * Date of last modification: 14 JUNE 2012
 * Source Info: n/a
 * 
 * Edit 2.1: Affected by refactoring.
 *--------------------------------------------------------------------------------------------------------------------------------
 */

import java.util.ArrayList;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

public class ShowAppsFragment extends SherlockFragment
{
	private ListView lv;
	private AppsDBAdapter appAdapter;

	Button CheckAllButton;
	Button ClearAllButton;
	Button ApplyButton;

	ImageView wifi;
	ImageView cell;

	SharedPreferences settings;

	ProgressDialog dialog;

	ArrayList<AppInfo> list;
	
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        appAdapter = new AppsDBAdapter(activity);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		setHasOptionsMenu(true);
		final View v = inflater.inflate(R.layout.view_apps, container, false);
		display();
		CheckAllButton = (Button) v.findViewById(R.id.check_all);
		ClearAllButton = (Button) v.findViewById(R.id.clear_all);
		ApplyButton = (Button) v.findViewById(R.id.apply);

		wifi = (ImageView) v.findViewById(R.id.imageWifi);
		cell = (ImageView) v.findViewById(R.id.imageCell);
		createListeners(getActivity());
		
		return v;
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
				display();
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
				display();
			}
		});

		ApplyButton.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				createRules();
				Toast.makeText(getActivity(), "Rules have been applied.", Toast.LENGTH_LONG)
						.show();
			}
		});

		wifi.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				builder.setMessage("Check or clear all wifi rules.").setCancelable(true)
						.setPositiveButton("Check All", new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialog, int id)
							{
								appAdapter.open();
								appAdapter.checkWifi(true);
								appAdapter.close();
								display();
							}
						}).setNegativeButton("Clear All", new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialog, int id)
							{
								appAdapter.open();
								appAdapter.checkWifi(false);
								appAdapter.close();
								display();
							}
						}).setNeutralButton("Cancel", new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialog, int id)
							{
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
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				builder.setMessage("Check or clear all cell rules.").setCancelable(true)
						.setPositiveButton("Check All", new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialog, int id)
							{
								appAdapter.open();
								appAdapter.checkCell(true);
								appAdapter.close();
								display();
							}
						}).setNegativeButton("Clear All", new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialog, int id)
							{
								appAdapter.open();
								appAdapter.checkCell(false);
								appAdapter.close();
								display();
							}
						}).setNeutralButton("Cancel", new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialog, int id)
							{
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
		Intent loadRules = new Intent(getActivity(), AppBlocker.class);
		getActivity().startService(loadRules);
	}

	/**
	 * Creates ArrayList of AppInfo objects (which contain application data) and
	 * uses it to create and set a list view.
	 */
	public void display()
	{
		settings = getActivity().getSharedPreferences("main", 1);
		
		// Load apps if not already added
		if (!settings.getBoolean("loaded", false))
		{
			SharedMethods.loadApps(getActivity(), settings, appAdapter);
			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean("loaded", true);
			editor.commit();
		}
		
		class GetLV extends AsyncTask<Integer, Integer, Integer>
		{
			protected Integer doInBackground(Integer... integers)
			{
				list = new ArrayList<AppInfo>();
				appAdapter.open();
				Cursor c = appAdapter.fetchAllEntries();
				while (c.getPosition() < c.getCount() - 1)
				{
					c.moveToNext();
					AppInfo app = (new SharedMethods()).new AppInfo();
					app.uid = c.getInt(0);
					app.appname = c.getString(1);
					app.icon = new BitmapDrawable(getActivity().getResources(), BitmapFactory.decodeByteArray(c.getBlob(2), 0,
							c.getBlob(2).length));
					list.add(app);
				}
				c.close();
				appAdapter.close();

				getActivity().runOnUiThread(new Runnable()
				{
					public void run()
					{
						new Handler().postDelayed(new Runnable()
						{
							public void run()
							{
								AppAdapter adapter = new AppAdapter(getActivity(),
										R.layout.component_app_item_row, list);

								lv = (ListView) getActivity().findViewById(R.id.listView1);
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
				dialog = ProgressDialog.show(getActivity(), "", "Loading");
			}

			protected void onPostExecute(Integer result)
			{
				dialog.dismiss();
			}
		}
		new GetLV().execute();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		inflater.inflate(R.menu.menu_app, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle item selection
		switch (item.getItemId())
		{

			case R.id.refresh_apps:
				settings = getActivity().getSharedPreferences("main", 1);
				SharedPreferences.Editor editor = settings.edit();
				editor.putBoolean("loaded", false);
				editor.commit();
				appAdapter.open();
				appAdapter.clear();
				appAdapter.close();
				SharedMethods.loadApps(getActivity(), settings, appAdapter);
				display();
				return true;
			case R.id.settings_from_apps:
				Intent prefIntent = new Intent(getActivity(), EditPreferencesActivity.class);
				startActivity(prefIntent);
				return true;
			case R.id.export_rules_from_apps:
				SharedMethods.exportRules(getActivity());
				return true;
			case R.id.import_rules_from_apps:
				SharedMethods.importRules(getActivity());
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

}
