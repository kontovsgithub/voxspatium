package org.voxspatium;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

//Obsługa bazy danych.
public class DataBaseProvider extends ContentProvider {

    static final Uri CONTENT_URI = Uri.parse("content://org.voxspatium/bazydanych");
    static final int DATABASE_VERSION = 1;
    static final String DATABASE_NAME = "BazaDanychAplikacji";
    static final String TABLE_NAME = "Komunikaty";
    static final String TABLE_UPDATED = "DB:"+TABLE_NAME+" updated rows ";
    static final String TABLE_ROWS_DELETED = "DB:"+TABLE_NAME+" deleted rows ";
    static final String TABLE_QUERIED = "DB:"+TABLE_NAME+" queried";
    static final String TABLE_QUERY_EXCEPTION="DB:exception during query"+System.getProperty("line.separator");
    static final String TABLE_INSERT_OK = "DB:"+TABLE_NAME+" insert successfull";
    static final String TABLE_INSERT_ERROR = "DB:"+TABLE_NAME+" insert error";
    static final String DB_OPEN_OK = "DB:"+DATABASE_NAME+" opened";
    static final String DB_OPEN_ERROR = "DB:"+DATABASE_NAME+" open error";
    static final String DB_CLOSE_OK = "DB:"+DATABASE_NAME+" closed";
    static final String DB_CLOSE_ERROR = "DB:"+DATABASE_NAME+" close error";
    static final String DB_UPDATE_PENDING = "DB:"+DATABASE_NAME+" open/close suspended, update pending";
    static final String DB_CREATED_AND_IMPORTED ="DB:created and imported generic database";
    static final String DB_OPEN_EXCEPTION="DB:exception during database openning"+System.getProperty("line.separator");
    static final String DB_ERROR_LOADING_GENERIC_DATABASE="DB:exception during loading generic database";
    static final String DB_UPDATED= "DB:update finished to version from file ";
    static final String DB_UPDATE_EXCEPTION= "DB:update exception"+System.getProperty("line.separator");

    static private SQLiteDatabase db;

    @Override
    public boolean onCreate() {

        File Plik_bazy_danych=getContext().getDatabasePath(DATABASE_NAME);
        if (!Plik_bazy_danych.exists()) {
            //wygenerowanie ID instalacji
            String id_instalacji ="v"+getContext().getString(R.string.app_version)+".id";
            int random_part_of_ID  = (int) Math.round(Math.random() * 10000000);
            id_instalacji = String.valueOf(random_part_of_ID)+id_instalacji;
            SimpleDateFormat dataiczas = new SimpleDateFormat("yyMMddHHmmss", Locale.US);
            String date_part_of_ID = dataiczas.format(new Date());
            id_instalacji = date_part_of_ID+"a"+id_instalacji;
            File plik = new File(getContext().getFilesDir(), id_instalacji);
            try {
                plik.createNewFile();
            } catch (IOException e) {
            }

            LoadGenericDataBase(getContext());
        }

        //otwarcie bazy
        if (OpenDB(getContext())) {
            return true;}
        else {
            return false;}
    }

    public static boolean OpenDB(Context context) {

        if (!MyApplication.isDatabaseUpdatePending) {
            DatabaseHelper dbHelper = new DatabaseHelper(context);
            // open database for read
            if (db == null || db.isOpen()==false) {
                try {
                    db = dbHelper.getReadableDatabase();
                }
                catch (Exception e) {
                    StringWriter errors = new StringWriter();
                    e.printStackTrace(new PrintWriter(errors));
                    MyApplication.writeLog(DB_OPEN_EXCEPTION+errors.toString(),context,1);
                }
            }
            if (db == null || db.isOpen()==false) {
                MyApplication.writeLog(DB_OPEN_ERROR, context,1);
                return false;
            } else {
                MyApplication.writeLog(DB_OPEN_OK, context,1);
                return true;
            }
        }
        else {
            MyApplication.writeLog(DB_UPDATE_PENDING, context,1);
            return false;
        }
    }

