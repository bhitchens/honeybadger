package com.honeybadger.api.databases;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;

public class DBContentProvider extends ContentProvider
{
	private RulesDBAdapter rulesDb;
	private LogDBAdapter logDb;

	private static final String AUTHORITY = "com.honeybadger.api.databases";

	private static final String BASE_PATH = "logDB";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH);

	public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/logDB";
	public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
			+ "/logItem";

	private static final int LOGDB = 10;
	private static final int RULEDB = 20;

	private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	static
	{
		sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/log", LOGDB);
		sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/rules", RULEDB);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs)
	{
		int uriType = sURIMatcher.match(uri);
		switch (uriType)
		{
			case LOGDB:
				throw new UnsupportedOperationException();
			case RULEDB:
				rulesDb.open();
				rulesDb.deleteEntry(selection, selectionArgs);
				rulesDb.close();
				return 1;
			default:
				throw new IllegalArgumentException("Unknown URI: " + uri);
		}

	}

	@Override
	public String getType(Uri uri)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Uri insert(Uri uri, ContentValues values)
	{
		int uriType = sURIMatcher.match(uri);
		switch (uriType)
		{
			case LOGDB:
				throw new UnsupportedOperationException();
			case RULEDB:
				rulesDb.open();
				rulesDb.createEntry(values);
				rulesDb.close();

				return uri;
			default:
				throw new IllegalArgumentException("Unknown URI: " + uri);
		}
	}

	@Override
	public boolean onCreate()
	{
		rulesDb = new RulesDBAdapter(getContext());
		logDb = new LogDBAdapter(getContext());

		return false;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
			String sortOrder)
	{
		int uriType = sURIMatcher.match(uri);
		switch (uriType)
		{
			case LOGDB:
				logDb.open();
				Cursor cursor = logDb.fetchAllEntries(selection, selectionArgs);

				// Make sure that potential listeners are getting notified
				cursor.setNotificationUri(getContext().getContentResolver(), uri);

				return cursor;
			case RULEDB:
				rulesDb.open();
				return rulesDb.fetchAllEntries();
			default:
				throw new IllegalArgumentException("Unknown URI: " + uri);
		}
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs)
	{
		throw new UnsupportedOperationException();
	}
}
