package com.honeybadger.views;

/*--------------------------------------------------------------------------------------------------------------------------------
 * Version: 4.5
 * Date of last modification: 11SEP13
 *
 * Edit 1.3 (14JUN12): Effected by move of database adapter
 * Edit 4.5 (11SEP13): Revamp of database interaction
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
import com.honeybadger.api.SharedMethods;
import com.honeybadger.api.databases.DBContentProvider;
import com.honeybadger.api.databases.DBRules;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ViewRulesFragment extends SherlockListFragment implements
		LoaderManager.LoaderCallbacks<Cursor>
{

	Menu optionsMenu = null;

	private Cursor c;
	private ArrayList<String> RULES;
	private ArrayAdapter<String> arrayAdapter;

	String ruleText;
	String fileName;

	LayoutInflater mInflater;

	private LoaderManager.LoaderCallbacks<Cursor> mCallbacks;
	private SimpleCursorAdapter mAdapter;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		mInflater = inflater;
		mAdapter = new SimpleCursorAdapter(getActivity(), R.layout.view_log, null,
				new String[]
				{ DBRules.KEY_ROWID, DBRules.KEY_IP_ADDRESS, DBRules.KEY_PORT,
						DBRules.KEY_DIRECTION, DBRules.KEY_ACTION,
						DBRules.KEY_INTERFACE, DBRules.KEY_DOMAIN,
						DBRules.KEY_SAVED }, null, 0);

		setListAdapter(mAdapter);

		mCallbacks = this;
		LoaderManager lm = getLoaderManager();
		lm.initLoader(10, null, mCallbacks);

		setHasOptionsMenu(true);
		
		return super.onCreateView(mInflater, container, savedInstanceState);
	}

	@Override
	public void onResume()
	{
		getLoaderManager().restartLoader(10, null, ViewRulesFragment.this);
		super.onResume();		
	}
	
	/**
	 * Creates list view based on data in rules database.
	 */
	private void display()
	{
		c = mAdapter.getCursor();

		RULES = new ArrayList<String>();

		while (c.getPosition() < c.getCount() - 1)
		{
			c.moveToNext();
			RULES.add(c.getString(4) + " " + c.getString(3) + "bound traffic from "
					+ c.getString(1) + " over the " + c.getString(5) + " interface.");
		}

		if (RULES.isEmpty())
		{
			RULES.add("No current rules.");
		}

		arrayAdapter = new ArrayAdapter<String>(mInflater.getContext(), R.layout.view_log, RULES);
		setListAdapter(arrayAdapter);
	};

	@Override
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		ruleText = (String) ((TextView) v).getText();

		if (!ruleText.contains("No current rules"))
		{

			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setMessage(
					"This rule is currently enforced by Honeybadger.  Would you like to delete it?")
					.setCancelable(true)
					.setPositiveButton("Yes", new DialogInterface.OnClickListener()
					{
						public void onClick(DialogInterface dialog, int id)
						{

							String delims = " ";
							String[] tokens = ruleText.split(delims);

							String direction;

							if (tokens[1].contains("inbound"))
							{
								direction = "in";
							}
							else
							{
								direction = "out";
							}

							String dropAllow;

							if (tokens[0].contains("block"))
							{
								dropAllow = "DROP";
							}
							else
							{
								dropAllow = "ALLOW";
							}

							String netInt = tokens[7];
							
							getActivity().getContentResolver().delete(DBContentProvider.CONTENT_URI_RULES, DBRules.KEY_IP_ADDRESS + "= ? AND "
									+ DBRules.KEY_DIRECTION + "= ? AND "
									+ DBRules.KEY_INTERFACE + "= ?", new String[]
							{ tokens[4], direction, netInt });

							deleteRule(tokens[4], direction, dropAllow, netInt);
						}
					}).setNegativeButton("No", new DialogInterface.OnClickListener()
					{
						public void onClick(DialogInterface dialog, int id)
						{
							dialog.cancel();
						}
					});
			AlertDialog alert = builder.create();
			alert.show();
		}
	}

	/**
	 * Allows deletion of rules
	 * 
	 * @param ip
	 *            Helps ensure correct rule is deleted.
	 * @param direction
	 *            Helps ensure correct rule is deleted.
	 * @param dropAllow
	 *            Helps ensure correct rule is deleted.
	 */
	public void deleteRule(String ip, String direction, String dropAllow, String netInt)
	{
		String inOut;
		String rule = "";

		// set input/output strings to correct values
		if (direction == "out")
		{
			inOut = "OUT";
		}
		else
		{
			inOut = "IN";
		}

		if (Character.isDigit(ip.charAt(0)))
		{
			if (netInt.contains("wifi"))
			{
				rule = SharedMethods.ruleBuilder(getActivity(), rule, "IP", ip, false, dropAllow,
						inOut, true, false);
			}
			else
			{
				rule = SharedMethods.ruleBuilder(getActivity(), rule, "IP", ip, false, dropAllow,
						inOut, false, true);
			}
		}
		else
		{
			if (netInt.contains("wifi"))
			{
				rule = SharedMethods.ruleBuilder(getActivity(), rule, "Domain", ip, false, "",
						inOut, true, false);
			}
			else
			{
				rule = SharedMethods.ruleBuilder(getActivity(), rule, "Domain", ip, false, "",
						inOut, false, true);
			}
		}

		SharedMethods.execScript(rule);

		getLoaderManager().restartLoader(10, null, ViewRulesFragment.this);
	}

	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1)
	{
		return new CursorLoader(getActivity(), DBContentProvider.CONTENT_URI_RULES,
				new String[]
				{ DBRules.KEY_ROWID, DBRules.KEY_IP_ADDRESS, DBRules.KEY_PORT,
						DBRules.KEY_DIRECTION, DBRules.KEY_ACTION,
						DBRules.KEY_INTERFACE, DBRules.KEY_DOMAIN,
						DBRules.KEY_SAVED }, null, null, null);
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

	private String currentQuery = null;

	private OnQueryTextListener queryListener = new OnQueryTextListener()
	{

		public boolean onQueryTextSubmit(String query)
		{
			if (currentQuery == null)
			{
				arrayAdapter = new ArrayAdapter<String>(mInflater.getContext(),
						R.layout.view_log, RULES);
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

	/**
	 * Initializes options menu.
	 */
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		inflater.inflate(R.menu.menu_view_rules, menu);

		SearchView searchView = (SearchView) menu.findItem(R.id.search_rules).getActionView();

		searchView.setOnQueryTextListener(queryListener);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle item selection
		switch (item.getItemId())
		{
			case R.id.refresh_rules:
				getLoaderManager().restartLoader(10, null, ViewRulesFragment.this);
				return true;
			case R.id.settingsFromViewRules:
				Intent prefIntent = new Intent(getActivity(), EditPreferencesActivity.class);
				startActivity(prefIntent);
				return true;
			case R.id.exportIPRules:
				SharedMethods.exportRules(getActivity());
				return true;
			case R.id.importIPRules:
				SharedMethods.importRules(getActivity());
				return true;
			case R.id.rv_raw_rules:
				Intent rawRulesIntent = new Intent(getActivity(), ViewRawRulesActivity.class);
				startActivity(rawRulesIntent);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
}
