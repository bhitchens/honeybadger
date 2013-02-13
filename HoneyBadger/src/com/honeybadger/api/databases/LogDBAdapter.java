package com.honeybadger.api.databases;

/*--------------------------------------------------------------------------------------------------------------------------------
 * Version: 4.0
 * Date of last modification: 11FEB13
 *
 * Edit 1.3: Moved to new package
 * Edit 4.0: Clean up; better fetch all
 *--------------------------------------------------------------------------------------------------------------------------------
 */

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class LogDBAdapter
{

	public static final String KEY_BODY = "body";
	public static final String KEY_ROWID = "_id";

	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;
	public String check = "bad";

	/**
	 * Database creation sql statement
	 */
	private static final String DATABASE_CREATE = "create table logs (_id integer, INOUT text not null, SRC text not null, DST text not null, TOS text not null, PREC text not null, ID text not null, Proto text not null, SPT text not null,"
			+ "DPT text not null, UID text not null, GID text not null, total integer not null)";

	private static final String DATABASE_NAME = "logDB";
	private static final String DATABASE_TABLE = "logs";
	private static final int DATABASE_VERSION = 2;

	private final Context mCtx;

	private static class DatabaseHelper extends SQLiteOpenHelper
	{
		DatabaseHelper(Context context)
		{
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		/**
		 * Creates database for logging
		 */
		public void onCreate(SQLiteDatabase db)
		{
			db.execSQL(DATABASE_CREATE);
		}
		
		@Override
		/**
		 * replaces database with new database
		 */
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
		{			
		}
	}

	/**
	 * Constructor - takes the context to allow the database to be
	 * opened/created
	 * 
	 * @param ctx
	 *            the Context within which to work
	 */
	public LogDBAdapter(Context ctx)
	{
		this.mCtx = ctx;
	}

	
	/**
	 * Open the Log database. If it cannot be opened, try to create a new
	 * instance of the database. If it cannot be created, throw an exception to
	 * signal the failure
	 * 
	 * @return this (self reference, allowing this to be chained in an
	 *         initialization call)
	 * @throws SQLException
	 *             if the database could be neither opened or created
	 */
	public LogDBAdapter open() throws SQLException
	{
		mDbHelper = new DatabaseHelper(mCtx);
		mDb = mDbHelper.getWritableDatabase();
		check = mDb.getPath();
		return this;
	}

	public void close()
	{
		mDbHelper.close();
	}

	/**
	 * Create a new entry using the body provided. If the entry is successfully
	 * created return the new rowId for that entry, otherwise return a -1 to
	 * indicate failure.
	 * 
	 * @param body
	 *            the body of the note
	 * @return rowId or -1 if failed
	 */
	public long createEntry(String inOut, String src, String dst, String tos, String prec,
			String id, String proto, String spt, String dpt, String uid, String gid)
	{
		// attempts to query table entry with same data (source, destination,
		// protocol, etc)
		Cursor c = mDb.query(DATABASE_TABLE, null, "INOUT='" + inOut + "' AND SRC='" + src
				+ "' AND DST='" + dst + "' AND Proto='" + proto + "' AND SPT='" + spt
				+ "' AND DPT='" + dpt + "'", null, null, null, null);

		// if such an entry does not exist, c will be null
		if (c == null || c.getCount() == 0)
		{
			// this prepares values to be placed into entry
			ContentValues initialValues = new ContentValues();
			initialValues.put("INOUT", inOut);
			initialValues.put("SRC", src);
			initialValues.put("DST", dst);
			initialValues.put("TOS", tos);
			initialValues.put("PREC", prec);
			initialValues.put("ID", id);
			initialValues.put("Proto", proto);
			initialValues.put("SPT", spt);
			initialValues.put("DPT", dpt);
			initialValues.put("UID", uid);
			initialValues.put("GID", gid);
			initialValues.put("total", 1);

			// inserts entry with data from initialValues
			return mDb.insert(DATABASE_TABLE, null, initialValues);
		}
		// if the entry does exist...
		else
		{
			// This will increment the count in total by 1
			mDb.execSQL("UPDATE logs SET total=total+1 WHERE (SRC='" + src + "')");
			return 0;
		}
	}


	/**
	 * Return a Cursor over the list of all entries in the database
	 * 
	 * @return Cursor over all entries
	 */
	public Cursor fetchAllEntries(String selection, String[] selectionArgs)
	{
		return mDb.query(DATABASE_TABLE, new String[]
				{ "_id", "INOUT", "SRC", "DST", "Proto", "SPT", "DPT", "UID", "total" }, selection, selectionArgs, null, null, null);
	}


	/**
	 * Clears log data by dropping then recreating table.
	 */
	public void clearLog()
	{
		mDb.execSQL("DROP TABLE logs");
		mDb.execSQL(DATABASE_CREATE);
	}

}
