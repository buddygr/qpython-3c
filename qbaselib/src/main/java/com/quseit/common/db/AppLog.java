package com.quseit.common.db;

import java.util.ArrayList;

import com.quseit.config.BASE_CONF;
import com.quseit.util.VeDate;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
 
public class AppLog {
	private static final String TAG = "appLog";

    public static final String KEY_ROWID = "_id";
    public static final String KEY_TITLE = "m_title";  
    public static final String KEY_UID 	= "m_uid";  

    public static final String KEY_VER = "m_ver";    
    public static final String KEY_TIME = "m_time";

    public static final String KEY_DESC = "m_desc";
    public static final String KEY_STAT = "stat";

    private static final String DATABASE_NAME = "applog.db";
    private static final String DATABASE_TABLE = "applog";
    private static final int DATABASE_VERSION = 1;
    
    // 包括创建titles表的SQL语句
    private static final String DATABASE_CREATE1 = "create table applog (_id integer primary key autoincrement, "
    		+ "m_title char(64),"
    		+ "m_ver int(8), "
    		+ "m_uid char(32),"
            + "m_desc text,"
    		+ "m_time date,"
            + "stat int(1));";
    
    private final Context context;
    //private DatabaseHelper DBHelper;
 
    public AppLog(Context ctx) {
        this.context = ctx;
        
        
    	//Log.d(TAG, "db init");
    }
 
    // 扩展SQLiteOpenHelper类，用于数据库创建和版本管理。可以覆盖onCreate和onUpgrade犯法
    private static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }
 
        // 创建一个新的数据库
        @Override
        public void onCreate(SQLiteDatabase db) {
        	if (BASE_CONF.DEBUG) Log.d(TAG, "db create"+DATABASE_CREATE1);
            db.execSQL(DATABASE_CREATE1);
            //db.execSQL(DATABASE_CREATE2);

        }
 
        // 用于升级数据库，可以通过检查DATABASE_VERSION常量定义的值来实现，对于onUpgrader()
        // 方法而言，只不过是简单地删除表，然后再创建表
        // 定义不同的方法来打开和关闭数据库，添加/编辑/删除/行的函数
        // 定义打开和关闭数据库以及增加/编辑/删除表中行的方法
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS enc");
            onCreate(db);
        }
    }
 
    public boolean ifLogExists( String ver, String data) {
    	DatabaseHelper DBHelper = new DatabaseHelper(context);
    	
    	if (BASE_CONF.DEBUG) Log.d(TAG, "ifLogExists");
    	try {
	    	SQLiteDatabase database = DBHelper.getReadableDatabase();
	
	        String sql = "select "+KEY_ROWID+", "+KEY_VER+", "+KEY_DESC+" from "+DATABASE_TABLE+" where "+KEY_VER+"=? and "+KEY_DESC+"=?";
	        Cursor cursor = database.rawQuery(sql, new String[] {  ver, data });
	        if (cursor.moveToNext()) {
	        	
	            cursor.close();
	            database.close();
	            Log.d(TAG, "ifLogExists-yes");
	            DBHelper.close();
	
	        	return true;
	        } else {
	            Log.d(TAG, "ifLogExists-no");

	            database.close();
	
	        	DBHelper.close();
	
	        	return false;
	        }
	   	 } catch (Exception e) {
	   		DBHelper.close();
	
		}
    	return false;
    }

    public boolean deleteLog(long rowId) {
    	DatabaseHelper DBHelper = new DatabaseHelper(context);

    	if (BASE_CONF.DEBUG)  Log.d(TAG, "deleLog:"+rowId);
    	try {

	    	SQLiteDatabase db = DBHelper.getWritableDatabase();
	        //ContentValues args = new ContentValues();
	
	        boolean ret = (db.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0);
	        db.close();
	        
	        DBHelper.close();
	        return ret;
    	} catch (SQLiteException e) {
    		
    		Log.e(TAG, "deleteLog exception:"+e.getMessage());
    		
            DBHelper.close();
    		return false;
    	}
    }
    
    // 插入信息
    public long insertNewLog(String title, String ver,String uid, String desc) {
    	DatabaseHelper DBHelper = new DatabaseHelper(context);

    	if (BASE_CONF.DEBUG)  Log.d(TAG, "insertNewLog:"+title+"-type:"+ver);
    	try {
	    	SQLiteDatabase db = DBHelper.getWritableDatabase();
	
	        ContentValues initialValues = new ContentValues();
	        initialValues.put(KEY_TITLE, title);
	
	        initialValues.put(KEY_VER, ver);
	        initialValues.put(KEY_UID, uid);

	        initialValues.put(KEY_DESC, desc);
	        
	        String now = VeDate.getStringDate();
	        initialValues.put(KEY_TIME, now);
	        initialValues.put(KEY_STAT, "0");
	        
	        long id =  db.insert(DATABASE_TABLE, null, initialValues);    	
	        db.close();
	        
            DBHelper.close();

	        return id;
    	} catch (SQLiteException e) {
    		Log.e(TAG, "insertNewLog exception:"+e.getMessage());
    		
            DBHelper.close();

    		return 0;
    	}
    }
    
    // 获取所有的信息
    @SuppressWarnings({ "rawtypes", "unchecked" })
	public ArrayList<String[]> getLogs(int istat) {
    	DatabaseHelper DBHelper = new DatabaseHelper(context);

		ArrayList<String[]> feeds = new ArrayList();
    	try {

	    	SQLiteDatabase db = DBHelper.getReadableDatabase();
	    	Cursor result;
		    result = db.query(true, DATABASE_TABLE, new String[] { KEY_ROWID, KEY_TITLE, KEY_VER,  KEY_TIME,
		    			KEY_STAT,KEY_DESC,KEY_UID }, KEY_STAT + "=" + istat, null, null, null, KEY_ROWID+" ASC", null);   
	    	
	    	
			if (result.getCount()!=0) {
				result.moveToFirst();
				do {
					String id = result.getString(0);
					String title = result.getString(1);
					String type = result.getString(2);
	
					String time = result.getString(3);
					String stat = result.getString(4);
		
					String desc = result.getString(5);
					String uid = result.getString(6);

					String[] row = {id, title, type, time, stat, desc,uid };
					feeds.add(row);
	
	
	
				} while (result.moveToNext());
				
			}
			
			result.close();
			db.close();
			
            DBHelper.close();

			return feeds;
    	} catch (SQLiteException e) {
    		Log.e(TAG, "getLogs exception:"+e.getMessage());
    		
            DBHelper.close();

    		return feeds;
    	}
    }
}
