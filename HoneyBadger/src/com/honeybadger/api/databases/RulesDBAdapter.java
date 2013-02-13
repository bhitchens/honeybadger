package com.honeybadger.api.databases;

/*--------------------------------------------------------------------------------------------------------------------------------
 * Version: 4.0
 * Date of last modification: 11FEB13
 * 
 * Edit 1.3: Moved to new package
 * Edit 4.0: Clean up
 *--------------------------------------------------------------------------------------------------------------------------------
 */

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class RulesDBAdapter
{
	public static final String KEY_IP_ADDRESS = "IPAddress";
	public static final String KEY_PORT = "Port";
	public static final String KEY_DIRECTION = "Direction";
	public static final String KEY_ACTION = "Action";
	public static final String KEY_ROWID = "_id";
	public static final String KEY_DOMAIN = "Domain";
	public static final String KEY_INTERFACE = "Interface";
	public static final String KEY_SAVED = "Saved";

	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;
	public String check = "bad";

	/**
	 * Database creation sql statement
	 */
	private static final String DATABASE_CREATE = "create table rules (_id integer, IPAddress text not null, Port text, Direction text not null, Action text not null, Domain text not null, Interface text not null, Saved text not null, PRIMARY KEY (IPAddress, Direction, Interface));";

	private static final String DATABASE_NAME = "ruleDB";
	private static final String DATABASE_TABLE = "rules";
	private static final int DATABASE_VERSION = 3;

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
	 * Open the Rules database. If it cannot be opened, try to create a new
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

	
	public long createEntry(ContentValues values)
	{
		return mDb.insert(DATABASE_TABLE, null, values);
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
	 * Delete the entry with the given parameters
	 * 
	 * @param rowId
	 *            id of note to delete
	 * @return true if deleted, false otherwise
	 */
	public void deleteEntry(String where, String[] args)
	{
		mDb.delete(DATABASE_TABLE, where, args);
	}

	/**
	 * Return a Cursor over the list of all entries in the database
	 * 
	 * @return Cursor over all entries
	 */
	public Cursor fetchAllEntries()
	{
		return mDb.query(DATABASE_TABLE,
				new String[]
				{ KEY_ROWID, KEY_IP_ADDRESS, KEY_PORT, KEY_DIRECTION, KEY_ACTION, KEY_INTERFACE, KEY_DOMAIN,
						KEY_SAVED }, null, null, null, null, null);
	}

	public void clear()
	{
		mDb.execSQL("DROP TABLE rules");
		mDb.execSQL(DATABASE_CREATE);
	}

}
