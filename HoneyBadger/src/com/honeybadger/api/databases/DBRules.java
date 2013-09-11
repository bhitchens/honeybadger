package com.honeybadger.api.databases;

/*--------------------------------------------------------------------------------------------------------------------------------
 * Version: 4.0
 * Date of last modification: 11FEB13
 * 
 * Edit 1.3: Moved to new package
 * Edit 4.0: Clean up
 *--------------------------------------------------------------------------------------------------------------------------------
 */

import android.database.sqlite.SQLiteDatabase;

public class DBRules
{
	public static final String KEY_IP_ADDRESS = "IPAddress";
	public static final String KEY_PORT = "Port";
	public static final String KEY_DIRECTION = "Direction";
	public static final String KEY_ACTION = "Action";
	public static final String KEY_ROWID = "_id";
	public static final String KEY_DOMAIN = "Domain";
	public static final String KEY_INTERFACE = "Interface";
	public static final String KEY_SAVED = "Saved";

	public String check = "bad";

	/**
	 * Database creation sql statement
	 */
	private static final String DATABASE_CREATE = "create table rules (_id integer, IPAddress text not null, Port text, Direction text not null, Action text not null, Domain text not null, Interface text not null, Saved text not null, PRIMARY KEY (IPAddress, Direction, Interface));";

	public static final String DATABASE_TABLE = "rules";
	
	public static void onCreate(SQLiteDatabase database)
	{
		database.execSQL(DATABASE_CREATE);
	}

	/**
	 * 
	 * @param database
	 * @param oldVersion
	 * @param newVersion
	 */
	public static void onUpgrade(SQLiteDatabase database, int oldVersion,
			int newVersion)
	{
		database.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
		onCreate(database);
	}

}
