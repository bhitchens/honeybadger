package com.honeybadger;

/*--------------------------------------------------------------------------------------------------------------------------------
 * Version: 4.4
 * Date of last modification: 17JUN13
 * 
 * Edit 4.4: Removed notification on installation of iptables
 --------------------------------------------------------------------------------------------------------------------------------
 */

import com.honeybadger.api.SharedMethods;
import com.honeybadger.api.databases.DBApps;
import com.honeybadger.api.databases.DBContentProvider;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import java.lang.Runnable;

public class SplashScreen extends Activity
{
	String startScript = "";

	SharedPreferences settings;
	SharedPreferences.Editor editor;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// Splash screen view
		setContentView(R.layout.splash);

		new Handler().postDelayed(new Runnable()
		{
			public void run()
			{
				settings = getSharedPreferences("main", 1);
				editor = settings.edit();

				SharedMethods.installIPTables(SplashScreen.this, settings, editor);

				if (settings.getBoolean("fwEnabled", true))
				{
					startScript = SharedMethods.initialString(startScript, SplashScreen.this);

					startScript = SharedMethods
							.setLogging(startScript, settings, SplashScreen.this);

					startScript = SharedMethods.setBlock(startScript, settings, SplashScreen.this);
				}

				SharedMethods.execScript(startScript);

				if (!settings.getBoolean("4_5", false))
				{
					upgrade();
					editor.putBoolean("loaded", false);
				}

				// Load apps if not already added
				if (!settings.getBoolean("loaded", false))
				{
					SharedMethods.loadApps(SplashScreen.this, settings);
					SharedPreferences.Editor editor = settings.edit();
					editor.putBoolean("loaded", true);
					editor.commit();
				}

				Intent intent = new Intent(SplashScreen.this, HBTabActivity.class);
				startActivity(intent);
			}
		}, 100);

	}

	public void upgrade()
	{
		getContentResolver().update(Uri.parse(DBContentProvider.CONTENT_URI_APPS+ "/delete"), null, null, null);

		editor.putBoolean("4_5", true);
		editor.commit();
	}
}