package com.honeybadger.views;

/*--------------------------------------------------------------------------------------------------------------------------------
 * Author(s): Todd Berry Ann, Alex Harris, Brad Hitchens
 * Version: 1.3
 * Date of last modification: 14 JUNE 2012
 * Source Info:    
 * The majority of form code is the adaptation of tutorials from the Android Developers Resource page  
 * located at the following link: http://developer.android.com/resources/tutorials/views/hello-formstuff.html
 *
 * Edit 1.3: Effected by move of database adapter
 *--------------------------------------------------------------------------------------------------------------------------------
 */

import java.util.ArrayList;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.honeybadger.R;
import com.honeybadger.api.databases.LogDBAdapter;
import com.honeybadger.api.scripts.LogScript;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

public class ViewLogFragment extends SherlockListFragment
{

	private Cursor c;
	private LogDBAdapter dbAdapter;
	private ArrayList<String> DATA;

	LayoutInflater mInflater;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		setHasOptionsMenu(true);
		mInflater = inflater;
		display(mInflater);
		return super.onCreateView(mInflater, container, savedInstanceState);
	}

	/**
	 * Uses {@link LogScript} to parse raw log data into database and displays
	 * this data as a list.
	 */
	@SuppressWarnings("deprecation")
	public void display(LayoutInflater inflater)
	{

		Intent logIntent = new Intent(getActivity(), LogScript.class);
		logIntent.putExtra("script", "dmesg -c | busybox grep HoneyBadger");
		getActivity().startService(logIntent);

		dbAdapter = new LogDBAdapter(getActivity());
		dbAdapter.open();

		c = dbAdapter.fetchAllEntries();
		getActivity().startManagingCursor(c);

		DATA = new ArrayList<String>();

		setData(c);
		dbAdapter.close();

		setListAdapter(new ArrayAdapter<String>(inflater.getContext(), R.layout.log_viewer, DATA));
	}

	/**
	 * Iterates through cursor to add data to array.
	 * 
	 * @param c
	 *            Cursor for iterating through rows of database.
	 */
	private void setData(Cursor c)
	{

		while (c.getPosition() < c.getCount() - 1)
		{
			c.moveToNext();
			if (c.getString(0).contains("ACCEPTIN"))
			{
				DATA.add("Allowed recieving of " + c.getString(11) + " packet(s) from "
						+ c.getString(1) + " via " + c.getString(6) + " protocol on port "
						+ c.getString(7));
			}
			else if (c.getString(0).contains("ACCEPTOUT"))
			{
				DATA.add("Allowed sending of " + c.getString(11) + " packet(s) to "
						+ c.getString(2) + " via " + c.getString(6) + " protocol on port "
						+ c.getString(8));
			}
			else if (c.getString(0).contains("DROPIN"))
			{
				DATA.add("Blocked recieving of " + c.getString(11) + " packet(s) from "
						+ c.getString(1) + " via " + c.getString(6) + " protocol on port "
						+ c.getString(7));

			}
			else if (c.getString(0).contains("DROPOUT"))
			{
				DATA.add("Blocked sending of " + c.getString(11) + " packet(s) to "
						+ c.getString(2) + " via " + c.getString(6) + " protocol on port "
						+ c.getString(8));
			}
		}
	}

	/**
	 * Initializes options menu.
	 */
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		inflater.inflate(R.menu.rulesviewoptionsmenu, menu);
	}

	/**
	 * Handles selection of items from options menu. Basic structure of this
	 * method from <i>Pro Android 2</i>.
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle item selection
		switch (item.getItemId())
		{
			case R.id.refresh:
				display(mInflater);
				return true;
			case R.id.clearLog:
				LogDBAdapter logDB = new LogDBAdapter(getActivity());
				logDB.open();
				logDB.clearLog();
				logDB.close();
				display(mInflater);
				return true;
			case R.id.settingsFromLog:
				Intent prefIntent = new Intent(getActivity(), EditPreferencesActivity.class);
				startActivity(prefIntent);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
}
