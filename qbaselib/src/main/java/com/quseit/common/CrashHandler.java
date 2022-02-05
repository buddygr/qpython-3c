package com.quseit.common;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;
import android.view.InflateException;
import android.widget.Toast;

import com.quseit.android.R;
import com.quseit.common.db.AppLog;
import com.quseit.util.NAction;
import com.quseit.util.NUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class CrashHandler implements UncaughtExceptionHandler {  
	private static String TAG = "CrashHandler";
    private static CrashHandler INSTANCE;
    private Context mContext;
    private Thread.UncaughtExceptionHandler mDefaultHandler;
	private Map<String, String> infos = new HashMap<String, String>();

    public static CrashHandler getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new CrashHandler();
        }
        return INSTANCE;
    }
	
    public static void WriteSettings(Context context, String data,String name) {
    	//NAction.recordUserLog(context, "gexception", data);
    	//Log.d(TAG, "WriteSettings:"+data);

    	if (!NAction.getExtP(context, "conf_enable_crash_log").equals("0")) {
	    	AppLog appDB = new AppLog(context);

	    	String ver = String.valueOf(NUtil.getVersinoCode(context));
	    	if (!appDB.ifLogExists(ver, data)) {
	    		Log.d(TAG, "WriteSettings no exits:"+data);
	    		appDB.insertNewLog(name, ver, NAction.getUserNoId(context), data);
	    	} else {
	    		Log.d(TAG, "WriteSettings exits");
	    	}

	    	File log = new File(Environment.getExternalStorageDirectory()+"/"+NAction.getCode(context)+"_last_err.log");
	    	if (log.exists()) {
	    		log.delete();
	    	}
			byte[] datas = data.getBytes();
			FileOutputStream outStream;
			try {
				outStream = new FileOutputStream(log);
				outStream.write(datas);
				outStream.close();
			} catch (FileNotFoundException e) {

			} catch (IOException e) {
				e.printStackTrace();
			}

		}
    	//appDB.close();
    	/*
    	FileOutputStream fOut = null;
        OutputStreamWriter osw = null;
        try{
        	fOut = context.openFileOutput(name,Context.MODE_PRIVATE);
            osw = new OutputStreamWriter(fOut);
            osw.write(data);
            osw.flush();
            //Toast.makeText(context, "Settings saved",Toast.LENGTH_SHORT).show();
          } catch (Exception e) {
            e.printStackTrace();
            //Toast.makeText(context, "Settings not saved",Toast.LENGTH_SHORT).show();
          } finally {
        	  try {
        		  osw.close();
                  fOut.close();
              } catch (IOException e) {
            	  e.printStackTrace();
              }
          }*/
    }
	
    /**
	 * 当UncaughtException发生时会转入该函数来处理
	 */
	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		if (!handleException(ex) && mDefaultHandler != null) {
			//如果用户没有处理则让系统默认的异常处理器来处理
			mDefaultHandler.uncaughtException(thread, ex);
			//退出程序
			android.os.Process.killProcess(android.os.Process.myPid());
			System.exit(1);

		} else {
			new Thread() {
                @Override
                public void run() {
                	try {
                        Looper.prepare();
            			Toast.makeText(mContext, MessageFormat.format(mContext.getString(R.string.err_caught), Environment.getExternalStorageDirectory()+"/"+NAction.getCode(mContext)+"_last_err.log"), Toast.LENGTH_LONG).show();
                        Looper.loop();
                	} catch (InflateException e) {
						Log.e(TAG, "error : ", e);
                	}
                }
			}.start();
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				Log.e(TAG, "error : ", e);
			}
			//退出程序
			android.os.Process.killProcess(android.os.Process.myPid());
			System.exit(1);
		}
	}

	/**
	 * 自定义错误处理,收集错误信息 发送错误报告等操作均在此完成.
	 *
	 * @param ex
	 * @return true:如果处理了该异常信息;否则返回false.
	 */
	private boolean handleException(Throwable ex) {
		if (ex == null) {
			return false;
		}
		ex.printStackTrace();

		//collect device info
		collectDeviceInfo(mContext);

		StringBuffer sb = new StringBuffer();
		for (Map.Entry<String, String> entry : infos.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			if (!key.equals("TIME")) {
				sb.append(key + "=" + value + "\n");
			}
		}

		Writer writer = new StringWriter();
		PrintWriter printWriter = new PrintWriter(writer);
		ex.printStackTrace(printWriter);
		Throwable cause = ex.getCause();
		while (cause != null) {
			cause.printStackTrace(printWriter);
			cause = cause.getCause();
		}
		printWriter.close();
		String result = writer.toString();
		sb.append(result);

		// todo
        WriteSettings(mContext,sb.toString(),"error") ;

		return true;
	}
      
	/**
	 * 收集设备参数信息
	 * @param ctx
	 */
	public void collectDeviceInfo(Context ctx) {
		Field[] fields = Build.class.getDeclaredFields();
		for (Field field : fields) {
			try {
				field.setAccessible(true);
				infos.put(field.getName(), field.get(null).toString());
				Log.d(TAG, field.getName() + " : " + field.get(null));
			} catch (Exception e) {
				Log.e(TAG, "an error occured when collect crash info", e);
			}
		}

	}
      
	/**
	 * 保存错误信息到文件中
	 *
	 * @return	返回文件名称,便于将文件传送到服务器
	 */
	/*private String saveCrashInfo2File(StringBuffer sb) {

		String path = util.preparePath(mContext) + "crash/";
		try {
			long timestamp = System.currentTimeMillis();
			String time = formatter.format(new Date());
			String fileName = "crash-" + time + "-" + timestamp + ".log";
			File dir = new File(path);
			if (!dir.exists()) {
				dir.mkdirs();
			}
			FileOutputStream fos = new FileOutputStream(path + fileName);
			fos.write(sb.toString().getBytes());
			fos.close();
			return path + fileName;
		} catch (Exception e) {
			Log.e(TAG, "an error occured while writing file...", e);
		}
		return "";
	}*/

    public void init(Context ctx) {
        mContext = ctx;
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }
}  
