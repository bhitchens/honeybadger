package com.honeybadger.api.databases;

/*--------------------------------------------------------------------------------------------------------------------------------
 * Author(s): Brad Hitchens
 * Version: 1.3
 * Date of last modification: 14 JUNE 2012
 *
 * Edit 1.3: Created
 *--------------------------------------------------------------------------------------------------------------------------------
 */

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class AppsDBAdapter
{

	public static final String KEY_BODY = "body";
	public static final String KEY_ROWID = "_id";

	private static final String TAG = "AppsDBAdapter";
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;

	private static final String DATABASE_CREATE = "create table apps (UID int not null, NAME text not null, ICON blob not null, WSTATUS text not null, CSTATUS text not null)";

	private static final String DATABASE_NAME = "appDB";
	private static final String DATABASE_TABLE = "apps";
	private static final int DATABASE_VERSION = 2;

	private final Context mCtx;

	public String check = "bad";

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
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion
					+ ", which will destroy all old data");
		}
	}

	/**
	 * Constructor - takes the context to allow the database to be
	 * opened/created
	 * 
	 * @param ctx
	 *            the Context within which to work
	 */
	public AppsDBAdapter(Context ctx)
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
	public AppsDBAdapter open() throws SQLException
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
	public void createEntry(int uid, String name, byte[] icon, String wStatus, String cStatus)
	{
		// Check to see if entry already exists
		Cursor c = mDb.query(DATABASE_TABLE, new String[]
		{ "NAME" }, "UID='" + uid + "'", null, null, null, null);

		// if there is no entry
		if (c == null || c.getCount() == 0)
		{
			// this prepares values to be placed into entry
			ContentValues initialValues = new ContentValues();
			initialValues.put("UID", uid);
			initialValues.put("NAME", name);
			initialValues.put("ICON", icon);
			initialValues.put("WSTATUS", wStatus);
			initialValues.put("CSTATUS", cStatus);

			// inserts entry with data from initialValues
			mDb.insert(DATABASE_TABLE, null, initialValues);
		}
		else
		{
			c.moveToFirst();
			if (!c.getString(0).contains(name))
			{
				String sql = "UPDATE apps SET NAME= ? WHERE UID= ? ";
				Object[] bindArgs = new Object[]
				{ c.getString(0) + ", " + name, uid };
				mDb.execSQL(sql, bindArgs);
			}
		}

	}

	/**
	 * Used to change block/allow status of apps.
	 * 
	 * @param uid
	 *            UID of app
	 * @param status
	 *            whether app is blocked or allowed
	 */
	public void changeStatus(int uid, String name, String wStatus, String cStatus)
	{
		// Check to see if entry already exists
		Cursor c = mDb.query(DATABASE_TABLE, new String[]
		{ "ICON" }, "UID='" + uid + "'", null, null, null, null);
		// if there is no entry
		if (c == null || c.getCount() == 0)
		{
			this.createEntry(uid, name, c.getBlob(0), wStatus, cStatus);
		}
		else
		{
			if (!(wStatus == "default"))
			{
				String sql = "UPDATE apps SET WSTATUS= ? WHERE UID= ? ";
				Object[] bindArgs = new Object[]
				{ wStatus, uid };
				mDb.execSQL(sql, bindArgs);
				/*
				 * mDb.execSQL("UPDATE apps SET WSTATUS='" + wStatus + "' " +
				 * "WHERE (UID='" + uid + "')");
				 */
			}
			if (!(cStatus == "default"))
			{
				String sql = "UPDATE apps SET CSTATUS= ? WHERE UID= ? ";
				Object[] bindArgs = new Object[]
				{ cStatus, uid };
				mDb.execSQL(sql, bindArgs);
				/*
				 * mDb.execSQL("UPDATE apps SET CSTATUS='" + cStatus + "' " +
				 * "WHERE (UID='" + uid + "')");
				 */
			}
		}

	}

	public void checkAll(Boolean check)
	{
		String block = "";
		if (check)
		{
			block = "block";
		}
		else
		{
			block = "allow";
		}

		// fetch the table
		Cursor c = mDb.query(DATABASE_TABLE, new String[]
		{ "UID", "NAME", "ICON" }, null, null, null, null, null);

		while (c.getPosition() < c.getCount() - 1)
		{
			c.moveToNext();
			this.changeStatus(c.getInt(0), c.getString(1), block, block);
		}
	}

	/**
	 * Return a Cursor over the list of all entries in the database
	 * 
	 * @return Cursor over all entries
	 */
	public Cursor fetchAllEntries()
	{
		return mDb.query(DATABASE_TABLE, null, null, null, null, null, null);
	}

	public Boolean checkBlockW(int uid)
	{
		Cursor c = mDb.query(DATABASE_TABLE, new String[]
		{ "WSTATUS" }, "UID= ? ", new String[]
		{ Integer.toString(uid) }, null, null, null);

		if (c != null && c.getCount() != 0)
		{
			c.moveToFirst();
			if (c.getString(0).contains("block"))
			{
				return true;
			}
			else
			{
				return false;
			}
		}
		else
		{
			return false;
		}
	}

	public Boolean checkBlockC(int uid)
	{
		Cursor c = mDb.query(DATABASE_TABLE, new String[]
		{ "CSTATUS" }, "UID= ? ", new String[]
		{ Integer.toString(uid) }, null, null, null);
		if (c != null && c.getCount() != 0)
		{
			c.moveToFirst();
			if (c.getString(0).contains("block"))
			{
				return true;
			}
			else
			{
				return false;
			}
		}
		else
		{
			return false;
		}
	}

	public void clear()
	{
		mDb.execSQL("DROP TABLE apps");
		mDb.execSQL(DATABASE_CREATE);
	}
}
