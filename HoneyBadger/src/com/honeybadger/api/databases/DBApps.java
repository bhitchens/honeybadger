package com.honeybadger.api.databases;

/*--------------------------------------------------------------------------------------------------------------------------------
 * Version: 4.5
 * Date of last modification: 11SEP13
 *
 * Edit 1.3: Created
 * Edit 4.4 (17JUN13): Added deleteEntry
 * Edit 4.5 (11SEP13): Revamp of database interaction
 *--------------------------------------------------------------------------------------------------------------------------------
 */

import android.database.sqlite.SQLiteDatabase;

public class DBApps
{

	public static final String KEY_ROWID = "_id";
	public static final String KEY_UID = "UID";
	public static final String KEY_NAME = "NAME";
	public static final String KEY_ICON = "ICON";
	public static final String KEY_WSTATUS = "WSTATUS";
	public static final String KEY_CSTATUS = "CSTATUS";

	private static final String DATABASE_CREATE = "create table apps (UID int not null, NAME text not null, ICON blob, WSTATUS text not null, CSTATUS text not null)";

	public static final String DATABASE_TABLE = "apps";

	/**
	 * Creates database for logging
	 */
	public static void onCreate(SQLiteDatabase db)
	{
		db.execSQL(DATABASE_CREATE);
	}

	/**
	 * replaces database with new database
	 */
	public static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
		onCreate(db);
	}
}
