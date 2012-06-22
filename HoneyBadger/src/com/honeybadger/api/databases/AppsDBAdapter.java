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

	private static final String DATABASE_CREATE = "create table apps (UID int not null, NAME text not null, ICON blob not null, STATUS text not null)";

	private static final String DATABASE_NAME = "appDB";
	private static final String DATABASE_TABLE = "apps";
	private static final int DATABASE_VERSION = 1;

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
			db.execSQL("DROP TABLE IF EXISTS apps");
			onCreate(db);
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
	public void createEntry(int uid, String name, byte[] icon, String status)
	{
		// Check to see if entry already exists
		Cursor c = mDb.query(DATABASE_TABLE, new String[]{"NAME"}, "UID='" + uid + "'", null, null, null, null);

		// if there is no entry
		if (c == null || c.getCount() == 0)
		{
			// this prepares values to be placed into entry
			ContentValues initialValues = new ContentValues();
			initialValues.put("UID", uid);
			initialValues.put("NAME", name);
			initialValues.put("ICON", icon);
			initialValues.put("STATUS", status);

			// inserts entry with data from initialValues
			mDb.insert(DATABASE_TABLE, null, initialValues);
		}
		else
		{
			c.moveToFirst();
			if (!c.getString(0).contains(name))
			{
				mDb.execSQL("UPDATE apps SET NAME='" + c.getString(0) + ", " + name + "'" + "WHERE (UID='" + uid + "')");
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
	public void changeStatus(int uid, String name, String status)
	{
		// Check to see if entry already exists
		Cursor c = mDb.query(DATABASE_TABLE, new String[]{"ICON"}, "UID='" + uid + "'", null, null, null, null);
		// if there is no entry
		if (c == null || c.getCount() == 0)
		{
			this.createEntry(uid, name, c.getBlob(0), status);
		}
		else
		{
			mDb.execSQL("UPDATE apps SET STATUS='" + status + "'" + "WHERE (UID='" + uid + "')");
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
		Cursor c = mDb.query(DATABASE_TABLE, new String[]{"UID", "NAME", "ICON"}, null, null, null, null, null);
	
		while (c.getPosition() < c.getCount() - 1)
		{
			c.moveToNext();
			this.changeStatus(c.getInt(0), c.getString(1), block);
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

	public Boolean checkBlock(int uid)
	{
		Cursor c = mDb.query(DATABASE_TABLE, new String[]
		{ "STATUS" }, "UID='" + uid + "'", null, null, null, null);
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
