package com.quseit.common.db;
 
 import android.content.Context;
 import android.database.sqlite.SQLiteDatabase;
 import android.database.sqlite.SQLiteOpenHelper;
 
     /**
      * 建立一个数据库帮助类
      */
 public class DBHelper extends SQLiteOpenHelper {
	 private static DBHelper sInstance;
	 
	 public static synchronized DBHelper getInstance(Context context) {
		    
		    if (sInstance == null) {
		      sInstance = new DBHelper(context.getApplicationContext());
		    }
		    return sInstance;
	}
	 
	 
     //download.db-->数据库名
     public DBHelper(Context context) {
         super(context, "download2.db", null, 1);
     }
     
     /**
      * 在download.db数据库下创建一个download_info表存储下载信息
      */
     @Override
     public void onCreate(SQLiteDatabase db) {
         db.execSQL("create table download_info(_id integer PRIMARY KEY AUTOINCREMENT, thread_id integer, "
                 + "start_pos integer, end_pos integer, compelete_size integer,url char, path char,  orglink char, quality integer,stat integer,title char,artist char,album char,service_stat integer,service_json char)");
         db.execSQL("create table cache (key char, value text, type INTEGER, created INTEGER, expired INTEGER)");
     }
     
     @Override
     public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
 
     }
 
 }
