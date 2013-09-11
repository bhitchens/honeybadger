package com.honeybadger.api;

/*--------------------------------------------------------------------------------------------------------------------------------
 * Version: 2.1
 * Date of last modification: 14APR13
 *
 * Edit 2.1: Combined startup and script creation; added methods for fetching application data.
 * Edit 4.11: See loadApps
 *--------------------------------------------------------------------------------------------------------------------------------
 */

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.honeybadger.R;
import com.honeybadger.api.databases.DBApps;
import com.honeybadger.api.databases.DBContentProvider;
import com.honeybadger.api.databases.DBRules;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.support.v4.widget.SimpleCursorAdapter;
import android.widget.EditText;
import android.widget.Toast;

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
		if (!file.exists())
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
				+ "/iptables -N APPS"
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
				+ "/iptables -D OUTPUT -j APPS"
				+ "\n"
				+ ctx.getDir("bin", 0)
				+ "/iptables -I OUTPUT -j APPS"
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
				+ "/iptables -D DROPIN -j REJECT"
				+ "\n"
				+ ctx.getDir("bin", 0)
				+ "/iptables -D DROPOUT -j REJECT"
				+ "\n"
				+ ctx.getDir("bin", 0)
				+ "/iptables -A DROPIN -j REJECT"
				+ "\n"
				+ ctx.getDir("bin", 0)
				+ "/iptables -A DROPOUT -j REJECT"
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
				+ ctx.getDir("bin", 0)
				+ "/iptables -D OUTPUT -m state --state NEW,RELATED,ESTABLISHED -j DROPOUT" + "\n"
				+ ctx.getDir("bin", 0)
				+ "/iptables -D INPUT -m state --state RELATED,ESTABLISHED -j ACCEPTIN" + "\n"
				+ ctx.getDir("bin", 0)
				+ "/iptables -I INPUT -m state --state RELATED,ESTABLISHED -j ACCEPTIN" + "\n";
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
			// add drop in jump
			return input
					+ ctx.getDir("bin", 0)
					+ "/iptables -A INPUT -j DROPIN"
					+ "\n"
					// add drop out jump
					+ ctx.getDir("bin", 0)
					+ "/iptables -A OUTPUT -m state --state NEW,RELATED,ESTABLISHED -j DROPOUT"
					+ "\n"
					// make sure dns rule isn't duplicated outbound
					+ ctx.getDir("bin", 0)
					+ "/iptables -D OUTPUT -p udp --dport 53 -j RETURN"
					+ "\n"
					// make sure dns rule isn't duplicated inbound
					+ ctx.getDir("bin", 0)
					+ "/iptables -D INPUT -p udp --sport 53 -j RETURN"
					+ "\n"
					// add dns rule out
					+ ctx.getDir("bin", 0) + "/iptables -I OUTPUT -p udp --dport 53 -j RETURN"
					+ "\n"
					// add dns rule in
					+ ctx.getDir("bin", 0) + "/iptables -I INPUT -p udp --sport 53 -j RETURN"
					+ "\n";
		}
		else
		{
			return input + ctx.getDir("bin", 0) + "/iptables -A INPUT -j ACCEPTIN" + "\n"
					+ ctx.getDir("bin", 0) + "/iptables -A OUTPUT -j ACCEPTOUT" + "\n";
		}
	}

	public static String ruleBuilder(Context ctx, String rule, String type, String target,
			Boolean add, String block, String in, Boolean wifi, Boolean cell)
	{
		String newRule = rule;
		if (wifi)
		{
			newRule = ruleBuilderF(ctx, newRule, type, target, add, block, in, "tiwlan+");
			newRule = ruleBuilderF(ctx, newRule, type, target, add, block, in, "wlan+");
			newRule = ruleBuilderF(ctx, newRule, type, target, add, block, in, "eth+");
			newRule = ruleBuilderF(ctx, newRule, type, target, add, block, in, "ra+");
		}
		if (cell)
		{
			newRule = ruleBuilderF(ctx, newRule, type, target, add, block, in, "rmnet+");
			newRule = ruleBuilderF(ctx, newRule, type, target, add, block, in, "pdp+");
			newRule = ruleBuilderF(ctx, newRule, type, target, add, block, in, "ppp+");
			newRule = ruleBuilderF(ctx, newRule, type, target, add, block, in, "uwbr+");
			newRule = ruleBuilderF(ctx, newRule, type, target, add, block, in, "wimax+");
			newRule = ruleBuilderF(ctx, newRule, type, target, add, block, in, "vsnet+");
			newRule = ruleBuilderF(ctx, newRule, type, target, add, block, in, "ccmni+");
			newRule = ruleBuilderF(ctx, newRule, type, target, add, block, in, "usb+");
		}
		return newRule;
	}

	private static String ruleBuilderF(Context ctx, String rule, String type, String target,
			Boolean add, String block, String in, String netInt)
	{
		StringBuilder newRule = new StringBuilder();
		newRule.append(rule);
		if (type == "App")
		{
			if (add)
			{
				newRule.append(ctx.getDir("bin", 0) + "/iptables -A APPS -m owner --uid-owner "
						+ target + " -o " + netInt + " -j " + block + in + "\n");
			}
			else
			{
				newRule.append(ctx.getDir("bin", 0) + "/iptables -D APPS -m owner --uid-owner "
						+ target + " -o " + netInt + " -j " + block + in + "\n");
			}

		}
		else if (type == "Domain" || type == "domain")
		{
			if (add && in == "IN")
			{
				// create chain with name of domain name + direction
				newRule.append(ctx.getDir("bin", 0) + "/iptables -N " + target + in + netInt
						+ "\n"
						// create rule(s) for domain in chain
						+ ctx.getDir("bin", 0) + "/iptables -A " + target + in + netInt + " -s "
						+ target + " -i " + netInt + " -j " + block + in + "\n"
						// create rule to jump to the chain
						+ ctx.getDir("bin", 0) + "/iptables -I " + in + "PUT -i " + netInt + " -j "
						+ target + in + netInt + "\n");
			}
			else if (add && in == "OUT")
			{
				newRule.append(ctx.getDir("bin", 0) + "/iptables -N " + target + in
						+ netInt
						+ "\n"
						// create rule(s) for domain in chain
						+ ctx.getDir("bin", 0) + "/iptables -A " + target + in + netInt + " -d "
						+ target + " -o " + netInt
						+ " -m state --state NEW,RELATED,ESTABLISHED -j " + block + in + "\n"
						// create rule to jump to the chain
						+ ctx.getDir("bin", 0) + "/iptables -I " + in + "PUT -o " + netInt + " -j "
						+ target + in + netInt + "\n");
			}
			else if (in == "IN")
			{
				// delete rule that jumps to chain
				newRule.append(ctx.getDir("bin", 0) + "/iptables -D " + in + "PUT -i " + netInt
						+ " -j " + target + in + netInt + "\n"
						// delete rules from in chain
						+ ctx.getDir("bin", 0) + "/iptables -F " + target + in + netInt + "\n"
						// attempt to delete chain (will fail if not empty)
						+ ctx.getDir("bin", 0) + "/iptables -X " + target + in + netInt + "\n");
			}
			else
			{
				// delete rule that jumps to chain
				newRule.append(ctx.getDir("bin", 0) + "/iptables -D " + in + "PUT -o " + netInt
						+ " -j " + target + in + netInt + "\n"
						// delete rules from in chain
						+ ctx.getDir("bin", 0) + "/iptables -F " + target + in + netInt + "\n"
						// attempt to delete chain (will fail if not empty)
						+ ctx.getDir("bin", 0) + "/iptables -X " + target + in + netInt + "\n");
			}
		}
		else if (type == "IP")
		{
			if (add & in == "IN")
			{
				newRule.append(ctx.getDir("bin", 0) + "/iptables -I " + in + "PUT" + " -s "
						+ target + " -i " + netInt + " -j " + block + in + "\n");
			}
			else if (add & in == "OUT")
			{
				newRule.append(ctx.getDir("bin", 0) + "/iptables -I " + in + "PUT" + " -d "
						+ target + " -o " + netInt
						+ " -m state --state NEW,RELATED,ESTABLISHED -j " + block + in + "\n");
			}
			else if (in == "IN")
			{
				newRule.append(ctx.getDir("bin", 0) + "/iptables -D " + in + "PUT" + " -s "
						+ target + " -i " + netInt + " -j " + block + in + "\n");
			}
			else
			{
				newRule.append(ctx.getDir("bin", 0) + "/iptables -D " + in + "PUT" + " -d "
						+ target + " -o " + netInt
						+ " -m state --state NEW,RELATED,ESTABLISHED -j " + block + in + "\n");
			}
		}
		return newRule.toString();
	}

	/*************************************************
	 * Application information
	 *************************************************/

	/**
	 * Loads applications into database. Last Update: 14APR13 - used try/catch
	 * to prevent crashes due to non-bitmap app icons
	 * 
	 * @param ctx
	 *            Passed in context.
	 * @param settings
	 *            Passed in SharedPreferences.
	 * @param appAdapter
	 *            Passed in AppsDBAdapter
	 */
	public static void loadApps(Context ctx, SharedPreferences settings)//, DBApps appAdapter)
	{
		if (!settings.getBoolean("loaded", false))
		{
			String block = "";
			//appAdapter = new DBApps(ctx);
			//appAdapter.open();

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
				if (!settings.getBoolean("hideIcons", false))
				{
					Drawable d = list.get(i).icon;

					BitmapDrawable bitIcon;
					Bitmap bm;

					try
					{
						bitIcon = (BitmapDrawable) d;
						bm = bitIcon.getBitmap();
					}
					catch (Exception e)
					{
						bm = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
					}

					ByteArrayOutputStream stream = new ByteArrayOutputStream();
					bm.compress(Bitmap.CompressFormat.JPEG, 100, stream);
					byte[] imageInByte = stream.toByteArray();

					/*appAdapter.createEntry(list.get(i).uid, list.get(i).appname, imageInByte,
							block, block);*/
					ContentValues initialValues = new ContentValues();
					
					initialValues.put("UID", list.get(i).uid);
					initialValues.put("NAME", list.get(i).appname);
					initialValues.put("ICON", imageInByte);
					initialValues.put("WSTATUS", block);
					initialValues.put("CSTATUS", block);
					ctx.getContentResolver().insert(DBContentProvider.CONTENT_URI_APPS, initialValues);
				}
				else
				{
					/*appAdapter.createEntry(list.get(i).uid, list.get(i).appname, new byte[] {},
							block, block);*/
					ContentValues initialValues = new ContentValues();
					initialValues.put("UID", list.get(i).uid);
					initialValues.put("NAME", list.get(i).appname);
					initialValues.put("ICON", new byte[] {});
					initialValues.put("WSTATUS", block);
					initialValues.put("CSTATUS", block);
					ctx.getContentResolver().insert(DBContentProvider.CONTENT_URI_APPS, initialValues);
				}
			}

			for (i = 0; i < list.size(); i++)
			{

				if (!((packs.get(i).applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 1))
				{
					continue;
				}
				if (!settings.getBoolean("hideIcons", false))
				{
					Drawable d = list.get(i).icon;

					BitmapDrawable bitIcon;
					Bitmap bm;

					try
					{
						bitIcon = (BitmapDrawable) d;
						bm = bitIcon.getBitmap();
					}
					catch (Exception e)
					{
						bm = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
					}

					ByteArrayOutputStream stream = new ByteArrayOutputStream();
					bm.compress(Bitmap.CompressFormat.JPEG, 100, stream);
					byte[] imageInByte = stream.toByteArray();

					/*appAdapter.createEntry(list.get(i).uid, list.get(i).appname, imageInByte,
							block, block);*/
					ContentValues initialValues = new ContentValues();
					initialValues.put("UID", list.get(i).uid);
					initialValues.put("NAME", list.get(i).appname);
					initialValues.put("ICON", imageInByte);
					initialValues.put("WSTATUS", block);
					initialValues.put("CSTATUS", block);
					ctx.getContentResolver().insert(DBContentProvider.CONTENT_URI_APPS, initialValues);
					
				}
				else
				{
					/*appAdapter.createEntry(list.get(i).uid, list.get(i).appname, new byte[] {},
							block, block);*/
					ContentValues initialValues = new ContentValues();
					initialValues.put("UID", list.get(i).uid);
					initialValues.put("NAME", list.get(i).appname);
					initialValues.put("ICON", new byte[] {});
					initialValues.put("WSTATUS", block);
					initialValues.put("CSTATUS", block);
					ctx.getContentResolver().insert(DBContentProvider.CONTENT_URI_APPS, initialValues);
				}
			}

			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean("loaded", true);
			editor.commit();
			//appAdapter.close();
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
	
	public static Boolean checkBlockW(Context context, int uid)
	{
		Cursor c = context.getContentResolver().query(DBContentProvider.CONTENT_URI_APPS, new String[] {DBApps.KEY_WSTATUS}, "UID= ? ", new String[]
				{ Integer.toString(uid) }, null);	
		if (c != null && c.getCount() != 0)
		{
			c.moveToFirst();
			if (c.getString(0).contains("block"))
			{
				return true;
			}
			else
			{
				return false;
			}
		}
		else
		{
			return false;
		}
	}
	
	public static Boolean checkBlockC(Context context, int uid)
	{
		Cursor c = context.getContentResolver().query(DBContentProvider.CONTENT_URI_APPS, new String[] {DBApps.KEY_CSTATUS}, "UID= ? ", new String[]
				{ Integer.toString(uid) }, null);		
		if (c != null && c.getCount() != 0)
		{
			c.moveToFirst();
			if (c.getString(0).contains("block"))
			{
				return true;
			}
			else
			{
				return false;
			}
		}
		else
		{
			return false;
		}
	}

	/****************************************************************
	 * Importing and Exporting
	 * *************************************************************/

	/**
	 * Exports rules to a csv file
	 * 
	 * @param ctx
	 *            passed in Context (activity from which method is called)
	 */
	public static void exportRules(final Context ctx)
	{
		//final DBApps appAdapter = new DBApps(ctx);

		// Create an edit text view for user input
		final EditText filePrompt = new EditText(ctx);

		// present an alert dialog to get name of file to be saved
		AlertDialog.Builder prompt = new AlertDialog.Builder(ctx);

		prompt.setMessage("Enter name for file to be saved.");
		prompt.setView(filePrompt);
		prompt.setNeutralButton("Save", new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int whichButton)
			{
				String state = Environment.getExternalStorageState();
				String fileName = filePrompt.getText().toString();

				if (Environment.MEDIA_MOUNTED.equals(state)
						&& !(Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)))
				{
					try
					{
						// create new file
						File dir = new File(Environment.getExternalStorageDirectory()
								.getAbsolutePath() + "/HoneyBadger");
						dir.mkdirs();
						File expFile = new File(Environment.getExternalStorageDirectory()
								.getAbsolutePath() + "/HoneyBadger", fileName + ".csv");
						FileWriter writer = new FileWriter(expFile);
						// create header of file
						writer.append("IP Address, Port, Direction, Action, Domain, Interface\n");

						SimpleCursorAdapter mAdapter;
						mAdapter = new SimpleCursorAdapter(ctx, R.layout.view_log, null,
								new String[]
								{ DBRules.KEY_ROWID, DBRules.KEY_IP_ADDRESS, DBRules.KEY_PORT,
										DBRules.KEY_DIRECTION, DBRules.KEY_ACTION,
										DBRules.KEY_INTERFACE, DBRules.KEY_DOMAIN,
										DBRules.KEY_SAVED }, null, 0);
						
						Cursor c = mAdapter.getCursor();
						
						// loop through all the entries and add them to the file
						while (c.getPosition() < c.getCount() - 1)
						{
							c.moveToNext();
							writer.append(c.getString(1) + ", " + c.getString(2) + ", "
									+ c.getString(3) + ", " + c.getString(4) + ", "
									+ c.getString(6) + ", " + c.getString(5) + "\n");
						}

						// App Rules
						// Insert barrier
						writer.append("App Rules Start Here.\n");

						// create header of section
						writer.append("UID, App Name, Block WiFi, Block Cell Data\n");

						// open rule DB and fetch all entries
						//appAdapter.open();
						Cursor c2 = ctx.getContentResolver().query(DBContentProvider.CONTENT_URI_APPS, null, null, null, null);
						//Cursor c2 = appAdapter.fetchAllEntries();

						// loop through all the entries and add them to the file
						while (c2.getPosition() < c2.getCount() - 1)
						{
							c2.moveToNext();
							writer.append(c2.getString(0) + ", " + c2.getString(1) + ", "
									+ c2.getString(3) + ", " + c2.getString(4) + "\n");
						}

						writer.flush();
						writer.close();
						Toast.makeText(ctx, "Saved", Toast.LENGTH_SHORT).show();

						// close cursors
						c.close();
						c2.close();

						// close db adapters
						//ruleAdapter.close();
						//appAdapter.close();
					}
					catch (Exception e)
					{
						Toast.makeText(ctx, "File failed to save.", Toast.LENGTH_LONG).show();
					}

				}
				else
				{
					Toast.makeText(ctx, "Unable to write to external storage", Toast.LENGTH_SHORT)
							.show();
				}
			}
		});

		prompt.show();
	}

	public static void importRules(final Context ctx)
	{
		//final DBRules ruleAdapter = new DBRules(ctx);
		//final DBApps appAdapter = new DBApps(ctx);

		// Create an edit text view for user input
		final EditText filePrompt = new EditText(ctx);

		// present an alert dialog to get name of file to be saved
		AlertDialog.Builder prompt = new AlertDialog.Builder(ctx);

		prompt.setMessage("Enter name for file to be imported (do not include \".csv\").");
		prompt.setView(filePrompt);
		prompt.setNeutralButton("Import", new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int whichButton)
			{
				String state = Environment.getExternalStorageState();
				String fileName = filePrompt.getText().toString();

				Boolean apps = false;

				if (Environment.MEDIA_MOUNTED.equals(state))
				{

					try
					{
						// open file to be imported
						File dir = new File(Environment.getExternalStorageDirectory()
								.getAbsolutePath() + "/HoneyBadger");
						dir.mkdirs();
						File impFile = new File(Environment.getExternalStorageDirectory()
								.getAbsolutePath() + "/HoneyBadger", fileName + ".csv");

						// get BufferedReader of file
						BufferedReader br = new BufferedReader(new FileReader(impFile));
						String line;

						// skip header
						br.readLine();

						// open rule DB
						//ruleAdapter.open();
						//appAdapter.open();

						// go through the rest of the lines and add them to the
						// db
						while ((line = br.readLine()) != null)
						{
							// check to see if we have reached the app rules
							if (line.contains("App Rules Start Here"))
							{
								// set apps bool to true
								apps = true;

								// skip header
								br.readLine();
							}
							else
							{
								// split up line on commas
								String[] tokens = line.split(", ");

								if (!apps)
								{
									// create entry in db
									ContentValues initialValues = new ContentValues();
									initialValues.put(DBRules.KEY_IP_ADDRESS, tokens[0]);
									initialValues.put(DBRules.KEY_PORT, tokens[1]);
									initialValues.put(DBRules.KEY_DIRECTION, tokens[2]);
									initialValues.put(DBRules.KEY_ACTION, tokens[3]);
									initialValues.put(DBRules.KEY_DOMAIN, tokens[4]);
									initialValues.put(DBRules.KEY_INTERFACE, tokens[5]);
									initialValues.put(DBRules.KEY_SAVED, "false");

									ctx.getContentResolver().insert(DBContentProvider.CONTENT_URI_RULES, initialValues);
								}
								else
								{
									ctx.getContentResolver().update(DBContentProvider.CONTENT_URI_RULES, null, tokens[0], new String[] {tokens[2], tokens[3]});
									/*appAdapter.changeStatus(Integer.parseInt((tokens[0])),
											tokens[2], tokens[3]);*/
								}
							}
						}

						// close buffered reader
						br.close();

						// close db
						//appAdapter.close();

						// apply rules
						Intent loadIPRules = new Intent(ctx, Blocker.class);
						loadIPRules.putExtra("reload", "false");
						ctx.startService(loadIPRules);

						Intent loadRules = new Intent(ctx, AppBlocker.class);
						ctx.startService(loadRules);

						Toast.makeText(ctx, "File imported.", Toast.LENGTH_LONG).show();
					}
					catch (Exception e)
					{
						Toast.makeText(ctx, "File failed to save.", Toast.LENGTH_LONG).show();
					}
				}
				else
				{
					Toast.makeText(ctx, "Unable to access file.", Toast.LENGTH_LONG).show();
				}
			}
		});
		prompt.show();
	}

	/********************************
	 * Scripts
	 ********************************/

	public interface ScriptMethod
	{
		public void execute(String data);
	}

	@SuppressWarnings("deprecation")
	public static String execScript(String script)
	{
		String output = "";
		String line;

		Process process;
		try
		{
			process = Runtime.getRuntime().exec("su");

			DataOutputStream stdin = new DataOutputStream(process.getOutputStream());
			DataInputStream stdout = new DataInputStream(process.getInputStream());
			DataInputStream stderr = new DataInputStream(process.getErrorStream());

			stdin.writeBytes(script + " \n");
			stdin.writeBytes("exit\n");

			stdin.flush();

			StringBuilder sb = new StringBuilder();

			while ((line = stdout.readLine()) != null)
			{
				sb.append(line);
				sb.append("\n");
			}

			while ((line = stderr.readLine()) != null)
			{
				sb.append(line);
				sb.append("\n");
			}

			output = sb.toString();

			process.waitFor();
			stderr.close();
			stdout.close();
			stdin.close();

		}
		catch (IOException e)
		{

		}
		catch (InterruptedException e)
		{
		}

		return output;
	}

	/**
	 * Fetches IP addresses from database and creates rules to block them.
	 * 
	 * @param ctx
	 *            Context
	 */
	public static void fetch(final Context ctx)
	{
		new Thread(new Runnable()
		{
			public void run()
			{
				String output = SharedMethods.execScript(ctx.getDir("bin", 0)
						+ "/iptables -F FETCH"
						+ "\n"
						+ "busybox wget http://www.malwaredomainlist.com/mdlcsv.php -O - | "
						+ "busybox egrep -o '[[:digit:]]{1,3}\\.[[:digit:]]{1,3}\\.[[:digit:]]{1,3}\\.[[:digit:]]{1,3}' | uniq");
				String lines[] = output.split("\n");

				StringBuilder sb = new StringBuilder();
				for (int x = 0; x < lines.length; x += 1)
				{
					sb.append(ctx.getDir("bin", 0) + "/iptables -A FETCH -s " + lines[x]
							+ " -j DROP\n");
				}

				SharedMethods.execScript(sb.toString());
			}
		}).start();
	}

}
