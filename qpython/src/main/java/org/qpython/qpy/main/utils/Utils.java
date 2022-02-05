package org.qpython.qpy.main.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.qpython.qpy.R;
import org.qpython.qpy.main.activity.HomeMainActivity;
import org.qpython.qpy.main.activity.QWebViewActivity;
import org.qpython.qpysdk.QPyConstants;
import org.qpython.qpysdk.utils.FileHelper;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by mathiasluo on 16-6-1.
 */

public class Utils extends org.qpython.qpysdk.utils.Utils {
    static final String TAG = "Utils";
    public static String logFile[] = {""};

    public static void startWebActivityWithUrl(Context context, String title, String url) {
        Utils.startWebActivityWithUrl(context, title, url, "", false, false);
    }
    public static void startWebActivityWithUrl(Context context, String title, String url, String script, boolean isNoHead, boolean isDrawer) {
        Intent intent = new Intent(context, QWebViewActivity.class);
        if (script.equals("")) {
            Uri u = Uri.parse(url);
            intent.setData(u);
        }

        intent.putExtra(QWebViewActivity.ACT, "main");
        intent.putExtra(QWebViewActivity.TITLE, title);
        intent.putExtra(QWebViewActivity.SRC, url);
        intent.putExtra(QWebViewActivity.LOG_PATH, script);
        intent.putExtra(QWebViewActivity.IS_NO_HEADER, isNoHead ? "1" : "0");
        intent.putExtra(QWebViewActivity.IS_DRAWER, isDrawer ? "drawer" : "");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_MULTIPLE_TASK|Intent.FLAG_ACTIVITY_NEW_DOCUMENT);

