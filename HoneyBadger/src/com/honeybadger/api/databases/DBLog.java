package com.honeybadger.api.databases;

/*--------------------------------------------------------------------------------------------------------------------------------
 * Version: 4.0
 * Date of last modification: 11FEB13
 *
 * Edit 1.3: Moved to new package
 * Edit 4.0: Clean up; better fetch all
 *--------------------------------------------------------------------------------------------------------------------------------
 */

import android.database.sqlite.SQLiteDatabase;

public class DBLog
{

	public static final String KEY_BODY = "body";
	public static final String KEY_ROWID = "_id";
	
	public static final String CULUMN_ID_INDEX = "_id";
	public static final String CULUMN_INOUT = "INOUT";
	public static final String CULUMN_SRC = "SRC";
	public static final String CULUMN_DST = "DST";
	public static final String CULUMN_TOS = "TOS";
	public static final String CULUMN_PREC = "PREC";
	public static final String CULUMN_ID = "ID";
	public static final String CULUMN_PROTO = "Proto";
	public static final String CULUMN_SPT = "SPT";
	public static final String CULUMN_DPT = "DPT";
	public static final String CULUMN_UID = "UID";
	public static final String CULUMN_GID = "GID";
	public static final String CULUMN_TOTAL = "total";

	//private DatabaseHelper mDbHelper;
	public String check = "bad";

	/**
	 * Database creation sql statement
	 */
	private static final String DATABASE_CREATE = "create table logs (_id integer, INOUT text not null, SRC text not null, DST text not null, TOS text not null, PREC text not null, ID text not null, Proto text not null, SPT text not null,"
			+ "DPT text not null, UID text not null, GID text not null, total integer not null)";

	public static final String DATABASE_TABLE = "logs";

	public static void onCreate(SQLiteDatabase database)
	{
		database.execSQL(DATABASE_CREATE);
	}
	
	public static void onUpgrade(SQLiteDatabase database, int oldVersion,
			int newVersion)
	{
		database.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
		onCreate(database);
	}

}
