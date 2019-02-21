package com.cyclebikeapp.lookup.airplanes;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import static android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE;
import static com.cyclebikeapp.lookup.airplanes.Constants.AIRPLANE_DB;
import static com.cyclebikeapp.lookup.airplanes.Constants.AIRPLANE_TABLE;
import static com.cyclebikeapp.lookup.airplanes.Constants.DB_KEY_AIR_MANUFACTURER;
import static com.cyclebikeapp.lookup.airplanes.Constants.DB_KEY_AIR_MODEL;
import static com.cyclebikeapp.lookup.airplanes.Constants.DB_KEY_AIR_OWNER;
import static com.cyclebikeapp.lookup.airplanes.Constants.DB_KEY_AIR_REGISTRATION;
import static com.cyclebikeapp.lookup.airplanes.Constants.DB_KEY_AIR_SPECIES;
import static com.cyclebikeapp.lookup.airplanes.Constants.DB_KEY_ICAO24;
import static com.cyclebikeapp.lookup.airplanes.Constants.PREFS_NAME;
import static com.cyclebikeapp.lookup.airplanes.Constants.PREF_KEY_AIRFILES_READ_SUCCESS;
import static com.cyclebikeapp.lookup.airplanes.Constants.PREF_KEY_AIRFILE_DOC_READ;
import static com.cyclebikeapp.lookup.airplanes.Constants.PREF_KEY_READING_AIRFILES;


class AirplaneDBAdapter {

    private static final String TAB = "\t";
    private final Context mContext;
    private DBHelper mDBHelper;
    private SQLiteDatabase airplaneDB;
    private final Runnable readAirplaneFileFromAssetsRunnable = new Runnable() {
        /**
         * Only called internally from readAirplaneFileFromAssetAsync()
         * Read airplane file from assets and update the airplane database.
         * pass the application Context so we can access the assets folder.
         */
        @Override
        public void run() {
            long startTime = System.currentTimeMillis();
            SharedPreferences settings = mContext.getSharedPreferences(PREFS_NAME, 0);
            SharedPreferences.Editor editor = settings.edit();
            //GridSatView will read this and display a Loading airplanes message
            editor.putBoolean(PREF_KEY_READING_AIRFILES, true).apply();
            // assume we will succeed
            editor.putBoolean(PREF_KEY_AIRFILES_READ_SUCCESS, true).apply();
            try {
                // read-in any .csv file in assets folder
                String[] fileList = mContext.getAssets().list("airFiles");
                // test for .csv extension, open files in for loop
                for (String airFileName : fileList) {
                    if (MainActivity.DEBUG) {
                        Log.w(this.getClass().getName(), "adding file " + airFileName + " to dataBase");
                    }
                    if (airFileName.endsWith(".csv") && !readAirFileCSVAndUpdateDB(mContext, "airFiles/" + airFileName)) {
                        if (MainActivity.DEBUG) {
                            Log.w(this.getClass().getName(), String.format("readAirFile failed: %s", airFileName));
                        }
                        // If any airFile isn't read, set a SharedPref flag that we can easily test in LookAfterStuff
                        // to repeat this Task.
                        // A file may not be read if the user navigates away while files are being read
                        editor.putBoolean(PREF_KEY_AIRFILES_READ_SUCCESS, false).apply();
                    }
                }
            } catch (IOException e) {
                editor.putBoolean(PREF_KEY_AIRFILES_READ_SUCCESS, false).apply();
                if (MainActivity.DEBUG) { e.printStackTrace(); }
            } finally {
                // indicate that we've finished reading files
                settings.edit().putBoolean(PREF_KEY_READING_AIRFILES, false).apply();
            }
            if (MainActivity.DEBUG) {
                Log.w(this.getClass().getName(), "reading air files Runnable took "
                        + String.format("%3.1f", (System.currentTimeMillis() - startTime) / 1000.) + " sec");
            }
        }
    };

    AirplaneDBAdapter(Context context) {
        this.mContext = context;
    }

    /**
     * This is the primary call from onCreate() in MainActivity after openning the database. From here the
     * asset satellite data files are loaded, database updated and TLEs refreshed, all in Background Tasks.
     */
    void readAirplaneFileFromAssetAsync() {
        new ThreadPerTaskExecutor().execute(readAirplaneFileFromAssetsRunnable);
    }

