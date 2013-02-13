package com.honeybadger.api.scripts;

/*--------------------------------------------------------------------------------------------------------------------------------
 * Version: 1.1
 * Date of last modification: 4 MAR 2012
 --------------------------------------------------------------------------------------------------------------------------------
 */

import java.io.DataInputStream;
import java.io.DataOutputStream;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

public class Scripts extends Service
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
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		handleStart(intent);
		return START_STICKY;
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
		scriptOutput += line + "\n";
	}

	/**
	 * Handles each line of the standard error.
	 * 
	 * @param line
	 *            Line of standard error.
	 */
	public void handleErr(String line)
	{
	}
	
	public void handleComplete()
	{
		
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
					@SuppressWarnings("deprecation")
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
					@SuppressWarnings("deprecation")
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

	@Override
	public IBinder onBind(Intent intent)
	{
		return null;
	}

}
