package com.honeybadger;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockListFragment;
import com.honeybadger.views.AddRulesFragment;
import com.honeybadger.views.ShowAppsFragment;
import com.honeybadger.views.ViewLogFragment;
import com.honeybadger.views.ViewRulesFragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;

public class HBTabActivity extends SherlockFragmentActivity
{
	private String curTag;
	
	SharedPreferences settings;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		settings = getSharedPreferences("main", 1);
		
		ActionBar bar = getSupportActionBar();
		
		int set = settings.getInt("selectedTab", 0);
		
		bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		bar.addTab(bar.newTab().setText("Home")
				.setTabListener(new TabListener<HoneyBadgerFragment>(this, "home", HoneyBadgerFragment.class, null)), set == 0);
		bar.addTab(bar.newTab().setText("Apps")
				.setTabListener(new TabListener<ShowAppsFragment>(this, "apps", ShowAppsFragment.class, null)), set == 1);
		bar.addTab(bar.newTab().setText("Add Rules")
				.setTabListener(new TabListener<AddRulesFragment>(this, "rules", AddRulesFragment.class, null)), set == 2);
		bar.addTab(bar.newTab().setText("View Rules")
				.setTabListener(new TabListListener<ViewRulesFragment>(this, "viewRules", ViewRulesFragment.class, null)), set == 3);
		bar.addTab(bar.newTab().setText("Log")
				.setTabListener(new TabListListener<ViewLogFragment>(this, "log", ViewLogFragment.class, null)), set == 4);
		
	}
	
	public class TabListener<T extends SherlockFragment> implements ActionBar.TabListener
	{
		private final SherlockFragmentActivity mActivity;
		private final String mTag;
		private final Class<T> mClass;
		private SherlockFragment mFragment;

		public TabListener(SherlockFragmentActivity activity, String tag, Class<T> clz, Bundle args)
		{
			mActivity = activity;
			mTag = tag;
			mClass = clz;
			FragmentTransaction ft = mActivity.getSupportFragmentManager().beginTransaction();

			// Check to see if we already have a fragment for this tab, probably
			// from a previously saved state. If so, deactivate it, because our
			// initial state is that a tab isn't shown.
			mFragment = (SherlockFragment) mActivity.getSupportFragmentManager().findFragmentByTag(mTag);
			if (mFragment != null && !mFragment.isDetached())
			{
				ft.detach(mFragment);
				ft.commit();
			}
		}

		public void onTabSelected(Tab tab, FragmentTransaction ft)
		{
			curTag = mTag;

			if (mActivity.getSupportFragmentManager().findFragmentByTag(curTag) == null)
			{
				mFragment = (SherlockFragment) SherlockFragment.instantiate(mActivity, mClass.getName());
				ft.add(android.R.id.content, mFragment, mTag);
			}
			else
			{
				ft.attach(mFragment);
			}
		}

		public void onTabUnselected(Tab tab, FragmentTransaction ft)
		{
			if (mFragment != null)
			{				
				ft.detach(mActivity.getSupportFragmentManager().findFragmentByTag(curTag));
			}
		}

		public void onTabReselected(Tab tab, FragmentTransaction ft)
		{
		
		}
		
	}
	
	public class TabListListener<T extends SherlockListFragment> implements ActionBar.TabListener
	{
		private final SherlockFragmentActivity mActivity;
		private final String mTag;
		private final Class<T> mClass;
		@SuppressWarnings("unused")
		private final Bundle mArgs;
		private SherlockListFragment mFragment;

		public TabListListener(SherlockFragmentActivity activity, String tag, Class<T> clz, Bundle args)
		{
			mActivity = activity;
			mTag = tag;
			mClass = clz;
			mArgs = args;
			FragmentTransaction ft = mActivity.getSupportFragmentManager().beginTransaction();

			// Check to see if we already have a fragment for this tab, probably
			// from a previously saved state. If so, deactivate it, because our
			// initial state is that a tab isn't shown.
			mFragment = (SherlockListFragment) mActivity.getSupportFragmentManager().findFragmentByTag(mTag);
			if (mFragment != null && !mFragment.isDetached())
			{
				ft.detach(mFragment);
				ft.commit();
			}
		}

		public void onTabSelected(Tab tab, FragmentTransaction ft)
		{
			curTag = mTag;

			if (mActivity.getSupportFragmentManager().findFragmentByTag(curTag) == null)
			{
				mFragment = (SherlockListFragment) SherlockListFragment.instantiate(mActivity, mClass.getName());
				ft.add(android.R.id.content, mFragment, mTag);
			}
			else
			{
				ft.attach(mFragment);
			}
		}

		public void onTabUnselected(Tab tab, FragmentTransaction ft)
		{
			if (mFragment != null)
			{				
				ft.detach(mActivity.getSupportFragmentManager().findFragmentByTag(curTag));
			}
		}

		public void onTabReselected(Tab tab, FragmentTransaction ft)
		{
		
		}
		
	}
}