package com.honeybadger.api;

/*--------------------------------------------------------------------------------------------------------------------------------
 * Author(s): Brad Hitchens
 * Version: 2.1
 * Date of last modification: 19 June 2012
 *
 * Edit 2.1: Combined startup and script creation; added methods for fetching application data.
 *--------------------------------------------------------------------------------------------------------------------------------
 */

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.honeybadger.R;
import com.honeybadger.api.databases.AppsDBAdapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public final class SharedMethods
{

	/********************************************************
	 * Start up method
	 ********************************************************/

	/**
	 * This method ensures that IPTables is installed.
	 * 
	 * @param ctx
	 *            Context of the calling Activity or Service
	 * @return Return true if IPTables is installed, false if it is not.
	 */
	public static boolean installIPTables(Context ctx, SharedPreferences settings,
			SharedPreferences.Editor editor)
	{
		boolean ret = false;
		File file = new File(ctx.getDir("bin", 0), "iptables");
		if (!file.exists() || !settings.getBoolean("newIPT", false))
		{
			try
			{
				final String path = file.getAbsolutePath();
				final FileOutputStream os = new FileOutputStream(file);
				final InputStream is = ctx.getResources().openRawResource(R.raw.iptables);
				byte buffer[] = new byte[1024];
				int count;
				while ((count = is.read(buffer)) > 0)
				{
					os.write(buffer, 0, count);
				}
				os.close();
				is.close();

				Runtime.getRuntime().exec("chmod 755 " + path);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			ret = true;
			editor.putBoolean("newIPT", true);
			editor.commit();
		}
		return ret;
	}

	/********************************************************
	 * Script Creation
	 ********************************************************/

	/**
	 * Returns the initial string for the script. The script does the following:
	 * <p>
	 * 1. Removes any existing log rules to prevent duplications and allow for
	 * there to be no logging.
	 * <p>
	 * 2. Removes and creates rules which allow DNS traffic in order to ensure
	 * their existence without duplication.
	 * <p>
	 * 3. Creates a chain named FETCH. If the chain already exists, nothing will
	 * happen.
	 * <p>
	 * 4. Removes and creates rules to jump to the FETCH chain in order to
	 * ensure their existence without duplication.
	 * 
	 * @param input
	 *            string containing previously generated script. Should be empty
	 *            at this point.
	 * @return string consisting of input + initial string rules.
	 */
	public static String initialString(String input, Context ctx)
	{
		return input
				+ "\n"
				+ ctx.getDir("bin", 0)
				+ "/iptables -D OUTPUT -p udp --dport 53 -j ACCEPT"
				+ "\n"
				+ ctx.getDir("bin", 0)
				+ "/iptables -D INPUT -p udp --sport 53 -j ACCEPT"
				+ "\n"
				+ ctx.getDir("bin", 0)
				+ "/iptables -I OUTPUT -p udp --dport 53 -j ACCEPT"
				+ "\n"
				+ ctx.getDir("bin", 0)
				+ "/iptables -I INPUT -p udp --sport 53 -j ACCEPT"
				+ "\n"

				+ ctx.getDir("bin", 0)
				+ "/iptables -N FETCH"
				+ "\n"
				+ ctx.getDir("bin", 0)
				+ "/iptables -N ACCEPTIN"
				+ "\n"
				+ ctx.getDir("bin", 0)
				+ "/iptables -N ACCEPTOUT"
				+ "\n"
				+ ctx.getDir("bin", 0)
				+ "/iptables -N DROPIN"
				+ "\n"
				+ ctx.getDir("bin", 0)
				+ "/iptables -N DROPOUT"
				+ "\n"

				+ ctx.getDir("bin", 0)
				+ "/iptables -D INPUT -j FETCH"
				+ "\n"
				+ ctx.getDir("bin", 0)
				+ "/iptables -D OUTPUT -j FETCH"
				+ "\n"
				+ ctx.getDir("bin", 0)
				+ "/iptables -I OUTPUT -j FETCH"
				+ "\n"
				+ ctx.getDir("bin", 0)
				+ "/iptables -I INPUT -j FETCH"
				+ "\n"

				+ ctx.getDir("bin", 0)
				+ "/iptables -D ACCEPTIN -j ACCEPT"
				+ "\n"
				+ ctx.getDir("bin", 0)
				+ "/iptables -D ACCEPTOUT -j ACCEPT"
				+ "\n"
				+ ctx.getDir("bin", 0)
				+ "/iptables -A ACCEPTOUT -j ACCEPT"
				+ "\n"
				+ ctx.getDir("bin", 0)
				+ "/iptables -A ACCEPTIN -j ACCEPT"
				+ "\n"

				+ ctx.getDir("bin", 0)
				+ "/iptables -D DROPIN -j DROP"
				+ "\n"
				+ ctx.getDir("bin", 0)
				+ "/iptables -D DROPOUT -j DROP"
				+ "\n"
				+ ctx.getDir("bin", 0)
				+ "/iptables -A DROPOUT -j DROP"
				+ "\n"
				+ ctx.getDir("bin", 0)
				+ "/iptables -A DROPIN -j DROP"
				+ "\n"

				// these two commands are meant to clear out incorrect entries
				// in a previous version
				+ ctx.getDir("bin", 0)
				+ "/iptables -D INPUT -m limit --limit 1/second -j LOG --log-level 7 --log-prefix \"[HoneyBadger - INPUT]\" --log-uid"
				+ "\n"
				+ ctx.getDir("bin", 0)
				+ "/iptables -D OUTPUT -m limit --limit 1/second -j LOG --log-level 7 --log-prefix \"[HoneyBadger - OUTPUT]\" --log-uid"
				+ "\n"

				// clears previous version
				+ ctx.getDir("bin", 0)
				+ "/iptables -D ACCEPTOUT -m limit --limit 100/second -j LOG --log-level 7 --log-prefix \"[HoneyBadger - ACCEPTOUT]\" --log-uid"
				+ "\n"
				+ ctx.getDir("bin", 0)
				+ "/iptables -D ACCEPTIN -m limit --limit 100/second -j LOG --log-level 7 --log-prefix \"[HoneyBadger - ACCEPTIN]\" --log-uid"
				+ "\n"
				+ ctx.getDir("bin", 0)
				+ "/iptables -D DROPOUT -m limit --limit 100/second -j LOG --log-level 7 --log-prefix \"[HoneyBadger - DROPOUT]\" --log-uid"
				+ "\n"
				+ ctx.getDir("bin", 0)
				+ "/iptables -D DROPIN -m limit --limit 100/second -j LOG --log-level 7 --log-prefix \"[HoneyBadger - DROPIN]\" --log-uid"
				+ "\n"

				+ ctx.getDir("bin", 0)
				+ "/iptables -D ACCEPTOUT -m limit --limit 100/second -j LOG --log-level 7 --log-prefix \"[HoneyBadger-ACCEPTOUT]\" --log-uid"
				+ "\n"
				+ ctx.getDir("bin", 0)
				+ "/iptables -D ACCEPTIN -m limit --limit 100/second -j LOG --log-level 7 --log-prefix \"[HoneyBadger-ACCEPTIN]\" --log-uid"
				+ "\n"
				+ ctx.getDir("bin", 0)
				+ "/iptables -D DROPOUT -m limit --limit 100/second -j LOG --log-level 7 --log-prefix \"[HoneyBadger-DROPOUT]\" --log-uid"
				+ "\n"
				+ ctx.getDir("bin", 0)
				+ "/iptables -D DROPIN -m limit --limit 100/second -j LOG --log-level 7 --log-prefix \"[HoneyBadger-DROPIN]\" --log-uid"
				+ "\n"

				+ ctx.getDir("bin", 0) + "/iptables -D INPUT -j ACCEPTIN" + "\n"
				+ ctx.getDir("bin", 0) + "/iptables -D INPUT -j DROPIN" + "\n"
				+ ctx.getDir("bin", 0) + "/iptables -D OUTPUT -j ACCEPTOUT" + "\n"
				+ ctx.getDir("bin", 0) + "/iptables -D OUTPUT -j DROPOUT" + "\n";
	}

	/**
	 * Returns string consisting of the input string plus either nothing or
	 * logging rules. If logging is selected in the settings, then rules
	 * implementing logging are added to the string. Otherwise, nothing is added
	 * to the string.
	 * 
	 * @param input
	 *            string containing previously generated script.
	 * @return string consisting of input + either logging rules or nothing.
	 */
	public static String setLogging(String input, SharedPreferences settings, Context ctx)
	{
		if (settings.getBoolean("log", true))
		{
			return input
					+ ctx.getDir("bin", 0)
					+ "/iptables -I ACCEPTOUT -m limit --limit 100/second -j LOG --log-level 7 --log-prefix \"[HoneyBadger-ACCEPTOUT]\" --log-uid"
					+ "\n"
					+ ctx.getDir("bin", 0)
					+ "/iptables -I ACCEPTIN -m limit --limit 100/second -j LOG --log-level 7 --log-prefix \"[HoneyBadger-ACCEPTIN]\" --log-uid"
					+ "\n"
					+ ctx.getDir("bin", 0)
					+ "/iptables -I DROPOUT -m limit --limit 100/second -j LOG --log-level 7 --log-prefix \"[HoneyBadger-DROPOUT]\" --log-uid"
					+ "\n"
					+ ctx.getDir("bin", 0)
					+ "/iptables -I DROPIN -m limit --limit 100/second -j LOG --log-level 7 --log-prefix \"[HoneyBadger-DROPIN]\" --log-uid"
					+ "\n";
		}
		else
		{
			return input;
		}
	}

	/**
	 * Returns string consisting of the input string plus commands to set the
	 * standard policy for input and output based on the settings. If blocking
	 * is set in the settings, the default policy will be to drop all traffic;
	 * otherwise the default policy will be to accept all traffic.
	 * 
	 * @param input
	 *            string containing previously generated script.
	 * @return string consisting of input + commands for policy
	 */
	public static String setBlock(String input, SharedPreferences settings, Context ctx)
	{
		if (settings.getBoolean("block", false))
		{
			return input + ctx.getDir("bin", 0) + "/iptables -P INPUT DROP" + "\n"
					+ ctx.getDir("bin", 0) + "/iptables -P OUTPUT DROP" + "\n"
					+ ctx.getDir("bin", 0) + "/iptables -A INPUT -j DROPIN" + "\n"
					+ ctx.getDir("bin", 0) + "/iptables -A OUTPUT -j DROPOUT" + "\n";
		}
		else
		{
			return input + ctx.getDir("bin", 0) + "/iptables -P INPUT ACCEPT" + "\n"
					+ ctx.getDir("bin", 0) + "/iptables -P OUTPUT ACCEPT" + "\n"
					+ ctx.getDir("bin", 0) + "/iptables -A INPUT -j ACCEPTIN" + "\n"
					+ ctx.getDir("bin", 0) + "/iptables -A OUTPUT -j ACCEPTOUT" + "\n";
		}
	}

	public static String ruleBuilder(Context ctx, String rule, String type, String target,
			Boolean add, String block, String in)
	{
		String newRule = rule;
		if (type == "App")
		{
			if (add)
			{
				newRule += ctx.getDir("bin", 0) + "/iptables -A OUTPUT -m owner --uid-owner "
						+ target + " -j " + block + in + "\n";
			}
			else
			{
				newRule += ctx.getDir("bin", 0) + "/iptables -D OUTPUT -m owner --uid-owner "
						+ target + " -j " + block + in + "\n";
			}
		}
		else if (type == "Domain")
		{
			if (add)
			{
				// create chain with name of domain name + direction
				newRule += ctx.getDir("bin", 0) + "/iptables -N " + target + in
						+ "\n"
						// create rule(s) for domain in chain
						+ ctx.getDir("bin", 0) + "/iptables -A " + target + in + " -s " + target
						+ " -j " + block + in + "\n"
						// create rule to jump to the chain
						+ ctx.getDir("bin", 0) + "/iptables -I " + in + "PUT" + " -j " + target
						+ in + "\n";
			}
			else
			{
				newRule += ctx.getDir("bin", 0) + "/iptables -D " + in + "PUT" + " -j " + target
						+ in + "\n" + ctx.getDir("bin", 0) + "/iptables -F " + target + in + "\n"
						+ ctx.getDir("bin", 0) + "/iptables -X " + target + in + "\n";
			}
		}
		else if (type == "IP")
		{
			if (add & in == "IN")
			{
				newRule += ctx.getDir("bin", 0) + "/iptables -I " + in + "PUT" + " -s " + target
						+ " -j " + block + in + "\n";
			}
			else if (add & in == "OUT")
			{
				newRule += ctx.getDir("bin", 0) + "/iptables -I " + in + "PUT" + " -d " + target
						+ " -m state --state NEW,RELATED,ESTABLISHED -j " + block + in + "\n";
			}
			else if (in == "IN")
			{
				newRule += ctx.getDir("bin", 0) + "/iptables -D " + in + "PUT" + " -s " + target
						+ " -j " + block + in + "\n";
			}
			else
			{
				newRule += ctx.getDir("bin", 0) + "/iptables -D " + in + "PUT" + " -d " + target
						+ " -m state --state NEW,RELATED,ESTABLISHED -j " + block + in + "\n";
			}
		}
		return newRule;
	}

	/*************************************************
	 * Application information
	 *************************************************/

	/**
	 * Loads applications into database
	 * 
	 * @param ctx
	 *            Passed in context.
	 * @param settings
	 *            Passed in SharedPreferences.
	 * @param appAdapter
	 *            Passed in AppsDBAdapter
	 */
	public static void loadApps(Context ctx, SharedPreferences settings, AppsDBAdapter appAdapter)
	{
		if (!settings.getBoolean("loaded", false))
		{
			String block = "";
			appAdapter = new AppsDBAdapter(ctx);
			appAdapter.open();

			appAdapter.clear();

			if (settings.getBoolean("block", false))
			{
				block = "block";
			}
			else
			{
				block = "allow";
			}

			ArrayList<AppInfo> list = getPackages(ctx);
			List<PackageInfo> packs = ctx.getPackageManager().getInstalledPackages(0);
			int i;
			for (i = 0; i < list.size(); i++)
			{
				if ((packs.get(i).applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 1)
				{
					continue;
				}
				Drawable d = list.get(i).icon;
				BitmapDrawable bitIcon = (BitmapDrawable) d;
				Bitmap bm = bitIcon.getBitmap();
				ByteArrayOutputStream stream = new ByteArrayOutputStream();
				bm.compress(Bitmap.CompressFormat.JPEG, 100, stream);
				byte[] imageInByte = stream.toByteArray();

				appAdapter.createEntry(list.get(i).uid, list.get(i).appname, imageInByte, block);
			}

			for (i = 0; i < list.size(); i++)
			{
				if (!((packs.get(i).applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 1))
				{
					continue;
				}
				Drawable d = list.get(i).icon;
				BitmapDrawable bitIcon = (BitmapDrawable) d;
				Bitmap bm = bitIcon.getBitmap();
				ByteArrayOutputStream stream = new ByteArrayOutputStream();
				bm.compress(Bitmap.CompressFormat.JPEG, 100, stream);
				byte[] imageInByte = stream.toByteArray();

				appAdapter.createEntry(list.get(i).uid, list.get(i).appname, imageInByte, block);
			}

			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean("loaded", true);
		}
	}

	/**
	 * Returns array of applications.
	 * 
	 * @param ctx
	 *            Passed in Context.
	 * @return ArrayList of AppInfo
	 */
	public static ArrayList<AppInfo> getPackages(Context ctx)
	{
		ArrayList<AppInfo> res = new ArrayList<AppInfo>();
		List<PackageInfo> packs = ctx.getPackageManager().getInstalledPackages(0);
		for (int i = 0; i < packs.size(); i++)
		{
			PackageInfo p = packs.get(i);

			AppInfo newApp = (new SharedMethods()).new AppInfo();
			newApp.appname = p.applicationInfo.loadLabel(ctx.getPackageManager()).toString();
			newApp.icon = p.applicationInfo.loadIcon(ctx.getPackageManager());
			newApp.uid = p.applicationInfo.uid;
			res.add(newApp);
		}
		return res;
	}

	/**
	 * AppInfo object.
	 */
	public class AppInfo
	{
		public String appname = "";
		public Drawable icon;
		public int uid = 0;
	}

}
