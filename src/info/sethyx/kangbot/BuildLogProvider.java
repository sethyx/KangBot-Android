package info.sethyx.kangbot;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

public class BuildLogProvider extends ContentProvider {
    
    public static final String PROVIDER_NAME =
            "info.sethyx.kangbot.BuildLog";

    public static final Uri CONTENT_URI =
            Uri.parse("content://" + PROVIDER_NAME + "/buildlog");

    public static final String _ID = "_id";
    public static final String C_TIME = "time";
    public static final String C_DEVICE = "device";
    public static final String C_SRC = "src";
    public static final String C_RESULT = "result";
    public static final String C_ERRLOG = "errLog";

    private static final int LOGS = 1;
    private static final int LOG_ID = 2;

    private static final UriMatcher uriMatcher;
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(PROVIDER_NAME, "buildlog", LOGS);
        uriMatcher.addURI(PROVIDER_NAME, "buildlog/#", LOG_ID);
    }

    // ---for database use-- 
    private SQLiteDatabase logDB;
    private static final String DATABASE_NAME = "BuildLog";
    private static final String DATABASE_TABLE = "log";
    private static final int DATABASE_VERSION = 2;
    private static final String DATABASE_CREATE =
            "create table " + DATABASE_TABLE +
                    " (_id integer primary key autoincrement, "
                    + "time integer not null, device text not null, " +
                    "src text not null, result text not null, errlog text);";

    private static class DatabaseHelper extends SQLiteOpenHelper
    {
        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db)
        {
            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion,
                int newVersion) {
            /*
            Log.w("Content provider database",
                    "Upgrading database from version " +
                            oldVersion + " to " + newVersion +
                            ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS log");
            onCreate(db);
            ####
            if (oldVersion == 1) {
                db.execSQL("ALTER TABLE " + DATABASE_TABLE +
                " ADD " + C_ERRLOG + " text;");
                oldVersion++;
            }*/
        }
    }

    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)) {
            case LOGS: // ---get all vibrations---
                return "vnd.android.cursor.dir/vnd.kangbot.buildlog";
            case LOG_ID: // ---get a particular vibration---
                return "vnd.android.cursor.item/vnd.kangbot.buildlog";
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    @Override
    public boolean onCreate() {
        Context context = getContext();
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        logDB = dbHelper.getWritableDatabase();
        return (logDB == null) ? false : true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {

        SQLiteQueryBuilder sqlBuilder = new SQLiteQueryBuilder();
        sqlBuilder.setTables(DATABASE_TABLE);

        if (uriMatcher.match(uri) == LOG_ID) {
            // ---if getting a particular vibration---
            sqlBuilder.appendWhere(
                    _ID + " = " + uri.getLastPathSegment());
        }

        if (sortOrder == null || sortOrder == "")
            sortOrder = C_TIME + " DESC";

        Cursor c = sqlBuilder.query(
                logDB,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder);

        // ---register to watch a content URI for changes---
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // ---add a new vibration---
        long rowID = logDB.insert(
                DATABASE_TABLE, "", values);

        // ---if added successfully---
        if (rowID > 0)
        {
            Uri _uri = ContentUris.withAppendedId(CONTENT_URI, rowID);
            getContext().getContentResolver().notifyChange(_uri, null);
            return _uri;
        }
        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public int delete(Uri arg0, String arg1, String[] arg2) {
        // arg0 = uri
        // arg1 = selection
        // arg2 = selectionArgs
        int count = 0;
        switch (uriMatcher.match(arg0)) {
            case LOGS:
                count = logDB.delete(
                        DATABASE_TABLE,
                        arg1,
                        arg2);
                break;
            case LOG_ID:
                String id = arg0.getPathSegments().get(1);
                count = logDB.delete(
                        DATABASE_TABLE,
                        _ID + " = " + id +
                                (!TextUtils.isEmpty(arg1) ? " AND (" +
                                        arg1 + ')' : ""),
                        arg2);
                break;
            default:
                throw new IllegalArgumentException(
                        "Unknown URI " + arg0);
        }
        getContext().getContentResolver().notifyChange(arg0, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values,
            String selection, String[] selectionArgs)
    {
        int count = 0;
        switch (uriMatcher.match(uri)) {
            case LOGS:
                count = logDB.update(
                        DATABASE_TABLE,
                        values,
                        selection,
                        selectionArgs);
                break;
            case LOG_ID:
                count = logDB.update(
                        DATABASE_TABLE,
                        values,
                        _ID + " = " + uri.getPathSegments().get(1) +
                                (!TextUtils.isEmpty(selection) ? " AND (" +
                                        selection + ')' : ""),
                        selectionArgs);
                break;
            default:
                throw new IllegalArgumentException(
                        "Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

}
