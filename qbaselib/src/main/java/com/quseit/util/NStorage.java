package com.quseit.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class NStorage {

	static final String DB_NAME = "passinger_db";
	public static String getSP(Context context, String key)	{
		String val;
		SharedPreferences obj = context.getSharedPreferences(DB_NAME,0);
		val = obj.getString(key,"");
		return val;
	}
	public static void setSP(Context context, String key,String val) {
		SharedPreferences obj = context.getSharedPreferences(DB_NAME,0);
		Editor wobj; 
		wobj = obj.edit();
		wobj.putString(key, val);
		wobj.commit();
	}
	
	public static int getIntSP(Context context, String key)	{
		int val;
		SharedPreferences obj = context.getSharedPreferences(DB_NAME,0);
		val = obj.getInt(key, -1);
		return val;
	}
	public static void setIntSP(Context context, String key,int val) {
		SharedPreferences obj = context.getSharedPreferences(DB_NAME,0);
		Editor wobj; 
		wobj = obj.edit();
		wobj.putInt(key, val);
		wobj.commit();
	}
	
	public static long getLongSP(Context context, String key)	{
		long val;
		SharedPreferences obj = context.getSharedPreferences(DB_NAME,0);
		val = obj.getLong(key, -1);
		return val;
	}
	public static void setLongSP(Context context, String key,long val) {
		SharedPreferences obj = context.getSharedPreferences(DB_NAME,0);
		Editor wobj; 
		wobj = obj.edit();
		wobj.putLong(key, val);
		wobj.commit();
	}
}
