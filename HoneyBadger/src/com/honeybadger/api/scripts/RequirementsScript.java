package com.honeybadger.api.scripts;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;

public class RequirementsScript extends IntentService
{
	protected String scriptOutput = "";
	protected String processName = "su"; // Name of process to be called
	protected String processScript;

	protected DataOutputStream stdin = null;
	protected DataInputStream stdout = null;
	protected DataInputStream stderr = null;

	protected Boolean scriptRunning = false;
	protected Boolean stdoutFinished = false;
	protected Boolean stderrFinished = false;

	protected Thread stdoutThread = null;
	protected Thread stderrThread = null;
	protected Thread stdinThread = null;

	protected Process process = null;

	public RequirementsScript()
	{
		super("Req_Script");
	}

	@Override
	protected void onHandleIntent(Intent intent)
	{
		handleStart(intent);
	}

	/**
	 * Initiates the thread which runs the process for the script.
	 * 
	 * @param intent
	 */
	protected void handleStart(Intent intent)
	{
		Bundle extras = intent.getExtras();
		processScript = extras.getString("script");
		ExecuteThread script = new ExecuteThread(); // creates new script thread
		script.run(); // runs thread

		while (script.isAlive())
		{

		}
		;
		this.stopSelf();
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
	}

	/**
	 * Handles each line of the standard output.
	 * 
	 * @param line
	 *            Line of standard output.
	 */
	public void handleOut(String line)
	{
	}
	
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

	/**
	 * 
	 * Thread which runs and controls input and output of process for script.
	 * 
	 */
	public class ExecuteThread extends Thread
	{
		public void run()
		{
			super.setPriority(MIN_PRIORITY);
			Execute();
		}

		void Execute()
		{
			try
			{
				process = Runtime.getRuntime().exec(processName);

				stdin = new DataOutputStream(process.getOutputStream());
				stdout = new DataInputStream(process.getInputStream());
				stderr = new DataInputStream(process.getErrorStream());

				stdinThread = new Thread()
				{
					public void run()
					{
						super.setPriority(MIN_PRIORITY);

						while (scriptRunning)
						{
							try
							{
								super.sleep(200);
							}
							catch (Exception e)
							{
							}
						}
					}
				};

				stdoutThread = new Thread()
				{
					public void run()
					{
						super.setPriority(MIN_PRIORITY);

						try
						{
							String line;
							while ((line = stdout.readLine()) != null)
							{
								super.sleep(10);
								handleOut(line);
							}
							stdoutFinished = true;
						}
						catch (Exception e)
						{
						}
					}
				};

				stderrThread = new Thread()
				{
					public void run()
					{
						super.setPriority(MIN_PRIORITY);
						try
						{
							String line;
							while ((line = stderr.readLine()) != null)
							{
								super.sleep(10);
								handleErr(line);
							}
							stderrFinished = true;
						}
						catch (Exception e)
						{
						}
					}
				};

				scriptRunning = true;

				stdoutThread.start();
				stderrThread.start();
				stdinThread.start();

				stdin.writeBytes(processScript + " \n");
				stdin.writeBytes("exit\n");

				stdin.flush();

				process.waitFor();

				while (!stdoutFinished || !stderrFinished)
				{
				}

				stderr.close();
				stdout.close();
				stdin.close();

				stdoutThread = null;
				stderrThread = null;
				stdinThread = null;

				scriptRunning = false;

				process.destroy();

			}
			catch (Exception e)
			{
			}
		}
	}

	
}
