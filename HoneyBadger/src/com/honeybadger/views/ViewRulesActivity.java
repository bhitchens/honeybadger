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

import com.honeybadger.R;
import com.honeybadger.api.Scripts;
import com.honeybadger.api.databases.RulesDBAdapter;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class ViewRulesActivity extends ListActivity
{

	Menu optionsMenu = null;

	private Cursor c;
	private RulesDBAdapter ruleAdapter;
	private ArrayList<String> RULES;

	String ruleText;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		display();
	}

	/**
	 * Creates list view based on data in rules database.
	 */
	private void display()
	{
		ruleAdapter = new RulesDBAdapter(this);
		ruleAdapter.open();

		c = ruleAdapter.fetchAllEntries();
		startManagingCursor(c);

		RULES = new ArrayList<String>();

		while (c.getPosition() < c.getCount() - 1)
		{
			c.moveToNext();
			RULES.add(c.getString(3) + " " + c.getString(2) + "bound traffic from IP address "
					+ c.getString(0));
		}

		ruleAdapter.close();

		setListAdapter(new ArrayAdapter<String>(this, R.layout.log_viewer, RULES));

		ListView lv = getListView();
		lv.setTextFilterEnabled(true);

		createListener(lv);
	};

	/**
	 * Allows for selection of items in list view and gives option for deleting
	 * rules.
	 * 
	 * @param lv
	 */
	private void createListener(ListView lv)
	{
		lv.setOnItemClickListener(new OnItemClickListener()
		{
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				ruleText = (String) ((TextView) view).getText();

				AlertDialog.Builder builder = new AlertDialog.Builder(ViewRulesActivity.this);
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

								ruleAdapter.open();
								ruleAdapter.deleteEntry(tokens[6], direction);
								ruleAdapter.close();

								deleteRule(tokens[6], direction, dropAllow);
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
		});

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
	public void deleteRule(String ip, String direction, String dropAllow)
	{
		String inOut;
		String rule = "";

		// set input/output strings to correct values
		if (direction == "out")
		{
			inOut = "OUTPUT";
		}
		else
		{
			inOut = "INPUT";
		}

		if (Character.isDigit(ip.charAt(0)))
		{
			rule += this.getDir("bin", 0) + "/iptables -D " + inOut + " -s " + ip + " -j "
					+ dropAllow;
		}
		else
		{
			rule += this.getDir("bin", 0) + "/iptables -D " + inOut + " -j " + ip + inOut + "\n"
					+ rule + this.getDir("bin", 0) + "/iptables -F " + ip + inOut + "\n"
					+ this.getDir("bin", 0) + "/iptables -X " + ip + inOut;
		}

		Intent intent2 = new Intent();
		intent2.setClass(this, Scripts.class);
		intent2.putExtra("script", rule);
		startService(intent2);

		// refresh rule list
		display();
	}

	/**
	 * Initializes options menu. Basic structure of this method from <i>Pro
	 * Android 2</i>.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.rulesviewoptionsmenu, menu);
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
			case R.id.settingsFromViewRules:
				Intent prefIntent = new Intent(this, EditPreferencesActivity.class);
				startActivity(prefIntent);
				return true;
			case R.id.viewLog:
				Intent viewLogIntent = new Intent(this, ViewLogActivity.class);
				startActivity(viewLogIntent);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

}
