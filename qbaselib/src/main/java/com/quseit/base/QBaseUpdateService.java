package com.quseit.base;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.res.Resources.NotFoundException;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.quseit.android.R;
import com.quseit.config.BASE_CONF;
import com.quseit.util.FileHelper;
import com.quseit.util.NAction;
import com.quseit.util.NUtil;
import com.quseit.util.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public abstract class QBaseUpdateService extends Service {
	private static int NOTIFICATION_ID = 0x20001;//通知栏消息id

	private static final String TAG = "QBaseUpdateService";
	private int NotifyIndex;

	private int titleId = 0;
	private String UPDATELINK;
	//private File updateDir = null;
	private File updateFile = null;
	
	private final static int DOWNLOAD_COMPLETE = 0;
	private final static int DOWNLOAD_FAIL = 1;
	
	private NotificationManager updateNotificationManager = null;

	private PendingIntent updatePendingIntent = null;
	
	private String type;
	private String dst;
	private String from;
	private boolean replace = true;
	
	@TargetApi(5)
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
	    //获取传值
		//NotifyIndex = VeDate.getDatemsAsInt();
		NotifyIndex = NOTIFICATION_ID;
		try {
			titleId = intent.getIntExtra(BASE_CONF.EXTRA_CONTENT_URL1, 0);
		} catch (Exception e) {
			titleId = 0;
		}
	    UPDATELINK = intent.getStringExtra(BASE_CONF.EXTRA_CONTENT_URL2);
	    type = intent.getStringExtra(BASE_CONF.EXTRA_CONTENT_URL3);
	    dst = intent.getStringExtra(BASE_CONF.EXTRA_CONTENT_URL4);
	    from = intent.getStringExtra(BASE_CONF.EXTRA_CONTENT_URL5);
	    String xx = intent.getStringExtra(BASE_CONF.EXTRA_CONTENT_URL6);
	    if (xx!=null && xx.equals("0")) {
	    	replace = false;
	    } else {
	    	replace = true;
	    }
	    
	    //创建文件
	    if (NUtil.isExternalStorageExists()){
	        try {
				updateFile = new File(FileHelper.getBasePath(getDst(), "tmp"), getResources().getString(titleId)+"."+type);
			} catch (NotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	   	 
		    this.updateNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

		    //设置下载过程中，点击通知栏，回到主界面
		    Intent x = getSrvUpdateRet();
		    if (x == null) {
		    	Intent updateIntent = new Intent();
		    	updateIntent.setClassName(this.getPackageName(), this.getPackageName()+".MIndexAct");
		    	
		    	updatePendingIntent = PendingIntent.getActivity(this,NOTIFICATION_ID,updateIntent,0);
		    } else {
		    	Intent updateIntent = x;
			    updatePendingIntent = PendingIntent.getActivity(this,NOTIFICATION_ID,updateIntent,0);
		    }

			Notification updateNotification = NAction.getNotification(getApplicationContext(), getString(R.string.up_soft_update),"%0", updatePendingIntent, R.drawable.ic_download_nb, null,Notification.FLAG_ONGOING_EVENT);

		    //发出通知
		    updateNotificationManager.notify(NotifyIndex,updateNotification);
		 
		    //开启一个新的线程下载，如果使用Service同步下载，会导致ANR问题，Service本身也会阻塞
		    new Thread(new updateRunnable()).start();//这个是下载的重点，是下载的过程
	    } else {
	    	Toast.makeText(getApplicationContext(), R.string.not_sd, Toast.LENGTH_SHORT).show();
			stopSelf();
	    }
	     
	    return super.onStartCommand(intent, flags, startId);
	}

	private Handler updateHandler = new  Handler() {
	    @Override
	    public void handleMessage(Message msg) {
			Intent intentS =  new Intent(getApplicationContext(), getSelf());

	        switch(msg.what){
	            case DOWNLOAD_COMPLETE:
		             updateNotificationManager.cancel(NotifyIndex);

	    	    	if (from!=null && !from.equals("")) {	// NOTIFY
	    	    		Log.d(TAG, "send notify to:"+from);
	    				Intent intent1 = new Intent(from);
	    				sendBroadcast(intent1);
	    	    	}
	    	    	
	                //点击安装PendingIntent

	                //if (type.equals("apk")) {
		               

	                	/*Notification updateNotification = new Notification();
	                	Uri uri = Uri.fromFile(updateFile);
		                Intent installIntent = new Intent(Intent.ACTION_VIEW);
		                
		                installIntent.setDataAndType(uri, "application/vnd.android.package-archive");
		                updatePendingIntent = PendingIntent.getActivity(QBaseUpdateService.this, 0, installIntent, 0);
		                
		                //updateNotification.defaults = Notification.DEFAULT_VIBRATE;
		                updateNotification.setLatestEventInfo(QBaseUpdateService.this, getString(R.string.app_name), getString(R.string.up_soft_done), updatePendingIntent);
		                updateNotificationManager.notify(NotifyIndex, updateNotification);*/
	                //}
	                //停止服务
	                stopService(intentS);
	                break;
	            case DOWNLOAD_FAIL:

					Notification updateNotification2 = NAction.getNotification(getApplicationContext(), getString(R.string.app_name),getString(R.string.up_update_failed), updatePendingIntent, R.drawable.ic_warning_nb, null, Notification.FLAG_AUTO_CANCEL);

	                updateNotificationManager.notify(NotifyIndex, updateNotification2);
	            default:
	                stopService(intentS);

	        }
	    }
	};


	class updateRunnable implements Runnable {
		Message message = updateHandler.obtainMessage();
		public void run() {
			message.what = DOWNLOAD_COMPLETE;
			try {
				//增加权限<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE">;

		        if (!updateFile.exists()) {
		        	updateFile.createNewFile();
		        }
		        //下载函数，以QQ为例子
		        //增加权限<uses-permission android:name="android.permission.INTERNET">;
		        long downloadSize = downloadUpdateFile(UPDATELINK, updateFile);
		        //Log.d(TAG, "UPDATELINK:"+UPDATELINK+"-size:"+downloadSize+":type"+type+":dst"+dst);

		        if (downloadSize>0){

		        	//下载成功
		        	if (type.equals("apk")) {
	    				Log.d(TAG, "apk");

		                Uri uri = Uri.fromFile(updateFile);
		                Intent installIntent = new Intent(Intent.ACTION_VIEW);
		                installIntent.setDataAndType(uri, "application/vnd.android.package-archive");
		                installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		                startActivity(installIntent);
		        	} else {
		        		String ext = FileHelper.getExt(updateFile.getAbsolutePath().toString(), "");
		        		String dext = FileHelper.getExt(dst, "");
		        		File destF = new File(dst);
		        		Log.d(TAG, "ext:"+ext+"-dext:"+dext);
	        			if (destF.exists()) {
	        				if (destF.isFile()) {
	        					destF.delete();
	        				} else if (destF.isDirectory()) {
	        					Utils.deleteDir(destF);
	        				}
	        			}
	        			
		        		if (ext.equals(dext)) {	// 后缀相同
	        				Log.d(TAG, "same ext");

		        			updateFile.renameTo(destF);
		        			
		        		} else {
		        			
		        			if (ext.equals("zip")) {
		        				Log.d(TAG, "zip found and unzip");
		        				InputStream content = new FileInputStream(updateFile);
		        				Utils.unzip(content, dst, replace);
		        			}
		        		}
		        	}
		            updateHandler.sendMessage(message);
		        }
		    } catch (Exception ex) {
		    	ex.printStackTrace();
		        message.what = DOWNLOAD_FAIL;
		        //下载失败
		        updateHandler.sendMessage(message);
		    }
		}
		
		public long downloadUpdateFile(String downloadUrl, File saveFile) throws Exception {
			int downloadCount = 0;
			int currentSize = 0;
			long totalSize = 0;
			int updateTotalSize = 0;
			             
			HttpURLConnection httpConnection = null;
			InputStream is = null;
			FileOutputStream fos = null;
			             
			try {
				URL url = new URL(downloadUrl);
			    httpConnection = (HttpURLConnection)url.openConnection();
			    httpConnection.setRequestProperty("User-Agent", "PacificHttpClient");
			    if (currentSize > 0) {
			    	httpConnection.setRequestProperty("RANGE", "bytes=" + currentSize + "-");
			     }
			     httpConnection.setConnectTimeout(10000);
			     httpConnection.setReadTimeout(20000);
			     updateTotalSize = httpConnection.getContentLength();
			     if (httpConnection.getResponseCode() == 404) {
			    	 throw new Exception("fail!");
			     }
			     is = httpConnection.getInputStream();                  
			     fos = new FileOutputStream(saveFile, false);
			     byte buffer[] = new byte[4096];
			     int readsize = 0;
			     while ((readsize = is.read(buffer)) > 0) {
			    	 fos.write(buffer, 0, readsize);
			         totalSize += readsize;
			         //为了防止频繁的通知导致应用吃紧，百分比增加10才通知一次
			         if ((downloadCount == 0)||(int) (totalSize*100/updateTotalSize)-10>downloadCount) {
			        	 downloadCount += 10;

						 Notification updateNotification2 = NAction.getNotification(getApplicationContext(), getString(R.string.up_soft_downloading),(int)totalSize*100/updateTotalSize+"%", updatePendingIntent, R.drawable.ic_download_nb, null, Notification.FLAG_AUTO_CANCEL);

			             updateNotificationManager.notify(NotifyIndex, updateNotification2);
			         }                       
			     }
			} finally {
				if (httpConnection != null) {
			    	httpConnection.disconnect();
			    }
			    if (is != null) {
			    	is.close();
			    }
			    if (fos != null) {
			    	fos.close();
			    }
			}
			return totalSize;
		}

	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	public abstract Intent getSrvUpdateRet();
	public abstract String getDst();
	public abstract Class<?> getSelf();
}
