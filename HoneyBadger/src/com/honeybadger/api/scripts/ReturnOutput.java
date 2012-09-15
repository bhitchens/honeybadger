package com.honeybadger.api.scripts;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

public class ReturnOutput extends IntentService
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

	SharedPreferences settings;
	SharedPreferences.Editor editor;

	public ReturnOutput()
	{
		super("Ret_Out");
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

	public void handleOut(String line)
	{
		scriptOutput += line + "\n";
	}

	public void handleErr(String line)
	{

	}

	public void handleComplete()
	{
		settings = getSharedPreferences("main", 0);
		editor = settings.edit();
		editor.putString("tempText", scriptOutput);
		editor.commit();
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

				handleComplete();

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