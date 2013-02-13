package com.honeybadger.api.scripts;

/*--------------------------------------------------------------------------------------------------------------------------------
 * Version: 1.1
 * Date of last modification: 22 April 2012
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

		Intent intent = new Intent(this, Scripts.class);
		intent.putExtra("script", this.getDir("bin", 0) + "/iptables -A FETCH -s " + line
				+ " -j DROP");
		startService(intent);
	}
}
