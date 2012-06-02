package com.honeybadger.api;

/*--------------------------------------------------------------------------------------------------------------------------------
 * Author(s): Brad Hitchens
 * Version: 1.1
 * Date of last modification: 4 MAR 2012
 --------------------------------------------------------------------------------------------------------------------------------
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.honeybadger.R;

import android.content.Context;

public final class StartUp
{

	/**
	 * This method ensures that IPTables is installed.
	 * 
	 * @param ctx
	 *            Context of the calling Activity or Service
	 * @return Return true if IPTables is installed, false if it is not.
	 */
	public static boolean installIPTables(Context ctx)
	{
		boolean ret = false;
		File file = new File(ctx.getDir("bin", 0), "iptables");
		if (!file.exists())
		{
			try
			{
				final String path = file.getAbsolutePath();
				final FileOutputStream os = new FileOutputStream(file);
				final InputStream is = ctx.getResources().openRawResource(R.raw.iptables_armv5);
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
		}
		return ret;
	}
}
