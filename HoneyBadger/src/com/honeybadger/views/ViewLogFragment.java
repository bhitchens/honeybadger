package com.honeybadger.views;

/*--------------------------------------------------------------------------------------------------------------------------------
 * Version: 1.3
 * Date of last modification: 11FEB13
 *
 * Edit 1.3: Effected by move of database adapter
 * Edit 4.0: Adapted to use loader manager and for search
 *--------------------------------------------------------------------------------------------------------------------------------
 */

import java.util.ArrayList;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.SearchView;
import com.actionbarsherlock.widget.SearchView.OnQueryTextListener;
import com.honeybadger.R;
import com.honeybadger.api.databases.DBContentProvider;
import com.honeybadger.api.databases.LogDBAdapter;
import com.honeybadger.api.scripts.LogScript;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;

public class ViewLogFragment extends SherlockListFragment implements
		LoaderManager.LoaderCallbacks<Cursor>
{
	private static final int LOADER_ID = 20;
	private LoaderManager.LoaderCallbacks<Cursor> mCallbacks;
	private SimpleCursorAdapter mAdapter;

	String mCurFilter;

	private ArrayList<String> DATA;

	private ArrayAdapter<String> arrayAdapter;

	LayoutInflater mInflater;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		setHasOptionsMenu(true);

		mInflater = inflater;
		mAdapter = new SimpleCursorAdapter(getActivity(), R.layout.log_viewer, null, new String[]
		{ "_id", "INOUT", "SRC", "DST", "Proto", "SPT", "DPT", "UID", "total" }, null, 0);

		setListAdapter(mAdapter);

		mCallbacks = this;
		LoaderManager lm = getLoaderManager();
		lm.initLoader(LOADER_ID, null, mCallbacks);

		return super.onCreateView(mInflater, container, savedInstanceState);
	}

	/**
	 * Uses {@link LogScript} to parse raw log data into database and displays
	 * this data as a list.
	 */
	public void display()
	{
		Intent logIntent = new Intent(getActivity(), LogScript.class);
		logIntent.putExtra("script", "dmesg -c | busybox grep HoneyBadger");
		getActivity().startService(logIntent);

		DATA = new ArrayList<String>();

		setData(mAdapter);
		arrayAdapter = new ArrayAdapter<String>(mInflater.getContext(), R.layout.log_viewer, DATA);
		setListAdapter(arrayAdapter);
	}

	/**
	 * Iterates through cursor to add data to array.
	 * 
	 * @param c
	 *            Cursor for iterating through rows of database.
	 */
	private void setData(SimpleCursorAdapter c)
	{
		Cursor curs = c.getCursor();
		while (curs.getPosition() < curs.getCount() - 1)
		{
			curs.moveToNext();
			if (curs.getString(1).contains("ACCEPTIN"))
			{
				DATA.add("Allowed recieving of " + curs.getString(8) + " packet(s) from "
						+ curs.getString(2) + " via " + curs.getString(4) + " protocol on port "
						+ curs.getString(5));
			}
			else if (curs.getString(1).contains("ACCEPTOUT"))
			{
				DATA.add("Allowed sending of " + curs.getString(8) + " packet(s) to "
						+ curs.getString(3) + " via " + curs.getString(4) + " protocol on port "
						+ curs.getString(6));
			}
			else if (curs.getString(1).contains("DROPIN"))
			{
				DATA.add("Blocked recieving of " + curs.getString(8) + " packet(s) from "
						+ curs.getString(2) + " via " + curs.getString(4) + " protocol on port "
						+ curs.getString(5));

			}
			else if (curs.getString(1).contains("DROPOUT"))
			{
				DATA.add("Blocked sending of " + curs.getString(8) + " packet(s) to "
						+ curs.getString(3) + " via " + curs.getString(4) + " protocol on port "
						+ curs.getString(6));
			}

		}
	}

	private String currentQuery = null;

	private OnQueryTextListener queryListener = new OnQueryTextListener()
	{

		public boolean onQueryTextSubmit(String query)
		{
			if (currentQuery == null)
			{
				arrayAdapter = new ArrayAdapter<String>(mInflater.getContext(),
						R.layout.log_viewer, DATA);
			}
			else
			{
				Toast.makeText(getActivity(), "Searching for \"" + currentQuery + "\"...",
						Toast.LENGTH_LONG).show();
				arrayAdapter.getFilter().filter(currentQuery);
				setListAdapter(arrayAdapter);
			}

			return false;
		}

		public boolean onQueryTextChange(String newText)
		{
			if (TextUtils.isEmpty(newText))
			{
				currentQuery = null;
			}
			else
			{
				currentQuery = newText;
			}

			return false;
		}
	};
	

	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1)
	{
		return new CursorLoader(getActivity(), Uri.parse(DBContentProvider.CONTENT_URI + "/log"),
				new String[]
				{ "_id", "INOUT", "SRC", "DST", "Proto", "SPT", "DPT", "UID", "total" }, null,
				null, null);
	}

	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor)
	{
		mAdapter.swapCursor(cursor);
		display();
	}

	public void onLoaderReset(Loader<Cursor> arg0)
	{
		mAdapter.swapCursor(null);
	}

	
	/**
	 * Initializes options menu.
	 */
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		inflater.inflate(R.menu.logviewoptionsmenu, menu);

		SearchView searchView = (SearchView) menu.findItem(R.id.search_log).getActionView();

		searchView.setOnQueryTextListener(queryListener);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle item selection
		switch (item.getItemId())
		{
			case R.id.refresh_log:
				getLoaderManager().restartLoader(LOADER_ID, null, ViewLogFragment.this);
				return true;
			case R.id.clearLog:
				LogDBAdapter logDB = new LogDBAdapter(getActivity());
				logDB.open();
				logDB.clearLog();
				logDB.close();
				getLoaderManager().restartLoader(LOADER_ID, null, ViewLogFragment.this);
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
