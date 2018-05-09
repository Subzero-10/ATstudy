package com.example.administrator.myapplication;


import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by YangG on 2018/5/9.
 */

public class ICdbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "ic.db";
    public static final int DB_VERSION = 1;
    public static final String CREATE_IC ="create table ic (_id integer primary key autoincrement, icid text);";


    public ICdbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(CREATE_IC);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}
