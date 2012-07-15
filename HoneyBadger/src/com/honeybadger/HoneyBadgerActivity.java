package com.honeybadger;

/*--------------------------------------------------------------------------------------------------------------------------------
 * Author(s): Todd Berry Ann, Alex Harris, Brad Hitchens
 * Version: 2.1
 * Date of last modification: 19 June 2012
 * Source Info:    
 * The majority of the form code used in this activity is the adaptation of tutorials from the Android Developers Resource page  
 * located at the following link: http://developer.android.com/resources/tutorials/views/hello-formstuff.html
 *
 * Edit 2.1: Added method call to load apps; conformed to change from StartUp to SharedMethods
 *--------------------------------------------------------------------------------------------------------------------------------
 */

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.honeybadger.api.databases.LogDBAdapter;
import com.honeybadger.views.EditPreferencesActivity;
import com.honeybadger.views.EditRulesActivity;
import com.honeybadger.views.ViewLogActivity;
import com.honeybadger.views.ViewRulesActivity;

public class HoneyBadgerActivity extends Activity
{

	Menu optionsMenu = null;

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

		/*
		 * settings = getSharedPreferences("main", 1); editor = settings.edit();
		 */

		// Set the view for the activity
		setContentView(R.layout.home);

		AppRater.app_launched(this);

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
			default:
				return super.onOptionsItemSelected(item);
		}
	}

}