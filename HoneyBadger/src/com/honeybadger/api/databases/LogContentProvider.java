package com.honeybadger.api.databases;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

public class LogContentProvider extends ContentProvider
{

	private LogDBAdapter db;

	private static final int LOGDB = 10;
	private static final int LOGDB_ID = 20;

	private static final String AUTHORITY = "com.honeybadger.api.databases";

	private static final String BASE_PATH = "logDB";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH);

	public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/logDB";
	public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
			+ "/logItem";

	private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	static
	{
		sURIMatcher.addURI(AUTHORITY, BASE_PATH, LOGDB);
		sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/#", LOGDB_ID);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public String getType(Uri uri)
	{
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean onCreate()
	{
		db = new LogDBAdapter(getContext());
		return false;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
			String sortOrder)
	{
		// Uisng SQLiteQueryBuilder instead of query() method
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

		// Set the table
		queryBuilder.setTables("logs");

		int uriType = sURIMatcher.match(uri);
		switch (uriType)
		{
			case LOGDB:
				break;
			case LOGDB_ID:
				// Adding the ID to the original query
				queryBuilder.appendWhere("_id" + "=" + uri.getLastPathSegment());
				break;
			default:
				throw new IllegalArgumentException("Unknown URI: " + uri);
		}

		db.open();
		Cursor cursor = db.fetchAllEntries(selection, selectionArgs);

		Log.d("test", "1b:" + cursor.getCount());
		// Make sure that potential listeners are getting notified
		cursor.setNotificationUri(getContext().getContentResolver(), uri);

		return cursor;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs)
	{
		throw new UnsupportedOperationException();
	}
}
