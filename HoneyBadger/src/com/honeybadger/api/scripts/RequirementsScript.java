package com.honeybadger.api.scripts;

import android.content.Intent;

public class RequirementsScript extends Scripts
{
	/**
	 * Overrides method from {@link Scripts}; parses raw logging data and places
	 * it into database.
	 */
	@Override
	public void handleErr(String line)
	{	
		if (processScript.contains("iptables") && line.contains("can't initialize"))
		{
			Intent intent = new Intent();
			intent.setAction("com.honeybadger.ERROR");
			intent.putExtra("error", "iptables");
			sendBroadcast(intent);
			return;
		}
		if (processScript.contains("busybox") && line.contains("not found"))
		{
			Intent intent = new Intent();
			intent.setAction("com.honeybadger.ERROR");
			intent.putExtra("error", "busybox");
			sendBroadcast(intent);
			return;
		}
		if (processScript.contains("wget") && line.contains("bad address"))
		{
			Intent intent = new Intent();
			intent.setAction("com.honeybadger.ERROR");
			intent.putExtra("error", "wget");
			sendBroadcast(intent);
			return;
		}
	}
}