    /**
     * This method is called to handle updating the airplane database by loading airplane file from assets,
     * and calls addSatToDBFromFile with the content. It is called from the AsyncTask readAirplaneFileFromAssetAsync
     * so this process is handled in a background task.
     *
     * @param mContext    needed to access file from assets
     * @param airFileName the name of the file containing airplane data in a csv format;
     * @return true if the data base was updated
     */
    private boolean readAirFileCSVAndUpdateDB(Context mContext, String airFileName) {
        ContentValues airContent;
        SharedPreferences settings = mContext.getSharedPreferences(PREFS_NAME, 0);
        boolean alreadyReadDocumentNum = settings.getBoolean(PREF_KEY_AIRFILE_DOC_READ + airFileName, false);
        if (alreadyReadDocumentNum){
            return true;
        }
        InputStream is = null;
        boolean success = true;
        try {
            is = mContext.getAssets().open(airFileName);
        } catch (IOException e) {
            if (MainActivity.DEBUG) { e.printStackTrace(); }
        }
        BufferedReader reader = null;
        if (is != null) {
            reader = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")), 65536);
        }
        String line;
        StringTokenizer st;
        int lineNum = 0;
        int species;
        ArrayList<ContentValues> airContentList = new ArrayList<>();
        try {
            if (reader != null) {
                while ((line = reader.readLine()) != null) {
                    // tab-delimited text file
                    lineNum++;
                    st = new StringTokenizer(line, TAB);
                    airContent = new ContentValues();
                    airContent.put(DB_KEY_ICAO24, Integer.parseInt(st.nextToken()));
                    airContent.put(DB_KEY_AIR_REGISTRATION, st.nextToken());
                    try {
                        airContent.put(DB_KEY_AIR_MANUFACTURER, st.nextToken());
                        airContent.put(DB_KEY_AIR_MODEL, st.nextToken());
                        airContent.put(DB_KEY_AIR_OWNER, st.nextToken());
                        String speciesString = st.nextToken();
                        try {
                            species = Integer.parseInt(speciesString);
                        } catch (NumberFormatException e){
                            species = 1;
                        }
                        airContent.put(DB_KEY_AIR_SPECIES, species);
                    } catch (NoSuchElementException ignore) {
                    }
                    airContentList.add(airContent);
                }
                success = addAircraftToDBFromFile(airContentList);
                // when successfully added data, store the document name so we won't read it again
                if (success) {
                    settings.edit().putBoolean(PREF_KEY_AIRFILE_DOC_READ + airFileName, true).apply();
                }
            }
        } catch (IOException e) {
            if (MainActivity.DEBUG) { e.printStackTrace(); }
        } finally {
            if (MainActivity.DEBUG) {Log.i(this.getClass().getName(), String.format("read: %d lines from file", lineNum));}
        }
        return success;
    }

    /**
     *
     * @param content  an Object containing the new information
     */
    @SuppressLint("DefaultLocale")
    private void updateAirplaneRecord(ContentValues content) {
        if(!isClosed()) {
            airplaneDB.beginTransaction();
            try {
                String[] whereArgs = {content.getAsString(DB_KEY_ICAO24)};
                airplaneDB.update(AIRPLANE_TABLE, content, DB_KEY_ICAO24 + "=?", whereArgs);
                airplaneDB.setTransactionSuccessful();
            } catch (Exception e1) {
                if (MainActivity.DEBUG) { e1.printStackTrace(); }
            } finally {
                airplaneDB.endTransaction();
            }
        }
    }

