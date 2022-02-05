package com.quseit.util;

import java.util.List;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class RandomTools {

	private static Random random=new Random();
	private static int SIZE;

	public static List<String> setRandomList(List<String> strs){
		SIZE=strs.size();
		changePositions(strs);
		return strs;
	}
	
	public static JSONArray setRandomArray(JSONArray objs){
		SIZE=objs.length();
		changePositions(objs);
		return objs;
	}
	
	public static void changePositions(JSONArray objs){
		for(int i=SIZE-1;i>0;i--){
			exchange(objs,random.nextInt(i+1),i);
		}
	}

	
	public static void changePositions(List<String> strs){
		for(int i=SIZE-1;i>0;i--){
			exchange(strs,random.nextInt(i+1),i);
		}
	}

	private static void exchange(JSONArray objs,int p1,int p2){
		JSONObject temp;
		try {
			temp = (JSONObject)objs.get(p1);
			JSONObject op2 = (JSONObject)objs.get(p2);
			objs.put(p1, op2);
			objs.put(p2, temp);
		}
		catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	
	private static void exchange(List<String> strs,int p1,int p2){
		String temp=strs.get(p1);
		strs.set(p1, strs.get(p2));
		strs.set(p2, temp);
	}
	

	
}
