package com.honeybadger.api.scripts;

/*--------------------------------------------------------------------------------------------------------------------------------
 * Author(s): Todd Berry Ann
 * Version: 1.3
 * Date of last modification: 14 JUNE 2012
 * Source Info:    
 * This file receives logs and parses them to the log database.
 *
 * Edit 1.3: Effected by move of database adapter.
 *--------------------------------------------------------------------------------------------------------------------------------
 */

import com.honeybadger.api.databases.LogDBAdapter;

import android.content.Intent;
import android.os.Bundle;

public class LogScript extends Scripts
{
	private LogDBAdapter logAdapter;

	/**
	 * Overrides method from {@link Scripts}; only difference is that it
	 * initializes the object for dealing with the log database.
	 */
	@Override
	public void handleStart(Intent intent)
	{
		Bundle extras = intent.getExtras();
		processScript = extras.getString("script");
		logAdapter = new LogDBAdapter(this);

		ExecuteThread logScript = new ExecuteThread(); // creates new script
														// thread
		logScript.run(); // runs thread

		while (logScript.isAlive())
		{

		}
		;

		this.stopSelf();
	}

	/**
	 * Overrides method from {@link Scripts}; parses raw logging data and places
	 * it into database.
	 */
	@Override
	public void handleOut(String line)
	{
		String inout = "";
		String src = "";
		String dst = "";
		String tos = "";
		String prec = "";
		String id = "";
		String proto = "";
		String spt = "";
		String dpt = "";
		String uid = "";
		String gid = "";

		String delims = "[ =]+";
		//String inoutDelim = "[]]";
		String[] tokens;
		//String[] inoutToken;

		scriptOutput += line + "\n";
		tokens = line.split(delims);
		inout = tokens[1];
		
		for (int i = 3; i < tokens.length; i++)
		{
			if (tokens[i].contains("SRC"))
			{
				src = tokens[i + 1];
			}
			else if (tokens[i].contains("DST"))
			{
				dst = tokens[i + 1];
			}
			else if (tokens[i].contains("TOS"))
			{
				tos = tokens[i + 1];
			}
			else if (tokens[i].contains("PREC"))
			{
				prec = tokens[i + 1];
			}
			else if (tokens[i].contains("ID"))
			{
				id = tokens[i + 1];
			}
			else if (tokens[i].contains("PROTO"))
			{
				proto = tokens[i + 1];
			}
			else if (tokens[i].contains("SPT"))
			{
				spt = tokens[i + 1];
			}
			else if (tokens[i].contains("DPT"))
			{
				dpt = tokens[i + 1];
			}
			else if (tokens[i].contains("UID"))
			{
				uid = tokens[i + 1];
			}
			else if (tokens[i].contains("GID"))
			{
				gid = tokens[i + 1];
			}

		}		
		logAdapter.open();
		logAdapter.createEntry(inout, src, dst, tos, prec, id, proto, spt, dpt, uid, gid);
		logAdapter.close();
	}

}