    /**
     * Read the data pertaining to a particular icao24. Icao24 is a hex string, but the database is indexed with the Integer value.
     * Have to parse to an Integer before doing query
     * Include the registration, manufacturer, type, owner, engines to add content to popup window
     *
     * @param icao24 the String Hex number of the airplane to read
     * @return a String[] containing the data
     */
    HashMap<String, String> fetchAirplaneData(String icao24) {

        HashMap<String, String> returnHashmap = new HashMap<>();
        String[] columns = {DB_KEY_AIR_REGISTRATION, DB_KEY_AIR_MANUFACTURER,
                DB_KEY_AIR_MODEL, DB_KEY_AIR_OWNER, DB_KEY_AIR_SPECIES};
        String filter = DB_KEY_ICAO24 + "= '" + Integer.parseInt(icao24, 16) + "'";
        Cursor mCursor = null;
        if (!isClosed()) {
            airplaneDB.beginTransaction();
            try {
                mCursor = airplaneDB.query(AIRPLANE_TABLE, columns, filter, null, null, null, null);
                if (mCursor == null) {
                    Log.wtf(this.getClass().getName(), String.format("fetchAirplaneData cursor null for hex icao24: %s", icao24));
                }
                if (mCursor != null && mCursor.moveToFirst()) {
                    do {
                        String reg = mCursor.getString(mCursor.getColumnIndexOrThrow(DB_KEY_AIR_REGISTRATION));
                        String man = mCursor.getString(mCursor.getColumnIndexOrThrow(DB_KEY_AIR_MANUFACTURER));
                        String mod = mCursor.getString(mCursor.getColumnIndexOrThrow(DB_KEY_AIR_MODEL));
                        String own = mCursor.getString(mCursor.getColumnIndexOrThrow(DB_KEY_AIR_OWNER));
                        String species = mCursor.getString(mCursor.getColumnIndexOrThrow(DB_KEY_AIR_SPECIES));
                        returnHashmap.put(DB_KEY_AIR_REGISTRATION, reg);
                        returnHashmap.put(DB_KEY_AIR_MANUFACTURER, man);
                        returnHashmap.put(DB_KEY_AIR_MODEL, mod);
                        returnHashmap.put(DB_KEY_AIR_OWNER, own);
                        returnHashmap.put(DB_KEY_AIR_SPECIES, species);
                    } while (mCursor.moveToNext());
                }
                airplaneDB.setTransactionSuccessful();
            } catch (IllegalArgumentException e) {
                if (MainActivity.DEBUG) { e.printStackTrace(); }
            } finally {
                if (mCursor != null) { mCursor.close(); }
                airplaneDB.endTransaction();
            }
        }
        return returnHashmap;
    }

    /**
     * @param query the search text of a Mode S address (icao24)= hex number
     * @return a Cursor containing the exact match; could be null
     */
    Cursor searchDataBaseByICAO24Number(String query) {
        // database stores icao24 number as decimal, do conversion before searching
        String selection = DB_KEY_ICAO24 + " = ? ";
        String[] columns = {DB_KEY_ICAO24, DB_KEY_AIR_REGISTRATION};
        String decimalQuery = String.valueOf(Integer.parseInt(query.trim(), 16));
        String[] selectionArgs = new String[]{decimalQuery};
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(AIRPLANE_TABLE);
        Cursor cursor = null;
        if (!isClosed()) {
            airplaneDB.beginTransaction();
            cursor = builder.query(airplaneDB, columns, selection, selectionArgs, null, null, null);
            airplaneDB.setTransactionSuccessful();
            airplaneDB.endTransaction();
            if (cursor != null && !cursor.moveToFirst()) {
                cursor.close();
                cursor = null;
            }
        }
        return cursor;
    }

    Cursor searchDataBaseByOwner(String query) {
        String selection = DB_KEY_AIR_OWNER + " LIKE ?";
        String[] columns = {DB_KEY_ICAO24, DB_KEY_AIR_OWNER, DB_KEY_AIR_REGISTRATION};
        String[] selectionArgs = new String[]{query};
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(AIRPLANE_TABLE);
        Cursor cursor = null;
        if (!isClosed()) {
            airplaneDB.beginTransaction();
            cursor = builder.query(airplaneDB, columns, selection, selectionArgs, null, null, null);
            airplaneDB.setTransactionSuccessful();
            airplaneDB.endTransaction();
            if (cursor != null && !cursor.moveToFirst()) {
                cursor.close();
                cursor = null;
            }
        }
        return cursor;
    }

    /**
     * @param query the search text of a registration String
     * @return a Cursor containing the exact match; could be null
     */
    Cursor searchDataBaseByRegistration(String query) {
        String selection = DB_KEY_AIR_REGISTRATION + " LIKE ?";
        String[] columns = {DB_KEY_ICAO24, DB_KEY_AIR_REGISTRATION};
        String[] selectionArgs = new String[]{query.toUpperCase()};
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(AIRPLANE_TABLE);
        Cursor cursor = null;
        if (!isClosed()) {
            airplaneDB.beginTransaction();
            cursor = builder.query(airplaneDB, columns, selection, selectionArgs, null, null, null);
            airplaneDB.setTransactionSuccessful();
            airplaneDB.endTransaction();
            if (cursor != null && !cursor.moveToFirst()) {
                cursor.close();
                cursor = null;
            }
        }
        return cursor;
    }

