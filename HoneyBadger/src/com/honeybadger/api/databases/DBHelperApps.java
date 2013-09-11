package com.honeybadger.api.databases;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelperApps extends SQLiteOpenHelper
{
	private static final String DATABASE_NAME = "apps.db";
	public static final int DATABASE_VERSION = 1;

	public DBHelperApps(Context context)
	{		
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	/**
	 * 
	 */
	@Override
	public void onCreate(SQLiteDatabase database)
	{
		DBApps.onCreate(database);
	}

	/**
	 * 
	 */
	@Override
	public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion)
	{
		DBApps.onUpgrade(database, oldVersion, newVersion);
	}
}
