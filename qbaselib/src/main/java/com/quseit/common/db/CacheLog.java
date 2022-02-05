package com.quseit.common.db;

import com.quseit.config.BASE_CONF;
import com.quseit.util.DateTimeHelper;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

public class CacheLog {
    private Context context;
	private static final String TAG = "CacheLog";

    public CacheLog(Context context) {
    	this.context = context;
    }
    
    public String[] get(String key, int type) {
    	DBHelper dbHelper = new DBHelper(context);

    	String value = "";
    	String isExpired = "1";

    	long expired = 1;
    	SQLiteDatabase database = null;
    	Cursor cursor = null;
    	try {
	        database = dbHelper.getReadableDatabase();
	        String sql = "select value,expired from cache where key=? AND type=?";
	        cursor = database.rawQuery(sql, new String[] {  key, String.valueOf(type) });
	        if (cursor.moveToNext()) {
	        	value = cursor.getString(0);
	        	expired = cursor.getLong(1);
	
	        	if (expired!=0 && DateTimeHelper.getNowTime()>cursor.getLong(1)) {
	        		isExpired = "1";
	        	} else {
	        		isExpired = "0";
	        	}
	        	if (BASE_CONF.DEBUG)  Log.d(TAG, "found:"+key+"["+value+"]");
	
	        }
	        cursor.close();
	        cursor = null;
	        database.close();
	        database = null;
	      
    	} catch (OutOfMemoryError e) {
    	} catch (SQLiteException e) {
    	} catch (Exception e) {
    		
    	} finally {
    		if (cursor!=null) {
    			if (!cursor.isClosed()) {
    				cursor.close();
    			}
    		}
     		if (database!=null) {
    			if (database.isOpen()) {
    				database.close();
    			}
    		}
    		
    	}
        dbHelper.close();

    	if (BASE_CONF.DEBUG) Log.d(TAG, "get: key("+key+")type:"+type+")expired("+expired+")");

        String[] ret =  {value, isExpired};
        return ret;
    }
    
    public void set(String key, String val, int type, long expired) {
    	DBHelper dbHelper = new DBHelper(context);

    	long now = DateTimeHelper.getNowTime();
    	try {
	    	if (BASE_CONF.DEBUG) Log.d(TAG, "set: key("+key+")type"+type+")now("+now+")expired("+expired+")value("+val+")");
	
	        SQLiteDatabase database = dbHelper.getWritableDatabase();
	        database.delete("cache", "key=? AND type=?", new String[] { key, ""+type });
	        
	        String sql = "insert into cache(key,value,type,created,expired) values (?,?,?,?,?)";
	        Object[] bindArgs = { key, val, type, now,((expired==0)?0:(now+expired)) };
	        database.execSQL(sql, bindArgs);
	        database.close();
	        
	        dbHelper.close();
    	} catch (IllegalArgumentException e) {
	        dbHelper.close();

    	} catch (SQLiteException e) {
	        dbHelper.close();

    	}
    }
    
    public void cleanCache(){
    	DBHelper dbHelper = new DBHelper(context);

    	//long now = DateTimeHelper.getNowTime();
    	try {
	
	        SQLiteDatabase database = dbHelper.getWritableDatabase();
	        database.delete("cache", "expired!=0", new String[] {  });
	        
	        database.close();
	        
	        dbHelper.close();
	        
	        Log.d(TAG, "cleanCache success");
    	} catch (IllegalArgumentException e) {
	        dbHelper.close();

    	} catch (SQLiteException e) {
	        dbHelper.close();

    	}
    }
    
}
