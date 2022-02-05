package com.quseit.util;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.quseit.config.BASE_CONF;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//import android.net.sip.SipSession.State;

public class NUtil {

	public static boolean isNumeric(String str) {
		try {
			double d = Double.parseDouble(str);

		} catch(NumberFormatException nfe) {
			return false;
		}
		return true;
	}

	// 返回a到b之間(包括a,b)的任意一個自然数,如果a > b || a < 0，返回-1
	public static int getRandomInt(int min, int max) {
		if (min > max || min < 0)
			return -1;
		// 下面两种形式等价
		// return a + (int) (new Random().nextDouble() * (b - a + 1));
		Random random = new Random();
		int s = random.nextInt(max)%(max-min+1) + min;
		return s;
	}
	public static Map<String, List<String>> getQueryParams(String url) {
		try {
			Map<String, List<String>> params = new HashMap<String, List<String>>();
			String[] urlParts = url.split("\\?");
			if (urlParts.length > 1) {
				String query = urlParts[1];
				for (String param : query.split("&")) {
					String[] pair = param.split("=");
					String key = URLDecoder.decode(pair[0], "UTF-8");
					String value = "";
					if (pair.length > 1) {
						value = URLDecoder.decode(pair[1], "UTF-8");
					}

					List<String> values = params.get(key);
					if (values == null) {
						values = new ArrayList<String>();
						params.put(key, values);
					}
					values.add(value);
				}
			}

			return params;
		} catch (UnsupportedEncodingException ex) {
			throw new AssertionError(ex);
		}
	}

	/**
	 * Convert byte array to hex string
	 * @param bytes toConvert
	 * @return hexValue
	 */
	public static String bytesToHex(byte[] bytes) {
		StringBuilder sbuf = new StringBuilder();
		for(int idx=0; idx < bytes.length; idx++) {
			int intVal = bytes[idx] & 0xff;
			if (intVal < 0x10) sbuf.append("0");
			sbuf.append(Integer.toHexString(intVal).toUpperCase());
		}
		return sbuf.toString();
	}

	/**
	 * Get utf8 byte array.
	 * @param str which to be converted
	 * @return  array of NULL if error was found
	 */
	public static byte[] getUTF8Bytes(String str) {
		try { return str.getBytes("UTF-8"); } catch (Exception ex) { return null; }
	}

