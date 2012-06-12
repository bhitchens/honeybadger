package com.honeybadger.api;

/*--------------------------------------------------------------------------------------------------------------------------------
 * Author(s): Brad Hitchens
 * Version: 1.2
 * Date of last modification: 12 June 2012
 *
 * Edit 1.2 (Initial): This class contains static functions that are used to create the initial script.
 *--------------------------------------------------------------------------------------------------------------------------------
 */

import android.content.Context;
import android.content.SharedPreferences;

public final class ScriptCreation
{

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
					+ "/iptables -I ACCEPTOUT -m limit --limit 100/second -j LOG --log-level 7 --log-prefix \"[HoneyBadger - ACCEPTOUT]\" --log-uid"
					+ "\n"
					+ ctx.getDir("bin", 0)
					+ "/iptables -I ACCEPTIN -m limit --limit 100/second -j LOG --log-level 7 --log-prefix \"[HoneyBadger - ACCEPTIN]\" --log-uid"
					+ "\n"
					+ ctx.getDir("bin", 0)
					+ "/iptables -I DROPOUT -m limit --limit 100/second -j LOG --log-level 7 --log-prefix \"[HoneyBadger - DROPOUT]\" --log-uid"
					+ "\n"
					+ ctx.getDir("bin", 0)
					+ "/iptables -I DROPIN -m limit --limit 100/second -j LOG --log-level 7 --log-prefix \"[HoneyBadger - DROPIN]\" --log-uid"
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

}
