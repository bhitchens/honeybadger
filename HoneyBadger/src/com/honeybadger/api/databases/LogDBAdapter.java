package com.honeybadger.api.databases;

/*--------------------------------------------------------------------------------------------------------------------------------
 * Author(s): Todd Berry Ann, Brad Hitchens
 * Version: 1.3
 * Date of last modification: 14 JUNE 2012
 * Source Info: n/a
 * Information regarding the creation of a database was obtained and adapted from the notepad tutorial on the official 
 * Android developers website.
 *
 * Edit 1.3: Moved to new package
 *--------------------------------------------------------------------------------------------------------------------------------
 */

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class LogDBAdapter
{

	public static final String KEY_BODY = "body";
	public static final String KEY_ROWID = "_id";

	private static final String TAG = "LogDBAdapter";
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;
	public String check = "bad";

	/**
	 * Database creation sql statement
	 */
	private static final String DATABASE_CREATE = "create table logs (INOUT text not null, SRC text not null, DST text not null, TOS text not null, PREC text not null, ID text not null, Proto text not null, SPT text not null,"
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
	 * Delete the entry with the given rowId
	 * 
	 * @param rowId
	 *            id of note to delete
	 * @return true if deleted, false otherwise
	 */
	public boolean deleteEntry(long rowId)
	{

		return mDb.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
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

	/**
	 * Fetches row specified by rowId parameter.
	 * 
	 * @param rowId
	 *            Row to be returned.
	 * @return Cursor over cells of returned row.
	 * @throws SQLException
	 */
	public Cursor fetchEntry(long rowId) throws SQLException
	{
		Cursor mCursor =

		mDb.query(true, DATABASE_TABLE, new String[]
		{ KEY_BODY }, KEY_ROWID + "=" + rowId, null, null, null, null, null);
		if (mCursor != null)
		{
			mCursor.moveToFirst();
		}
		return mCursor;
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
