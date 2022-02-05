package com.quseit.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class UnhandledExceptionHandler implements UncaughtExceptionHandler {

    private UncaughtExceptionHandler defaultUEH;

    /* 
     * if any of the parameters is null, the respective functionality 
     * will not be used 
     */
    public UnhandledExceptionHandler() {
    	this("/sdcard/");
    }
    
    public UnhandledExceptionHandler(String localPath) {
        this.defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
    }

    public void uncaughtException(Thread t, Throwable e) {
    	
    	if (!"sdk".equals(android.os.Build.MODEL) && 
    		!"google_sdk".equals(android.os.Build.MODEL)) 
    	{
	        StringBuilder sb = new StringBuilder();
	        
	        Log.e("Exception", "Uncaught Exception", e);
	        e.printStackTrace();
	        
	    	Calendar cal = Calendar.getInstance();
	    	cal.setTimeInMillis(System.currentTimeMillis());
	
	        String format = "yyyy-MM-dd'T'HH:mm:ss";
	        SimpleDateFormat sdf = new SimpleDateFormat(format);
	
	        sb.append("-------------\n");
	        sb.append("Time of crash: " + sdf.format(cal.getTime()) + "\n");
	        sb.append("Phone: " + android.os.Build.MODEL + "\n");
	        sb.append("Android Version: " + android.os.Build.VERSION.RELEASE + "\n");
	        sb.append("-------------\n");
	        
	        final Writer result = new StringWriter();
	        final PrintWriter printWriter = new PrintWriter(result);
	        e.printStackTrace(printWriter);
	        sb.append(result.toString());
	        printWriter.close();
	        
	        //StringWriter sw = new StringWriter();
	        //StringWriter logw = new StringWriter();
	        
	        //JsonWriter w = new JsonWriter(sw);
	        //String uuid = "0";
	        /*
	        if (ConfigurationManager.getInstance().getLayoutInflater() != null) {
	        	uuid = android.provider.Settings.Secure.getString(
	        			ConfigurationManager.getInstance().getLayoutInflater().getContext().getContentResolver(),
	        			android.provider.Settings.Secure.ANDROID_ID);
	        }*/
	        
	        /*try {
	        	w.beginObject();
	            
	        	w.name("uuid");
	            w.value(uuid);
				
	            w.name("report");
				w.value(sb.toString());
				
				w.name("log");
				Log.dump(logw);
				w.value(logw.toString());
				
		        w.endObject();
		        w.flush();
		        
		        //Net.getHTTPContent("http://mobile.nativeconcierge.com/api/report/crash/", "request="  + sw.toString(), "application/x-www-form-urlencoded");
			} catch (IOException e1) {
			}*/
	        
	        //String filename = "nativeconcierge-crash-" + timestamp + ".stacktrace";
	        //writeToFile(stacktrace, filename);
    	}
    	
        defaultUEH.uncaughtException(t, e);
    }

    //private void writeToFile(String stacktrace, String filename) {
        /*try {
        	File root = ConfigurationManager.getFilesDir(); 
        	if (root.canWrite()) {
        		File file = new File(root, filename);
        		//Log.d("Exception", "Writing to " + file.getAbsolutePath());
        		BufferedWriter bos = new BufferedWriter(new FileWriter(file));
	            bos.write(stacktrace);
	            bos.flush();
	            bos.close();
        	} else {
        		//Log.d("Exception", "Can't write to " + root.getAbsolutePath());
        	}
        } catch (Exception e) {
            e.printStackTrace();
        }*/
    //}
}
