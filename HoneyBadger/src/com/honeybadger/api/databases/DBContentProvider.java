package com.honeybadger.api.databases;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

public class DBContentProvider extends ContentProvider
{
	private DBHelperRules RulesDB;
	private DBHelperLog LogDB;
	private DBHelperApps AppsDB;

	private static final String AUTHORITY = "com.honeybadger.api.databases";

	private static final String BASE_PATH_LOG = "logDB";
	public static final Uri CONTENT_URI_LOG = Uri.parse("content://" + AUTHORITY + "/"
			+ BASE_PATH_LOG);

	public static final String CONTENT_TYPE_LOG = ContentResolver.CURSOR_DIR_BASE_TYPE + "/logDB";
	public static final String CONTENT_ITEM_TYPE_LOG = ContentResolver.CURSOR_ITEM_BASE_TYPE
			+ "/logItem";

	private static final int LOGDB_ID = 10;
	private static final int LOGDB = 20;

	private static final String BASE_PATH_RULES = "rulesDB";
	public static final Uri CONTENT_URI_RULES = Uri.parse("content://" + AUTHORITY + "/"
			+ BASE_PATH_RULES);

	public static final String CONTENT_TYPE_RULES = ContentResolver.CURSOR_DIR_BASE_TYPE
			+ "/rulesDB";
	public static final String CONTENT_ITEM_TYPE_RULES = ContentResolver.CURSOR_ITEM_BASE_TYPE
			+ "/rule";

	private static final int RULEDB_ID = 30;
	private static final int RULEDB = 40;
	private static final int RULEDB_MATCH = 50;

	private static final String BASE_PATH_APPS = "appsDB";
	public static final Uri CONTENT_URI_APPS = Uri.parse("content://" + AUTHORITY + "/"
			+ BASE_PATH_APPS);

	public static final String CONTENT_TYPE_APPS = ContentResolver.CURSOR_DIR_BASE_TYPE + "/appsDB";
	public static final String CONTENT_ITEM_TYPE_APPS = ContentResolver.CURSOR_ITEM_BASE_TYPE
			+ "/app";

	// private static final int APPDB_ID = 60;
	private static final int APPDB = 70;
	private static final int APPDB_MATCH = 80;

	private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	static
	{
		sURIMatcher.addURI(AUTHORITY, BASE_PATH_LOG, LOGDB);
		sURIMatcher.addURI(AUTHORITY, BASE_PATH_LOG + "/#", LOGDB_ID);

		sURIMatcher.addURI(AUTHORITY, BASE_PATH_RULES, RULEDB);
		sURIMatcher.addURI(AUTHORITY, BASE_PATH_RULES + "/*", RULEDB_MATCH);
		sURIMatcher.addURI(AUTHORITY, BASE_PATH_RULES + "/#", RULEDB_ID);

		sURIMatcher.addURI(AUTHORITY, BASE_PATH_APPS, APPDB);
		sURIMatcher.addURI(AUTHORITY, BASE_PATH_APPS + "/*", APPDB_MATCH);
	}

	@Override
	public boolean onCreate()
	{
		RulesDB = new DBHelperRules(getContext());
		LogDB = new DBHelperLog(getContext());
		AppsDB = new DBHelperApps(getContext());
		return false;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs)
	{
		int rowsDeleted = 0;
		SQLiteDatabase sqlDB;
		int uriType = sURIMatcher.match(uri);
		switch (uriType)
		{
			case LOGDB:
				throw new UnsupportedOperationException();
			case LOGDB_ID:
				throw new UnsupportedOperationException();
			case RULEDB:
				sqlDB = RulesDB.getWritableDatabase();
				rowsDeleted = sqlDB.delete(DBRules.DATABASE_TABLE, selection, selectionArgs);
				break;
			case RULEDB_ID:
				throw new UnsupportedOperationException();
			case APPDB:
				sqlDB = AppsDB.getWritableDatabase();
				sqlDB.delete(DBApps.DATABASE_TABLE, "UID= ? ", selectionArgs);
				break;
			default:
				throw new IllegalArgumentException("Unknown URI: " + uri);

		}
		return rowsDeleted;
	}

