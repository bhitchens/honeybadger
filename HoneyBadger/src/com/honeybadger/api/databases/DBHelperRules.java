package com.honeybadger.api.databases;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelperRules extends SQLiteOpenHelper
{
	private static final String DATABASE_NAME = "rules.db";
	public static final int DATABASE_VERSION = 1;

	public DBHelperRules(Context context)
	{		
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	/**
	 * 
	 */
	@Override
	public void onCreate(SQLiteDatabase database)
	{
		DBRules.onCreate(database);
	}

	/**
	 * 
	 */
	@Override
	public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion)
	{
		DBRules.onUpgrade(database, oldVersion, newVersion);
	}
}