    /**
     * Determines if a airplane is stored in the data base
     *
     * @param icao24 the airplane in question
     * @return true if that airplane is in the data base
     */
    private boolean isAirplaneInDataBase(int icao24) {
        boolean found = false;
        if (!isClosed()) {
            String filter = DB_KEY_ICAO24 + "= '" + Integer.toString(icao24) + "'";
            String[] columns = {DB_KEY_ICAO24};
            Cursor mCursor = null;
            try {
                mCursor = airplaneDB.query(AIRPLANE_TABLE, columns, filter, null, null, null, null);
                if (mCursor != null && mCursor.moveToFirst()) {
                    int devNum;
                    do {
                        devNum = mCursor.getInt(mCursor.getColumnIndexOrThrow(DB_KEY_ICAO24));
                        found = (devNum == icao24);
                    } while (mCursor.moveToNext());
                }
            } catch (Exception e) {
                if (MainActivity.DEBUG) {
                    e.printStackTrace();
                }
            } finally {
                if (mCursor != null) { mCursor.close(); }
            }
        }
        return found;
    }

    /**
     * When loading file of airplanes add airplane to the data base if its not already there. If
     * airplane is already in the database, this must be new information because the document number is different,
     * so replace the information.
     *
     * @param contentList has the airplane data
     */
    private boolean addAircraftToDBFromFile(ArrayList<ContentValues> contentList) {
        boolean returnSuccess = !isClosed();
        // the fastest way to add data is under a "transaction"
        if (!isClosed()) {
            try {
                airplaneDB.beginTransaction();
                for (ContentValues content : contentList) {
                    if (airplaneDB.insertWithOnConflict(AIRPLANE_TABLE,
                            "", content, CONFLICT_IGNORE) == -1) {
                        String[] whereArgs = {String.valueOf(content.getAsString(DB_KEY_ICAO24))};
                        airplaneDB.update(AIRPLANE_TABLE, content, DB_KEY_ICAO24 + "=?", whereArgs);
                    }
                }
                airplaneDB.setTransactionSuccessful();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            } finally {
                airplaneDB.endTransaction();
                if (MainActivity.DEBUG) {
                    long count = DatabaseUtils.queryNumEntries(airplaneDB, AIRPLANE_TABLE);
                    Log.w(this.getClass().getName(), String.format("added %d rows to DB ", count));
                }
            }
        }
        return returnSuccess;
    }

    /**
     * This call creates a new DBHelper, creates the airplaneDB if it doesn't exist and opens a writeable database instance.
     * Should be called in MainActivity.onCreate() before trying to use the database.
     *
     * @return a AirplaneDBAdapter
     * @throws SQLiteException a bad thing
     */
    public AirplaneDBAdapter open() throws SQLiteException {
        mDBHelper = new DBHelper(mContext);
        airplaneDB = mDBHelper.getWritableDatabase();
        return this;
    }

    /**
     * This call creates a new DBHelper, creates the airplaneDB if it doesn't exist and opens a writeable database instance.
     * Should be called in MainActivity.onCreate() before trying to use the database.
     *
     * @return a AirplaneDBAdapter
     * @throws SQLiteException a bad thing
     */
    public AirplaneDBAdapter openRead() throws SQLiteException {
        mDBHelper = new DBHelper(mContext);
        airplaneDB = mDBHelper.getReadableDatabase();
        return this;
    }

    /**
     * close the airplaneTable database
     */
    void close() {
        try {
            if (mDBHelper != null) {
                mDBHelper.close();
            }
        } catch (IllegalStateException e) {
            if (MainActivity.DEBUG) {
                e.printStackTrace();
            }
        }
    }

    boolean isClosed() {
        return airplaneDB == null || !airplaneDB.isOpen();
    }

    private class DBHelper extends SQLiteOpenHelper {
        static final String TEXT_NOT_NULL = " TEXT NOT NULL";
        static final String INTEGER_UNIQUE = " INTEGER UNIQUE";
        private static final int DB_VERSION = 1;

        DBHelper(Context context) {
            super(context, AIRPLANE_DB, null, DB_VERSION);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            String dropString = "DROP TABLE IF EXISTS airplaneTable;";
            db.execSQL(dropString);
            onCreate(db);
        }

        @Override
        public void onCreate(SQLiteDatabase db) throws SQLException {
            String createString = "CREATE TABLE IF NOT EXISTS airplaneTable "
                    + "( _id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + DB_KEY_ICAO24 + INTEGER_UNIQUE + ", "
                    + DB_KEY_AIR_REGISTRATION + TEXT_NOT_NULL + ", "
                    + DB_KEY_AIR_MANUFACTURER + " TEXT,"
                    + DB_KEY_AIR_MODEL + " TEXT,"
                    + DB_KEY_AIR_OWNER + " TEXT,"
                    + DB_KEY_AIR_SPECIES + " INTEGER" + ");";
            db.execSQL(createString);
        }
    }

}
