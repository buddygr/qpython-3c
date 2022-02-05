package com.quseit.config;

public interface BASE_CONF {

    // 插件版本
    int plugin_ver=0;
    int[] THREA_STAT = {0,0,0};	//FIXME DOWNLOAD STATUS

    boolean DEBUG = false;
    String UPDATER_URL="https://dl.qpy.io/update.json";
    int TRY_COUNT = 3;
    long TRY_DELAY = 1000;
	int ROUND_PIX = 0;	// IMAGE ROUND PIX
	int DOWNLOAD_NOTIFY_INDEX = 10001;

	String COLLECT_INFO = "#fingerprint#model#brand#";
	int UPDATEQ = 6;
	int UPDATE_VER = 2;	// update backend

	String DCACHE = "cache";
	String LOG_LIMIT = "30";
    String EXTRA_CONTENT_URL0 = "com.quseit.common.extra.CONTENT_URL0";
    String EXTRA_CONTENT_URL1 = "com.quseit.common.extra.CONTENT_URL1";
    String EXTRA_CONTENT_URL2 = "com.quseit.common.extra.CONTENT_URL2";
    String EXTRA_CONTENT_URL3 = "com.quseit.common.extra.CONTENT_URL3";
    String EXTRA_CONTENT_URL4 = "com.quseit.common.extra.CONTENT_URL4";
    String EXTRA_CONTENT_URL5 = "com.quseit.common.extra.CONTENT_URL5";
    String EXTRA_CONTENT_URL6 = "com.quseit.common.extra.CONTENT_URL6";
    String EXTRA_CONTENT_URL7 = "com.quseit.common.extra.CONTENT_URL7";
    String EXTRA_CONTENT_URL8 = "com.quseit.common.extra.CONTENT_URL8";
    String EXTRA_CONTENT_URL9 = "com.quseit.common.extra.CONTENT_URL9";
    String EXTRA_CONTENT_URL10 = "com.quseit.common.extra.CONTENT_URL10";
    String EXTRA_CONTENT_URL11 = "com.quseit.common.extra.CONTENT_URL11";
	int PAGE_NUM = 10;
    String DEFAULT_ROOT = "qpython";
    //版本更新
    String VERSION_KEY="ver";
    String DOWNLOAD_LINK_KEY="link";
    String VERIOSN_DESC_KEY="ver_desc";
    String APP_KEY="app";
    String VERSION_NAME_KEY="ver_name";
    String VERSION_PLUGIN_KEY="ver_plugin";
    String PLUGIN_KEY="plugins";

     //speed
     double TIME_SPAN = 2000d;
     long CHANGE_DELAY = 300;
     int SMALL_WINDOW_TYPE = 0;
     int BIG_WINDOW_TYPE = 1;
     String SP_BOOT = "SP_BOOT";
     String SP_BG = "SP_BG";
     String SP_LOC = "SP_LOC";
     String SP_X = "SP_X";
     String SP_Y = "SP_Y";
     String SP_STATUSBAR_HEIGHT = "SP_STATUSBAR_HEIGHT";
}
