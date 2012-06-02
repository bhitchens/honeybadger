package com.honeybadger.views;

/*--------------------------------------------------------------------------------------------------------------------------------
 * Author(s): Todd Berry Ann, Alex Harris, Brad Hitchens
 * Version: 1.1
 * Date of last modification: 16 APRIL 2012
 * Source Info:    
 |The majority of form code is the adaptation of tutorials from the Android Developers Resource page  
 |located at the following link: http://developer.android.com/resources/tutorials/views/hello-formstuff.html
 |
 --------------------------------------------------------------------------------------------------------------------------------
 */

import java.util.ArrayList;

import com.honeybadger.R;
import com.honeybadger.api.LogDBAdapter;
import com.honeybadger.api.LogScript;
import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class ViewLogActivity extends ListActivity
{

	private Cursor c;
	private LogDBAdapter dbAdapter;
	private ArrayList<String> DATA;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		display();
	}

	/**
	 * Uses {@link LogScript} to parse raw log data into database and displays
	 * this data as a list.
	 */
	public void display()
	{

		Intent logIntent = new Intent();
		logIntent.setClass(this, LogScript.class);
		logIntent.putExtra("script", "dmesg -c | busybox grep HoneyBadger");
		startService(logIntent);

		dbAdapter = new LogDBAdapter(this);
		dbAdapter.open();

		c = dbAdapter.fetchAllEntries();
		startManagingCursor(c);

		DATA = new ArrayList<String>();

		setData(c);
		dbAdapter.close();

		setListAdapter(new ArrayAdapter<String>(this, R.layout.log_viewer, DATA));

		ListView lv = getListView();
		lv.setTextFilterEnabled(true);
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
				DATA.add("Allowed sending of " + c.getString(11) + " packet(s) to " + c.getString(1) + " via "
						+ c.getString(6) + " protocol on port " + c.getString(8));
			}
			else if (c.getString(0).contains("ACCEPTOUT"))
			{
				DATA.add("Allowed recieving of " + c.getString(11) + " packet(s) from " + c.getString(2)
						+ " via " + c.getString(6) + " protocol on port " + c.getString(7));
			}
			else if (c.getString(0).contains("DROPIN"))
			{
				DATA.add("Blocked sending of " + c.getString(11) + " packet(s) to " + c.getString(1) + " via "
						+ c.getString(6) + " protocol on port " + c.getString(8));
			}
			else if (c.getString(0).contains("DROPOUT"))
			{
				DATA.add("Blocked recieving of " + c.getString(11) + " packet(s) from " + c.getString(2)
						+ " via " + c.getString(6) + " protocol on port " + c.getString(7));
			}
			/*else
			{
				DATA.add(c.getString(1) + " " + c.getString(2) + c.getString(0));
			}*/
		}
	}

	/**
	 * Initializes options menu. Basic structure of this method from <i>Pro
	 * Android 2</i>.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.logviewoptionsmenu, menu);
		return true;
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
				display();
				return true;
			case R.id.addRule:
				Intent addRuleIntent = new Intent(this, EditRulesActivity.class);
				startActivity(addRuleIntent);
				return true;
			case R.id.clearLog:
				LogDBAdapter logDB = new LogDBAdapter(this);
				logDB.open();
				logDB.clearLog();
				logDB.close();
				display();
				return true;
			case R.id.viewRules:
				Intent viewLogIntent = new Intent(this, ViewRulesActivity.class);
				startActivity(viewLogIntent);
				return true;
			case R.id.settingsFromLog:
				Intent prefIntent = new Intent(this, EditPreferencesActivity.class);
				startActivity(prefIntent);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
}