	@Override
	public String getType(Uri uri)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Uri insert(Uri uri, ContentValues values)
	{
		
		SQLiteDatabase sqlDB;
		Cursor c;
		int uriType = sURIMatcher.match(uri);
		switch (uriType)
		{
			case LOGDB:
				sqlDB = LogDB.getWritableDatabase();
				c = sqlDB.query(
						DBLog.DATABASE_TABLE,
						new String[]
						{ "_id" },
						"INOUT= ? AND SRC= ? AND DST= ? AND Proto= ? AND SPT= ? AND DPT= ?",
						new String[]
						{ values.getAsString(DBLog.CULUMN_INOUT),
								values.getAsString(DBLog.CULUMN_SRC),
								values.getAsString(DBLog.CULUMN_DST),
								values.getAsString(DBLog.CULUMN_PROTO),
								values.getAsString(DBLog.CULUMN_SPT),
								values.getAsString(DBLog.CULUMN_DPT) }, null, null, null);

				if (c == null || c.getCount() == 0)
				{
					sqlDB.insert(DBLog.DATABASE_TABLE, null, values);
				}
				else
				{
					sqlDB.execSQL("UPDATE logs SET total=total+1 WHERE (SRC='"
							+ values.getAsString(DBLog.CULUMN_SRC) + "')");
				}
				return uri;
			case RULEDB:
				sqlDB = RulesDB.getWritableDatabase();
				sqlDB.insert(DBRules.DATABASE_TABLE, null, values);
				return uri;
			case APPDB:
				sqlDB = AppsDB.getWritableDatabase();
				// Check to see if entry already exists
				c = sqlDB.query(DBApps.DATABASE_TABLE, new String[]
				{ "NAME" }, "UID=?", new String[]
				{ values.getAsString(DBApps.KEY_UID) }, null, null, null);

				// if there is no entry
				if (c == null || c.getCount() == 0)
				{
					// inserts entry with data from initialValues
					sqlDB.insert(DBApps.DATABASE_TABLE, null, values);
				}
				else
				{
					c.moveToFirst();
					if (!c.getString(0).contains(values.getAsString(DBApps.KEY_NAME)))
					{
						String sql = "UPDATE apps SET NAME= ? WHERE UID= ? ";
						Object[] bindArgs = new Object[]
						{ c.getString(0) + "; " + values.getAsString(DBApps.KEY_NAME),
								values.getAsString(DBApps.KEY_UID) };
						sqlDB.execSQL(sql, bindArgs);
					}
				}
				return uri;
			default:
				throw new IllegalArgumentException("Unknown URI: " + uri);
		}
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
			String sortOrder)
	{
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		SQLiteDatabase db;

		int uriType = sURIMatcher.match(uri);
		switch (uriType)
		{
			case LOGDB:
				queryBuilder.setTables(DBLog.DATABASE_TABLE);
				db = LogDB.getWritableDatabase();
				break;
			case RULEDB:
				queryBuilder.setTables(DBRules.DATABASE_TABLE);
				db = RulesDB.getWritableDatabase();
				break;
			case APPDB:
				queryBuilder.setTables(DBApps.DATABASE_TABLE);
				db = AppsDB.getWritableDatabase();
				break;
			default:
				throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		Cursor cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null,
				sortOrder);
		cursor.setNotificationUri(getContext().getContentResolver(), uri);

		return cursor;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs)
	{
		SQLiteDatabase sqlDB;
		String id;

		int uriType = sURIMatcher.match(uri);
		switch (uriType)
		{
			case LOGDB:
				LogDB.onUpgrade(LogDB.getWritableDatabase(), DBHelperLog.DATABASE_VERSION,
						DBHelperLog.DATABASE_VERSION);
				break;
			case RULEDB_MATCH:
				sqlDB = RulesDB.getWritableDatabase();
				id = uri.getLastPathSegment();
				sqlDB.execSQL("UPDATE rules SET Saved='true' WHERE (IPAddress='" + id + "')");
				break;
			case RULEDB:
				throw new UnsupportedOperationException();
			case APPDB:
				sqlDB = AppsDB.getWritableDatabase();
				if (!(selectionArgs[0].contains("default")))
				{
					String sql = "UPDATE apps SET WSTATUS= ? WHERE UID= ? ";
					Object[] bindArgs = new Object[]
					{ selectionArgs[0], selection };
					sqlDB.execSQL(sql, bindArgs);
				}
				if (!(selectionArgs[1].contains("default")))
				{
					String sql = "UPDATE apps SET CSTATUS= ? WHERE UID= ? ";
					Object[] bindArgs = new Object[]
					{ selectionArgs[1], selection };
					sqlDB.execSQL(sql, bindArgs);
				}
				break;
			case APPDB_MATCH:
				id = uri.getLastPathSegment();
				sqlDB = AppsDB.getWritableDatabase();
				Cursor c;
				if (id.contains("delete"))
				{
					AppsDB.onUpgrade(sqlDB, DBHelperApps.DATABASE_VERSION,
							DBHelperApps.DATABASE_VERSION);
				}
				else
				{
					String block = "";
					if (selection == "true")
					{
						block = "block";
					}
					else
					{
						block = "allow";
					}

					// fetch the table
					c = sqlDB.query(DBApps.DATABASE_TABLE, new String[]
					{ "UID", "NAME", "ICON" }, null, null, null, null, null);
					if (id.contains("all"))
					{
						while (c.getPosition() < c.getCount() - 1)
						{
							c.moveToNext();
							this.update(DBContentProvider.CONTENT_URI_APPS, null,
									Integer.toString(c.getInt(0)), new String[]
									{ block, block });
						}
					}
					else if (id.contains("wifi"))
					{
						while (c.getPosition() < c.getCount() - 1)
						{
							c.moveToNext();
							this.update(DBContentProvider.CONTENT_URI_APPS, null,
									Integer.toString(c.getInt(0)), new String[]
									{ block, "default" });
						}
					}
					else if (id.contains("cell"))
					{
						while (c.getPosition() < c.getCount() - 1)
						{
							c.moveToNext();
							this.update(DBContentProvider.CONTENT_URI_APPS, null,
									Integer.toString(c.getInt(0)), new String[]
									{ "default", block });
						}
					}
					else
					{
						throw new IllegalArgumentException("Unknown URI: " + uri);
					}
				}
				break;
			default:
				throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		return 1;
	}
}