        context.startActivity(intent);
    }

    public static void createDirectoryOnExternalStorage(String path) {
        try {
            if (Environment.getExternalStorageState().equalsIgnoreCase("mounted")) {
                File file = new File(Environment.getExternalStorageDirectory(), path);
                if (!file.exists()) {
                    try {
                        file.mkdirs();

                        Log.d(TAG, "createDirectoryOnExternalStorage created " + Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + path);
                    } catch (Exception e) {
                        Log.e(TAG, "createDirectoryOnExternalStorage error: ", e);
                    }
                }
            } else {
                Log.e(TAG, "createDirectoryOnExternalStorage error: " + "External storage is not mounted");
            }
        } catch (Exception e) {
            Log.e(TAG, "createDirectoryOnExternalStorage error: " + e);
        }

    }


    public static boolean netOk(Context context) {
        try {
            ConnectivityManager nInfo = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            nInfo.getActiveNetworkInfo().isConnectedOrConnecting();


            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            if (netInfo != null && netInfo.isConnectedOrConnecting()) {
                return true;
            } else {
                return false;
            }

        } catch (Exception e) {
            Log.d(TAG, "Network available:false");

            return false;
        }
    }

    public static String getLang() {
        return Locale.getDefault().getLanguage();
    }

    public static int getVersinoCode(Context context) {
        int intVersioinCode = 0;
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            intVersioinCode = info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return intVersioinCode;
    }

    public static Intent openRemoteLink(Context context, String link) {
        //LogUtil.d(TAG, "openRemoteLink:"+link);
        String vlowerFileName = link.toLowerCase();
        if (vlowerFileName.startsWith("lgmarket:")) {
            String[] xx = link.split(":");
            //LogUtil.d(TAG, "lgmarket:"+xx[1]);

            Intent intent = new Intent("com.lge.lgworld.intent.action.VIEW");
            intent.setClassName("com.lge.lgworld", "com.lge.lgworld.LGReceiver");
            intent.putExtra("lgworld.receiver", "LGSW_INVOKE_DETAIL");
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

            Intent intent = new Intent(Intent.ACTION_VIEW, uLink);

            return intent;
        }
    }

    public static String getCode(Context context) {
        String packageName = context.getPackageName();
        String[] xcode = packageName.split("\\.");
        String code = xcode[xcode.length - 1];
        return code;
    }

    public static String getSrv(String script) {
        //LogUtil.d(TAG, "getSrv:"+script);

        String content = FileHelper.getFileContents(script);

        String srv = "http://localhost";
        Pattern srvPattern = Pattern.compile("#qpy://(.+)[\\s]+", Pattern.CASE_INSENSITIVE);
        Matcher matcher1 = srvPattern.matcher(content);

        if (matcher1.find()) {
            srv = "http://" + matcher1.group(1);

            //LogUtil.d(TAG, "URL2:"+srv);

        }
        try {
            URL n = new URL(srv);
            srv = n.getProtocol() + "://" + n.getHost() + ":" + (n.getPort() > 0 ? n.getPort() : 80);
            //LogUtil.d(TAG, "getsrv:"+srv);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return srv;
    }

    public static boolean isZn() {
        String local = Locale.getDefault().getDisplayLanguage();
        return "中文".equals(local);
    }

    public static String getWebLog(String url,String script){
        String file, temp="";
        int tmp;
        file = logFile[0];
        if (file.equals("")) {
            if (script!=null) {
                if (script.endsWith("/main.py")){
                    tmp = script.length()-8;
                    temp = script.substring(0,tmp);
                } else {
                    tmp = script.lastIndexOf(".");
                    if (tmp>=0) {
                        temp = script.substring(0,tmp);
                    } else {
                        temp = script;
                    }
                }
                tmp = temp.lastIndexOf("/");
                if (tmp>=0){
                    temp = temp.substring(tmp+1);
                }
                temp="_"+temp;
            }
            tmp = QPyConstants.WEB_LOG.length()-4;
            temp = QPyConstants.WEB_LOG.substring(0,tmp)+temp;
        if (url != null) {
            Pattern pattern = Pattern.compile(":[0-9]+", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(url);
            if (matcher.find()) {
                file = temp+"_"+ matcher.group(0).substring(1)+".log";
            } else {
                file = temp+".log";
            }
        } else {
            file = temp+".log";
        }
        return getLogFile(file);
    } else {
            logFile[0] = "";
            return file;
        }
    }

    public static String getQuietLog(String script) {
        int i,j,k;
        i = QPyConstants.QUIET_LOG.length()-4;
        if (script.endsWith("/main.py")){
            j = script.length()-8;
        } else {
            j = script.length()-3;
        }
        k = script.substring(0,j).lastIndexOf("/") + 1;
        String f = QPyConstants.QUIET_LOG.substring(0,i) + "_" + script.substring(k,j) + ".log";
        return getLogFile(f);
    }

    public static String getLogFile(String logFile) {
        File LogFile = new File(logFile);

        if (!LogFile.getAbsoluteFile().getParentFile().exists()) {
            LogFile.getAbsoluteFile().getParentFile().mkdirs();
        } else if (LogFile.exists()) {    // clear log
            LogFile.delete();
        }

        return LogFile.getAbsolutePath();
    }

    public static void showNotification(Context context,char id,int title,int text){
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent resultIntent = new Intent(context, HomeMainActivity.class);
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, resultIntent, PendingIntent.FLAG_ONE_SHOT);
        NotificationCompat.Builder builder ;
        String CHANNEL_ID = "background_service_"+id;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){        //Android 8.0适配
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Background Service "+id,
                    NotificationManager.IMPORTANCE_DEFAULT);//如果这里用IMPORTANCE_NONE就需要在系统的设置里面开启渠道， //通知才能正常弹出
            manager.createNotificationChannel(channel);
            builder = new NotificationCompat.Builder(context, CHANNEL_ID);
        }else{
            builder = new NotificationCompat.Builder(context);
        }
        builder.setContentTitle(context.getString(title))            //指定通知栏的标题内容
                .setContentText(context.getString(text))             //通知的正文内容
                .setWhen(System.currentTimeMillis())               //通知创建的时间
                .setSmallIcon(R.drawable.terminal_mini_icon)       //通知显示的小图标，只能用alpha图层的图片进行设置
                .setContentIntent(pendingIntent);
        //.setLargeIcon(BitmapFactory.decodeResource(getResources(),R.drawable.ic_launcher_background));

        Notification notification = builder.build() ;
        //tartForeground(1, notification);
        manager.notify(1,notification);
    }

    public static void backTaskNotify(Context context){
        showNotification(context,'2',R.string.qpy_back_task,R.string.qpy_back_run);
    }
}