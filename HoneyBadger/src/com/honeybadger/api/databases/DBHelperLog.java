package com.honeybadger.api.databases;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelperLog extends SQLiteOpenHelper
{
	private static final String DATABASE_NAME = "library.db";
	public static final int DATABASE_VERSION = 1;

	public DBHelperLog(Context context)
	{
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	/**
	 * 
	 */
	@Override
	public void onCreate(SQLiteDatabase database)
	{
		DBLog.onCreate(database);
	}

	/**
	 * 
	 */
	@Override
	public void onUpgrade(SQLiteDatabase database, int oldVersion,
			int newVersion)
	{
		DBLog.onUpgrade(database, oldVersion, newVersion);
	}
}