	/**
	 * Load UTF8withBOM or any ansi text file.
	 * @param filename which to be converted to string
	 * @return String value of File
	 * @throws java.io.IOException if error occurs
	 */
	public static String loadFileAsString(String filename) throws java.io.IOException {
		final int BUFLEN=1024;
		BufferedInputStream is = new BufferedInputStream(new FileInputStream(filename), BUFLEN);
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream(BUFLEN);
			byte[] bytes = new byte[BUFLEN];
			boolean isUTF8=false;
			int read,count=0;
			while((read=is.read(bytes)) != -1) {
				if (count==0 && bytes[0]==(byte)0xEF && bytes[1]==(byte)0xBB && bytes[2]==(byte)0xBF ) {
					isUTF8=true;
					baos.write(bytes, 3, read-3); // drop UTF8 bom marker
				} else {
					baos.write(bytes, 0, read);
				}
				count+=read;
			}
			return isUTF8 ? new String(baos.toByteArray(), "UTF-8") : new String(baos.toByteArray());
		} finally {
			try{ is.close(); } catch(Exception ignored){}
		}
	}

	/**
	 * Returns MAC address of the given interface name.
	 * @param interfaceName eth0, wlan0 or NULL=use first interface
	 * @return  mac address or empty string
	 */
	public static String getMACAddress(String interfaceName) {
		try {
			List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
			for (NetworkInterface intf : interfaces) {
				if (interfaceName != null) {
					if (!intf.getName().equalsIgnoreCase(interfaceName)) continue;
				}
				byte[] mac = intf.getHardwareAddress();
				if (mac==null) return "";
				StringBuilder buf = new StringBuilder();
				for (byte aMac : mac) buf.append(String.format("%02X:",aMac));
				if (buf.length()>0) buf.deleteCharAt(buf.length()-1);
				return buf.toString();
			}
		} catch (Exception ignored) { } // for now eat exceptions
		return "";
        /*try {
            // this is so Linux hack
            return loadFileAsString("/sys/class/net/" +interfaceName + "/address").toUpperCase().trim();
        } catch (IOException ex) {
            return null;
        }*/
	}

	/**
	 * Get IP address from first non-localhost interface
	 * @param useIPv4   true=return ipv4, false=return ipv6
	 * @return  address or empty string
	 */
	public static String getIPAddress(boolean useIPv4) {
		try {
			List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
			for (NetworkInterface intf : interfaces) {
				List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
				for (InetAddress addr : addrs) {
					if (!addr.isLoopbackAddress()) {
						String sAddr = addr.getHostAddress();
						//boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
						boolean isIPv4 = sAddr.indexOf(':')<0;

						if (useIPv4) {
							if (isIPv4)
								return sAddr;
						} else {
							if (!isIPv4) {
								int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
								return delim<0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
							}
						}
					}
				}
			}
		} catch (Exception ignored) { } // for now eat exceptions
		return "";
	}

	@SuppressLint("NewApi")
	public static boolean checkCameraHardware(Context context) {
		// PackageManager.FEATURE_CAMERA / PackageManager.FEATURE_CAMERA_FRONT / PackageManager.FEATURE_CAMERA_ANY
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }
	public static String getCsFromRE(String pt, String content) {
    	Pattern pa1 = Pattern.compile(pt, Pattern.CASE_INSENSITIVE);
    	Matcher matcher1 = pa1.matcher(content);
		if (matcher1.find()) {
			return matcher1.group(1);
		} else {
			return "";
		}
	}
	public static String getCpuProcessFamilyInfo() {
		String cInfo = NUtil.getCpuInfo();
		Log.d(TAG, "getCpuProcessFamilyInfo:"+cInfo);
		String[] items = cInfo.split("\n");
		String ret = "";
		for (int i=0;i<items.length;i++) {
			String line = items[i].toLowerCase();
			if (line.startsWith("processor")) {
				String[] xitem = line.split(":");
				ret += xitem[1].trim()+"|";
				//String[] yy = xitem[1].trim().split("\\s+"); 
				//return yy[0];
			}
			if (line.startsWith("features")) {
				String[] xitem = line.split(":");
				ret +=xitem[1]+"|";
			}
		}
		return ret;
	}
	public static String getCpuFeaturesFromByInfo() {
		String cInfo = NUtil.getCpuInfo();
		String[] items = cInfo.split("\n");
		for (int i=0;i<items.length;i++) {
			String line = items[i].toLowerCase();
			if (line.startsWith("features")) {
				String[] xitem = line.split(":");
				return xitem[1];
			}
		}
		return "";
	}
	public static String getCpuProcessFromByInfo() {
		String cInfo = NUtil.getCpuInfo();
		String[] items = cInfo.split("\n");
		for (int i=0;i<items.length;i++) {
			String line = items[i].toLowerCase();
			if (line.startsWith("processor")) {
				String[] xitem = line.split(":");
				return xitem[1].trim();
			}
		}
		return "";
	}
	@TargetApi(4)
	public static String getCpuInfo() {
	    StringBuffer sb = new StringBuffer();
	    sb.append("abi: ").append(Build.CPU_ABI).append("\n");
	    if (new File("/proc/cpuinfo").exists()) {
	        try {
	            BufferedReader br = new BufferedReader(new FileReader(new File("/proc/cpuinfo")));
	            String aLine;
	            while ((aLine = br.readLine()) != null) {
	                sb.append(aLine + "\n");
	            }
	            if (br != null) {
	                br.close();
	            }
	        } catch (IOException e) {
	            e.printStackTrace();
	        } 
	    }

	    return sb.toString();
	}
	
	public static boolean isEmail(String pInput) {
        if(pInput == null){
            return false;
        }
        String checkPattern = "^([a-z0-9A-Z]+[-|\\.]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}$";
        Pattern regex = Pattern.compile(checkPattern);
        Matcher matcher = regex.matcher(pInput);
        return matcher.matches();
    }
	
	public static String getParameterFromUrl(String url, String param) {
		//Log.d(TAG, "getParameterFromUrl:"+url+"-p:"+param);
		URL iurl;
		try {
			iurl = new URL(url);
			String iquery = iurl.getQuery();
			if (iquery == null) {
				return "";
			} else {
				String[] q = iquery.split("&");
				for (int i=0;i<q.length;i++) {
					String[] xx = q[i].split("=");
					if (xx[0].equals(param) && xx.length>1) {
						return xx[1];
					}
				}
				return "";
			}

		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		}
	}

	public static String getFileFromUrl(String url) {
		String path = getPathFromUrl(url);
		String xx[] = path.split("/");
		return xx[xx.length-1];
	}

	public static String getPathFromUrl(String url) {
		URL iurl;
		try {
			iurl = new URL(url);
			try {
				return java.net.URLDecoder.decode(iurl.getPath(), "UTF-8");
			} catch (UnsupportedEncodingException e1) {
				return iurl.getPath();

			}

		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "unkown.dat";
		}
	}
	
	@TargetApi(4)
	public static String getCpuType() {
		return Build.CPU_ABI;
	}
	
	public static boolean isIP(String checkStr) {   
		try {   
			String number = checkStr.substring(0,checkStr.indexOf('.'));   
	        if(Integer.parseInt(number) > 255) 
	        	return false;   
	        checkStr = checkStr.substring(checkStr.indexOf('.')+ 1);   
	        number = checkStr.substring(0,checkStr.indexOf('.'));   
	        if(Integer.parseInt(number) > 255)
	        	return false;   
	        checkStr = checkStr.substring(checkStr.indexOf('.')+ 1);   
	        number = checkStr.substring(0,checkStr.indexOf('.'));   
	        if(Integer.parseInt(number) > 255)
	        	return false;   
	        number = checkStr.substring(checkStr.indexOf('.')+ 1);   
	        if (Integer.parseInt(number) > 255)   
	        	return false;   
	        return true;   
	    } catch (Exception e) {   
	        return false;   
	    }   
	}
	
	public static boolean isInt(String str) { 
		try { 
			Integer.parseInt(str)   ; 
			return true; 
		} catch (NumberFormatException  e)   { 
			return   false; 
		} 
	}
	
	public static String getLang() {
		return Locale.getDefault().getLanguage();
	}
	
	private static final String TAG = "NUtil";
	
	public static String sescape(String str) {
		try {
			return str.replace("'", "_").replace("\"","_").replace(":", "_").replace("+", "_").replace("?", "_").replace("!", "_").replace("#", "_").replace("(", "_").replace(")", "_").replace("{", "_").replace("}", "_").replace("\\", "_").replace("&", "_").replace("\n","_").replace("|", "_").replace("*", "_").replace("/", "_").replace(",", "_");
		} catch (NullPointerException e) {
			return "unkown";
		}
	}
	public static String getLocalNumber(Context context) {
		try {
			TelephonyManager tManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
	        @SuppressLint("MissingPermission") String number = tManager.getLine1Number();
	        return number;
		} catch (Exception e) {
			return "x";
		}
	}
	
	public static String getIMEI(Context context) {
		try {
			TelephonyManager telephonyManager=(TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
			@SuppressLint("MissingPermission") String imei=telephonyManager.getDeviceId();
			return imei;
		} catch (Exception e) {
			return "x";
		}
	}
	
	public static String getWifiMac(Context context) {
		try {
			WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
			@SuppressLint("MissingPermission") WifiInfo info = wifi.getConnectionInfo();
			return info.getMacAddress();
		} catch (Exception e) {
			return "x";
		}
	}
	
	public static int getRate(long c, long t) {
        double k = (double) (100*c/t);
        int x = (int)k;
        
        //Log.d(TAG, "getRate:"+ x);
        return x;
	}
	public static String getSizeAsKS(long size) {
		//java.text.DecimalFormat  df = new java.text.DecimalFormat("#.##");  
		double s = (double) ((size/1024));
		//return df.format(s)+"";
		int x = (int)s;
		if (x==0) {
			return "< 1";
		} else {
			return x+"";
		}
	}
	public static String getSizeAsMS(long size) {
		//java.text.DecimalFormat df = new java.text.DecimalFormat("#.##");  
		double s = (double) ((size/1024)/1024);
		//return df.format(s)+"";
		int x = (int)s;
		if (x==0) {
			return "< 1";
		} else {
			return x+"";
		}
	}
	public static String getSizeAsK(long size) {
		//java.text.DecimalFormat df = new java.text.DecimalFormat("#.##");  
		double s = (double) ((size/1024));
		//return df.format(s)+"";
		int x = (int)s;
		if (x==0) {
			return "< 1";
		} else {
			return x+"";
		}
	}	
	public static int getSizeAsM(long size) {
		//java.text.DecimalFormat  df = new java.text.DecimalFormat("#.##");  
		double s = (double) ((size/1024)/1024);
		//return df.format(s)+"";
		int x = (int)s;
		return x;
	}
	
	public static List<PackageInfo> getAllApps(Context context) {  
	    List<PackageInfo> apps = new ArrayList<PackageInfo>();  
	    PackageManager pManager = context.getPackageManager();  
	    //获取手机内所有应用  
	    List<PackageInfo> paklist = pManager.getInstalledPackages(0);  
	    for (int i = 0; i < paklist.size(); i++) {  
	        PackageInfo pak = (PackageInfo) paklist.get(i);  
	        //判断是否为非系统预装的应用程序  
	        //if ((pak.applicationInfo.flags & pak.applicationInfo.FLAG_SYSTEM) <= 0) {  
	            // customs applications  
	            apps.add(pak);  
	        //}  
	    }  
	    return apps;  
	}  
	public static boolean checkAppInstalledByName(Context context, Intent intent) {
		PackageManager pm = context.getPackageManager(); 
        List<ResolveInfo> activities = pm.queryIntentActivities(intent, 0);
        if (activities.size() == 0) {
        	if (true)  Log.d(TAG, "packaged not installed:"+intent.getAction());

        	return false;
        } else {
        	if (true) Log.d(TAG, "packaged installed:"+intent.getAction());

        	return true;
        }
	}

	@SuppressWarnings("unused")
	public static boolean checkAppInstalledByName(Context context, String packageName) {
		    if (packageName == null || "".equals(packageName))  
		        return false;  
		    try {  
		        ApplicationInfo info = context.getPackageManager().getApplicationInfo(  
		                packageName, PackageManager.GET_UNINSTALLED_PACKAGES);  
		        
		        //Log.d(TAG,  "checkAppInstalledByName:"+packageName+" found");
		        return true;  
		    } catch (NameNotFoundException e) {  
		        //Log.d(TAG,  "checkAppInstalledByName:"+packageName+" not found");

		        return false;  
		    }  
		
		
		/*List<PackageInfo> apps = NUtil.getAllApps(context);
		for(int i=0;i<apps.size();i++) {  
		    PackageInfo pinfo = apps.get(i);  
		    if (pinfo.applicationInfo.packageName.equals(packageName)) {
		    	if (BASE_CONF.DEBUG) Log.d(TAG, "packaged installed:"+packageName);
		    	return true;
		    }
		}
		return false;
		    Intent intent = new Intent(Intent.ACTION_VIEW);  
    intent.setClassName("com.android.settings", //$NON-NLS-1$  
            "com.android.settings.InstalledAppDetails"); //$NON-NLS-1$  
    intent.putExtra("com.android.settings.ApplicationPkgName", //$NON-NLS-1$  
            mCurrentPkgName);  
    List<ResolveInfo> acts = getPackageManager().queryIntentActivities(  
            intent, 0);  
    if (acts.size() > 0) {  
        startActivity(intent);  
    } else {  
        Toast.makeText(this,  
                getString(R.string.failed_to_resolve_activity),  
                Toast.LENGTH_SHORT).show();  
    }  
		
		*/
	}

	public static double formatd6e(double m) {
		return (double)((int) (m * 1E6)/1E6);
	}
	
	public static double round(double value,  int scale,  int roundingMode) {
		BigDecimal bd = new BigDecimal(value);
		bd = bd.setScale(scale, roundingMode);
		double d = bd.doubleValue();
		bd = null;
		return d;
	}

    public static void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            // pre-condition
            return;
        }
 
        int totalHeight = 0;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(0, 0);
            totalHeight += listItem.getMeasuredHeight();
        }
 
        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
    }
    
	public static int getSCWidth(Context context) {
		WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		int width = wm.getDefaultDisplay().getWidth();//屏幕宽度
    	//Toast.makeText(context, "Screen width:"+width, Toast.LENGTH_SHORT).show(); 
		return width;
	}
	
	public static void myNotify(Context context, String info) {
    	Toast.makeText(context, info, Toast.LENGTH_SHORT).show(); 
	}
	
    /**
     * 获取应用程序版本编号
     * @param context
     * @return
     */
    public static int getVersinoCode(Context context){
        int intVersioinCode=0;
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            intVersioinCode=info.versionCode;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return intVersioinCode;
    }
    
    
    /**
     * 获取应用程序版本号
     * @param context
     * @return
     */
    public static String getVersionName(Context context){
        String strVersionName=null;
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            strVersionName=info.versionName;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return strVersionName;
    }
    
    public static ProgressDialog progressWindow(Context context, int resourceId) {
    	ProgressDialog dialog = new ProgressDialog(context); 
    	if (resourceId!=0) {
    		dialog.setMessage(context.getString(resourceId));
    	}
    	return dialog;
    	/*
		LayoutInflater li = LayoutInflater.from(context);  
		View view = li.inflate(R.layout.m_waiting, null);  
		   
		AlertDialog.Builder builder = new AlertDialog.Builder(context);  
		//builder.setTitle(context.getString(resourceId));  
		//builder.setIcon(R.drawable.icon);  
		//之前inflate的View 放到dialog中  
		builder.setView(view);  
 
		builder.create();  
		return builder;
		*/
    }
    
    @SuppressLint("NewApi") 
    public static ProgressDialog progressWindow(Context context, String message) {
    	ProgressDialog dialog = new ProgressDialog(context); 
    	if (!message.isEmpty()) {
    		dialog.setMessage(message);
    	}
    	return dialog;
    	/*
		LayoutInflater li = LayoutInflater.from(context);  
		View view = li.inflate(R.layout.m_waiting, null);  
		   
		AlertDialog.Builder builder = new AlertDialog.Builder(context);  
		//builder.setTitle(context.getString(resourceId));  
		//builder.setIcon(R.drawable.icon);  
		//之前inflate的View 放到dialog中  
		builder.setView(view);  
 
		builder.create();  
		return builder;
		*/
    }
    
    public static boolean in_array(String[] haystack, String needle) {
        for(int i=0;i<haystack.length;i++) {
            if(haystack[i].equals(needle)) {
                return true;
            }
        }
        return false;
    }
    public static String implode(String[] ary, String delim) {
        String out = "";
        for(int i=0; i<ary.length; i++) {
        	//if (ary[i]!=null) {
	            out += ary[i];
	            if(i<(ary.length-1)) { out += delim; }

        	//}
        }
        return out;
    }
    
    public static String getHostFromUrl(String detailUrl) {
		URL du;
		try {
			du = new URL(detailUrl);
			//int p = du.getPort();
			return du.getHost();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		}

    }
    @SuppressWarnings("rawtypes")
	public static String implode(ArrayList ary, String delim) {
        String out = "";
        for(int i=0; i<ary.size(); i++) {
        	//if (ary.get(i)!=null) {
	            out += (String)ary.get(i);
	            if(i!=ary.size()-1) { out += delim; }

        	//}
        }
        return out;
    }
    public static boolean isEmulator(Context context) {
    	if (BASE_CONF.DEBUG) Log.d(TAG, "isEmulator");
    	TelephonyManager tm = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE); 
    	if (tm != null) {
    		//String number = tm.getLine1Number();
    		//Log.d(TAG, "getLine1Number:"+number);
    		
    		//String strSubId = tm.getSubscriberId();
    		//Log.d(TAG, "strSubId:"+strSubId);
    		//tm.getCellLocation();
    		@SuppressLint("MissingPermission") String deviceid = tm.getDeviceId();
    		if (BASE_CONF.DEBUG) Log.d(TAG, "deviceid:"+deviceid);
    		
    		if (deviceid==null || deviceid.equals("000000000000000")) {
    			return true;
    		}
    	} 
    	//String strSubId = tm.getSubscriberId(); 
    	return false;
    }
    public static boolean check3GNetworkInfo(Context context) {
    	try {
	        ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
	        //mobile 3G Data Network
	        State mobile = conMan.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState();
	        
	        return (mobile==State.CONNECTED||mobile==State.CONNECTING);
    	} catch (Exception e) {
    		return false;
    	}
    }
    public static boolean checkWifyNetworkInfo(Context context) {
    	try {
	        ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
	        //wifi
	        State wifi = conMan.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();
	        return (wifi==State.CONNECTED||wifi==State.CONNECTING);
    	} catch (Exception e) {
    		return true;
    	}
    }
    
    public static boolean netCheckin(Context context) {
        try {
            ConnectivityManager nInfo = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            nInfo.getActiveNetworkInfo().isConnectedOrConnecting();

            if (BASE_CONF.DEBUG)  Log.d(TAG, "Net avail:"
                    + nInfo.getActiveNetworkInfo().isConnectedOrConnecting());

            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            	if (BASE_CONF.DEBUG) Log.d(TAG, "Network available:true");
                return true;
            } else {
            	if (BASE_CONF.DEBUG) Log.d(TAG, "Network available:false");
                return false;
            }

        } catch (Exception e) {
            Log.d(TAG, "Network available:false");

            return false;
        }
    }
    
    public static boolean isRunning(Context c,String serviceName)  {
    	
        final ActivityManager activityManager = (ActivityManager)c.getSystemService(Context.ACTIVITY_SERVICE);
        final List<RunningServiceInfo> services = activityManager.getRunningServices(Integer.MAX_VALUE);

        for (RunningServiceInfo runningServiceInfo : services) {
            if (runningServiceInfo.service.getClassName().equals(serviceName)){
                return true;
            }
        }
        return false;
    	
/*    	ActivityManager myAM=(ActivityManager)c.getSystemService(Context.ACTIVITY_SERVICE); 
     
    	ArrayList<ActivityManager.RunningServiceInfo> runningServices = (ArrayList<ActivityManager.RunningServiceInfo>) myAM.getRunningServices(60);
    	//获取最多60个当前正在运行的服务，放进ArrList里,以现在手机的处理能力，要是超过40个服务，估计已经卡死，所以不用考虑超过40个该怎么办
    	for(int i = 0 ; i<runningServices.size();i++)//循环枚举对比
    	{
    		if(runningServices.get(i).service.getClassName().toString().equals(serviceName))
    		{
    	    	Log.d(TAG, "check if running:"+serviceName+"-yes");

    			return true;
    		}
    	}
    	Log.d(TAG, "check if running:"+serviceName+"-no");

    	return false;*/
    }
	public static String getMimeType(byte[] bytes) {
		String suffix = getFileSuffix(bytes);
		String mimeType;

		if ("JPG".equals(suffix)) {
			mimeType = "image/jpeg";
		} else if ("GIF".equals(suffix)) {
			mimeType = "image/gif";
		} else if ("PNG".equals(suffix)) {
			mimeType = "image/png";
		} else if ("BMP".equals(suffix)) {
			mimeType = "image/bmp";
		}else {
			mimeType = "application/octet-stream";
		}

		return mimeType;
	}
	 
	public static String getFileSuffix(byte[] bytes) {
		if (bytes == null || bytes.length < 10) {
			return null;
		}

		if (bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F') {
			return "GIF";
		} else if (bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') {
			return "PNG";
		} else if (bytes[6] == 'J' && bytes[7] == 'F' && bytes[8] == 'I' && bytes[9] == 'F') {
			return "JPG";
		} else if (bytes[0] == 'B' && bytes[1] == 'M') {
			return "BMP";
		} else {
			return null;
		}
	}
	
	public static boolean checkIfLogin(Context context) {
		if (NStorage.getSP(context, "user.uid").equals("") || NStorage.getSP(context, "user.token").equals("")) {
    		//PassingerApp.logOut(getApplicationContext());
    		//Intent intent13 = new Intent(this, ULoginAct.class);
    		//startActivity(intent13);
    		return false;
		}
		return true;
	}
	
	public static void openGPS(Context context) {
		Log.d(TAG, "openGPS");
		LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

		if (!locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
	        Intent gpsIntent = new Intent();
	        gpsIntent.setClassName("com.android.settings",
	                        "com.android.settings.widget.SettingsAppWidgetProvider");
	        gpsIntent.addCategory("android.intent.category.ALTERNATIVE");
	        gpsIntent.setData(Uri.parse("custom:3"));
	        try {
	        	/*AlarmManager alarms = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
	        	int alarmType = AlarmManager.ELAPSED_REALTIME_WAKEUP;
	        	long timeOrLengthofWait = 10000;
	        	String ALARM_ACTION = "ALARM_ACTION";
	        	Intent intentToFire = new Intent(ALARM_ACTION);
	        	alarms.set(alarmType, timeOrLengthofWait, gpsIntent);*/


	        	PendingIntent.getBroadcast(context, 0, gpsIntent, 0).send();
	        	
	        } catch (CanceledException e) {
	    		Log.d(TAG, "openGPS exception:"+e.getMessage());

	            e.printStackTrace();
	        }

		}
	}
	
	public static boolean isExternalStorageExists() {
		boolean exists = Environment.getExternalStorageState().equals(
				android.os.Environment.MEDIA_MOUNTED);
		if (exists)
			return true;
		else
			return false;
	}
	
	public static void secSet(Context context, String key, String val) {
        //long now = System.currentTimeMillis();
        FileOutputStream fos = null;
        try {
            DataOutputStream dos = new DataOutputStream((fos = context.openFileOutput(key + ".mits", android.content.Context.MODE_PRIVATE)));
            dos.writeUTF(val);
        } catch (FileNotFoundException e1) {
            //e1.printStackTrace();
        } catch (IOException e1) {
            //e1.printStackTrace();
        } finally {
            if (fos != null)
                try {
                    fos.close();
                } catch (IOException ignored) {
                }
        }
    }
	
	@SuppressWarnings("unused")
	public static String secGet(Context context, String key) {
        FileInputStream fis = null;
        try {
            DataInputStream dis = new DataInputStream((fis = context.openFileInput(key + ".mits")));

			String val = dis.readUTF();
			return val;
        } catch (FileNotFoundException e) {
        	if (BASE_CONF.DEBUG) Log.d(TAG, "secGet file not found");
			//e.printStackTrace();

        	return "";
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			return "";
		}

	}
	
	public static int[] getScreenWH(Context context) {
		WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		int width = wm.getDefaultDisplay().getWidth();//屏幕宽度
		int height = wm.getDefaultDisplay().getHeight();
		int[] wh = new int[2];
		wh[0] = width;
		wh[1] = height;
		return wh;
	}
	
	public static int getSDKVersionNumber() {
		int sdkVersion;
		try {
		     sdkVersion = Integer.valueOf(android.os.Build.VERSION.SDK);

		} catch (NumberFormatException e) {
		     sdkVersion = 0;

		   }

		   return sdkVersion;

		}
	
	public static String getSignString(Context context, String packgeName) {
		android.content.pm.Signature[] sigs;
		try {
			sigs = context.getPackageManager().getPackageInfo(packgeName, 64).signatures;			
			//Log.d(TAG, "sigs.len=" + sigs.length);
			//Log.d(TAG,sigs[0].toCharsString());
			return sigs[0].toCharsString();
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		}

	}
}
