package com.gf.secretsanta.model;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.gf.secretsanta.model.ParticipantContract.ParticipantEntry;

public class ParticipantDbHelper extends SQLiteOpenHelper {
    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = ",";

    private static final String TAG = "ParticipantDbHelper";

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + ParticipantEntry.TABLE_NAME + " (" +
                    ParticipantEntry._ID + " INTEGER PRIMARY KEY," +
                    ParticipantEntry.COLUMN_NAME_PARTICIPANT_ID + TEXT_TYPE + COMMA_SEP +
                    ParticipantEntry.COLUMN_NAME_PARTICIPANT_NAME + TEXT_TYPE + COMMA_SEP +
                    ParticipantEntry.COLUMN_NAME_PARTICIPANT_EMAIL_ADDRESS + TEXT_TYPE + COMMA_SEP +
                    ParticipantEntry.COLUMN_NAME_PARTICIPANT_EXCLUSION_LIST + TEXT_TYPE + COMMA_SEP +
                    "UNIQUE(" + ParticipantEntry.COLUMN_NAME_PARTICIPANT_NAME + COMMA_SEP + " " +
                    ParticipantEntry.COLUMN_NAME_PARTICIPANT_EMAIL_ADDRESS + ") ON CONFLICT IGNORE" + COMMA_SEP +
                    "UNIQUE(" + ParticipantEntry.COLUMN_NAME_PARTICIPANT_ID + ") " +
                    "ON CONFLICT IGNORE)";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + ParticipantEntry.TABLE_NAME;

    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "SecretSanta.db";

    public ParticipantDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i(TAG, "Creating Secret Santa database using version " + DATABASE_VERSION + " schema.");
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Logs that the database is being upgraded
        Log.i(TAG, "Upgrading Secret Santa database from version " + oldVersion + " to " + newVersion);

       /*
        * Every time you add new columns to the database, you will want
        * to increment the Database version above and then add a condition in here for
        * upgrading to it. Otherwise it will cause upgrading users to be nontrivial and
        * lead to unnecessary crashes or upgrade instructions.
        */
//        if (newVersion > oldVersion && newVersion <= DATABASE_VERSION) {
//        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Downgrading Secret Santa database from version " + oldVersion + " to " + newVersion);
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }
}
