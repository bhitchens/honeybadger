package com.honeybadger.views;

/*--------------------------------------------------------------------------------------------------------------------------------
 * Author(s): Alex Harris, Brad Hitchens
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
import com.honeybadger.api.SharedMethods;
import com.honeybadger.api.databases.RulesDBAdapter;
import com.honeybadger.api.scripts.Scripts;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class ViewRulesFragment extends SherlockListFragment// implements LoaderManager.LoaderCallbacks<Cursor>
{

	Menu optionsMenu = null;

	private SimpleCursorAdapter c;
	private RulesDBAdapter ruleAdapter;
	private ArrayList<String> RULES;

	String ruleText;
	String fileName;

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
	 * Creates list view based on data in rules database.
	 */
	private void display(LayoutInflater inflater)
	{
		ruleAdapter = new RulesDBAdapter(getActivity());
		ruleAdapter.open();

		c = ruleAdapter.fetchAllEntriesNew();
		
		RULES = new ArrayList<String>();

		Cursor curs = c.getCursor();
		while (curs.getPosition() < curs.getCount() - 1)
		{
			curs.moveToNext();
			RULES.add(curs.getString(4) + " " + curs.getString(3) + "bound traffic from "
					+ curs.getString(1) + " over the " + curs.getString(5) + " interface.");
		}

		ruleAdapter.close();

		if (RULES.isEmpty())
		{
			RULES.add("No current rules.");
		}

		setListAdapter(new ArrayAdapter<String>(inflater.getContext(), R.layout.log_viewer, RULES));
	};

	@Override
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		ruleText = (String) ((TextView) v).getText();

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setMessage(
				"This rule is currently enforced by Honeybadger.  Would you like to delete it?")
				.setCancelable(true).setPositiveButton("Yes", new DialogInterface.OnClickListener()
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

						ruleAdapter.open();
						ruleAdapter.deleteEntry(tokens[4], direction, netInt);
						ruleAdapter.close();

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

		Intent intent2 = new Intent(getActivity(), Scripts.class);
		intent2.putExtra("script", rule);
		getActivity().startService(intent2);

		// refresh rule list
		display(mInflater);
	}

	/**
	 * Initializes options menu. Basic structure of this method from <i>Pro
	 * Android 2</i>.
	 */
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		inflater.inflate(R.menu.rulesviewoptionsmenu, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle item selection
		switch (item.getItemId())
		{
			case R.id.refresh_rules:
				display(mInflater);
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
