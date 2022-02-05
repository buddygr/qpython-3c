package com.quseit.base;

import android.app.Application;

import java.util.ArrayList;

public class MyApp extends Application {
    private static final String TAG = "MyApp";

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ArrayList<Object> taskQueue = new ArrayList();

    private static MyApp instance;


    public ArrayList<Object> getTaskQueue() {
        return taskQueue;
    }

    protected MyApp() {
    }
    public static MyApp getInstance() {
        if(null == instance) {
            instance = new MyApp();
        }
        return instance;

    }

}