    /**
     * Helper class that actually creates and manages
     * the provider's underlying data repository.
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context){
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            //wykonywane tylko przy instalacji aplikacji, w kolejnych uruchomieniach jest pomijane
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }
    }

    public static void LoadGenericDataBase(Context context) {

        //usuniecie poprzedniej bazy jezeli istnieje
        context.deleteDatabase(DATABASE_NAME);

        //utworzenie generycznej bazy
        Resources mResources = context.getResources();
        int resourceId = context.getResources().getIdentifier("generycznabazadanych", "raw", context.getPackageName());
        try {
            InputStream iS = mResources.openRawResource(resourceId);
            //create a buffer that has the same size as the InputStream
            byte[] buffer = new byte[iS.available()];
            //read the text file as a stream, into the buffer
            iS.read(buffer);
            //create a output stream to write the buffer into
            ByteArrayOutputStream oS = new ByteArrayOutputStream();
            //write this buffer to the output stream
            oS.write(buffer);
            //Close the Input and Output streams
            oS.close();
            iS.close();
            //return the output stream as a String
            String KomendySQL = oS.toString();
            DatabaseUtils.createDbFromSqlStatements(context, DATABASE_NAME, DATABASE_VERSION, KomendySQL);
            MyApplication.writeLog(DB_CREATED_AND_IMPORTED, context, 1);
        } catch (Exception e) {
            MyApplication.writeLog(DB_ERROR_LOADING_GENERIC_DATABASE, context, 1);
        }
    }

    public static boolean UpdateDB(String NazwaPlikuBazy,Context context){

        // open database for write assuming that it is closed at the moment
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        try {
            db = dbHelper.getWritableDatabase();
            MyApplication.writeLog(DB_OPEN_OK+" to write for DB update", context,1);
        }
        catch (Exception e) {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            MyApplication.writeLog(DB_OPEN_EXCEPTION+" to write for DB update"+errors.toString(),context,1);
            db.close();
            if (db.isOpen()==false) {
                MyApplication.writeLog(DB_CLOSE_OK+" to write for DB update", context,1);
            } else {
                MyApplication.writeLog(DB_CLOSE_ERROR+" to write for DB update", context,1);
            }
            return false;
        }

        // database update
        try {
            FileInputStream inputStream = context.openFileInput(NazwaPlikuBazy);
            InputStreamReader inputReader = new InputStreamReader(inputStream);
            BufferedReader buffReader = new BufferedReader(inputReader);
            String SQLcommand;
            while ((SQLcommand = buffReader.readLine()) != null) {
                db.execSQL(SQLcommand);
            }
            inputStream.close();
            MyApplication.writeLog(DB_UPDATED+NazwaPlikuBazy, context,1);
            db.close();
            if (db.isOpen()==false) {
                MyApplication.writeLog(DB_CLOSE_OK+" to write for DB update", context,1);
            } else {
                MyApplication.writeLog(DB_CLOSE_ERROR+" to write for DB update", context,1);
            }
            return true;
        } catch (Exception e) {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            MyApplication.writeLog(DB_UPDATE_EXCEPTION+errors.toString(),context,1);

            db.close();
            if (db.isOpen()==false) {
                MyApplication.writeLog(DB_CLOSE_OK+" to write for DB update", context,1);
            } else {
                MyApplication.writeLog(DB_CLOSE_ERROR+" to write for DB update", context,1);
            }

            //Generic database recovery.
            //This operation causes discrepancy between database content and content of file in filesystem used to check if new version is avalible.
            //It doesn't matter if version number is always incremented
            LoadGenericDataBase(context);
            return false;
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // jeżeli będzie wykorzystywane to dodać obsługę flagi MyApplication.isDatabaseUpdatePending
        //Add a new student record
        long rowID = db.insert(TABLE_NAME,null, values);

        //If record is added successfully
        if (rowID > 0) {
            Uri _uri = ContentUris.withAppendedId(CONTENT_URI, rowID);
            getContext().getContentResolver().notifyChange(_uri, null);
            MyApplication.writeLog(TABLE_INSERT_OK, getContext(),5);
            return _uri;
        }
        else {
            MyApplication.writeLog(TABLE_INSERT_ERROR, getContext(),1);
            return CONTENT_URI;
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if (!MyApplication.isDatabaseUpdatePending) {
            try {
                Cursor c = db.query(TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                //register to watch a content URI for changes
                c.setNotificationUri(getContext().getContentResolver(), uri);
                MyApplication.writeLog(TABLE_QUERIED, getContext(),5);
                return c;
            } catch (Exception e) {
                StringWriter errors = new StringWriter();
                e.printStackTrace(new PrintWriter(errors));
                MyApplication.writeLog(TABLE_QUERY_EXCEPTION+errors.toString(),getContext(),1);
                return null;
            }
        }
        else {
            return null;
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // jeżeli będzie wykorzystywane to dodać obsługę flagi MyApplication.isDatabaseUpdatePending

        int count = 0;
        count = db.delete(TABLE_NAME, selection, selectionArgs);

        getContext().getContentResolver().notifyChange(uri, null);

        MyApplication.writeLog(TABLE_ROWS_DELETED+Integer.toString(count), getContext(),5);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // jeżeli będzie wykorzystywane to dodać obsługę flagi MyApplication.isDatabaseUpdatePending

        int count = 0;
        count = db.update(TABLE_NAME, values, selection, selectionArgs);

        getContext().getContentResolver().notifyChange(uri, null);

        MyApplication.writeLog(TABLE_UPDATED+Integer.toString(count), getContext(),5);
        return count;
    }

    @Override
    public String getType(Uri uri) {
        //This method returns the MIME type of the data at the given URI.
        return null;
    }

    public static synchronized boolean CloseDB (Context context) {
        if (!MyApplication.isDatabaseUpdatePending) {
            db.close();
            if (db.isOpen()==false) {
                MyApplication.writeLog(DB_CLOSE_OK, context,1);
                return true;
            } else {
                MyApplication.writeLog(DB_CLOSE_ERROR, context,1);
                return false;
            }
        }
        else {
            MyApplication.writeLog(DB_UPDATE_PENDING, context,1);
            return false;
        }
    }
}

