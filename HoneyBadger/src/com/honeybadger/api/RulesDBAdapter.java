package com.honeybadger.api;

/*--------------------------------------------------------------------------------------------------------------------------------
 * Author(s): Brad Hitchens
 * Version: 1.1
 * Date of last modification: 4 MAR 2012
 * Source Info: n/a
 |Information regarding the creation of a database was obtained and adapted from the notepad tutorial on the official 
 |Android developers website
 --------------------------------------------------------------------------------------------------------------------------------
 */

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class RulesDBAdapter
{
	public static final String KEY_IP_ADDRESS = "IPAddress";
	public static final String KEY_PORT = "Port";
	public static final String KEY_DIRECTION = "Direction";
	public static final String KEY_ACTION = "Action";
	public static final String KEY_ROWID = "_id";
	public static final String KEY_DOMAIN = "Domain";
	public static final String KEY_SAVED = "Saved";

	private static final String TAG = "RulesDBAdapter";
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;
	public String check = "bad";

	/**
	 * Database creation sql statement
	 */
	private static final String DATABASE_CREATE = "create table rules (_id integer, "
			+ "IPAddress text not null, " + "Port text, " + "Direction text not null, "
			+ "Action text not null, " + "Domain text not null, " + "Saved text not null, "
			+ "PRIMARY KEY (IPAddress, Direction));";

	private static final String DATABASE_NAME = "ruleDB";
	private static final String DATABASE_TABLE = "rules";
	private static final int DATABASE_VERSION = 2;

	private final Context mCtx;

	private static class DatabaseHelper extends SQLiteOpenHelper
	{

		DatabaseHelper(Context context)
		{
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db)
		{

			db.execSQL(DATABASE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
		{
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion
					+ ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS logs");
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
	public RulesDBAdapter(Context ctx)
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
	public RulesDBAdapter open() throws SQLException
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
	public long createEntry(String ip, String port, String direction, String action, String domain)
	{
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_IP_ADDRESS, ip);
		initialValues.put(KEY_PORT, port);
		initialValues.put(KEY_DIRECTION, direction);
		initialValues.put(KEY_ACTION, action);
		initialValues.put(KEY_DOMAIN, domain);
		initialValues.put(KEY_SAVED, "false");

		return mDb.insert(DATABASE_TABLE, null, initialValues);
	}

	/**
	 * "Saved" field of row to 'true' for given ip address.
	 * 
	 * @param ip
	 *            IP address used as key for row to be updated.
	 */
	public void changeSaved(String ip)
	{
		mDb.execSQL("UPDATE rules SET Saved='true' WHERE (IPAddress='" + ip + "')");
	}

	/**
	 * Delete the entry with the given rowId
	 * 
	 * @param rowId
	 *            id of note to delete
	 * @return true if deleted, false otherwise
	 */
	public boolean deleteEntry(String ip, String direction)
	{

		return mDb.delete(DATABASE_TABLE, KEY_IP_ADDRESS + "='" + ip + "' AND " + KEY_DIRECTION
				+ "='" + direction + "'", null) > 0;
	}

	/**
	 * Return a Cursor over the list of all entries in the database
	 * 
	 * @return Cursor over all entries
	 */
	public Cursor fetchAllEntries()
	{
		return mDb.query(DATABASE_TABLE, new String[]
		{ KEY_IP_ADDRESS, KEY_PORT, KEY_DIRECTION, KEY_ACTION, KEY_DOMAIN, KEY_SAVED }, null, null,
				null, null, null);
	}

	/**
	 * Fetches row specified by rowId parameter.
	 * 
	 * @param rowId
	 * @return
	 * @throws SQLException
	 */
	public Cursor fetchEntry(long rowId) throws SQLException
	{
		Cursor mCursor =

		mDb.query(true, DATABASE_TABLE, new String[]
		{ KEY_IP_ADDRESS, KEY_PORT, KEY_DIRECTION, KEY_ACTION, KEY_DOMAIN, KEY_SAVED }, KEY_ROWID
				+ "=" + rowId, null, null, null, null, null);
		if (mCursor != null)
		{
			mCursor.moveToFirst();
		}
		return mCursor;
	}

}
