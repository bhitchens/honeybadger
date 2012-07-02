package com.honeybadger;

import com.honeybadger.views.EditRulesActivity;
import com.honeybadger.views.ShowAppsActivity;
import com.honeybadger.views.ViewLogActivity;
import com.honeybadger.views.ViewRulesActivity;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;

public class HBTabActivity extends TabActivity
{
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		TabHost tabHost = getTabHost();

		TabSpec homespec = tabHost.newTabSpec("Home");
		homespec.setIndicator("Home", getResources().getDrawable(R.drawable.ic_menu_home));
		Intent homeIntent = new Intent(this, HoneyBadgerActivity.class);
		homespec.setContent(homeIntent);

		TabSpec rulesspec = tabHost.newTabSpec("Rules");
		rulesspec.setIndicator("View Rules", getResources().getDrawable(R.drawable.ic_menu_show_list));
		Intent rulesIntent = new Intent(this, ViewRulesActivity.class);
		rulesspec.setContent(rulesIntent);
		
		TabSpec addspec = tabHost.newTabSpec("Add Rules");
		addspec.setIndicator("Add Rules", getResources().getDrawable(R.drawable.ic_menu_add));
		Intent addIntent = new Intent(this, EditRulesActivity.class);
		addspec.setContent(addIntent);
		
		TabSpec appspec = tabHost.newTabSpec("Apps");
		appspec.setIndicator("Apps", getResources().getDrawable(R.drawable.small_tiles));
		Intent appsIntent = new Intent(this, ShowAppsActivity.class);
		appspec.setContent(appsIntent);
		
		TabSpec logspec = tabHost.newTabSpec("Log");
		logspec.setIndicator("Log", getResources().getDrawable(R.drawable.ic_menu_info_details));
		Intent logIntent = new Intent(this, ViewLogActivity.class);
		logspec.setContent(logIntent);
		
		tabHost.addTab(homespec);
		tabHost.addTab(appspec);
		tabHost.addTab(addspec);		
		tabHost.addTab(rulesspec);
		tabHost.addTab(logspec);
	}
}
