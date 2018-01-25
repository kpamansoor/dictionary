package com.mansoor.manglima;

/**
 * Created by L4208412 on 13/11/2017.
 */
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;

public class DBHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "manglima.db";
    public static final String HISTORY_TABLE_NAME = "history";
    SQLiteDatabase db;

    public DBHelper(Context context) {
        super(context, DATABASE_NAME , null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // TODO Auto-generated method stub
        db.execSQL(
                "create table " + HISTORY_TABLE_NAME +"(id integer primary key, word text,meaning text)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub
        db.execSQL("DROP TABLE IF EXISTS "+ HISTORY_TABLE_NAME);
        onCreate(db);
    }

    public boolean insertHistory (String word,String meaning) {
        db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("word", word);
        contentValues.put("meaning", meaning);
        db.insert(HISTORY_TABLE_NAME, null, contentValues);
        return true;
    }

    public List<String> getData() {
        db = this.getReadableDatabase();
        List<String> resultList = new ArrayList<>();
        Cursor res =  db.rawQuery( "select * from "+ HISTORY_TABLE_NAME+" ORDER BY id DESC", null );
        while(res.moveToNext()) {
            resultList.add(res.getString(1));
        }
        return resultList;
    }
    public List<String> getMeaning() {
        db = this.getReadableDatabase();
        List<String> resultList = new ArrayList<>();
        Cursor res =  db.rawQuery( "select * from "+ HISTORY_TABLE_NAME+" ORDER BY id DESC", null );
        while(res.moveToNext()) {
            resultList.add(res.getString(2));
        }
        return resultList;
    }

    public void deleteWord(String word) {
        db = this.getReadableDatabase();
        db.execSQL( "delete from "+ HISTORY_TABLE_NAME+" WHERE word='"+word+"'");
    }

    public boolean deleteHistory () {
        db = this.getWritableDatabase();
        db.delete(HISTORY_TABLE_NAME, null,null);
        return true;
    }
}