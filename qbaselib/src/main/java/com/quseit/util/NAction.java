package com.quseit.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ConfigurationInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import com.quseit.android.R;
import com.quseit.config.BASE_CONF;
import com.quseit.common.db.UserLog;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.Properties;
import java.util.UUID;


public class NAction {

	private static final String TAG = "NAction";
	// check rooted
	private final static int kSystemRootStateUnknow=-1;
	private final static int kSystemRootStateDisable=0;
	private final static int kSystemRootStateEnable=1;
	private static int systemRootState=kSystemRootStateUnknow;

	public static Notification getNotification(Context context, String contentTitle, String contentText, PendingIntent intent,
										int smallIconId, Bitmap largeIconId, int flags) {
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
			Notification notification = new Notification.Builder(context) //new Notification(icon, tickerText, when);
					.setTicker(contentTitle)
					.setContentTitle(contentTitle)
                    .setContentText(contentText)
                    .setSmallIcon(smallIconId)
					.setLargeIcon(largeIconId)
					.setAutoCancel(true)
					.setContentIntent(intent)
                    .build();

			return notification;
		} else if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
			Notification notification = new Notification.Builder(context) //new Notification(icon, tickerText, when);
					.setTicker(contentTitle)
					.setContentTitle(contentTitle)
					.setContentText(contentText)
					.setSmallIcon(smallIconId)
					.setSmallIcon(smallIconId)
					.setLargeIcon(largeIconId)
					.setAutoCancel(true)
					.setContentIntent(intent)
					.getNotification();
			return notification;
		} else {
			Notification notification = new Notification(smallIconId, contentTitle, System.currentTimeMillis());
			notification.tickerText = contentTitle;
			notification.contentIntent = intent;
			notification.flags |= flags;
			return null;
		}
	}

	@SuppressLint("NewApi")
	public static boolean isOpenGL2supported(Context context) {

		final ActivityManager activityManager =
			    (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
			final ConfigurationInfo configurationInfo =
			    activityManager.getDeviceConfigurationInfo();
			final boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000;
			return supportsEs2;
	}

	public static void setInstallLink(Context context, String link) {
		NStorage.setSP(context, "config.installlink", link);
	}

	public static String getDefaultRoot(Context context) {
		return NStorage.getSP(context, "config.defaultroot");
	}

	public static void sendEmail(Context context, String mailto, String title, String body) {
	    Intent intent = new Intent(android.content.Intent.ACTION_SEND);
        intent.setType("plain/text");
        String[] strEmailReciver = new String[]{mailto};
		intent.putExtra(android.content.Intent.EXTRA_EMAIL, strEmailReciver); //设置收件人
        intent.putExtra(android.content.Intent.EXTRA_SUBJECT, title); //设置主题

        intent.putExtra(android.content.Intent.EXTRA_TEXT, body); //设置内容
        context.startActivity(Intent.createChooser(intent, context.getResources().getString(R.string.send_email)));
	}

	public static void setExtConf(Context context, String conf) {
		NStorage.setSP(context, "config.ext", conf);
	}

	public static String getExtConf(Context context) {
		return NStorage.getSP(context, "config.ext");
	}

	public static void setExtPluginsConf(Context context, String conf) {
		NStorage.setSP(context, "config.ext_plugins", conf);
	}

	public static String getExtPluginsConf(Context context) {
		return NStorage.getSP(context, "config.ext_plugins");
	}

	public static void setExtAdConf(Context context, String conf) {
		NStorage.setSP(context, "config.ext_ad", conf);
	}

	public static String getExtP(Context context, String key) {
		String conf = NAction.getExtConf(context);
		if (conf.equals("")) {
			return  "";
		} else {
			try {
				JSONObject a = new JSONObject(conf);
				return a.getString(key);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				if (BASE_CONF.DEBUG) Log.d(TAG, "getExtP:"+key+"-not found");
				//e.printStackTrace();
				return "";
			}
		}
	}

	public static void setUpdateHost(Context context, String host) {
		NStorage.setSP(context, "service.updatehost", host);
	}

	public static String getUpdateHost(Context context) {
		String h = NStorage.getSP(context, "service.updatehost");

		return h;
	}
	

	public static long getRemoteFileSize(Context context, String downloadUrl, long startPos) {
		NAction.userProxy(context);

	     try {
	 		URL url = new URL(downloadUrl);
			HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
		    httpConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; U; Android 4.0.1; en-us; Nexus One Build/FRF91) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1");
		    //httpConnection.setRequestProperty("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
		    //httpConnection.setRequestProperty("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		     httpConnection.setRequestProperty("RANGE", "bytes=" + startPos + "-");

		     httpConnection.setConnectTimeout(30000);
		     httpConnection.setReadTimeout(30000);
		     httpConnection.connect();
		     long fileTotalSize = httpConnection.getContentLength();

		     if (httpConnection.getResponseCode() >= 400) {
			     httpConnection.disconnect();

				 return -1;
			 }
		     httpConnection.disconnect();

		     return fileTotalSize;

		} catch (IOException e) {
			if (BASE_CONF.DEBUG) Log.d(TAG, "getRemouteSize IOException:"+e.getMessage());
			e.printStackTrace();
		}
	     return -1;
	}
	
	public static void userProxy(Context context) {
		String proxyHost = NAction.getProxyHost(context);
		String proxyPort = NAction.getProxyPort(context);
		String proxyUsername = NAction.getProxyUsername(context);
		String proxyPwd = NAction.getProxyPwd(context);

		if (!proxyHost.equals("")) {
			Properties props = System.getProperties();
			props.put("http.proxyHost", proxyHost);
			props.put("http.proxyPort", proxyPort);
			if (!proxyUsername.equals("")) {
				props.put("http.proxyUsername", proxyUsername);
				props.put("http.proxyPassword", proxyPwd);

			}
		}
	}

	public static Intent getLinkAsIntent(Context context, String link) {
		//Log.d(TAG, "openRemoteLink:"+link);
		String vlowerFileName = link.toLowerCase();
		if (vlowerFileName.startsWith("lgmarket:")) {
			String[] xx = link.split(":");
			//Log.d(TAG, "lgmarket:"+xx[1]);

			Intent intent = new Intent("com.lge.lgworld.intent.action.VIEW");
			intent.setClassName("com.lge.lgworld", "com.lge.lgworld.LGReceiver");
			intent.putExtra("lgworld.receiver","LGSW_INVOKE_DETAIL");
			intent.putExtra("APP_PID", xx[1]);

			/*Intent intent = new Intent();
			intent.setClassName("com.lg.apps.cubeapp", "com.lg.apps.cubeapp.PreIntroActivity");
			intent.putExtra("type", "APP_DETAIL ");
			intent.putExtra("codeValue", ""); // value is not needed when moving to Detail page
			intent.putExtra("content_id", xx[1]);   */

			context.sendBroadcast(intent);

			return null;

		} else {
			Uri uLink = Uri.parse(link);

			Intent intent = new Intent( Intent.ACTION_VIEW, uLink );

			return intent;
		}
	}

	public static String getUserNoId(Context context) {
		String usernoid = NStorage.getSP(context, "user.usernoid");
		if (usernoid.equals("")) {
			// TODO
			//UUID uuid  =  UUID.randomUUID();
			usernoid = UUID.randomUUID().toString();
			NStorage.setSP(context, "user.usernoid", usernoid);
		}

		return usernoid;
	}


	public static void recordAdLog(Context context, String act, String key) {
		if (NAction.getExtP(context, "conf_log_ad_enable").equals("1")) {
			UserLog pq = new UserLog(context);
			if (!pq.checkIfLogExists(act, key, "", "13", "")) {

				pq.insertNewLog(act, key, "", "13", "", 0);
			}
			//pq.close();
		}
	}
	
	public static int getUpdateQ(Context context) {
		String seq = NStorage.getSP(context, "app.update_seq");
		if (BASE_CONF.DEBUG) Log.d(TAG, "getUpdateQ:"+seq);
		if (seq.equals("")) {
			return 0;
		} else {
			try {
				return Integer.parseInt(seq);
			} catch (Exception e) {
				return 3;
			}
		}
	}

	public static void setAd(Context context, String who, String banner, String link, String key, String term, String act) {
		NStorage.setSP(context, "ad.who", who);
		NStorage.setSP(context, "ad.banner", banner);
		NStorage.setSP(context, "ad.link", link);
		NStorage.setSP(context, "ad.key", key);
		NStorage.setSP(context, "ad.term", term);
		NStorage.setSP(context, "ad.act", act);
	}

	public static void setProxyPort(Context context, String val) {
		NStorage.setSP(context, "proxy.port", val);
	}

	public static String getProxyHost(Context context) {
		String val = NStorage.getSP(context, "proxy.host");
		return val;
	}
	
	public static String getProxyPort(Context context) {
		String val = NStorage.getSP(context, "proxy.port");
		return val;
	}

	public static String getProxyUsername(Context context) {
		String val = NStorage.getSP(context, "proxy.username");
		return val;
	}

	public static String getProxyPwd(Context context) {
		String val = NStorage.getSP(context, "proxy.pwd");
		return val;
	}


	public static int getUpdateCheckTime(Context context) {
		String s = NStorage.getSP(context, "tmp.update_check_time");
		if (s.equals("")) {
			return 0;
		} else {
			return Integer.parseInt(s);
		}
	}

	public static void setUpdateCheckTime(Context context) {
        NStorage.setSP(context, "tmp.update_check_time", String.valueOf(VeDate.getStringDateHourAsInt()));
	}

	public static String getUserName(Context context) {
		return NStorage.getSP(context, "user.username");
	}

	public static String getUID(Context context) {
		return NStorage.getSP(context, "user.uid");
	}

	public static String getToken(Context context) {
		return NStorage.getSP(context, "user.token");
	}
	

	public static String getCode(Context context) {
		String packageName = context.getPackageName();
		String[] xcode = packageName.split("\\.");
		String code = xcode[xcode.length-1];
		return code;
	}

	public static String getUserUrl(Context context) {
		String sdk = "0";
		try {
			sdk = Build.VERSION.SDK;
		} catch (Exception e) {

		}
		return "uid="+NAction.getUID(context)+"&token="+NAction.getToken(context)+"&userno="+NAction.getUserNoId(context)+"&lang="+NUtil.getLang()+
				"&ver="+NUtil.getVersinoCode(context)+"&code="+NAction.getCode(context)+"&sdk="+sdk+"&appid="+context.getPackageName();
	}

	// ftp
	public static void setFtpRoot(Context context, String root) {
    	NStorage.setSP(context, "ftp.root", root);
	}

	public static String getFtpRoot(Context context) {
    	return NStorage.getSP(context, "ftp.root");
	}

	public static void setFtpUsername(Context context, String username) {
    	NStorage.setSP(context, "ftp.username", username);
	}

	public static void setFtpPwd(Context context, String pwd) {
    	NStorage.setSP(context, "ftp.pwd", pwd);
	}

	public static String getFtpUsername(Context context) {
    	return NStorage.getSP(context, "ftp.username");
	}

	public static String getFtpPwd(Context context) {
    	return NStorage.getSP(context, "ftp.pwd");
	}

	public static void setFtpPort(Context context, String port) {
    	NStorage.setSP(context, "ftp.port", port);
	}

	public static String getFtpPort(Context context) {
    	return NStorage.getSP(context, "ftp.port");
	}


	public static boolean isQPy3(Context context) {
		return true;
		/*String code = NAction.getCode(context);
		if (code.contains("qpy3")) {
			return true;
		}

		if (NAction.getQPyInterpreter(context).startsWith("3.")) {
			return true;
		} else {
			return false;
		}*/
	}

	public static boolean isQPyInterpreterSet(Context context) {
		String qpyInterVal = NStorage.getSP(context, "conf.default_qpy_interpreter");
		return !qpyInterVal.equals("");
	}
	public static String getQPyInterpreter(Context context) {
		String qpyInterVal = NStorage.getSP(context, "conf.default_qpy_interpreter");
        if (!qpyInterVal.startsWith("3.")) {
        	qpyInterVal = "2.x";
        }

        return qpyInterVal;
	}

	public static void setQPyInterpreter(Context context, String qpyInterVal) {
		 NStorage.setSP(context, "conf.default_qpy_interpreter", qpyInterVal);
		// It shouldn't be here and need to be refactor
		ACache.get(context).clear();
	}
	
	public static boolean httpPing(String url, int timeout) {
		//Log.d(TAG, "httpPing:"+url+"-"+timeout);
	    url = url.replaceFirst("https", "http"); // Otherwise an exception may be thrown on invalid SSL certificates.

	    try {
	        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
	        connection.setConnectTimeout(timeout);
	        connection.setReadTimeout(timeout);
	        connection.setRequestMethod("HEAD");
	        int responseCode = connection.getResponseCode();
	        //Log.d(TAG, "responseCode:"+responseCode);
	        return (responseCode>0);
	        //return (200 <= responseCode && responseCode <= 399);
	    } catch (IOException exception) {
	        Log.d(TAG, "exception:"+exception.getLocalizedMessage());

	        return false;
	    }
	}

    static public boolean portIsOpen(String ip, int port, int timeout) {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port), timeout);
            socket.close();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    // thread utils
    static public void setThreadStat(Context context, int threadid, int stat) {
    	Log.d(TAG, "setThreadStat:"+threadid+"-"+stat);
    	NStorage.setIntSP(context, "thread_stat_"+threadid,stat);
    }

  	static public boolean isThreadsStop(Context context) {
  		boolean st = true;
  		for (int i = 1; i<= BASE_CONF.THREA_STAT.length; i++) {
  			int j = NStorage.getIntSP(context, "thread_stat_"+i);
  			Log.d(TAG, "isThreadsStop i:"+i+"-j:"+j);
  			if (j == 1) {
  				st = false;
  			}
  		}
  		Log.d(TAG, "isThreadsStop:"+st);
  		return st;
  	}

  	static public void clearThreadsStat(Context context) {
  		for (int i = 1; i<= BASE_CONF.THREA_STAT.length; i++) {
  			NStorage.setIntSP(context, "thread_stat_"+i,0);
  		}
  	}

	public static boolean isRootEnable(Context context) {
		boolean enabledRoot = NStorage.getSP(context, "app.root").equals("1");
		return isRootSystem() && enabledRoot;
	}

	public static boolean isRootSystem() {
		if(systemRootState==kSystemRootStateEnable) {
			return true;
		} else if(systemRootState==kSystemRootStateDisable) {

			return false;
		}
		File f=null;
		final String kSuSearchPaths[]={"/su/bin/", "/system/bin/","/system/xbin/","/system/sbin/","/sbin/","/vendor/bin/"};
		try {
			for(int i=0;i<kSuSearchPaths.length;i++)
			{
				f=new File(kSuSearchPaths[i]+"su");
				if(f!=null&&f.exists())
				{
					systemRootState=kSystemRootStateEnable;
					return true;
				}
			}
		}catch(Exception e) {
		}
		systemRootState=kSystemRootStateDisable;
		return false;
	}

	public static void startInstalledAppDetailsActivity(final Activity context) {
		if (context == null) {
			return;
		}
		final Intent i = new Intent();
		i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
		i.addCategory(Intent.CATEGORY_DEFAULT);
		i.setData(Uri.parse("package:" + context.getPackageName()));
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
		i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
		context.startActivity(i);
	}

	public static String getPyVer() {
		return "3";//isQPy3(context)?"3":"2";
	}
}
