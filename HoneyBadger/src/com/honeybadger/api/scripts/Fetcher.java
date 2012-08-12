package com.honeybadger.api.scripts;

/*--------------------------------------------------------------------------------------------------------------------------------
 * Author(s): Brad Hitchens, Todd Berry Ann
 * Version: 1.1
 * Date of last modification: 22 April 2012
 * Source Info: n/a
 |   This file was based primarily on the Exec class of the GScript application.
 |   The source for this is located here:
 |   http://code.google.com/p/gscript-android/source/browse/trunk/GScript/src/nl/rogro/GScript/GScriptExec.java
 |
 --------------------------------------------------------------------------------------------------------------------------------
 */


import android.content.Intent;

public class Fetcher extends Scripts
{
	/**
	 * Overrides the method in {@link Scripts}; creates new IPTables rules based
	 * on content of standard output.
	 */
	public void handleOut(String line)
	{
		scriptOutput += line + "\n";

		Intent intent2 = new Intent();
		intent2.setClass(this, Scripts.class);
		intent2.putExtra("script", this.getDir("bin", 0) + "/iptables -A FETCH -s " + line
				+ " -j DROP");
		startService(intent2);
	}
}
